package com.example.brick_breaker_ball.gameplay

import com.badlogic.gdx.math.Vector2
import com.example.brick_breaker_ball.campaign.VolleyStageDefinition

/** One reproducible shot from the real simulation, not a geometric approximation. */
data class VolleyStageShotEvidence(
    val turn: Int,
    val angleDegrees: Float,
    val objectiveBricksBefore: Int,
    val objectiveBricksAfter: Int,
    val objectiveHealthBefore: Long,
    val objectiveHealthAfter: Long,
    val guardsRemaining: Int,
    val bossVisible: Boolean,
    val bossDamaged: Boolean,
    val terminalPhase: VolleyPhase
)

data class VolleyStageAccessResult(
    val stageId: Int,
    val seed: Long,
    val playerLevel: Int,
    val outcome: String,
    val shots: List<VolleyStageShotEvidence>,
    val remainingObjectives: Int,
    val remainingGuards: Int,
    val bossReached: Boolean
) {
    val isWinCertificate: Boolean get() = outcome == "WIN"
}

/**
 * Bounded, non-production playtest. Branches each ready-to-aim snapshot into
 * candidate angles and keeps the strongest completed volley. Uses the same
 * physics, collisions, shield rules and victory condition as the shipped game.
 *
 * A win is reproducible evidence for the selected seed and player loadout.
 * A timeout or failure is NOT proof that a stage is impossible; angles, ball
 * collection and tactical choices outside this limited search remain untested.
 */
object VolleyStageAccessProbe {
    private val defaultAngles = floatArrayOf(
        -72f, -60f, -48f, -36f, -24f, -12f, 0f, 12f, 24f, 36f, 48f, 60f, 72f
    )

    fun search(
        stage: VolleyStageDefinition,
        seed: Long = stage.levelId.toLong() * 7919L,
        playerLevel: Int = 1,
        maxTurns: Int = 24,
        frameDelta: Float = .05f,
        angles: FloatArray = defaultAngles
    ): VolleyStageAccessResult {
        require(maxTurns in 1..60)
        require(frameDelta in .01f..05f)
        require(angles.isNotEmpty() && angles.size <= 25 && angles.all { it in -85f..85f })

        fun newSession() = VolleySession(
            stage = stage,
            world = stage.world,
            seed = seed,
            playerLevel = playerLevel,
            combatRunId = "stage-access-${stage.levelId}-$seed"
        )

        var session = newSession()
        val history = mutableListOf<VolleyStageShotEvidence>()
        var reachedBoss = false
        var stalled = false

        while (!session.levelComplete && !session.gameOver && history.size < maxTurns) {
            val snapshot = session.snapshot()
            val beforeCount = remainingCount(session)
            val beforeHealth = remainingHealth(session)
            val beforeBossHealth = bossHealth(session)
            var best: VolleySession? = null
            var bestAngle = 0f
            var bestScore = Long.MIN_VALUE

            for (angle in angles) {
                val candidate = newSession()
                candidate.restore(snapshot)
                if (!candidate.startVolley(Vector2(0f, 1f).rotateDeg(angle))) continue

                var completed = advanceShot(candidate, frameDelta)
                if (!completed) {
                    candidate.recall()
                    completed = advanceShot(candidate, frameDelta, maxFrames = 160)
                }
                if (!completed) continue

                // Prefer verified clears, then damage and removal of objectives.
                // Avoid runs that breach the danger line even with high damage.
                val score = when {
                    candidate.levelComplete -> Long.MAX_VALUE
                    candidate.gameOver -> Long.MIN_VALUE + 1
                    else -> (beforeCount - remainingCount(candidate)).toLong() * 1_000_000L +
                        (beforeHealth - remainingHealth(candidate)).coerceAtLeast(0L) * 100L +
                        (beforeBossHealth - bossHealth(candidate)).coerceAtLeast(0L) * 100L
                }
                if (score > bestScore) {
                    best = candidate
                    bestAngle = angle
                    bestScore = score
                }
                if (candidate.levelComplete) break
            }

            val selected = best
            if (selected == null) {
                stalled = true
                break
            }
            reachedBoss = reachedBoss || bossHealth(selected) < beforeBossHealth
            history += VolleyStageShotEvidence(
                turn = snapshot.turn,
                angleDegrees = bestAngle,
                objectiveBricksBefore = beforeCount,
                objectiveBricksAfter = remainingCount(selected),
                objectiveHealthBefore = beforeHealth,
                objectiveHealthAfter = remainingHealth(selected),
                guardsRemaining = remainingGuards(selected),
                bossVisible = selected.bricks.any {
                    it.health > 0 && it.type == BrickType.BOSS_CORE &&
                        it.bounds.y < selected.boardTop && it.bounds.y + it.bounds.height > VolleySession.LAUNCH_Y
                },
                bossDamaged = bossHealth(selected) < beforeBossHealth,
                terminalPhase = selected.phase
            )
            session = selected
        }
        return VolleyStageAccessResult(
            stageId = stage.levelId,
            seed = seed,
            playerLevel = playerLevel,
            outcome = when {
                session.levelComplete -> "WIN"
                session.gameOver -> "LOSS"
                stalled -> "SHOT_TIMEOUT"
                else -> "TURN_LIMIT"
            },
            shots = history,
            remainingObjectives = remainingCount(session),
            remainingGuards = remainingGuards(session),
            bossReached = reachedBoss
        )
    }

    private fun advanceShot(session: VolleySession, frameDelta: Float, maxFrames: Int = 1_600): Boolean {
        repeat(maxFrames) {
            session.advance(frameDelta)
            session.drainEvents()
            if (session.phase == VolleyPhase.READY_TO_AIM || session.levelComplete || session.gameOver) return true
        }
        return false
    }

    private fun remainingCount(session: VolleySession): Int =
        session.bricks.count { it.countsForClear && it.health > 0 }

    private fun remainingHealth(session: VolleySession): Long =
        session.bricks.asSequence().filter { it.countsForClear && it.health > 0 }
            .sumOf { it.health.toLong() }

    private fun remainingGuards(session: VolleySession): Int =
        session.bricks.count { it.volleyShieldLock && it.health > 0 }

    private fun bossHealth(session: VolleySession): Long =
        session.bricks.asSequence().filter { it.type == BrickType.BOSS_CORE && it.health > 0 }
            .sumOf { it.health.toLong() }
}
