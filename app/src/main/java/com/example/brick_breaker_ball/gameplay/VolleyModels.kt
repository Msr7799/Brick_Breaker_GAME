/*
 * المسار: app/src/main/java/com/example/brick_breaker_ball/gameplay/VolleyModels.kt
 * الوظيفة: حالات وجسيمات وسجلات جولة Volley.
 * الحسابات: مواضع وسرعات الكرات وحدود الأهداف.
 */

package com.example.brick_breaker_ball.gameplay

import com.badlogic.gdx.math.Vector2
import java.io.Serializable
import com.example.brick_breaker_ball.campaign.VolleyDifficulty

enum class VolleyPhase {
    READY_TO_AIM,
    AIMING,
    FIRING_SEQUENCE,
    BALLS_IN_FLIGHT,
    RESOLVING_EFFECTS,
    BOARD_DESCENT,
    DANGER_CHECK,
    LEVEL_COMPLETE,
    GAME_OVER,
    PAUSED
}

enum class VolleyModifier {
    FIRE,
    MEGA,
    PIERCING,
    SLOW,
    DUAL_LAUNCH,
    WIDE_LAUNCH,
    MAGNETIC_RETURN,
    STICKY_RETURN,
    LASER,
    TIMED_BOMBS,
    FAST,
    SHRINK_BALL,
    SHRINK_GUIDE,
    GHOST
}

enum class VolleyBonusNodeType { BALL, CROWN, GEMS, SHIELD, MYSTERY, STAR, SPEED, COINS }

enum class VolleyDamageSource { BALL, OPENING_BREAKER, FIRE_SPLASH, EXPLOSION, LASER, CHAIN, BOSS }
enum class VolleyHazardKind { SPIKED, DEATH, LASER }
enum class VolleyVfxKind { SPARK, SMOKE, BEAM, FIRE, PORTAL, HAZARD }

/** Logical silhouette for a Volley brick. Triangle edges participate in swept collision. */
enum class VolleyPieceShape { RECTANGLE, TRIANGLE_NW, TRIANGLE_NE, TRIANGLE_SW, TRIANGLE_SE }

data class VolleyDamageEvent(
    val eventId: Long,
    val source: VolleyDamageSource,
    val amount: Int,
    val targetBrickId: Int,
    val sourceBrickId: Int? = null,
    val sourceBallId: Int? = null,
    val visitedBrickIds: MutableSet<Int> = linkedSetOf()
)

enum class VolleyBrickVisualFamily {
    STANDARD_GRAY,
    STANDARD_BROWN,
    STANDARD_ORANGE,
    STANDARD_RED,
    STANDARD_GOLD,
    STANDARD_GREEN,
    STANDARD_CYAN,
    STANDARD_BLUE,
    STANDARD_PURPLE,
    STANDARD_PINK,
    ROUGH,
    REGENERATING,
    CARRIER,
    ARMORED,
    ICE,
    EXPLOSIVE,
    SWITCH,
    GHOST,
    LOCK,
    SHIELD_LOCK,
    CHAIN,
    GATE,
    SPIKE,
    PORTAL,
    BOSS
}

data class VolleyBallState(
    val id: Int,
    val position: Vector2,
    val velocity: Vector2,
    var radius: Float,
    var lastBrickId: Int = -1,
    var lastRailId: Int = -1,
    var portalCooldown: Float = 0f,
    var alive: Boolean = true
)

data class VolleyRailState(
    val id: Int,
    var x1: Float,
    var y1: Float,
    var x2: Float,
    var y2: Float,
    val thickness: Float,
    val fixed: Boolean
)

data class VolleyBonusNodeState(
    val id: Int,
    val position: Vector2,
    val amount: Int,
    var active: Boolean = true,
    val type: VolleyBonusNodeType = VolleyBonusNodeType.BALL,
    val scoreValue: Int = 0
)

typealias VolleyPickupState = VolleyBonusNodeState

