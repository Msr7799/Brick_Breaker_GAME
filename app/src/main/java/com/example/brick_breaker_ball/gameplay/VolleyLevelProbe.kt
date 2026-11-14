/*
 * الوظيفة: قياس تجريبي محدود للمراحل المولدة باستخدام محاكاة Volley الحقيقية.
 * الحسابات: جولات بزوايا ثابتة؛ النتيجة دليل على الضربات أو الفوز في التجارب فقط.
 */
package com.example.brick_breaker_ball.gameplay

import com.badlogic.gdx.math.Vector2
import com.example.brick_breaker_ball.campaign.VolleyStageDefinition

data class VolleyProbeResult(
    val trials: Int,
    val wins: Int,
    val losses: Int,
    val timeouts: Int,
    val turns: Int,
    val damage: Int,
    val remainingTargets: Int
)

object VolleyLevelProbe {
    fun measure(stage: VolleyStageDefinition, trials: Int = 3, maxTurns: Int = 4): VolleyProbeResult {
        require(trials in 1..10 && maxTurns in 1..20)
        var wins = 0
        var losses = 0
        var timeouts = 0
        var turns = 0
        var damage = 0
        var remaining = 0
        val angles = floatArrayOf(-24f, 0f, 24f, -12f, 12f)
        repeat(trials) { trial ->
            val session = VolleySession(stage, stage.world, seed = stage.levelId * 7919L + trial)
            var before = remainingHealth(session)
            var timedOut = false
            var trialTurns = 0
            while (!session.levelComplete && !session.gameOver && trialTurns < maxTurns) {
                val direction = Vector2(0f, 1f).rotateDeg(angles[(trial + session.turn - 1) % angles.size])
                if (!session.startVolley(direction)) {
                    timedOut = true
                    break
                }
                turns++
                trialTurns++
                var ended = false
                for (frame in 0 until 1_600) {
                    session.advance(.05f)
                    session.drainEvents()
                    if (session.phase == VolleyPhase.READY_TO_AIM || session.levelComplete || session.gameOver) {
                        ended = true
                        break
                    }
                }
                if (!ended) {
                    session.recall()
                    for (frame in 0 until 160) {
                        session.advance(.05f)
                        session.drainEvents()
                        if (session.phase == VolleyPhase.READY_TO_AIM || session.levelComplete || session.gameOver) {
                            ended = true
                            break
                        }
                    }
                }
                val after = remainingHealth(session)
                damage += (before - after).coerceAtLeast(0)
                before = after
                if (!ended) {
                    timedOut = true
                    break
                }
            }
            if (session.levelComplete) wins++
            if (session.gameOver) losses++
            if (timedOut) timeouts++
            remaining += session.bricks.count { it.countsForClear && it.health > 0 }
        }
        return VolleyProbeResult(trials, wins, losses, timeouts, turns, damage, remaining)
    }

    private fun remainingHealth(session: VolleySession): Int = session.bricks
        .filter { it.countsForClear && it.health > 0 }.sumOf { it.health }
}