sealed interface VolleyEvent {
    data class CombatExperience(val runId: String, val killSequence: Int,
                                val enemyLevel: Int, val difficulty: VolleyDifficulty,
                                val playerLevel: Int) : VolleyEvent
    data class Feedback(val text: String, val duration: Float = .8f) : VolleyEvent
    data class Impact(val x: Float, val y: Float, val destroyed: Boolean) : VolleyEvent
    data class BrickHit(val brickId: Int, val damage: Int) : VolleyEvent
    data class BrickBroken(val brickId: Int) : VolleyEvent
    data class PickupCollected(val pickupId: Int, val amount: Int) : VolleyEvent
    data class BonusNodeCollected(val nodeId: Int, val type: VolleyBonusNodeType, val amount: Int) : VolleyEvent
    data class WallDescending(val distance: Float) : VolleyEvent
    data object RescueUsed : VolleyEvent
    data object VolleyComplete : VolleyEvent
    data class Sound(val name: String, val pitch: Float = 1f) : VolleyEvent
    data class Haptic(val milliseconds: Int) : VolleyEvent
    data class Debris(val x: Float, val y: Float) : VolleyEvent
    data class Vfx(val kind: VolleyVfxKind, val x: Float, val y: Float) : VolleyEvent
    data class GrantRandomItem(val sourceId: Int, val turn: Int, val naturalDrop: Boolean = false,
                               val orbRank: Int = 0) : VolleyEvent

    /** Previous gems nodes from paused legacy sessions are converted into coins. */
    data class GrantGems(val sourceId: Int, val amount: Int) : VolleyEvent
    data class GrantCoins(val sourceId: Int, val amount: Int) : VolleyEvent
}

/** Short, bounded pitch ladder for rapid Volley brick impacts. */
class VolleyImpactPitch(
    private val resetAfterSeconds: Float = .48f,
    private val basePitch: Float = 1f,
    private val pitchStep: Float = .035f,
    private val maxPitch: Float = 1.35f
) {
    private var silenceSeconds = resetAfterSeconds
    private var streak = 0

    fun update(delta: Float) {
        silenceSeconds += delta.coerceIn(0f, .1f)
        if (silenceSeconds >= resetAfterSeconds) streak = 0
    }

    fun nextPitch(): Float {
        if (silenceSeconds >= resetAfterSeconds) streak = 0
        val pitch = (basePitch + streak * pitchStep).coerceAtMost(maxPitch)
        streak++
        silenceSeconds = 0f
        return pitch
    }
}

data class VolleySessionSnapshot(
    val version: Int = 2,
    val phase: VolleyPhase,
    val phaseBeforePause: VolleyPhase,
    val launcherX: Float,
    val ballCount: Int,
    val pendingLaunches: Int,
    val launchedThisVolley: Int,
    val firstReturnX: Float?,
    val turn: Int,
    val score: Int,
    val rescueCharges: Int,
    val queuedModifiers: Set<VolleyModifier>,
    val activeModifiers: Set<VolleyModifier>,
    val pendingReturnX: Float?,
    val earnedBallsThisTurn: Int,
    val launchCooldown: Float,
    val descentRemaining: Float,
    val combo: Int,
    val abilityHitCounter: Int,
    val volleyBreakCounter: Int,
    val laserHitCounter: Int,
    val laserGauge: Int,
    val timedBombHitsRemaining: Int,
    val bonusStarProgress: Int,
    val nextBallId: Int,
    val loopAccumulator: Float,
    val balls: List<VolleyBallSnapshot>,
    val bricks: List<VolleyBrickSnapshot>,
    val rails: List<VolleyRailSnapshot>,
    val pickups: List<VolleyPickupSnapshot>,
    /** Original inventory item for a queued effect; null in saves written before this feature. */
    val queuedItemSources: Map<VolleyModifier, PowerUpType>? = null,
    val orbPoison: Map<Int, VolleyPoisonSnapshot>? = null,
    val orbSecondWindUsed: Boolean = false,
    val orbMultishotUsed: Boolean = false,
    val bonusDamageFlat: Int = 0,
    val protectedBricks: Map<Int, Int>? = null,
    val frozenBricks: Set<Int>? = null,
    val spikeCooldowns: Map<Int, Float>? = null,
    val vortexBalls: Map<Int, Float>? = null,
    val vortexClock: Float = 0f,
    /** Null means a save from before equipped Orbs were captured per run. */
    val equippedOrbIds: List<String>? = null,
    /** Fire status belongs to individual balls and survives a paused Volley. */
    val ignitedBallIds: Set<Int>? = null,
    val orbWrath: List<VolleyWrathSnapshot>? = null,
    /** Copies earned earlier in this paused run, for the eventual results screen. */
    val earnedOrbUids: List<String>? = null,
    /** Natural and carrier drop receipts use this stable ID across pause/resume. */
    val orbRunId: String? = null,
    /** The first ball in a charged third volley retains its one-brick charge across pause/resume. */
    val openingBreakerBallId: Int? = null,
    val shotgunEnabled: Boolean? = null,
    /** Saved bonuses let a newly selected ball or Orb loadout take effect when a paused run resumes. */
    val ballStartingBonus: Int? = null,
    val orbStartingBonus: Int? = null,
    /** Null identifies paused encounters from before the boss was moved behind its guards. */
    val bossGuardLayoutVersion: Int? = null,
    val burnChargesLeft: Map<Int, Int>? = null,
    val rotatingAngles: Map<Int, Float>? = null,
    val rotatingStartAngles: Map<Int, Float>? = null,
    val rotatingTargetAngles: Map<Int, Float>? = null,
    val torchMotion: Map<Int, VolleyTorchMotionSnapshot>? = null,
    /** Null keeps previously serialized rounds on their original combat rules. */
    val sourceCombatRulesVersion: Int? = null,
    val sourceCombatPlayerLevel: Int? = null,
    val sourceRemainingHealth: Map<Int, Double>? = null,
    val combatKillSequence: Int? = null,
    /** Null in old v2 saves; absence never opts an existing run into new rules. */
    val evolution: VolleyEvolutionSnapshot? = null,
    val randomState: Long? = null
) : Serializable {
    companion object {
        // Preserve paused runs serialized by version 2 before queuedItemSources was added.
        private const val serialVersionUID = 5044936872263027514L
    }
}

data class VolleyTorchMotionSnapshot(val offset: Float = 0f, val from: Float = 0f,
                                    val target: Float = 0f, val progress: Float = 1f,
                                    val idle: Float = 0f, val towardEnd: Boolean = true) : Serializable {
    companion object { private const val serialVersionUID = 1L }
}

data class VolleyPoisonSnapshot(val damage: Int, val interval: Float, val remaining: Float, val ticks: Int) : Serializable
data class VolleyWrathSnapshot(val x: Float, val y: Float, val damage: Int,
                               val remaining: Float, val cooldown: Float, val interval: Float) : Serializable

data class VolleyBallSnapshot(
    val id: Int,
    val x: Float,
    val y: Float,
    val velocityX: Float,
    val velocityY: Float,
    val radius: Float,
    val lastBrickId: Int,
    val lastRailId: Int,
    val portalCooldown: Float,
    val alive: Boolean
) : Serializable

data class VolleyBrickSnapshot(
    val id: Int,
    val x: Float,
    val y: Float,
    val width: Float,
    val height: Float,
    val type: BrickType,
    val health: Int,
    val hue: Float,
    val groupId: Int,
    val locked: Boolean,
    val ghostVisible: Boolean,
    val initialHealth: Int,
    val gridRow: Int,
    val gridCol: Int,
    val countsForClear: Boolean,
    val movementDirection: Int,
    val rewardGranted: Boolean,
    val visualFamily: VolleyBrickVisualFamily?,
    val consumeOnHit: Boolean,
    val shieldLock: Boolean,
    val hazardKind: VolleyHazardKind?
) : Serializable

data class VolleyRailSnapshot(
    val id: Int,
    val x1: Float,
    val y1: Float,
    val x2: Float,
    val y2: Float,
    val thickness: Float,
    val fixed: Boolean
) : Serializable

data class VolleyPickupSnapshot(
    val id: Int,
    val x: Float,
    val y: Float,
    val amount: Int,
    val active: Boolean,
    val type: VolleyBonusNodeType,
    val scoreValue: Int
) : Serializable
