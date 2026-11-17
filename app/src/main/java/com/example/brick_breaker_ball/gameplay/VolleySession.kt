/*
 * المسار: app/src/main/java/com/example/brick_breaker_ball/gameplay/VolleySession.kt
 * الوظيفة: محاكاة جولة Volley وتصادماتها وتأثيراتها.
 * الحسابات: تقدم الزمن والسرعة والارتداد والضرر والاصطدام.
 */

package com.example.brick_breaker_ball.gameplay

import com.badlogic.gdx.math.Rectangle
import com.badlogic.gdx.math.Vector2
import com.example.brick_breaker_ball.assets.VolleyAssetCatalog
import com.example.brick_breaker_ball.campaign.VolleyCarrierReward
import com.example.brick_breaker_ball.campaign.VolleyAuthoredBrickSeed
import com.example.brick_breaker_ball.campaign.VolleyStageDefinition
import com.example.brick_breaker_ball.campaign.VolleyDifficulty
import com.example.brick_breaker_ball.campaign.VolleySourceHealth
import com.example.brick_breaker_ball.cosmetics.BallAbilityCatalog
import com.example.brick_breaker_ball.cosmetics.BallAbilityKind
import com.example.brick_breaker_ball.cosmetics.BallAbilityProfile
import com.example.brick_breaker_ball.economy.SHOP_ELIGIBLE_TYPES
import java.util.ArrayDeque
import java.util.Random
import kotlin.math.abs
import kotlin.math.ceil
import kotlin.math.max
import kotlin.math.min
import kotlin.math.roundToInt

/** Checks the visible part of a split bolt's straight path against the actual brick shape. */
internal fun volleySplitPathCrossesBrick(start: Vector2, end: Vector2, brick: Brick): Boolean =
    sweptCircleVsVolleyBrick(start, Vector2(end).sub(start), 0f, brick) != null

/** Secondary Orb attacks may only select bricks that have entered the play area. */
internal fun volleyOrbTargetVisible(brick: Brick, boardTop: Float, returnY: Float): Boolean =
    brick.health > 0 && brick.type.breakable &&
        (brick.type != BrickType.GHOST || brick.ghostVisible) &&
        !(brick.type == BrickType.LOCKED && brick.locked) &&
        brick.bounds.y < boardTop && brick.bounds.y + brick.bounds.height > returnY

/** Sweeps against the three visible edges of a triangular piece, never its transparent half. */
internal fun sweptCircleVsVolleyBrick(
    origin: Vector2,
    delta: Vector2,
    radius: Float,
    brick: Brick
): CollisionResult? {
    if (brick.volleyPieceShape == VolleyPieceShape.RECTANGLE) {
        return SweptCollision.circleVsAabb(origin, delta, radius, brick.bounds, brick.id)
    }
    val left = brick.bounds.x
    val right = brick.bounds.x + brick.bounds.width
    val bottom = brick.bounds.y
    val top = brick.bounds.y + brick.bounds.height
    val vertices = when (brick.volleyPieceShape) {
        VolleyPieceShape.TRIANGLE_NW -> arrayOf(left to top, right to top, left to bottom)
        VolleyPieceShape.TRIANGLE_NE -> arrayOf(left to top, right to top, right to bottom)
        VolleyPieceShape.TRIANGLE_SW -> arrayOf(left to top, left to bottom, right to bottom)
        VolleyPieceShape.TRIANGLE_SE -> arrayOf(right to top, left to bottom, right to bottom)
        VolleyPieceShape.RECTANGLE -> error("Handled above")
    }
    var best: CollisionResult? = null
    repeat(vertices.size) { index ->
        val start = vertices[index]
        val end = vertices[(index + 1) % vertices.size]
        val candidate = SweptCollision.circleVsSegment(
            origin,
            delta,
            radius,
            start.first,
            start.second,
            end.first,
            end.second,
            0f,
            brick.id
        )
        if (candidate != null && (best == null || candidate.time < best!!.time)) best = candidate
    }
    return best
}

/** Rotate into the bar's local frame for a continuous sweep, then restore its world normal. */
internal fun sweptCircleVsRotatedVolleyBrick(origin: Vector2, delta: Vector2, radius: Float,
                                            brick: Brick, angle: Float): CollisionResult? {
    if (angle == 0f) return sweptCircleVsVolleyBrick(origin, delta, radius, brick)
    val cx = brick.bounds.x + brick.bounds.width / 2f
    val cy = brick.bounds.y + brick.bounds.height / 2f
    val localOrigin = Vector2(origin).sub(cx, cy).rotateDeg(-angle).add(cx, cy)
    val hit = sweptCircleVsVolleyBrick(localOrigin, Vector2(delta).rotateDeg(-angle), radius, brick) ?: return null
    val normal = Vector2(hit.normalX, hit.normalY).rotateDeg(angle)
    return hit.copy(normalX = normal.x, normalY = normal.y)
}

/** Canonical simulation/state owner for Volley mode. Rendering and platform effects stay outside. */
class VolleySession(
    val stage: VolleyStageDefinition,
    private val world: Int,
    private val ballAbility: BallAbilityProfile = BallAbilityCatalog.default,
    seed: Long = stage.levelId.toLong() * 31L + world,
    private val reduceMotion: Boolean = false,
    private val difficulty: VolleyDifficulty = VolleyDifficulty.NORMAL,
    private val equippedOrb: VolleyOrbDefinition? = null,
    equippedOrbs: List<VolleyOrbDefinition> = listOfNotNull(equippedOrb),
    shotgunEnabled: Boolean = false,
    playerLevel: Int = 1,
    referenceRules: Boolean = stage.sourceScene != null,
    combatRunId: String = java.util.UUID.randomUUID().toString()
) {
    private var referenceRulesEnabled = referenceRules && stage.sourceScene != null
    private var sourceCombatPlayerLevel = playerLevel.coerceAtLeast(1)
    private var sourceCombatRunId = combatRunId
    private var combatKillSequence = 0
    private val sourceRemainingHealth = mutableMapOf<Int, Double>()
    val usesReferenceCombat: Boolean get() = referenceRulesEnabled
    private val maxBallCount: Int get() = if (referenceRulesEnabled) Int.MAX_VALUE else 160
    private val referenceBaseDamage: Int get() = VolleyReferenceCombat.baseDamage(sourceCombatPlayerLevel, activeOrbs)
    val currentHitDamage: Int get() = if (referenceRulesEnabled) {
        (referenceBaseDamage.toLong() + bonusDamageFlat + if (VolleyModifier.MEGA in activeModifiers) 1 else 0)
            .coerceIn(1L, Int.MAX_VALUE.toLong()).toInt()
    } else ballAbility.hitDamageForVolley(ballCount) + bonusDamageFlat +
        if (VolleyModifier.MEGA in activeModifiers) 1 else 0
    companion object {
        const val BOARD_LEFT = 0f
        const val BOARD_RIGHT = 900f
        const val SOURCE_BRICK_PITCH = 64f
        // RPG uses a 720-wide viewport. Keep its 64-unit tile at the same screen-width ratio.
        const val SOURCE_VIEWPORT_WIDTH = 720f
        const val TARGET_SOURCE_BRICK_PITCH = SOURCE_BRICK_PITCH * (BOARD_RIGHT - BOARD_LEFT) / SOURCE_VIEWPORT_WIDTH
        const val SOURCE_LANE_LEFT = (BOARD_RIGHT - BOARD_LEFT - 11f * TARGET_SOURCE_BRICK_PITCH) / 2f
        const val SOURCE_WAVE_BOTTOM = 1230f
        private const val ORIGINAL_IMPORT_PITCH = (BOARD_RIGHT - BOARD_LEFT) / 11f
        private const val PREVIOUS_IMPORT_PITCH = SOURCE_BRICK_PITCH
        // Default ceiling for direct sessions; the game screen aligns it with the real viewport.
        const val BOARD_TOP = 1720f
        const val BRICK_START_TOP = BOARD_TOP - 21f

        // The numbered Volley brick crops are wide, not square. Matching their authored aspect
        // ratio gives the board the dense, readable rows used by ball-volley games instead of the
        // stretched square blocks that previously dominated the screen.
        const val BRICK_HEIGHT = 72f
        const val BRICK_GAP = 2f
        const val ROW_PITCH = BRICK_HEIGHT + BRICK_GAP

        /** Pivot of the authored cannon artwork. The ball leaves from the muzzle, not its centre. */
        const val CANNON_PIVOT_Y = 154f
        const val CANNON_BARREL_LENGTH = 280f
        const val LAUNCH_Y = CANNON_PIVOT_Y + CANNON_BARREL_LENGTH
        const val RETURN_Y = 118f
        const val DANGER_Y = 300f
        // Source collision circles are 13.1 units at 0.9 scale, or about 15 board units here.
        const val DEFAULT_BALL_RADIUS = 15f
        const val MEGA_BALL_RADIUS = 27f
        // A 1000-unit source ball speed follows the same 64-to-(900/11) scale as authored bricks.
        const val BALL_SPEED = 1278.4f
        const val BOSS_HP_MULTIPLIER = 1.90f
        const val MAX_STANDARD_BRICK_HP = 30
        const val BALL_STORM_BONUS = 30
        const val LAUNCH_INTERVAL = .1f
        const val DESCENT_SECONDS = .4f
        const val FAST_FORWARD_MULTIPLIER = 3
        const val LASER_CHARGE_HITS = 8
        const val BOMB_TRIGGER_HITS = 8
        const val MAX_REWARDED_REVIVES = 3
        const val REWARDED_REVIVE_ROWS = 4
        private const val MAX_COLLISIONS_PER_STEP = 6
        private const val COLLISION_NUDGE = .65f
        // A 31.1448-unit spell hitbox grows to scale 2; map it to the 11-column board.
        private const val TURN_SPELL_RADIUS = 31.1448f * 2f
        private val IMPORTED_HIT_COUNTER_KINDS = setOf("DYNAMITE", "SUICIDE", "BONUS_BALLS", "BONUS_DAMAGE", "OSB")
        private val ORB_DROP_MATERIALS = setOf("GRASS", "SAND", "ICE", "BLACK", "BURN",
            "PROTECTOR", "ICE_FREEZE", "SUICIDE", "DYNAMITE")
    }

    var boardTop: Float = BOARD_TOP
        private set

    /** Keeps the ceiling, ball collision and visible clipping on the same edge after a resize. */
    fun alignBoardTop(visibleTop: Float) {
        val shift = visibleTop - boardTop
        if (stage.sourceScene == null && shift != 0f) {
            // Editor and optional generated boards use top-relative grid coordinates.
            bricks.forEach { it.bounds.y += shift }
            rails.forEach {
                it.y1 += shift
                it.y2 += shift
            }
            pickups.forEach { it.position.y += shift }
        }
        boardTop = visibleTop
        applyInitialImportedFreeze()
        balls.forEach { ball ->
            val limit = boardTop - ball.radius
            if (ball.position.y > limit) {
                ball.position.y = limit
                if (ball.velocity.y > 0f) ball.velocity.y = -ball.velocity.y
            }
        }
    }

    val bricks = mutableListOf<Brick>()
    val balls = mutableListOf<VolleyBallState>()
    val rails = mutableListOf<VolleyRailState>()
    val pickups = mutableListOf<VolleyPickupState>()

    var phase: VolleyPhase = VolleyPhase.READY_TO_AIM
        private set
    var launcherX: Float = 450f
        private set
    var ballCount: Int = stage.startingBalls
        private set
    var pendingLaunches: Int = 0
        private set
    var launchedThisVolley: Int = 0
        private set
    var firstReturnX: Float? = null
        private set
    var turn: Int = 1
        private set
    var score: Int = 0
        private set
    var combo: Int = 0
        private set
    var rescueCharges: Int = 0
        private set
    var bonusStarProgress: Int = 0
        private set
    var laserGauge: Int = 0
        private set
    var rewardedRevivesUsed: Int = 0
        private set
    var fastForwardActive: Boolean = false
        private set

    val queuedModifiers: MutableSet<VolleyModifier> = linkedSetOf()
    val activeModifiers: MutableSet<VolleyModifier> = linkedSetOf()
    private val queuedItemSources: MutableMap<VolleyModifier, PowerUpType> = linkedMapOf()

    /** Only inventory-backed queued effects can be cancelled and refunded. */
    val cancellableModifiers: Set<VolleyModifier> get() = queuedItemSources.keys.toSet()

    val firing: Boolean get() = phase == VolleyPhase.FIRING_SEQUENCE || phase == VolleyPhase.BALLS_IN_FLIGHT
    val descending: Boolean get() = phase == VolleyPhase.BOARD_DESCENT
    val gameOver: Boolean get() = phase == VolleyPhase.GAME_OVER
    val levelComplete: Boolean get() = phase == VolleyPhase.LEVEL_COMPLETE
    val rewardedRevivesRemaining: Int
        get() = (MAX_REWARDED_REVIVES - rewardedRevivesUsed).coerceAtLeast(0)

    private val loop = FixedStepLoop(hz = 120, maxSteps = 18)
    private val random = Random(seed)
    private val activeOrbs = equippedOrbs.distinctBy { it.uid ?: "legacy:${it.family}" }.take(8)
    private fun orbFor(family: String): VolleyOrbDefinition? = activeOrbs.firstOrNull { it.family == family }
    private fun createOrbEffects() = VolleyOrbEffects(activeOrbs, random, stage.levelId,
        difficulty == VolleyDifficulty.EXTREME, combatBoost = if (referenceRulesEnabled) 1f else 1.5f,
        referenceRules = referenceRulesEnabled, playerLevel = sourceCombatPlayerLevel)
    private var orbEffects = createOrbEffects()
    val orbCoinRewardMultiplier: Float get() = orbEffects.rewardMultiplier()
    private val ignitedBallIds = mutableSetOf<Int>()
    fun isBallIgnited(id: Int): Boolean = id in ignitedBallIds
    private val burnChargesLeft = mutableMapOf<Int, Int>()
    fun isBurnDepleted(id: Int): Boolean = burnChargesLeft[id] == 0
    private val rotatingAngles = mutableMapOf<Int, Float>()
    private val rotatingStartAngles = mutableMapOf<Int, Float>()
    private val rotatingTargetAngles = mutableMapOf<Int, Float>()
    fun brickRotation(id: Int): Float = rotatingAngles[id] ?: 0f
    private val torchMotion = mutableMapOf<Int, VolleyTorchMotionSnapshot>()
    private var initialImportedFreezeApplied = false
    private val orbPoison = mutableMapOf<Int, VolleyPoisonSnapshot>()
    private var orbFeedbackCooldown = 0f
    private val orbWrath = mutableListOf<VolleyWrathSnapshot>()
    private var orbSecondWindUsed = false
    private var orbMultishotUsed = false
    private var bonusDamageFlat = 0
    private val protectedBricks = mutableMapOf<Int, Int>()
    private val frozenBricks = mutableSetOf<Int>()
    private val spikeCooldowns = mutableMapOf<Int, Float>()
    private val vortexBalls = mutableMapOf<Int, Float>()
    private var vortexClock = 0f
    private val events = mutableListOf<VolleyEvent>()
    private val launchDirection = Vector2(0f, 1f)
    private val launchSpawn = Vector2()
    private var shotgunEnabledForRun = !referenceRulesEnabled && shotgunEnabled && ballAbility.shotgunAvailable
    private var openingBreakerBallId: Int? = null
    private var pendingReturnX: Float? = null
    private var earnedBallsThisTurn = 0
    private var launchCooldown = 0f
    private var descentRemaining = 0f
    private var abilityHitCounter = 0
    private var volleyBreakCounter = 0
    private var laserHitCounter = 0
    private var timedBombHitsRemaining = 0
    private var nextBallId = 1
    private var phaseBeforePause = VolleyPhase.READY_TO_AIM
    private val damageQueue = ArrayDeque<VolleyDamageEvent>()
    private var processingDamageQueue = false

    /**
     * True only while [updateVolley] is iterating [balls]. Level completion can be triggered from
     * deep inside collision/damage callbacks, so structural mutations of [balls] must be deferred
     * until that iteration has finished.
     */
    private var processingBallUpdates = false
    private var nextDamageEventId = 1L

    init {
        resetBoardContent()
    }

    fun drainEvents(): List<VolleyEvent> = events.toList().also { events.clear() }

    fun snapshot(): VolleySessionSnapshot = VolleySessionSnapshot(
        phase = phase,
        phaseBeforePause = phaseBeforePause,
        launcherX = launcherX,
        ballCount = ballCount,
        pendingLaunches = pendingLaunches,
        launchedThisVolley = launchedThisVolley,
        firstReturnX = firstReturnX,
        turn = turn,
        score = score,
        rescueCharges = rescueCharges,
        queuedModifiers = queuedModifiers.toSet(),
        activeModifiers = activeModifiers.toSet(),
        queuedItemSources = queuedItemSources.toMap(),
        pendingReturnX = pendingReturnX,
        earnedBallsThisTurn = earnedBallsThisTurn,
        launchCooldown = launchCooldown,
        descentRemaining = descentRemaining,
        combo = combo,
        abilityHitCounter = abilityHitCounter,
        volleyBreakCounter = volleyBreakCounter,
        laserHitCounter = laserHitCounter,
        laserGauge = laserGauge,
        timedBombHitsRemaining = timedBombHitsRemaining,
        bonusStarProgress = bonusStarProgress,
        nextBallId = nextBallId,
        openingBreakerBallId = openingBreakerBallId,
        shotgunEnabled = shotgunEnabledForRun,
        ballStartingBonus = if (referenceRulesEnabled) 0 else ballAbility.startingBalls,
        orbStartingBonus = if (referenceRulesEnabled) referenceOrbStartingBonus() else orbEffects.startingBalls(),
        bossGuardLayoutVersion = 1,
        loopAccumulator = loop.snapshotAccumulator(),
        balls = balls.map { VolleyBallSnapshot(it.id, it.position.x, it.position.y, it.velocity.x, it.velocity.y, it.radius, it.lastBrickId, it.lastRailId, it.portalCooldown, it.alive) },
        bricks = bricks.map { VolleyBrickSnapshot(it.id, it.bounds.x, it.bounds.y, it.bounds.width, it.bounds.height, it.type, it.health, it.hue, it.groupId, it.locked, it.ghostVisible, it.initialHealth, it.gridRow, it.gridCol, it.countsForClear, it.movementDirection, it.rewardGranted, it.volleyVisualFamily, it.consumeOnHit, it.volleyShieldLock, it.volleyHazardKind) },
        rails = rails.map { VolleyRailSnapshot(it.id, it.x1, it.y1, it.x2, it.y2, it.thickness, it.fixed) },
        pickups = pickups.map { VolleyPickupSnapshot(it.id, it.position.x, it.position.y, it.amount, it.active, it.type, it.scoreValue) },
        orbPoison = orbPoison.toMap(),
        orbWrath = orbWrath.toList(),
        orbSecondWindUsed = orbSecondWindUsed,
        orbMultishotUsed = orbMultishotUsed,
        bonusDamageFlat = bonusDamageFlat,
        protectedBricks = protectedBricks.toMap(),
        frozenBricks = frozenBricks.toSet(),
        spikeCooldowns = spikeCooldowns.toMap(),
        vortexBalls = vortexBalls.toMap(),
        vortexClock = vortexClock,
        equippedOrbIds = activeOrbs.map { it.uid?.let { uid -> "copy:$uid" } ?: it.id },
        ignitedBallIds = ignitedBallIds.toSet(),
        burnChargesLeft = burnChargesLeft.toMap(),
        rotatingAngles = rotatingAngles.toMap(),
        rotatingStartAngles = rotatingStartAngles.toMap(),
        rotatingTargetAngles = rotatingTargetAngles.toMap(),
        torchMotion = torchMotion.toMap(),
        sourceCombatRulesVersion = if (referenceRulesEnabled) 1 else null,
        sourceCombatPlayerLevel = if (referenceRulesEnabled) sourceCombatPlayerLevel else null,
        sourceRemainingHealth = if (referenceRulesEnabled) sourceRemainingHealth.toMap() else null,
        combatKillSequence = if (referenceRulesEnabled) combatKillSequence else null,
        orbRunId = sourceCombatRunId
    )

    fun restore(snapshot: VolleySessionSnapshot) {
        require(snapshot.version == 2) { "Unsupported Volley snapshot version ${snapshot.version}" }
        require(snapshot.sourceCombatRulesVersion == null || snapshot.sourceCombatRulesVersion == 1) {
            "Unsupported source combat rules"
        }
        // Never silently combine new damage with health saved by an older release.
        referenceRulesEnabled = snapshot.sourceCombatRulesVersion == 1 && stage.sourceScene != null
        sourceCombatPlayerLevel = snapshot.sourceCombatPlayerLevel?.coerceAtLeast(1) ?: sourceCombatPlayerLevel
        sourceCombatRunId = snapshot.orbRunId?.takeIf { it.isNotBlank() } ?: sourceCombatRunId
        combatKillSequence = snapshot.combatKillSequence?.coerceAtLeast(0) ?: 0
        orbEffects = createOrbEffects()
        sourceRemainingHealth.clear()
        val previousPitch = if (stage.sourceScene == null) null else snapshot.bricks.firstNotNullOfOrNull { saved ->
            val authored = stage.authoredBricks.getOrNull(saved.id - 1)
            if (authored == null || authored.width <= 0f) null else when {
                abs(saved.width / authored.width - PREVIOUS_IMPORT_PITCH / TARGET_SOURCE_BRICK_PITCH) < .01f -> PREVIOUS_IMPORT_PITCH
                abs(saved.width / authored.width - ORIGINAL_IMPORT_PITCH / TARGET_SOURCE_BRICK_PITCH) < .01f -> ORIGINAL_IMPORT_PITCH
                else -> null
            }
        }
        val previousScale = if (previousPitch == null) 1f else TARGET_SOURCE_BRICK_PITCH / previousPitch
        val previousLaneLeft = if (previousPitch == null) SOURCE_LANE_LEFT
            else (BOARD_RIGHT - BOARD_LEFT - 11f * previousPitch) / 2f
        phase = snapshot.phase
        phaseBeforePause = snapshot.phaseBeforePause
        launcherX = snapshot.launcherX
        val oldBallBonus = snapshot.ballStartingBonus ?: ballAbility.startingBalls
        val oldOrbBonus = snapshot.orbStartingBonus ?: orbEffects.startingBalls()
        ballCount = if (referenceRulesEnabled)
            (snapshot.ballCount.toLong() + referenceOrbStartingBonus() -
                (snapshot.orbStartingBonus ?: referenceOrbStartingBonus())).coerceIn(1L, Int.MAX_VALUE.toLong()).toInt() else
            (snapshot.ballCount + ballAbility.startingBalls - oldBallBonus +
                orbEffects.startingBalls() - oldOrbBonus).coerceIn(1, 160)
        pendingLaunches = snapshot.pendingLaunches
        launchedThisVolley = snapshot.launchedThisVolley
        firstReturnX = snapshot.firstReturnX
        pendingReturnX = snapshot.pendingReturnX
        turn = snapshot.turn
        score = snapshot.score
        rescueCharges = snapshot.rescueCharges
        earnedBallsThisTurn = snapshot.earnedBallsThisTurn
        launchCooldown = snapshot.launchCooldown
        descentRemaining = snapshot.descentRemaining * previousScale
        combo = snapshot.combo
        abilityHitCounter = snapshot.abilityHitCounter
        volleyBreakCounter = snapshot.volleyBreakCounter
        laserHitCounter = snapshot.laserHitCounter
        laserGauge = snapshot.laserGauge
        timedBombHitsRemaining = snapshot.timedBombHitsRemaining
        bonusStarProgress = snapshot.bonusStarProgress
        nextBallId = snapshot.nextBallId
        openingBreakerBallId = snapshot.openingBreakerBallId
        shotgunEnabledForRun = !referenceRulesEnabled &&
            (snapshot.shotgunEnabled ?: shotgunEnabledForRun) && ballAbility.shotgunAvailable
        queuedModifiers.clear()
        queuedModifiers += snapshot.queuedModifiers
        queuedItemSources.clear()
        queuedItemSources += snapshot.queuedItemSources.orEmpty().filterKeys { it in queuedModifiers }
        activeModifiers.clear()
        activeModifiers += snapshot.activeModifiers
        balls.clear()
        balls += snapshot.balls.map { VolleyBallState(it.id, Vector2(it.x, it.y), Vector2(it.velocityX, it.velocityY), it.radius, it.lastBrickId, it.lastRailId, it.portalCooldown, it.alive) }
        ignitedBallIds.clear()
        ignitedBallIds.addAll(snapshot.ignitedBallIds.orEmpty().filter { id -> balls.any { it.id == id && it.alive } })
        orbPoison.clear()
        orbPoison.putAll(snapshot.orbPoison.orEmpty())
        orbWrath.clear()
        orbWrath.addAll(snapshot.orbWrath.orEmpty())
        orbSecondWindUsed = snapshot.orbSecondWindUsed
        orbMultishotUsed = snapshot.orbMultishotUsed
        bonusDamageFlat = snapshot.bonusDamageFlat
        protectedBricks.clear()
        protectedBricks.putAll(snapshot.protectedBricks.orEmpty())
        frozenBricks.clear()
        frozenBricks.addAll(snapshot.frozenBricks.orEmpty())
        spikeCooldowns.clear()
        spikeCooldowns.putAll(snapshot.spikeCooldowns.orEmpty())
        vortexBalls.clear()
        vortexBalls.putAll(snapshot.vortexBalls.orEmpty())
        vortexClock = snapshot.vortexClock
        burnChargesLeft.clear()
        burnChargesLeft.putAll(snapshot.burnChargesLeft.orEmpty())
        rotatingAngles.clear()
        rotatingAngles.putAll(snapshot.rotatingAngles.orEmpty())
        rotatingStartAngles.clear()
        rotatingStartAngles.putAll(snapshot.rotatingStartAngles.orEmpty())
        rotatingTargetAngles.clear()
        rotatingTargetAngles.putAll(snapshot.rotatingTargetAngles.orEmpty())
        torchMotion.clear()
        torchMotion.putAll(snapshot.torchMotion.orEmpty())
        initialImportedFreezeApplied = true
        val rankedSeedsById = stage.cells.flatten().filterNotNull()
        val savedBoardShift = snapshot.bricks.firstNotNullOfOrNull { saved ->
            val authored = stage.authoredBricks.getOrNull(saved.id - 1)
            if (authored == null || authored.type == BrickType.BOSS_CORE) null
            else SOURCE_WAVE_BOTTOM + (saved.y - SOURCE_WAVE_BOTTOM) * previousScale - authored.y
        } ?: 0f
        bricks.clear()
        bricks += snapshot.bricks.mapNotNull { saved ->
            val authored = stage.authoredBricks.getOrNull(saved.id - 1)
            // Older saves may contain a synthetic boss brick no longer present in the source.
            if (stage.sourceScene != null && authored == null) return@mapNotNull null
            val suppressedByWorldBoss = authored?.suppressedByWorldBoss == true
            val importedSlider = stage.sourceScene != null && authored?.sourceKind == "SLIDER" &&
                authored?.type == BrickType.INDESTRUCTIBLE
            val restoredSliderHeart = stage.sourceScene != null && authored?.sourceKind == "SLIDER" &&
                authored?.type?.breakable == true && saved.type == BrickType.INDESTRUCTIBLE
            val restoredSourceObstacle = stage.sourceScene != null &&
                authored?.sourceKind in setOf("TORCH", "ROTATING") && saved.type != BrickType.INDESTRUCTIBLE
            val restoredBlocker = stage.sourceScene != null && authored?.sourceKind == "BLOCKER" &&
                authored?.type == BrickType.INDESTRUCTIBLE && saved.type != BrickType.INDESTRUCTIBLE
            val restoredGate = stage.sourceScene != null && authored?.sourceKind == "BLOCKER" &&
                authored?.type == BrickType.ARMORED_DARK && saved.type == BrickType.INDESTRUCTIBLE
            val restoredBoss = stage.sourceScene != null && authored?.sourceKind == "BOSS" &&
                saved.type != BrickType.BOSS_CORE
            val restoredGuard = authored?.sourceKind == "BOSS_GUARD" && !saved.shieldLock
            val currentInitialHealth = authored?.let(::importedBrickHealth)
            val restoredOsb = stage.sourceScene != null && authored?.sourceKind == "OSB" &&
                saved.initialHealth != currentInitialHealth
            val refreshHealth = stage.sourceScene != null && authored != null &&
                (authored.type == saved.type || restoredBoss || restoredGuard) && authored.type.breakable &&
                authored.sourceKind !in IMPORTED_HIT_COUNTER_KINDS &&
                saved.initialHealth > 0 && saved.initialHealth != currentInitialHealth &&
                saved.health in 0..saved.initialHealth
            val initialHealth = if (importedSlider || restoredBlocker || restoredSourceObstacle) Int.MAX_VALUE
                else if (restoredGate || restoredSliderHeart || restoredOsb || refreshHealth) currentInitialHealth!! else saved.initialHealth
            val health = if (suppressedByWorldBoss) 0
                else if (importedSlider || restoredBlocker || restoredSourceObstacle) Int.MAX_VALUE
                else if (restoredGate || restoredSliderHeart) initialHealth else if (refreshHealth) {
                kotlin.math.ceil(saved.health.toDouble() * initialHealth / saved.initialHealth)
                    .toInt().coerceIn(0, initialHealth)
            } else if (restoredOsb && saved.health > 0) initialHealth else saved.health
            val bounds = if (previousPitch != null) Rectangle(
                SOURCE_LANE_LEFT + (saved.x - previousLaneLeft) * previousScale,
                SOURCE_WAVE_BOTTOM + (saved.y - SOURCE_WAVE_BOTTOM) * previousScale,
                saved.width * previousScale, saved.height * previousScale
            ) else Rectangle(saved.x, saved.y, saved.width, saved.height)
            if (stage.sourceScene != null && stage.bossStage && authored?.type == BrickType.BOSS_CORE &&
                snapshot.bossGuardLayoutVersion == null) {
                bounds.set(authored.x, authored.y + savedBoardShift, authored.width, authored.height)
            }
            if (restoredGuard) {
                frozenBricks.remove(saved.id)
                protectedBricks.remove(saved.id)
            }
            if (restoredSourceObstacle && authored != null) {
                bounds.x += (bounds.width - authored.width) / 2f
                bounds.y += (bounds.height - authored.height) / 2f +
                    if (authored.sourceKind == "ROTATING") 8f else 0f
                bounds.width = authored.width
                bounds.height = authored.height
            }
            Brick(
                saved.id, bounds,
                if (importedSlider || restoredBlocker || restoredSourceObstacle) BrickType.INDESTRUCTIBLE
                    else if (restoredOsb) BrickType.ARMORED_THREE_HIT
                    else if (restoredGate || restoredSliderHeart) BrickType.ARMORED_DARK
                    else if (restoredBoss) BrickType.BOSS_CORE
                    else if (restoredGuard) BrickType.ARMORED_DARK else saved.type, health, saved.hue,
                groupId = saved.groupId, locked = saved.locked, ghostVisible = saved.ghostVisible, initialHealth = initialHealth,
                gridRow = saved.gridRow, gridCol = saved.gridCol, countsForClear = if (suppressedByWorldBoss || importedSlider || restoredBlocker || restoredSourceObstacle) false
                    else if (restoredGate || restoredSliderHeart) true else saved.countsForClear,
                movementDirection = if (importedSlider && saved.type == BrickType.MOVING_HORIZONTAL &&
                    snapshot.phase == VolleyPhase.BOARD_DESCENT) 2 else saved.movementDirection,
                rewardGranted = saved.rewardGranted,
                volleyVisualFamily = if (importedSlider || restoredBlocker || restoredSourceObstacle) VolleyAssetCatalog.familyFor(BrickType.INDESTRUCTIBLE, world)
                    else if (restoredOsb) VolleyAssetCatalog.familyFor(BrickType.ARMORED_THREE_HIT, world)
                    else if (restoredGate || restoredSliderHeart) VolleyAssetCatalog.familyFor(BrickType.ARMORED_DARK, world)
                    else if (restoredBoss) VolleyAssetCatalog.familyFor(BrickType.BOSS_CORE, world)
                    else if (restoredGuard) VolleyAssetCatalog.familyFor(BrickType.ARMORED_DARK, world)
                    else saved.visualFamily,
                volleyAtlasSpriteName = authored?.atlasSpriteName
                    ?: rankedSeedsById.getOrNull(saved.id - 1)?.atlasSpriteName,
                volleyForestSkin = stage.world == 1, consumeOnHit = saved.consumeOnHit,
                volleyPieceShape = authored?.shape
                    ?: rankedSeedsById.getOrNull(saved.id - 1)?.shape
                    ?: VolleyPieceShape.RECTANGLE,
                volleyShieldLock = authored?.shieldLock ?: saved.shieldLock, volleyHazardKind = saved.hazardKind
            )
        }
        // A paused run from before the nine-world boss update has no final core yet.
        // Restore its existing brick IDs and progress, then add the new core at the
        // formation's current descended position instead of silently skipping it.
        val newBossSeed = stage.authoredBricks.lastOrNull()
        if (stage.sourceScene != null && stage.bossStage && stage.levelId != 160 &&
            snapshot.bricks.size < stage.authoredBricks.size &&
            phase != VolleyPhase.LEVEL_COMPLETE && newBossSeed != null &&
            newBossSeed.sourceKind == "BOSS" &&
            bricks.none { it.type == BrickType.BOSS_CORE && it.health > 0 }) {
            val boardShift = if (bricks.isEmpty()) 0f
                else bricks.first().bounds.y - stage.authoredBricks.first().y
            val bossHealth = importedBrickHealth(newBossSeed)
            bricks += Brick(
                id = stage.authoredBricks.size,
                bounds = Rectangle(newBossSeed.x, newBossSeed.y + boardShift,
                    newBossSeed.width, newBossSeed.height),
                type = BrickType.BOSS_CORE, health = bossHealth, hue = 0f,
                initialHealth = bossHealth, gridRow = newBossSeed.sourceRow,
                gridCol = newBossSeed.sourceColumn, countsForClear = true,
                volleyVisualFamily = VolleyAssetCatalog.familyFor(BrickType.BOSS_CORE, world),
                volleyPieceShape = newBossSeed.shape
            )
        }
        rails.clear()
        rails += snapshot.rails.map { VolleyRailState(it.id, it.x1, it.y1, it.x2, it.y2, it.thickness, it.fixed) }
        pickups.clear()
        pickups += snapshot.pickups.map { saved ->
            val x = if (previousPitch == null) saved.x
                else SOURCE_LANE_LEFT + (saved.x - previousLaneLeft) * previousScale
            val y = if (previousPitch == null) saved.y
                else SOURCE_WAVE_BOTTOM + (saved.y - SOURCE_WAVE_BOTTOM) * previousScale
            VolleyBonusNodeState(saved.id, Vector2(x, y), saved.amount, saved.active, saved.type, saved.scoreValue)
        }
        loop.restoreAccumulator(snapshot.loopAccumulator)
        if (referenceRulesEnabled) {
            for (brick in bricks) {
                val seed = stage.authoredBricks.getOrNull(brick.id - 1) ?: continue
                val initial = referenceInitialHealth(seed) ?: continue
                val remaining = if (brick.health <= 0) 0.0 else
                    snapshot.sourceRemainingHealth?.get(brick.id)?.takeIf { it.isFinite() && it > 0.0 }
                        ?: (initial - (brick.initialHealth - brick.health)).coerceAtLeast(0.0)
                sourceRemainingHealth[brick.id] = remaining.coerceAtMost(initial)
                brick.health = if (remaining < 1.0 && remaining != initial) 0 else
                    kotlin.math.ceil(remaining).toInt().coerceAtLeast(if (remaining > 0.0) 1 else 0)
            }
        }
        events.clear()
        resetFastForward()
    }

    /** Restore ad-continue metadata stored beside the v2 simulation snapshot. */
    fun restoreRewardedRevivesUsed(count: Int) {
        rewardedRevivesUsed = count.coerceIn(0, MAX_REWARDED_REVIVES)
    }

    /**
     * Applies a continue only after the rewarded-ad gateway reports an earned reward. Moving the
     * complete formation together preserves authored geometry while creating four rows of safety.
     */
    fun reviveFromRewardedAd(): Boolean {
        if (phase != VolleyPhase.GAME_OVER || rewardedRevivesRemaining <= 0) return false
        rewardedRevivesUsed++
        val lift = (if (stage.sourceScene != null) stage.descentDistance else ROW_PITCH) * REWARDED_REVIVE_ROWS
        bricks.filter { it.health > 0 }.forEach { it.bounds.y += lift }
        rails.filter { !it.fixed }.forEach {
            it.y1 += lift
            it.y2 += lift
        }
        pickups.filter { it.active }.forEach { it.position.y += lift }
        balls.clear()
        ignitedBallIds.clear()
        pendingLaunches = 0
        launchedThisVolley = 0
        firstReturnX = null
        pendingReturnX = null
        earnedBallsThisTurn = 0
        launchCooldown = 0f
        descentRemaining = 0f
        combo = 0
        activeModifiers.clear()
        resetFastForward()
        turn++
        phase = VolleyPhase.READY_TO_AIM
        emit(VolleyEvent.Feedback("SECOND CHANCE • WALL PUSHED BACK $REWARDED_REVIVE_ROWS ROWS", 1.8f))
        return true
    }

    fun pause() {
        if (phase == VolleyPhase.PAUSED || gameOver || levelComplete) return
        resetFastForward()
        phaseBeforePause = phase
        phase = VolleyPhase.PAUSED
    }

    fun resume() {
        if (phase == VolleyPhase.PAUSED) phase = phaseBeforePause
    }

    fun beginAiming(): Boolean {
        if (phase != VolleyPhase.READY_TO_AIM) return false
        phase = VolleyPhase.AIMING
        return true
    }

    fun cancelAiming() {
        if (phase == VolleyPhase.AIMING) phase = VolleyPhase.READY_TO_AIM
    }

    /**
     * Computes the real muzzle position for the current aim. Rendering, prediction and physics use
     * this same origin so the first ball no longer appears from inside or beside the cannon.
     */
    fun launchOrigin(direction: Vector2, out: Vector2): Vector2 {
        val normalized = out.set(direction)
        if (normalized.len2() < .0001f) normalized.set(0f, 1f) else normalized.nor()
        return normalized.scl(CANNON_BARREL_LENGTH).add(launcherX, CANNON_PIVOT_Y)
    }

    fun startVolley(direction: Vector2): Boolean {
        if (phase !in setOf(VolleyPhase.READY_TO_AIM, VolleyPhase.AIMING) || ballCount <= 0) return false
        applyInitialImportedFreeze()
        launchDirection.set(direction)
        if (launchDirection.len2() < .0001f) launchDirection.set(0f, 1f)
        launchDirection.nor()
        if (launchDirection.y < .30f) {
            launchDirection.y = .30f
            launchDirection.nor()
        }
        pendingLaunches = ballCount
        openingBreakerBallId = if (!referenceRulesEnabled && ballAbility.openingBreakerEveryVolleys > 0 &&
            turn % ballAbility.openingBreakerEveryVolleys == 0) nextBallId else null
        launchedThisVolley = 0
        firstReturnX = null
        pendingReturnX = null
        earnedBallsThisTurn = 0
        launchCooldown = 0f
        combo = 0
        abilityHitCounter = 0
        volleyBreakCounter = 0
        laserHitCounter = 0
        laserGauge = 0
        activeModifiers.clear()
        activeModifiers += queuedModifiers
        queuedModifiers.clear()
        queuedItemSources.clear()
        timedBombHitsRemaining = if (VolleyModifier.TIMED_BOMBS in activeModifiers) BOMB_TRIGGER_HITS else 0
        resetFastForward()
        phase = VolleyPhase.FIRING_SEQUENCE
        emit(VolleyEvent.Feedback(when {
            openingBreakerBallId != null -> "BREAKER VOLLEY $turn"
            activeModifiers.isNotEmpty() -> "CHARMED VOLLEY $turn"
            else -> "VOLLEY $turn"
        }))
        return true
    }

    /** Apply the selected pattern before the next launch, including after a paused Bag visit. */
    fun setShotgunEnabled(enabled: Boolean) {
        if (!firing) shotgunEnabledForRun = !referenceRulesEnabled && enabled && ballAbility.shotgunAvailable
    }

    fun advance(frameSeconds: Float) {
        if (phase == VolleyPhase.PAUSED || gameOver || levelComplete) return
        val frame = frameSeconds.coerceIn(0f, .05f)
        // Foreground seconds keep feedback readable even while 3X simulation is held.
        orbFeedbackCooldown = (orbFeedbackCooldown - frame).coerceAtLeast(0f)
        advanceSimulationPass(frame)
        repeat(FAST_FORWARD_MULTIPLIER - 1) {
            if (fastForwardActive) advanceSimulationPass(frame)
        }
    }

    private fun advanceSimulationPass(frameSeconds: Float) {
        val passStartedDuringVolley = firing
        loop.advance(frameSeconds) { step ->
            // A fast-forward pass must not spill its remaining fixed steps into board descent.
            // Once the final ball returns, descent resumes at normal speed on the next frame.
            if (!passStartedDuringVolley || firing) {
                updateTorchMotion(step)
                when (phase) {
                    VolleyPhase.FIRING_SEQUENCE, VolleyPhase.BALLS_IN_FLIGHT -> updateVolley(step)
                    VolleyPhase.BOARD_DESCENT -> updateBoardDescent(step)
                    else -> Unit
                }
            }
        }
    }

    /** Speed is active ONLY while the player physically holds the button; release is immediate. */
    fun setFastForwardHeld(held: Boolean): Boolean {
        if (!held) {
            fastForwardActive = false
            return true
        }
        if (!firing || (pendingLaunches <= 0 && balls.none { it.alive })) return false
        if (!fastForwardActive) emit(VolleyEvent.Feedback("HOLD FOR ${FAST_FORWARD_MULTIPLIER}X", .7f))
        fastForwardActive = true
        return true
    }

    fun recall(): Boolean {
        if (!firing) return false
        resetFastForward()
        pendingLaunches = 0
        balls.clear()
        ignitedBallIds.clear()
        firstReturnX = null
        emit(VolleyEvent.Feedback("VOLLEY RECALLED", .6f))
        finishVolley()
        return true
    }

    fun armCharm(type: PowerUpType, inventorySource: PowerUpType? = null): Boolean {
        val modifier = when (type) {
            PowerUpType.LASER_AUTO_CHARGE -> VolleyModifier.LASER
            PowerUpType.EXPAND_PADDLE -> VolleyModifier.WIDE_LAUNCH
            PowerUpType.FIRE_BALL -> VolleyModifier.FIRE
            PowerUpType.MAGNETIC_PADDLE -> VolleyModifier.MAGNETIC_RETURN
            PowerUpType.SLOW_BALL -> VolleyModifier.SLOW
            PowerUpType.DUAL_PADDLE -> VolleyModifier.DUAL_LAUNCH
            PowerUpType.MEGA_BALL -> VolleyModifier.MEGA
            PowerUpType.STICKY_PADDLE -> VolleyModifier.STICKY_RETURN
            PowerUpType.ONE_HIT_ANY_BRICK -> VolleyModifier.PIERCING
            PowerUpType.TIMED_BOMB_BRICKS -> VolleyModifier.TIMED_BOMBS
            else -> null
        }
        if (modifier != null) {
            if (modifier in queuedModifiers || modifier in activeModifiers) return false
            queuedModifiers += modifier
            if (inventorySource != null) queuedItemSources[modifier] = inventorySource
            emit(VolleyEvent.Feedback("${type.name.replace('_', ' ')} ARMED", 1.05f))
            return true
        }
        return when (type) {
            PowerUpType.EXTRA_LIFE -> {
                if (rescueCharges >= 3) return false
                rescueCharges++
                emit(VolleyEvent.Feedback("RESCUE WARD +1", 1.05f))
                true
            }

            PowerUpType.MULTIBALL_PLUS_4 -> {
                if (ballCount >= maxBallCount) return false
                val granted = min(4, maxBallCount - ballCount)
                ballCount += granted
                emit(VolleyEvent.Feedback("+$granted BALLS", 1.05f))
                true
            }

            PowerUpType.MULTIBALL_15 -> {
                if (ballCount >= maxBallCount) return false
                val granted = min(BALL_STORM_BONUS, maxBallCount - ballCount)
                ballCount += granted
                emit(VolleyEvent.Feedback("BALL STORM +$granted", 1.05f))
                true
            }

            else -> false
        }
    }

    /** Remove a paid charm only before its volley begins; return the item that must be refunded. */
    fun disarmQueuedCharm(modifier: VolleyModifier): PowerUpType? {
        val betweenVolleys = phase == VolleyPhase.READY_TO_AIM ||
            (phase == VolleyPhase.PAUSED && phaseBeforePause == VolleyPhase.READY_TO_AIM)
        if (!betweenVolleys || modifier !in queuedModifiers) return null
        val inventorySource = queuedItemSources.remove(modifier) ?: return null
        queuedModifiers.remove(modifier)
        emit(VolleyEvent.Feedback("${modifier.name.replace('_', ' ')} DISARMED", 1.05f))
        return inventorySource
    }

    fun canArmCharm(type: PowerUpType): String? {
        val betweenVolleys = phase == VolleyPhase.READY_TO_AIM ||
            (phase == VolleyPhase.PAUSED && phaseBeforePause == VolleyPhase.READY_TO_AIM)
        if (!betweenVolleys) return "USE BETWEEN VOLLEYS"
        val modifier = when (type) {
            PowerUpType.LASER_AUTO_CHARGE -> VolleyModifier.LASER
            PowerUpType.EXPAND_PADDLE -> VolleyModifier.WIDE_LAUNCH
            PowerUpType.TIMED_BOMB_BRICKS -> VolleyModifier.TIMED_BOMBS
            PowerUpType.FIRE_BALL -> VolleyModifier.FIRE
            PowerUpType.MAGNETIC_PADDLE -> VolleyModifier.MAGNETIC_RETURN
            PowerUpType.SLOW_BALL -> VolleyModifier.SLOW
            PowerUpType.DUAL_PADDLE -> VolleyModifier.DUAL_LAUNCH
            PowerUpType.MEGA_BALL -> VolleyModifier.MEGA
            PowerUpType.STICKY_PADDLE -> VolleyModifier.STICKY_RETURN
            PowerUpType.ONE_HIT_ANY_BRICK -> VolleyModifier.PIERCING
            else -> null
        }
        if (modifier != null && (modifier in queuedModifiers || modifier in activeModifiers)) return "ALREADY ARMED"
        return when (type) {
            PowerUpType.EXTRA_LIFE -> if (rescueCharges >= 3) "WARD LIMIT REACHED" else null
            PowerUpType.MULTIBALL_PLUS_4, PowerUpType.MULTIBALL_15 -> if (ballCount >= maxBallCount) "BALL LIMIT REACHED" else null
            PowerUpType.RANDOM_GOOD -> null
            in SHOP_ELIGIBLE_TYPES -> null
            else -> "NOT AVAILABLE IN VOLLEY"
        }
    }

    fun applyChallenge(type: PowerUpType): Boolean = when (type) {
        PowerUpType.KILL_PADDLE, PowerUpType.INSTANT_KILL_BALL -> {
            val removed = when {
                balls.isNotEmpty() -> {
                    balls.first().alive = false
                    true
                }

                pendingLaunches > 0 -> {
                    pendingLaunches--
                    true
                }

                else -> false
            }
            if (removed) emit(VolleyEvent.Feedback("ONE BALL LOST", .7f))
            removed
        }

        PowerUpType.SHRINK_BALL -> {
            activeModifiers += VolleyModifier.SHRINK_BALL
            balls.forEach { it.radius = (it.radius * .72f).coerceAtLeast(7f) }
            true
        }

        PowerUpType.SHRINK_PADDLE -> {
            if (VolleyModifier.SHRINK_GUIDE in activeModifiers || VolleyModifier.SHRINK_GUIDE in queuedModifiers) {
                false
            } else {
                if (firing) activeModifiers += VolleyModifier.SHRINK_GUIDE else queuedModifiers += VolleyModifier.SHRINK_GUIDE
                true
            }
        }

        PowerUpType.FAST_BALL -> {
            activeModifiers.remove(VolleyModifier.SLOW)
            queuedModifiers.remove(VolleyModifier.SLOW)
            if (firing) {
                activeModifiers += VolleyModifier.FAST
                balls.forEach {
                    it.velocity.scl(1.16f)
                    clampBallSpeed(it)
                }
            } else {
                queuedModifiers += VolleyModifier.FAST
            }
            true
        }

        PowerUpType.GHOST_BALL -> {
            if (firing) activeModifiers += VolleyModifier.GHOST else queuedModifiers += VolleyModifier.GHOST
            true
        }

        else -> false
    }

    fun addRandomPositive(type: PowerUpType): Boolean = armCharm(type)

    fun queuedCharmSummary(): String = charmSummary(queuedModifiers)

    fun activeCharmSummary(): String = charmSummary(activeModifiers)

    private fun charmSummary(modifiers: Set<VolleyModifier>): String = modifiers.joinToString(" • ") { modifier ->
        when (modifier) {
            VolleyModifier.FIRE -> "FIRE"
            VolleyModifier.MEGA -> "MEGA"
            VolleyModifier.PIERCING -> "BRICK-THRU"
            VolleyModifier.SLOW -> "SLOW"
            VolleyModifier.DUAL_LAUNCH -> "DUAL"
            VolleyModifier.WIDE_LAUNCH -> "WIDE"
            VolleyModifier.MAGNETIC_RETURN -> "MAGNET"
            VolleyModifier.STICKY_RETURN -> "STICKY"
            VolleyModifier.LASER -> "LASER"
            VolleyModifier.TIMED_BOMBS -> "BOMBS"
            VolleyModifier.FAST -> "FAST"
            VolleyModifier.SHRINK_BALL -> "SMALL"
            VolleyModifier.SHRINK_GUIDE -> "TIGHT AIM"
            VolleyModifier.GHOST -> "GHOST"
        }
    }

    fun restart() {
        balls.clear()
        ignitedBallIds.clear()
        orbPoison.clear()
        orbWrath.clear()
        orbFeedbackCooldown = 0f
        orbSecondWindUsed = false
        orbMultishotUsed = false
        bonusDamageFlat = 0
        resetBoardContent()
        phase = VolleyPhase.READY_TO_AIM
        launcherX = 450f
        ballCount = initialBallCount()
        pendingLaunches = 0
        launchedThisVolley = 0
        firstReturnX = null
        pendingReturnX = null
        earnedBallsThisTurn = 0
        launchCooldown = 0f
        descentRemaining = 0f
        turn = 1
        score = 0
        combo = 0
        rescueCharges = 0
        rewardedRevivesUsed = 0
        bonusStarProgress = 0
        laserGauge = 0
        timedBombHitsRemaining = 0
        queuedModifiers.clear()
        queuedItemSources.clear()
        activeModifiers.clear()
        events.clear()
        nextBallId = 1
        openingBreakerBallId = null
        loop.reset()
        damageQueue.clear()
        processingDamageQueue = false
        nextDamageEventId = 1L
    }

    /** Deterministic seam for behavior tests; uses the same damage pipeline as live collisions. */
    internal fun applyDamageForTest(brickId: Int, amount: Int = 1) {
        bricks.firstOrNull { it.id == brickId }?.let {
            damageBrick(it, amount, VolleyDamageSource.BALL, mutableSetOf())
        }
    }

    internal fun directHitForTest(
        brickId: Int,
        piercing: Boolean = VolleyModifier.PIERCING in activeModifiers,
        ballId: Int = -1
    ) {
        bricks.firstOrNull { it.id == brickId }?.let { brick ->
            directBrickHit(
                VolleyBallState(ballId, Vector2(centerX(brick), centerY(brick)), Vector2(0f, BALL_SPEED), DEFAULT_BALL_RADIUS),
                brick,
                piercing
            )
        }
    }

    internal fun teleportForTest(brickId: Int, ball: VolleyBallState) {
        bricks.firstOrNull { it.id == brickId }?.let { teleportBall(ball, it) }
    }

    /** Runs the ordered end-of-volley brick updates without fabricating screen input. */
    internal fun resolveBrickTurnForTest() {
        applyImportedTurnSpecials()
        regenerateBricks()
        moveBricks()
    }

    /** Exercises one swept physics pass without altering the fixed-step game loop. */
    internal fun sweepBallForTest(ball: VolleyBallState, seconds: Float) = simulateBall(ball, seconds)

    internal fun descendAndCheckForTest() {
        phase = VolleyPhase.BOARD_DESCENT
        descentRemaining = stage.descentDistance
        advance(DESCENT_SECONDS + .05f)
    }

    internal fun collectNodeForTest(nodeId: Int): Boolean {
        val node = pickups.firstOrNull { it.id == nodeId && it.active } ?: return false
        applyNodeReward(node)
        return true
    }

    private fun resetBoardContent() {
        buildBricks()
        buildRails()
        buildPickups()
        ballCount = initialBallCount()
    }

    private fun initialBallCount(): Int = if (referenceRulesEnabled)
        VolleyReferenceCombat.startingBalls(sourceCombatPlayerLevel, activeOrbs)
        else (stage.startingBalls + ballAbility.startingBalls + orbEffects.startingBalls()).coerceAtMost(160)

    private fun referenceOrbStartingBonus(): Int =
        VolleyReferenceCombat.startingBalls(sourceCombatPlayerLevel, activeOrbs) -
            VolleyReferenceCombat.startingBalls(sourceCombatPlayerLevel, emptyList())

    private fun buildBricks() {
        sourceRemainingHealth.clear()
        burnChargesLeft.clear()
        rotatingAngles.clear()
        rotatingStartAngles.clear()
        rotatingTargetAngles.clear()
        torchMotion.clear()
        initialImportedFreezeApplied = false
        bricks.clear()
        if (stage.authoredBricks.isNotEmpty()) {
            stage.authoredBricks.forEachIndexed { index, seed ->
                val defaultObjective = seed.type.breakable && seed.type !in setOf(
                    BrickType.SWITCH,
                    BrickType.TRANSPARENT_SLOW_PASS_THROUGH,
                    BrickType.LIGHTNING_SPEED_PASS_THROUGH
                )
                val hp = if (seed.suppressedByWorldBoss) 0 else if (stage.sourceScene != null) {
                    importedBrickHealth(seed)
                } else when (seed.type) {
                    BrickType.NORMAL_ONE_HIT,
                    BrickType.ROUGH_STONE,
                    BrickType.LIGHTNING_SPEED_PASS_THROUGH,
                    BrickType.TRANSPARENT_SLOW_PASS_THROUGH
                    -> 1

                    BrickType.INDESTRUCTIBLE,
                    BrickType.BLACK_HOLE_TELEPORTER,
                    BrickType.SPIKED_HAZARD
                    -> Int.MAX_VALUE

                    else -> seed.health.coerceIn(1, MAX_STANDARD_BRICK_HP)
                }
                if (!seed.suppressedByWorldBoss) referenceInitialHealth(seed)?.let {
                    sourceRemainingHealth[index + 1] = it
                }
                bricks += Brick(
                    id = index + 1,
                    bounds = Rectangle(seed.x, seed.y, seed.width, seed.height),
                    type = seed.type,
                    health = hp,
                    hue = ((seed.sourceRow * 11 + seed.sourceColumn) * .071f) % 1f,
                    groupId = seed.groupId,
                    initialHealth = hp,
                    gridRow = seed.sourceRow,
                    gridCol = seed.sourceColumn,
                    movementDirection = if (seed.sourceKind == "SLIDER" && !seed.slideStartsMoved) 0 else 1,
                    countsForClear = !seed.suppressedByWorldBoss && seed.countsForClear && defaultObjective,
                    volleyVisualFamily = VolleyAssetCatalog.familyFor(seed.type, world),
                    volleyAtlasSpriteName = seed.atlasSpriteName,
                    volleyForestSkin = world == 1,
                    volleyPieceShape = seed.shape,
                    volleyShieldLock = seed.shieldLock,
                    volleyHazardKind = seed.hazardKind ?: if (seed.type == BrickType.SPIKED_HAZARD) VolleyHazardKind.SPIKED else null
                )
            }
            return
        }
        val columns = stage.columns.coerceAtLeast(1)
        val brickWidth = (BOARD_RIGHT - BOARD_LEFT - BRICK_GAP * (columns - 1)) / columns
        var id = 1
        stage.cells.forEachIndexed { row, line ->
            line.forEachIndexed { column, seed ->
                seed ?: return@forEachIndexed
                val rawScaledHp = max(1, (stage.baseHp * seed.hpScale).roundToInt())
                val scaledHp = rawScaledHp.coerceAtMost(MAX_STANDARD_BRICK_HP)
                val hp = when (seed.type) {
                    BrickType.NORMAL_ONE_HIT,
                    BrickType.ROUGH_STONE,
                    BrickType.LIGHTNING_SPEED_PASS_THROUGH,
                    BrickType.TRANSPARENT_SLOW_PASS_THROUGH
                    -> 1

                    BrickType.ARMORED_TWO_HIT -> max(2, (2f * seed.hpScale).roundToInt())

                    BrickType.ARMORED_THREE_HIT -> max(3, (3f * seed.hpScale).roundToInt())

                    BrickType.BOSS_CORE -> max(40, (rawScaledHp * BOSS_HP_MULTIPLIER).roundToInt())

                    BrickType.REGENERATING -> max(2, scaledHp)

                    BrickType.INDESTRUCTIBLE,
                    BrickType.BLACK_HOLE_TELEPORTER,
                    BrickType.SPIKED_HAZARD
                    -> Int.MAX_VALUE

                    else -> scaledHp
                }
                val defaultObjective = seed.type.breakable && seed.type !in setOf(
                    BrickType.SWITCH,
                    BrickType.TRANSPARENT_SLOW_PASS_THROUGH,
                    BrickType.LIGHTNING_SPEED_PASS_THROUGH
                )
                bricks += Brick(
                    id = id++,
                    bounds = Rectangle(
                        BOARD_LEFT + column * (brickWidth + BRICK_GAP),
                        BRICK_START_TOP - (row + 1) * ROW_PITCH,
                        brickWidth,
                        BRICK_HEIGHT
                    ),
                    type = seed.type,
                    health = hp,
                    hue = ((row * columns + column) * .071f) % 1f,
                    groupId = seed.groupId,
                    initialHealth = hp,
                    gridRow = row,
                    gridCol = column,
                    countsForClear = seed.countsForClear ?: defaultObjective,
                    volleyVisualFamily = VolleyAssetCatalog.familyFor(seed.type, world),
                    volleyForestSkin = world == 1,
                    volleyPieceShape = seed.shape,
                    volleyAtlasSpriteName = seed.atlasSpriteName,
                    consumeOnHit = seed.consumeOnHit,
                    volleyShieldLock = seed.shieldLock,
                    volleyHazardKind = seed.hazardKind ?: if (seed.type == BrickType.SPIKED_HAZARD) VolleyHazardKind.SPIKED else null
                )
            }
        }
    }

    private fun buildRails() {
        rails.clear()
        stage.rails.forEachIndexed { index, seed ->
            rails += VolleyRailState(
                id = index + 1,
                x1 = gridX(seed.x1),
                y1 = gridY(seed.y1),
                x2 = gridX(seed.x2),
                y2 = gridY(seed.y2),
                thickness = seed.thickness,
                fixed = seed.fixed
            )
        }
    }

    private fun buildPickups() {
        pickups.clear()
        stage.pickups.forEachIndexed { index, seed ->
            pickups += VolleyBonusNodeState(
                id = index + 1,
                position = Vector2(gridX(seed.x), gridY(seed.y)),
                amount = seed.balls,
                type = seed.type,
                scoreValue = seed.scoreValue
            )
        }
    }

    private fun gridX(x: Float): Float = BOARD_LEFT + (BOARD_RIGHT - BOARD_LEFT) * (x / stage.columns)
    private fun gridY(y: Float): Float = BRICK_START_TOP - y * ROW_PITCH

    /** Keep the HP calculated by the source stage loader; impact counters are separate. */
    private fun importedBrickHealth(seed: VolleyAuthoredBrickSeed): Int {
        if (!seed.type.breakable) return Int.MAX_VALUE
        if (seed.sourceKind in IMPORTED_HIT_COUNTER_KINDS) return seed.health.coerceAtLeast(1)
        referenceInitialHealth(seed)?.let { return kotlin.math.ceil(it).toInt().coerceAtLeast(1) }
        val sourceBase = seed.sourceBaseHealth ?: return (seed.health * difficulty.healthMultiplier).coerceAtLeast(1)
        return VolleySourceHealth.initialHealth(sourceBase, stage.levelId, difficulty)
    }

    /** The health label takes an int, truncating fractional stage HP for display. */
    fun displayedBrickHealth(brick: Brick): Int {
        if (referenceRulesEnabled) sourceRemainingHealth[brick.id]?.let {
            return if (brick.health <= 0) 0 else it.toInt().coerceAtLeast(0)
        }
        val seed = if (stage.sourceScene != null) stage.authoredBricks.getOrNull(brick.id - 1) else null
        val base = seed?.sourceBaseHealth ?: return brick.health
        val raw = VolleySourceHealth.rawHealth(base, stage.levelId, difficulty)
        return if (raw < brick.initialHealth.toDouble() - .00001 && brick.health > 0)
            brick.health - 1 else brick.health
    }

    /** Campaign attack is independent of the target's health; ball upgrades supply its power. */
    private fun importedBallAttack(brick: Brick): Int {
        val seed = stage.authoredBricks.getOrNull(brick.id - 1)
        if (stage.sourceScene == null) return 1
        if (brick.type == BrickType.BOSS_CORE) return VolleySourceHealth.bossBallAttack(world, brick.initialHealth)
        if (seed?.sourceBaseHealth == null ||
            seed.sourceKind in IMPORTED_HIT_COUNTER_KINDS) return 1
        return VolleySourceHealth.ballAttack(stage.levelId, brick.initialHealth)
    }

    private fun referenceInitialHealth(seed: VolleyAuthoredBrickSeed): Double? {
        if (!referenceRulesEnabled || !seed.type.breakable || seed.type == BrickType.BOSS_CORE ||
            seed.sourceKind in IMPORTED_HIT_COUNTER_KINDS) return null
        val base = seed.sourceBaseHealthExact ?: seed.sourceBaseHealth?.toDouble() ?: return null
        return VolleySourceHealth.referenceRawHealth(base, stage.levelId, difficulty)
    }

    private fun numericRemainingHealth(brick: Brick): Double =
        if (referenceRulesEnabled) sourceRemainingHealth[brick.id] ?: brick.health.toDouble()
        else brick.health.toDouble()

    private fun percentHealthDamage(brick: Brick, percent: Float): Int = if (referenceRulesEnabled)
        ceil(numericRemainingHealth(brick) * percent.toString().toDouble() / 100.0).toInt()
        else ceil(brick.health * percent / 100f).toInt()

    /** RPG boss replays make two independent rolls at +75 drop rate and 1.5 quality. */
    fun rollRepeatBossOrbRanks(): List<Int> {
        if (stage.sourceScene == null || !stage.bossStage) return emptyList()
        val difficultyDrop = when (difficulty) {
            VolleyDifficulty.NORMAL -> 1f
            VolleyDifficulty.HARD -> 1.1f
            VolleyDifficulty.EXTREME -> 1.2f
        }
        return List(2) {
            if (orbEffects.rollsExtraOrbPoint(difficultyDrop, 75f, 1.5f))
                orbEffects.rollNaturalOrbRank(1.5f) else -1
        }
    }

    private fun updateVolley(dt: Float) {
        vortexClock += dt
        spikeCooldowns.replaceAll { _, seconds -> (seconds - dt).coerceAtLeast(0f) }
        updateOrbPoison(dt)
        updateOrbWrath(dt)
        if (levelComplete) return
        launchCooldown -= dt
        while (pendingLaunches > 0 && launchCooldown <= 0f) {
            launchBall()
            pendingLaunches--
            launchedThisVolley++
            launchCooldown += LAUNCH_INTERVAL
        }
        if (pendingLaunches == 0 && phase == VolleyPhase.FIRING_SEQUENCE) phase = VolleyPhase.BALLS_IN_FLIGHT

        processingBallUpdates = true
        try {
            balls.forEach { ball ->
                if (!ball.alive) return@forEach
                vortexBalls[ball.id]?.let { remaining ->
                    if (remaining <= dt) {
                        ball.alive = false
                        vortexBalls.remove(ball.id)
                    } else vortexBalls[ball.id] = remaining - dt
                    return@forEach
                }
                ball.portalCooldown = (ball.portalCooldown - dt).coerceAtLeast(0f)
                simulateBall(ball, dt)
                // A collision may complete the level and retire every ball. Do not grant pickups
                // after completion, while preserving the existing return/hazard pickup behavior.
                if (!levelComplete) collectPickups(ball)
            }
        } finally {
            processingBallUpdates = false
            // Removing here (rather than inside checkLevelComplete) keeps the active iterator valid.
            balls.removeAll { !it.alive }
        }
        if (pendingLaunches == 0 && balls.isEmpty() && !levelComplete) finishVolley()
    }

    private fun launchBall() {
        val direction = Vector2(launchDirection)
        if (shotgunEnabledForRun) {
            direction.rotateDeg((launchedThisVolley % 5 - 2) * 6f)
        } else if (VolleyModifier.WIDE_LAUNCH in activeModifiers) {
            direction.rotateDeg(
                when (launchedThisVolley % 3) {
                    0 -> -8f
                    2 -> 8f
                    else -> 0f
                }
            )
        }
        val offset = if (VolleyModifier.DUAL_LAUNCH in activeModifiers) {
            if (launchedThisVolley % 2 == 0) -42f else 42f
        } else {
            0f
        }
        val speedModifier = when {
            VolleyModifier.SLOW in activeModifiers -> .70f
            VolleyModifier.FAST in activeModifiers -> 1.18f
            else -> 1f
        }
        val speed = BALL_SPEED * speedModifier
        val origin = launchOrigin(direction, launchSpawn)
        val lateralX = -direction.y * offset
        val lateralY = direction.x * offset
        balls += VolleyBallState(
            id = nextBallId++,
            position = Vector2(
                (origin.x + lateralX).coerceIn(BOARD_LEFT + DEFAULT_BALL_RADIUS, BOARD_RIGHT - DEFAULT_BALL_RADIUS),
                (origin.y + lateralY).coerceIn(RETURN_Y + DEFAULT_BALL_RADIUS, boardTop - DEFAULT_BALL_RADIUS)
            ),
            velocity = direction.scl(speed),
            radius = when {
                VolleyModifier.MEGA in activeModifiers -> MEGA_BALL_RADIUS
                VolleyModifier.SHRINK_BALL in activeModifiers -> DEFAULT_BALL_RADIUS * .72f
                else -> DEFAULT_BALL_RADIUS
            }
        )
        // One sound event per launched ball; presentation applies RPG's original sound settings.
        emit(VolleyEvent.Sound("volley_ball"))
    }

    private sealed interface HitTarget {
        data class Wall(val collision: CollisionResult) : HitTarget
        data class Return(val time: Float) : HitTarget
        data class Rail(val collision: CollisionResult, val rail: VolleyRailState) : HitTarget
        data class BrickHit(val collision: CollisionResult, val brick: Brick) : HitTarget
    }

    private fun simulateBall(ball: VolleyBallState, dt: Float) {
        var remaining = dt
        val ignoredBricks = mutableSetOf<Int>()
        repeat(MAX_COLLISIONS_PER_STEP) {
            if (!ball.alive || remaining <= .000001f) return
            val delta = Vector2(ball.velocity).scl(remaining)
            var best: HitTarget? = null
            var bestTime = 1.0001f
            fun consider(target: HitTarget, time: Float) {
                // Some imported scenes put a stone backdrop and a required enemy on the
                // exact same polygon. Equal-time hits used to select the earlier stone
                // forever. Hit the objective first, then leave the stone after it breaks.
                // Adjacent pieces and genuinely earlier obstacles keep their normal order.
                val previous = best
                val coincidentObjective = time == bestTime && target is HitTarget.BrickHit &&
                    previous is HitTarget.BrickHit && target.brick.countsForClear &&
                    target.brick.type.breakable && !previous.brick.type.breakable &&
                    sourceKind(previous.brick) == "BLOCKER" &&
                    target.brick.volleyPieceShape == previous.brick.volleyPieceShape &&
                    target.brick.bounds == previous.brick.bounds
                if (time in 0f..1f && (time < bestTime || coincidentObjective)) {
                    best = target
                    bestTime = time
                }
            }

            wallCollision(ball, delta)?.let { consider(HitTarget.Wall(it), it.time) }
            if (delta.y < 0f && ball.position.y >= RETURN_Y) {
                val time = (RETURN_Y - ball.position.y) / delta.y
                if (time in 0f..1f) consider(HitTarget.Return(time), time)
            }
            rails.forEach { rail ->
                SweptCollision.circleVsSegment(
                    ball.position, delta, ball.radius,
                    rail.x1, rail.y1, rail.x2, rail.y2, rail.thickness, rail.id
                )?.let { if (rail.id != ball.lastRailId) consider(HitTarget.Rail(it, rail), it.time) }
            }
            bricks.forEach { brick ->
                if (brick.health <= 0 || brick.id in ignoredBricks) return@forEach
                if (brick.type == BrickType.GHOST && !brick.ghostVisible) return@forEach
                brickCollision(ball, delta, brick)
                    ?.let { consider(HitTarget.BrickHit(it, brick), it.time) }
            }

            val target = best
            if (target == null) {
                ball.position.add(delta)
                ball.lastBrickId = -1
                ball.lastRailId = -1
                return
            }
            ball.position.mulAdd(delta, bestTime)
            remaining *= (1f - bestTime).coerceAtLeast(0f)
            when (target) {
                is HitTarget.Return -> {
                    ball.alive = false
                    if (firstReturnX == null) firstReturnX = ball.position.x.coerceIn(BOARD_LEFT + 16f, BOARD_RIGHT - 16f)
                    return
                }

                is HitTarget.Wall -> {
                    reflect(ball, target.collision)
                    nudge(ball, target.collision)
                    emit(VolleyEvent.Sound("wall_hit"))
                    ball.lastBrickId = -1
                    ball.lastRailId = -1
                }

                is HitTarget.Rail -> {
                    reflect(ball, target.collision)
                    nudge(ball, target.collision)
                    emit(VolleyEvent.Sound("wall_hit"))
                    emit(VolleyEvent.Vfx(VolleyVfxKind.SPARK, ball.position.x, ball.position.y))
                    ball.lastRailId = target.rail.id
                    ball.lastBrickId = -1
                }

                is HitTarget.BrickHit -> {
                    val brick = target.brick
                    if (VolleyModifier.GHOST in activeModifiers) {
                        ignoredBricks += brick.id
                        ball.position.mulAdd(Vector2(ball.velocity).nor(), COLLISION_NUDGE)
                        return@repeat
                    }
                    if (sourceKind(brick) == "VORTEX") {
                        if (vortexClock % 6f < 2f) {
                            ball.position.set(centerX(brick), centerY(brick))
                            ball.velocity.setZero()
                            vortexBalls[ball.id] = .4f
                            emit(VolleyEvent.Vfx(VolleyVfxKind.PORTAL, ball.position.x, ball.position.y))
                            return
                        }
                        ignoredBricks += brick.id
                        ball.position.mulAdd(Vector2(ball.velocity).nor(), COLLISION_NUDGE)
                        return@repeat
                    }
                    if (sourceKind(brick) == "SPIKE_TRAP") {
                        if ((spikeCooldowns[brick.id] ?: 0f) <= 0f) {
                            ball.alive = false
                            spikeCooldowns[brick.id] = 10f
                            emit(VolleyEvent.Vfx(VolleyVfxKind.HAZARD, ball.position.x, ball.position.y))
                            return
                        }
                        reflect(ball, target.collision)
                        nudge(ball, target.collision)
                        return@repeat
                    }
                    if (sourceKind(brick) == "TORCH" && ignitedBallIds.add(ball.id)) {
                        emit(VolleyEvent.Vfx(VolleyVfxKind.FIRE, ball.position.x, ball.position.y))
                        emit(VolleyEvent.Feedback("FIREBALL", .55f))
                    }
                    val passThroughType = brick.type == BrickType.LIGHTNING_SPEED_PASS_THROUGH ||
                        brick.type == BrickType.TRANSPARENT_SLOW_PASS_THROUGH
                    val piercing = VolleyModifier.PIERCING in activeModifiers && brick.type.breakable &&
                        brick.type != BrickType.BOSS_CORE && !(brick.type == BrickType.LOCKED && brick.locked)
                    when (brick.type) {
                        BrickType.BLACK_HOLE_TELEPORTER -> teleportBall(ball, brick)

                        BrickType.SPIKED_HAZARD -> {
                            ball.alive = false
                            emit(VolleyEvent.Feedback("BALL LOST", .5f))
                            emit(VolleyEvent.Sound("explosion"))
                            emit(VolleyEvent.Vfx(VolleyVfxKind.HAZARD, ball.position.x, ball.position.y))
                        }

                        else -> {
                            if (!passThroughType && !piercing) {
                                reflect(ball, target.collision)
                                nudge(ball, target.collision)
                            }
                            if (brick.id != ball.lastBrickId && brick.type.breakable) directBrickHit(ball, brick, piercing)
                            if (sourceKind(brick) == "BURN") {
                                val charges = burnChargesLeft[brick.id] ?:
                                    (stage.authoredBricks.getOrNull(brick.id - 1)?.sourceBurnCharges ?: 1)
                                if (charges > 0) {
                                    burnChargesLeft[brick.id] = charges - 1
                                    ball.alive = false
                                    emit(VolleyEvent.Vfx(VolleyVfxKind.FIRE, ball.position.x, ball.position.y))
                                    emit(VolleyEvent.Feedback("BALL BURNED", .55f))
                                    return
                                }
                            }
                            if (brick.type == BrickType.ROUGH_STONE && !piercing) {
                                ball.velocity.rotateDeg(-24f + random.nextFloat() * 48f)
                                clampBallSpeed(ball)
                            }
                            if (passThroughType) {
                                val multiplier = if (brick.type == BrickType.LIGHTNING_SPEED_PASS_THROUGH) 1.08f else .86f
                                ball.velocity.scl(multiplier)
                                clampBallSpeed(ball)
                                ignoredBricks += brick.id
                                val direction = Vector2(ball.velocity).nor()
                                ball.position.mulAdd(direction, COLLISION_NUDGE)
                            }
                        }
                    }
                    ball.lastBrickId = brick.id
                    ball.lastRailId = -1
                }
            }
        }
        // Reaching the collision budget must never advance through unchecked bricks or walls.
        // The next fixed step resumes the sweep from this safe position.
    }

    private fun wallCollision(ball: VolleyBallState, delta: Vector2): CollisionResult? {
        var best: CollisionResult? = null
        fun consider(candidate: CollisionResult?) {
            if (candidate != null && (best == null || candidate.time < best!!.time)) best = candidate
        }
        val wallDepth = 80f
        consider(SweptCollision.circleVsAabb(ball.position, delta, ball.radius, Rectangle(BOARD_LEFT - wallDepth, RETURN_Y, wallDepth, boardTop - RETURN_Y), -10))
        consider(SweptCollision.circleVsAabb(ball.position, delta, ball.radius, Rectangle(BOARD_RIGHT, RETURN_Y, wallDepth, boardTop - RETURN_Y), -11))
        consider(SweptCollision.circleVsAabb(ball.position, delta, ball.radius, Rectangle(BOARD_LEFT, boardTop, BOARD_RIGHT - BOARD_LEFT, wallDepth), -12))
        return best
    }

    /** Uses the visible diagonal edges for geometric pieces instead of their transparent AABB. */
    private fun brickCollision(ball: VolleyBallState, delta: Vector2, brick: Brick): CollisionResult? =
        sweptCircleVsRotatedVolleyBrick(ball.position, delta, ball.radius, brick, brickRotation(brick.id))

    private fun reflect(ball: VolleyBallState, collision: CollisionResult) {
        val dot = ball.velocity.x * collision.normalX + ball.velocity.y * collision.normalY
        ball.velocity.x -= 2f * dot * collision.normalX
        ball.velocity.y -= 2f * dot * collision.normalY
        clampBallSpeed(ball)
    }

    private fun nudge(ball: VolleyBallState, collision: CollisionResult) {
        ball.position.x += collision.normalX * COLLISION_NUDGE
        ball.position.y += collision.normalY * COLLISION_NUDGE
    }

    private fun clampBallSpeed(ball: VolleyBallState) {
        val speed = ball.velocity.len()
        val minSpeed = BALL_SPEED * .68f
        val maxSpeed = BALL_SPEED * 1.28f
        when {
            speed < minSpeed -> ball.velocity.setLength(minSpeed)
            speed > maxSpeed -> ball.velocity.setLength(maxSpeed)
        }
        if (abs(ball.velocity.y) < BALL_SPEED * .12f) {
            ball.velocity.y = if (ball.velocity.y < 0f) -BALL_SPEED * .12f else BALL_SPEED * .12f
            ball.velocity.setLength(speed.coerceIn(minSpeed, maxSpeed))
        }
    }

    private fun directBrickHit(ball: VolleyBallState, brick: Brick, piercing: Boolean) {
        if (!brick.type.breakable || brick.health <= 0) return
        // Godot Alt_Blocker starts open and switches vulnerability every board descent.
        // The existing ARMOR_BLOCKER HP and original authored locations are preserved.
        if (isSourceAlternatingBlocker(brick) && !isSourceAlternatingBlockerOpen()) {
            emit(VolleyEvent.Sound("wall_hit"))
            emit(VolleyEvent.Impact(centerX(brick), centerY(brick), false))
            return
        }
        if (stage.sourceScene != null) emit(VolleyEvent.Sound("source_ball_hit"))
        if (referenceRulesEnabled && brick.id in frozenBricks && !isBallIgnited(ball.id) &&
            VolleyModifier.FIRE !in activeModifiers) {
            emit(VolleyEvent.Impact(centerX(brick), centerY(brick), false))
            return
        }
        val openingBreakerHit = ball.id == openingBreakerBallId && brick.type != BrickType.BOSS_CORE
        if (brick.type == BrickType.LOCKED && brick.locked && !openingBreakerHit) {
            emit(VolleyEvent.Feedback("LOCKED — FIND THE KEY", .45f))
            return
        }
        if (brick.type == BrickType.SWITCH) {
            toggleSwitchGroup(brick.groupId)
            if (!brick.consumeOnHit) {
                emit(VolleyEvent.Impact(centerX(brick), centerY(brick), false))
                return
            }
        }
        abilityHitCounter++
        laserHitCounter++
        if (VolleyModifier.LASER in activeModifiers) laserGauge++
        var damage = if (referenceRulesEnabled) (referenceBaseDamage.toLong() +
            (if (VolleyModifier.MEGA in activeModifiers) 1 else 0)).coerceAtMost(Int.MAX_VALUE.toLong()).toInt()
            else ballAbility.hitDamageForVolley(ballCount) + bonusDamageFlat +
                (if (VolleyModifier.MEGA in activeModifiers) 1 else 0)
        if (piercing && !referenceRulesEnabled) damage = brick.health
        if (!referenceRulesEnabled) when (ballAbility.kind) {
            BallAbilityKind.ARMOR_BREAKER -> if (abilityTriggered(ballAbility.triggerEveryHits)) damage += ballAbility.bonusDamage
            BallAbilityKind.VOID_PHASE -> if (abilityTriggered(ballAbility.triggerEveryHits))
                damage += ballAbility.bonusDamage
            else -> Unit
        }
        val visited = mutableSetOf<Int>()
        if (isBallIgnited(ball.id)) clearFireStatus(brick)
        val orbHit = orbEffects.hit(
            damage, brick.type == BrickType.BOSS_CORE,
            totalBalls = ballCount, activeBalls = balls.count { it.alive },
            currentBonusDamage = bonusDamageFlat, fireball = isBallIgnited(ball.id)
        )
        // Imported RPG HP already needs a scaled baseline attack. Multiplying the
        // entire Orb bonus by that scale made one Orb erase many displayed hits.
        val ballDamage = (if (referenceRulesEnabled && brick.type != BrickType.BOSS_CORE)
            (if (piercing) brick.health else orbHit.damage).toLong()
            else damage.toLong() * importedBallAttack(brick) + (orbHit.damage - damage).coerceAtLeast(0))
            .coerceIn(0L, brick.health.toLong()).toInt()
        if (openingBreakerHit) openingBreakerBallId = null
        val healthBeforeHit = brick.health
        damageBrick(brick, if (openingBreakerHit) brick.health else ballDamage,
            if (openingBreakerHit) VolleyDamageSource.OPENING_BREAKER else VolleyDamageSource.BALL,
            visited, sourceBallId = ball.id)
        if (openingBreakerHit) {
            emit(VolleyEvent.Feedback("BREAKER SHOT", .85f))
            emit(VolleyEvent.Vfx(VolleyVfxKind.FIRE, centerX(brick), centerY(brick)))
            emit(VolleyEvent.Sound("explosion"))
        }
        // Empower adds to the attack bonus for later hits; it does not hit twice here.
        bonusDamageFlat = (bonusDamageFlat.toLong() + orbHit.empowerBonus).coerceAtMost(Int.MAX_VALUE.toLong()).toInt()
        if (orbHit.empowerBonus > 0) {
            emit(VolleyEvent.Vfx(VolleyVfxKind.SPARK, centerX(brick), centerY(brick)))
            notifyOrbActivation("EMPOWER +${orbHit.empowerBonus}")
        } else if (orbHit.critical && brick.health < healthBeforeHit) {
            emit(VolleyEvent.Vfx(VolleyVfxKind.SPARK, centerX(brick), centerY(brick)))
            notifyOrbActivation("CRITICAL HIT")
        }
        // Imported dynamite and four-way suicide bricks ignore secondary orb attacks.
        if (!levelComplete && sourceKind(brick) !in setOf("DYNAMITE", "SUICIDE")) applyOrbHit(ball, brick, orbHit)
        when {
            VolleyModifier.PIERCING in activeModifiers -> emit(VolleyEvent.Vfx(VolleyVfxKind.BEAM, centerX(brick), centerY(brick)))

            VolleyModifier.MEGA in activeModifiers -> {
                emit(VolleyEvent.Vfx(VolleyVfxKind.SPARK, centerX(brick), centerY(brick)))
                emit(VolleyEvent.Haptic(20))
            }
        }
        if (VolleyModifier.TIMED_BOMBS in activeModifiers && timedBombHitsRemaining > 0) {
            timedBombHitsRemaining--
            splashDamage(brick, 1, VolleyDamageSource.EXPLOSION, mutableSetOf())
            emit(VolleyEvent.Vfx(VolleyVfxKind.SMOKE, centerX(brick), centerY(brick)))
            emit(VolleyEvent.Sound("explosion"))
        }
        val inferno = !referenceRulesEnabled && ballAbility.kind == BallAbilityKind.INFERNO_BURST && abilityTriggered(ballAbility.triggerEveryHits)
        if (VolleyModifier.FIRE in activeModifiers || inferno) {
            // Fire thaws and strips protection before its splash is resolved.
            val thawedSource = clearFireStatus(brick)
            bricks.filter { neighbor ->
                neighbor !== brick && isVisibleOrbTarget(neighbor) &&
                    abs(centerX(neighbor) - centerX(brick)) <= brick.bounds.width * 1.55f &&
                    abs(centerY(neighbor) - centerY(brick)) <= brick.bounds.height * 1.65f
            }.forEach { clearFireStatus(it) }
            if (thawedSource && brick.health > 0) {
                damageBrick(brick, 1, VolleyDamageSource.FIRE_SPLASH, mutableSetOf(), sourceBallId = ball.id)
            }
            splashDamage(brick, 1, VolleyDamageSource.FIRE_SPLASH, mutableSetOf())
            emit(VolleyEvent.Vfx(VolleyVfxKind.FIRE, centerX(brick), centerY(brick)))
        }
        if (!referenceRulesEnabled && ballAbility.kind == BallAbilityKind.ARC_CHAIN && abilityTriggered(ballAbility.triggerEveryHits)) {
            bricks.asSequence()
                .filter { it !== brick && isVisibleOrbTarget(it) }
                .minByOrNull { distanceSquared(centerX(brick), centerY(brick), centerX(it), centerY(it)) }
                ?.let { damageBrick(it, ballAbility.chainTargets.coerceAtLeast(1), VolleyDamageSource.CHAIN, mutableSetOf(), sourceBallId = ball.id) }
        }
        if (VolleyModifier.LASER in activeModifiers && laserGauge >= LASER_CHARGE_HITS) {
            laserGauge = 0
            laserColumnStrike(brick)
        }
    }

    private fun applyOrbHit(ball: VolleyBallState, brick: Brick, hit: VolleyOrbHit) {
        if (hit.staticFieldPercent > 0f) {
            // One pulse replaces that lightning proc; secondary damage cannot roll more Orbs.
            bricks.filter(::isVisibleOrbTarget).forEach { target ->
                val pulse = percentHealthDamage(target, hit.staticFieldPercent).coerceAtLeast(1)
                damageBrick(target, pulse, VolleyDamageSource.CHAIN, mutableSetOf(), sourceBallId = ball.id)
            }
            emit(VolleyEvent.Vfx(VolleyVfxKind.BEAM, centerX(brick), centerY(brick)))
            notifyOrbActivation("STATIC FIELD")
        }
        if (hit.splashDamage > 0) {
            val radiusScale = 1f + hit.splashRadiusBonus * .5f
            splashDamage(brick, hit.splashDamage, VolleyDamageSource.EXPLOSION, mutableSetOf(),
                radiusScale)
            val radiusX = brick.bounds.width * 1.55f * radiusScale
            val radiusY = brick.bounds.height * 1.65f * radiusScale
            balls.filter { candidate ->
                candidate.alive && candidate.id !in ignitedBallIds &&
                    abs(candidate.position.x - centerX(brick)) <= radiusX &&
                    abs(candidate.position.y - centerY(brick)) <= radiusY
            }.forEach { candidate ->
                if (orbEffects.rollsIgnition()) {
                    ignitedBallIds += candidate.id
                    emit(VolleyEvent.Vfx(VolleyVfxKind.FIRE, candidate.position.x, candidate.position.y))
                }
            }
            emit(VolleyEvent.Vfx(VolleyVfxKind.SMOKE, centerX(brick), centerY(brick)))
            notifyOrbActivation("EXPLOSIVE ORB")
        }
        if (hit.lightningDamage > 0 || hit.hammerDamage > 0) {
            val target = bricks.asSequence().filter { it !== brick && isVisibleOrbTarget(it) }
                .minByOrNull { distanceSquared(centerX(brick), centerY(brick), centerX(it), centerY(it)) }
            if (target != null) {
                damageBrick(target, hit.lightningDamage + hit.hammerDamage, VolleyDamageSource.CHAIN, mutableSetOf(), sourceBallId = ball.id)
                emit(VolleyEvent.Vfx(VolleyVfxKind.BEAM, centerX(target), centerY(target)))
                notifyOrbActivation(if (hit.hammerDamage > 0) "HAMMER STRIKE" else "LIGHTNING STRIKE")
                // Legendary hammers pierce at 50%, Ancient at 100%; each target
                // gets one secondary strike, capped to the visible board.
                val struck = mutableSetOf(brick.id, target.id)
                var previous: Brick = target
                while (hit.hammerDamage > 0 && hit.hammerPierceChance > 0f && struck.size < 9 &&
                    orbEffects.rollsHammerPierce(hit.hammerPierceChance)) {
                    val next = bricks.asSequence().filter { it.id !in struck && isVisibleOrbTarget(it) }
                        .minByOrNull { distanceSquared(centerX(previous), centerY(previous),
                            centerX(it), centerY(it)) } ?: break
                    struck += next.id
                    damageBrick(next, hit.hammerDamage, VolleyDamageSource.CHAIN, mutableSetOf(), sourceBallId = ball.id)
                    emit(VolleyEvent.Vfx(VolleyVfxKind.BEAM, centerX(next), centerY(next)))
                    previous = next
                }
            }
        }
        if (hit.reaperDamage > 0) {
            // Source Reaper performs two strikes on the touched enemy. Once that
            // brick is gone, its next visible neighbour receives the cast.
            val target = if (brick.health > 0) brick else bricks.asSequence()
                .filter { it !== brick && isVisibleOrbTarget(it) }
                .minByOrNull { distanceSquared(centerX(brick), centerY(brick), centerX(it), centerY(it)) }
            if (target != null) {
                val wasAlive = target.health > 0
                damageBrick(target, hit.reaperDamage, VolleyDamageSource.CHAIN, mutableSetOf(), sourceBallId = ball.id)
                emit(VolleyEvent.Vfx(VolleyVfxKind.SMOKE, centerX(target), centerY(target)))
                notifyOrbActivation("REAPER STRIKE")
                if (hit.reaperWrathDuration > 0f && (wasAlive && target.health <= 0 ||
                    orbEffects.rollsReaperWrath(hit.reaperWrathChance))) {
                    orbWrath += VolleyWrathSnapshot(centerX(target), centerY(target),
                        (hit.reaperDamage / 2).coerceAtLeast(1), hit.reaperWrathDuration,
                        0f, hit.reaperWrathInterval)
                    emit(VolleyEvent.Vfx(VolleyVfxKind.SMOKE, centerX(target), centerY(target)))
                }
            }
        }
        // Recovered source marks an enemy poisoned once; later hits keep its active tick unchanged.
        if (hit.poisonDamage > 0 && brick.health > 0 &&
            (!referenceRulesEnabled || orbPoison[brick.id] == null)) {
            // Repeated procs keep the stronger tick without postponing its next due time.
            val previous = orbPoison[brick.id]
            val interval = min(previous?.interval ?: hit.poisonInterval, hit.poisonInterval).coerceAtLeast(.1f)
            orbPoison[brick.id] = VolleyPoisonSnapshot(
                damage = max(previous?.damage ?: 0, hit.poisonDamage), interval = interval,
                remaining = min(previous?.remaining ?: interval, interval), ticks = Int.MAX_VALUE)
            emit(VolleyEvent.Vfx(VolleyVfxKind.FIRE, centerX(brick), centerY(brick)))
            if (previous == null) notifyOrbActivation("POISON APPLIED")
        }
        if (hit.splitDamage > 0) {
            val targets = bricks.asSequence().filter { it !== brick && isVisibleOrbTarget(it) }
                .sortedBy { distanceSquared(centerX(brick), centerY(brick), centerX(it), centerY(it)) }
                .take(3).toList()
            val pathDamage = ceil(hit.splitDamage * hit.splitPathPercent / 100f).toInt()
            val pathDamaged = mutableSetOf<Int>()
            val start = Vector2(centerX(brick), centerY(brick))
            targets.forEach { target ->
                if (pathDamage > 0) {
                    val end = Vector2(centerX(target), centerY(target))
                    bricks.filter { candidate ->
                        candidate !== brick && candidate !in targets && candidate.id !in pathDamaged &&
                            isVisibleOrbTarget(candidate) && volleySplitPathCrossesBrick(start, end, candidate)
                    }.forEach { candidate ->
                        pathDamaged += candidate.id
                        damageBrick(candidate, pathDamage, VolleyDamageSource.CHAIN, mutableSetOf(), sourceBallId = ball.id)
                        emit(VolleyEvent.Vfx(VolleyVfxKind.SPARK, centerX(candidate), centerY(candidate)))
                    }
                }
                if (target.health > 0) {
                    damageBrick(target, hit.splitDamage, VolleyDamageSource.CHAIN, mutableSetOf(), sourceBallId = ball.id)
                    emit(VolleyEvent.Vfx(VolleyVfxKind.SPARK, centerX(target), centerY(target)))
                }
            }
            if (targets.isNotEmpty()) notifyOrbActivation("SPLIT STRIKE")
        }
    }

    private fun notifyOrbActivation(text: String) {
        if (orbFeedbackCooldown > 0f) return
        orbFeedbackCooldown = .9f
        emit(VolleyEvent.Feedback(text, .75f))
        emit(VolleyEvent.Sound("glass_hit", .8f))
    }

    private fun isVisibleOrbTarget(brick: Brick): Boolean =
        volleyOrbTargetVisible(brick, boardTop, RETURN_Y)

    private fun updateOrbPoison(dt: Float) {
        val updates = mutableMapOf<Int, VolleyPoisonSnapshot>()
        // FETID's current-health percentage belongs to its on-death explosion below.
        // Only the legacy PUTRID relic adds current-health damage to regular poison ticks.
        val currentHealthPercent = orbFor("PUTRID")?.stat("poison_damage_percent") ?: 0f
        orbPoison.forEach { (id, state) ->
            val brick = bricks.firstOrNull { it.id == id && it.health > 0 } ?: return@forEach
            val remaining = state.remaining - dt
            if (remaining > 0f) updates[id] = state.copy(remaining = remaining)
            else {
                val percentDamage = percentHealthDamage(brick, currentHealthPercent)
                val healthBeforeTick = brick.health
                damageBrick(brick, state.damage + percentDamage, VolleyDamageSource.FIRE_SPLASH, mutableSetOf())
                if (brick.health < healthBeforeTick) notifyOrbActivation("POISON DAMAGE")
                if (state.ticks > 1 && brick.health > 0) updates[id] = state.copy(remaining = state.interval, ticks = state.ticks - 1)
            }
        }
        orbPoison.clear()
        orbPoison.putAll(updates)
    }

    /** Source Reaper's Wrath lasts three seconds and pulses once per revolution. */
    private fun updateOrbWrath(dt: Float) {
        if (orbWrath.isEmpty() || levelComplete) return
        val radius = 26.249208f * TARGET_SOURCE_BRICK_PITCH / SOURCE_BRICK_PITCH
        val radiusSq = radius * radius
        val updates = mutableListOf<VolleyWrathSnapshot>()
        orbWrath.forEach { wrath ->
            var cooldown = wrath.cooldown - dt
            val remaining = wrath.remaining - dt
            if (remaining > 0f && cooldown <= 0f) {
                bricks.filter { target -> isVisibleOrbTarget(target) &&
                    distanceSquared(wrath.x, wrath.y, centerX(target), centerY(target)) <= radiusSq
                }.forEach { target ->
                    damageBrick(target, wrath.damage, VolleyDamageSource.CHAIN, mutableSetOf())
                    emit(VolleyEvent.Vfx(VolleyVfxKind.SMOKE, centerX(target), centerY(target)))
                }
                cooldown += wrath.interval.coerceAtLeast(.1f)
            }
            if (remaining > 0f) updates += wrath.copy(remaining = remaining, cooldown = cooldown)
        }
        orbWrath.clear()
        orbWrath.addAll(updates)
    }

    private fun damageBrick(
        brick: Brick,
        amount: Int,
        source: VolleyDamageSource,
        visited: MutableSet<Int>,
        sourceBrickId: Int? = null,
        sourceBallId: Int? = null
    ) {
        damageQueue.addLast(VolleyDamageEvent(nextDamageEventId++, source, amount, brick.id, sourceBrickId, sourceBallId, visited))
        if (processingDamageQueue) return
        processingDamageQueue = true
        try {
            while (damageQueue.isNotEmpty()) applyDamageEvent(damageQueue.removeFirst())
        } finally {
            processingDamageQueue = false
        }
    }

    private fun applyDamageEvent(event: VolleyDamageEvent) {
        val brick = bricks.firstOrNull { it.id == event.targetBrickId } ?: return
        val amount = event.amount
        val source = event.source
        val visited = event.visitedBrickIds
        if (amount <= 0 || brick.health <= 0 || !brick.type.breakable || !visited.add(brick.id)) return
        // Applies to ORB chains/explosions as well, not only direct ball hits.
        // No damage is permitted when Godot's wooden/rust door is closed.
        if (isSourceAlternatingBlocker(brick) && !isSourceAlternatingBlockerOpen()) {
            emit(VolleyEvent.Impact(centerX(brick), centerY(brick), false))
            return
        }
        val openingBreaker = source == VolleyDamageSource.OPENING_BREAKER && brick.type != BrickType.BOSS_CORE
        if (brick.type == BrickType.LOCKED && brick.locked && !openingBreaker) return
        if (brick.id in frozenBricks && !openingBreaker) {
            if (source == VolleyDamageSource.FIRE_SPLASH || source == VolleyDamageSource.EXPLOSION) {
                frozenBricks.remove(brick.id)
                emit(VolleyEvent.Vfx(VolleyVfxKind.SPARK, centerX(brick), centerY(brick)))
            } else {
                emit(VolleyEvent.Impact(centerX(brick), centerY(brick), false))
                return
            }
        }
        val protection = protectedBricks[brick.id] ?: 0
        if (protection > 0 && !openingBreaker) {
            protectedBricks[brick.id] = protection - 1
            emit(VolleyEvent.Impact(centerX(brick), centerY(brick), false))
            return
        }

        // Bosses are encounters rather than giant HP bricks. Authored guard blocks expose the
        // core only after they are defeated; one-hit/piercing effects therefore cannot skip phase 1.
        if (brick.type == BrickType.BOSS_CORE && stage.bossStage && bricks.any { it.volleyShieldLock && it.health > 0 }) {
            emit(VolleyEvent.Impact(centerX(brick), centerY(brick), false))
            emit(VolleyEvent.Feedback("BREAK THE CORE GUARDS", .42f))
            emit(VolleyEvent.Vfx(VolleyVfxKind.BEAM, centerX(brick), centerY(brick)))
            return
        }

        val groupShield = bricks.firstOrNull {
            it.volleyShieldLock && it.groupId > 0 && it.groupId == brick.groupId && it.health > 0 && it.id != brick.id
        }
        if (groupShield != null && !openingBreaker) {
            groupShield.health = 0
            emit(VolleyEvent.Impact(centerX(groupShield), centerY(groupShield), true))
            emit(VolleyEvent.Debris(centerX(groupShield), centerY(groupShield)))
            emit(VolleyEvent.Feedback("GROUP SHIELD ABSORBED HIT", .7f))
            checkLevelComplete()
            return
        }
        // These source enemies count impacts, not damage points, including three-hit OSB bricks.
        val hitCounter = sourceKind(brick) in IMPORTED_HIT_COUNTER_KINDS
        // Prevent an untouched boss from being deleted by one ability hit. Its final regular
        // ball hit must still be allowed to finish it, including with imported scaled attack.
        val freshBossOneShot = brick.type == BrickType.BOSS_CORE &&
            brick.health == brick.initialHealth && amount >= brick.health
        val applied = when {
            hitCounter && !openingBreaker -> 1
            brick.type == BrickType.BOSS_CORE && stage.sourceScene != null ->
                VolleySourceHealth.limitBossDamage(amount, brick.initialHealth)
            freshBossOneShot -> 1
            else -> amount
        }
        val before = brick.health
        val rawBefore = if (referenceRulesEnabled) sourceRemainingHealth[brick.id] else null
        val actual = if (rawBefore != null) {
            val rawAfter = (rawBefore - applied.toDouble()).coerceAtLeast(0.0)
            sourceRemainingHealth[brick.id] = rawAfter
            brick.health = if (rawAfter < 1.0) 0 else kotlin.math.ceil(rawAfter).toInt()
            minOf(applied.toDouble(), rawBefore).toInt()
        } else {
            brick.health = (brick.health - applied).coerceAtLeast(0)
            before - brick.health
        }
        // A charged one-shot can remove millions of HP, but it must not mint millions of score.
        val scoringDamage = if (openingBreaker) min(actual, 30) else actual
        combo = (combo.toLong() + scoringDamage).coerceAtMost(Int.MAX_VALUE.toLong()).toInt()
        score = (score.toLong() + scoringDamage.toLong() * (10 + min(combo, 25)))
            .coerceAtMost(Int.MAX_VALUE.toLong()).toInt()
        emit(VolleyEvent.Impact(centerX(brick), centerY(brick), brick.health == 0))
        emit(VolleyEvent.BrickHit(brick.id, actual))
        if (brick.health == 0 && before > 0) onBrickDestroyed(brick, source, visited)
    }

    private fun onBrickDestroyed(brick: Brick, source: VolleyDamageSource, visited: MutableSet<Int>) {
        val sourceSeed = stage.authoredBricks.getOrNull(brick.id - 1)
        if (referenceRulesEnabled && (sourceSeed?.sourceKind in setOf(
                "GRASS", "STONE", "SAND", "ICE", "BLACK", "BURN", "ENEMY", "BOSS", "BOSS_GUARD",
                "DYNAMITE", "SUICIDE", "OSB", "ICE_FREEZE", "PROTECTOR", "SLIDER") ||
                (sourceSeed?.sourceKind == "BLOCKER" && sourceSeed.sourceBaseHealth != null))) {
            combatKillSequence++
            emit(VolleyEvent.CombatExperience(sourceCombatRunId, combatKillSequence,
                stage.levelId, difficulty, sourceCombatPlayerLevel))
        }
        when (sourceSeed?.sourceKind) {
            "BONUS_DAMAGE" -> {
                val percent = sourceSeed.sourceBonusValue.coerceAtLeast(1)
                bonusDamageFlat += if (referenceRulesEnabled)
                    max(1, kotlin.math.ceil(referenceBaseDamage.toDouble() * percent / 100.0).toInt())
                    else max(1, ((1 + bonusDamageFlat) * percent / 100f).roundToInt())
                emit(VolleyEvent.Feedback("DAMAGE +$percent%", .9f))
            }
            else -> Unit
        }
        volleyBreakCounter++
        emit(VolleyEvent.BrickBroken(brick.id))
        emit(VolleyEvent.Debris(centerX(brick), centerY(brick)))
        var breakScore = 90 + min(brick.initialHealth, 80) * 4
        if (!referenceRulesEnabled && (source == VolleyDamageSource.BALL || source == VolleyDamageSource.OPENING_BREAKER)) {
            breakScore += (breakScore * ballAbility.masteryScorePercent / 100f).roundToInt()
        }
        if (!referenceRulesEnabled && ballAbility.kind == BallAbilityKind.IMPACT_CORE) {
            breakScore += (breakScore * ballAbility.scoreBonusPercent / 100f).roundToInt()
        }
        if (!referenceRulesEnabled && ballAbility.kind == BallAbilityKind.FORTUNE_CORE && ballAbility.fortuneEveryBreaks > 0 && volleyBreakCounter % ballAbility.fortuneEveryBreaks == 0) {
            breakScore += ballAbility.fortuneBonusScore
            emit(VolleyEvent.Feedback("FORTUNE +${ballAbility.fortuneBonusScore}", .7f))
        }
        score = (score.toLong() + (breakScore * orbEffects.rewardMultiplier()).roundToInt())
            .coerceAtMost(Int.MAX_VALUE.toLong()).toInt()
        val dropDifficulty = when (difficulty) {
            VolleyDifficulty.NORMAL -> 1f
            VolleyDifficulty.HARD -> 1.1f
            VolleyDifficulty.EXTREME -> 1.2f
        }
        if (stage.sourceScene != null && sourceSeed?.sourceKind in ORB_DROP_MATERIALS &&
            orbEffects.rollsExtraOrbPoint(dropDifficulty)) {
            emit(VolleyEvent.GrantRandomItem(brick.id, turn, naturalDrop = true,
                orbRank = orbEffects.rollNaturalOrbRank()))
        }
        val readyOrb = orbFor("READY")
        if (readyOrb != null && readyOrb.rank >= 5 && !orbMultishotUsed &&
            random.nextFloat() * 100f < readyOrb.stat("ancient_multishot_chance")) {
            orbMultishotUsed = true
            earnedBallsThisTurn += 20
            emit(VolleyEvent.Feedback("MULTISHOT +20 BALLS", 1f))
        }
        val fetidOrb = orbFor("FETID")
        if (fetidOrb != null && orbPoison.containsKey(brick.id) &&
            random.nextFloat() * 100f < fetidOrb.stat("ancient_poison_explosion_chance")) {
            splashPercentCurrentHealth(brick, fetidOrb.stat("poison_damage_percent"),
                VolleyDamageSource.EXPLOSION, visited)
            emit(VolleyEvent.Vfx(VolleyVfxKind.SMOKE, centerX(brick), centerY(brick)))
        }
        when (brick.type) {
            BrickType.POWERUP_CARRIER -> if (!brick.rewardGranted) {
                brick.rewardGranted = true
                val table = stage.carrierDropTable.ifEmpty { listOf(VolleyCarrierReward.BALL) }
                when (table[Math.floorMod(brick.id * 31 + turn, table.size)]) {
                    VolleyCarrierReward.BALL -> {
                        val amount = if (sourceSeed?.sourceKind == "BONUS_BALLS") sourceSeed.sourceBonusValue.coerceAtLeast(1) else 1
                        earnedBallsThisTurn += amount
                        emit(VolleyEvent.Feedback("+$amount BALLS NEXT VOLLEY"))
                    }

                    VolleyCarrierReward.RANDOM_POSITIVE_ITEM -> {
                        emit(VolleyEvent.GrantRandomItem(brick.id, turn))
                        emit(VolleyEvent.Feedback("ITEM FOUND"))
                    }
                }
            }

            BrickType.RANDOM_INVENTORY_POWERUP -> if (!brick.rewardGranted) {
                brick.rewardGranted = true
                emit(VolleyEvent.GrantRandomItem(brick.id, turn))
            }

            BrickType.KEY_BRICK -> unlockGroup(brick.groupId)

            BrickType.CHAIN_BRICK -> propagateChain(brick, visited)

            BrickType.EXPLOSIVE -> when (sourceSeed?.sourceKind) {
                "DYNAMITE" -> destroyConnectedBombBlockers(sourceSeed.groupId)
                "SUICIDE" -> destroyFourWayTargets(brick, visited)
                else -> splashDamage(brick, 1, VolleyDamageSource.EXPLOSION, visited)
            }

            BrickType.BOSS_CORE -> {
                if (sourceSeed?.sourceKind == "BOSS" && !brick.rewardGranted) {
                    brick.rewardGranted = true
                    emit(VolleyEvent.GrantRandomItem(brick.id, 0,
                        orbRank = if (stage.levelId >= 160) 3 else 2))
                }
                emit(VolleyEvent.Feedback("CORE DESTROYED", 1.1f))
            }

            else -> Unit
        }
        if (stage.bossStage && brick.volleyShieldLock) {
            val remainingGuards = bricks.count { it.volleyShieldLock && it.health > 0 }
            if (remainingGuards == 0) {
                emit(VolleyEvent.Feedback("CORE EXPOSED", 1.05f))
                bricks.firstOrNull { it.type == BrickType.BOSS_CORE && it.health > 0 }?.let { core ->
                    emit(VolleyEvent.Vfx(VolleyVfxKind.BEAM, centerX(core), centerY(core)))
                }
            } else {
                emit(VolleyEvent.Feedback("CORE LOCKS LEFT: $remainingGuards", .85f))
            }
        }
        // Ordinary impacts use the light pitch-rising pop emitted from BrickHit. Heavy materials
        // retain their own cue, and explosive bricks still produce a real explosion.
        if (brick.type in setOf(
                BrickType.ARMORED_TWO_HIT,
                BrickType.ARMORED_THREE_HIT,
                BrickType.ARMORED_DARK,
                BrickType.ELECTRIC_WHITE,
                BrickType.LOCKED,
                BrickType.CHAIN_BRICK,
                BrickType.BOSS_CORE
            )
        ) {
            emit(VolleyEvent.Sound("metal_hit"))
        }
        if (brick.type == BrickType.EXPLOSIVE) {
            emit(VolleyEvent.Sound("explosion"))
            emit(VolleyEvent.Vfx(VolleyVfxKind.SMOKE, centerX(brick), centerY(brick)))
        }
        emit(VolleyEvent.Haptic(if (brick.type == BrickType.BOSS_CORE) 28 else 12))
        checkLevelComplete()
    }

    private fun splashDamage(source: Brick, amount: Int, damageSource: VolleyDamageSource,
                             visited: MutableSet<Int>, radiusScale: Float = 1f) {
        val rangeX = source.bounds.width * 1.55f * radiusScale
        val rangeY = source.bounds.height * 1.65f * radiusScale
        bricks.filter {
            it !== source && isVisibleOrbTarget(it) &&
                abs(centerX(it) - centerX(source)) <= rangeX && abs(centerY(it) - centerY(source)) <= rangeY
        }.forEach { damageBrick(it, amount, damageSource, visited, sourceBrickId = source.id) }
    }

    /** Source dynamite removes its numbered, normally indestructible stone group. */
    private fun destroyConnectedBombBlockers(group: Int) {
        bricks.forEach { target ->
            val seed = stage.authoredBricks.getOrNull(target.id - 1)
            if (target.health <= 0 || seed?.bombDestroyable != true || seed.connectedBombGroup != group) return@forEach
            target.health = 0
            emit(VolleyEvent.Impact(centerX(target), centerY(target), true))
            emit(VolleyEvent.BrickBroken(target.id))
            emit(VolleyEvent.Debris(centerX(target), centerY(target)))
        }
    }

    /** Source suicide sends lethal beams along its row and column, across the board. */
    private fun destroyFourWayTargets(source: Brick, visited: MutableSet<Int>) {
        val sourceX = centerX(source)
        val sourceY = centerY(source)
        val sourceScale = stage.descentDistance / SOURCE_BRICK_PITCH
        val beamHalfWidth = 43.9023f * sourceScale / 2f
        val beamReach = 1300f * sourceScale
        bricks.forEach { target ->
            val dx = abs(centerX(target) - sourceX)
            val dy = abs(centerY(target) - sourceY)
            val inBeam = (dx <= beamHalfWidth + target.bounds.width / 2f && dy <= beamReach + target.bounds.height / 2f) ||
                (dy <= beamHalfWidth + target.bounds.height / 2f && dx <= beamReach + target.bounds.width / 2f)
            if (target === source || target.health <= 0 || !target.type.breakable ||
                target.type == BrickType.BOSS_CORE || !inBeam ||
                !visited.add(target.id)) return@forEach
            target.health = 0
            emit(VolleyEvent.Impact(centerX(target), centerY(target), true))
            emit(VolleyEvent.BrickHit(target.id, target.initialHealth))
            onBrickDestroyed(target, VolleyDamageSource.EXPLOSION, visited)
        }
    }

    private fun splashPercentCurrentHealth(source: Brick, percent: Float,
                                           damageSource: VolleyDamageSource, visited: MutableSet<Int>) {
        if (percent <= 0f) return
        val rangeX = source.bounds.width * 1.55f
        val rangeY = source.bounds.height * 1.65f
        bricks.filter { target ->
            target !== source && isVisibleOrbTarget(target) &&
                abs(centerX(target) - centerX(source)) <= rangeX &&
                abs(centerY(target) - centerY(source)) <= rangeY
        }.forEach { target ->
            val damage = percentHealthDamage(target, percent).coerceAtLeast(1)
            damageBrick(target, damage, damageSource, visited, sourceBrickId = source.id)
        }
    }

    private fun clearFireStatus(brick: Brick): Boolean {
        val thawed = frozenBricks.remove(brick.id)
        val unprotected = protectedBricks.remove(brick.id) != null
        return thawed || unprotected
    }

    private fun propagateChain(source: Brick, visited: MutableSet<Int>) {
        if (source.groupId <= 0) return
        bricks.filter { it !== source && it.type == BrickType.CHAIN_BRICK && it.groupId == source.groupId && it.health > 0 }
            .forEach { damageBrick(it, 1, VolleyDamageSource.CHAIN, visited, sourceBrickId = source.id) }
    }

    private fun unlockGroup(group: Int) {
        if (group <= 0) return
        bricks.filter { it.type == BrickType.LOCKED && it.groupId == group && it.health > 0 }.forEach { it.locked = false }
        emit(VolleyEvent.Feedback("LOCK GROUP $group OPEN", .9f))
    }

    private fun toggleSwitchGroup(group: Int) {
        if (group <= 0) return
        bricks.filter { it.type == BrickType.GHOST && it.groupId == group && it.health > 0 }
            .forEach { it.ghostVisible = !it.ghostVisible }
        bricks.filter { it.type == BrickType.LOCKED && it.groupId == group && it.health > 0 }
            .forEach { it.locked = !it.locked }
        emit(VolleyEvent.Feedback("SWITCH GROUP $group TOGGLED", .8f))
    }

    private fun laserColumnStrike(source: Brick) {
        val visited = mutableSetOf<Int>()
        bricks.filter {
            it.type.breakable && it.health > 0 && abs(centerX(it) - centerX(source)) <= source.bounds.width * .55f
        }.forEach { damageBrick(it, 1, VolleyDamageSource.LASER, visited, sourceBrickId = source.id) }
        emit(VolleyEvent.Feedback("LASER COLUMN", .65f))
        emit(VolleyEvent.Sound("laser_fire"))
        emit(VolleyEvent.Vfx(VolleyVfxKind.BEAM, centerX(source), centerY(source)))
    }

    private fun teleportBall(ball: VolleyBallState, portal: Brick) {
        if (ball.portalCooldown > 0f) return
        val paired = bricks.firstOrNull {
            it !== portal && it.type == BrickType.BLACK_HOLE_TELEPORTER && it.health > 0 &&
                (portal.groupId <= 0 || it.groupId == portal.groupId)
        }
        if (paired != null) {
            ball.position.set(centerX(paired), centerY(paired))
        } else {
            val toRight = centerX(portal) < (BOARD_LEFT + BOARD_RIGHT) * .5f
            ball.position.set(if (toRight) BOARD_RIGHT - 54f else BOARD_LEFT + 54f, (portal.bounds.y - 92f).coerceIn(DANGER_Y + 36f, boardTop - 90f))
        }
        ball.velocity.rotateDeg(if (ball.position.x > centerX(portal)) -31f else 31f)
        clampBallSpeed(ball)
        ball.portalCooldown = .18f
        emit(VolleyEvent.Feedback("VOID PORTAL", .4f))
        emit(VolleyEvent.Sound("powerup"))
        emit(VolleyEvent.Vfx(VolleyVfxKind.PORTAL, ball.position.x, ball.position.y))
    }

    private fun collectPickups(ball: VolleyBallState) {
        pickups.firstOrNull { pickup ->
            pickup.active && ball.position.dst2(pickup.position) <= (ball.radius + 19f) * (ball.radius + 19f)
        }?.let(::applyNodeReward)
    }

    private fun applyNodeReward(pickup: VolleyBonusNodeState) {
        if (!pickup.active) return
        pickup.active = false
        when (pickup.type) {
            VolleyBonusNodeType.BALL -> {
                earnedBallsThisTurn += pickup.amount
                score += 120 * pickup.amount
                emit(VolleyEvent.Feedback("+${pickup.amount} BALL${if (pickup.amount == 1) "" else "S"} NEXT VOLLEY", .9f))
            }

            VolleyBonusNodeType.CROWN -> {
                score += max(1, pickup.scoreValue)
                emit(VolleyEvent.Feedback("CROWN +${max(1, pickup.scoreValue)}", .9f))
            }

            VolleyBonusNodeType.GEMS, VolleyBonusNodeType.COINS -> {
                // Even restored legacy gem pickups pay coins under the new economy.
                val coins = pickup.amount.coerceAtLeast(1) * 100
                emit(VolleyEvent.GrantCoins(pickup.id, coins))
                emit(VolleyEvent.Feedback("+$coins COINS", .9f))
            }

            VolleyBonusNodeType.SHIELD -> {
                rescueCharges = (rescueCharges + pickup.amount.coerceAtLeast(1)).coerceAtMost(3)
                emit(VolleyEvent.Feedback("RESCUE WARD +${pickup.amount.coerceAtLeast(1)}", .9f))
            }

            VolleyBonusNodeType.MYSTERY -> {
                emit(VolleyEvent.GrantRandomItem(pickup.id, turn))
                emit(VolleyEvent.Feedback("POSITIVE ITEM FOUND", .9f))
            }

            VolleyBonusNodeType.STAR -> {
                bonusStarProgress += pickup.amount.coerceAtLeast(1)
                score += max(1, pickup.scoreValue)
                emit(VolleyEvent.Feedback("STAR ENERGY +${pickup.amount.coerceAtLeast(1)}", .9f))
            }

            VolleyBonusNodeType.SPEED -> {
                activeModifiers.remove(VolleyModifier.SLOW)
                activeModifiers += VolleyModifier.FAST
                balls.filter { it.alive }.forEach {
                    it.velocity.scl(1.10f)
                    clampBallSpeed(it)
                }
                emit(VolleyEvent.Feedback("SPEED VOLLEY", .9f))
            }
        }
        emit(VolleyEvent.PickupCollected(pickup.id, pickup.amount))
        emit(VolleyEvent.BonusNodeCollected(pickup.id, pickup.type, pickup.amount))
        emit(VolleyEvent.Sound("powerup"))
    }

    private fun finishVolley() {
        if (!firing) return
        resetFastForward()
        ignitedBallIds.clear()
        phase = VolleyPhase.RESOLVING_EFFECTS
        emit(VolleyEvent.VolleyComplete)
        pendingReturnX = firstReturnX
        firstReturnX = null
        resolveEndOfVolley()
    }

    private fun resolveEndOfVolley() {
        applySlayingOrb()
        applyImportedTurnSpecials()
        regenerateBricks()
        moveBricks()
        if (earnedBallsThisTurn > 0) {
            ballCount = (ballCount.toLong() + earnedBallsThisTurn).coerceIn(1L, maxBallCount.toLong()).toInt()
            earnedBallsThisTurn = 0
        }
        if (levelComplete) return
        phase = VolleyPhase.BOARD_DESCENT
        descentRemaining = stage.descentDistance
        emit(VolleyEvent.WallDescending(stage.descentDistance))
        emit(VolleyEvent.Feedback("WALL DESCENDING", .55f))
    }

    private fun applySlayingOrb() {
        val orb = orbFor("SLAYING") ?: return
        val threshold = orb.stat("slaying_percent")
        if (threshold <= 0f) return
        val targets = bricks.filter {
            it.health > 0 && it.type.breakable && it.type != BrickType.BOSS_CORE &&
                sourceKind(it) !in IMPORTED_HIT_COUNTER_KINDS
        }
        val ancientCount = orb.stat("ancient_slay_amount").toInt()
        targets.filter { target ->
            target.health < target.initialHealth * threshold / 100f ||
                (ancientCount > 0 && targets.size <= ancientCount)
        }.forEach { target ->
            damageBrick(target, target.health, VolleyDamageSource.BOSS, mutableSetOf())
        }
    }

    private fun applyImportedTurnSpecials() {
        bricks.filter { it.health > 0 && it.bounds.y < boardTop &&
            it.bounds.y + it.bounds.height > RETURN_Y }.forEach { caster ->
            when (sourceKind(caster)) {
                "PROTECTOR" -> bricks.filter { target ->
                    target.health > 0 && sourceKind(target) == "BLACK" &&
                        turnSpellTouches(caster, target)
                }.forEach { target ->
                    val amount = stage.authoredBricks.getOrNull(caster.id - 1)?.sourceProtectionPerRound ?: 2
                    protectedBricks[target.id] = ((protectedBricks[target.id] ?: 0).toLong() + amount)
                        .coerceAtMost(Int.MAX_VALUE.toLong()).toInt()
                }
                "ICE_FREEZE" -> bricks.filter { target ->
                    target.health > 0 && sourceKind(target) == "ICE" &&
                        turnSpellTouches(caster, target)
                }.forEach { frozenBricks += it.id }
                "ROTATING" -> {
                    val current = ((brickRotation(caster.id) % 360f) + 360f) % 360f
                    val candidates = (0 until 12).map { it * 30f }.filter {
                        abs(it - current) > .01f && abs(abs(it - current) - 180f) > .01f
                    }
                    rotatingStartAngles[caster.id] = current
                    rotatingTargetAngles[caster.id] = candidates[random.nextInt(candidates.size)]
                }
                else -> Unit
            }
        }
    }

    /** An expanding circular hitbox intersects the target's logical brick rectangle. */
    private fun turnSpellTouches(caster: Brick, target: Brick): Boolean {
        val x = centerX(caster)
        val y = centerY(caster)
        val nearestX = x.coerceIn(target.bounds.x, target.bounds.x + target.bounds.width)
        val nearestY = y.coerceIn(target.bounds.y, target.bounds.y + target.bounds.height)
        val dx = x - nearestX
        val dy = y - nearestY
        return dx * dx + dy * dy <= TURN_SPELL_RADIUS * TURN_SPELL_RADIUS
    }

    private fun sourceKind(brick: Brick): String? = stage.authoredBricks.getOrNull(brick.id - 1)?.sourceKind

    private fun isSourceAlternatingBlocker(brick: Brick): Boolean =
        stage.sourceScene != null && stage.authoredBricks.getOrNull(brick.id - 1)
            ?.sourceScene?.let(RpgAlternatingBlockerRules::isSourceGate) == true

    /**
     * Recovered RPG: Alt_Blocker.is_closed initially true, _ready() opens it;
     * Enemies.gd toggles it once per row descent. This run starts at turn 1
     * and increments turn after descent, including across v2 snapshot restore.
     * Derive this state from persisted turn; no new save field or ID migration.
     */
    private fun isSourceAlternatingBlockerOpen(): Boolean = RpgAlternatingBlockerRules.openAtTurn(turn)

    /** Source gate state for a visible mobile hint or regression test. */
    fun sourceAlternatingBlockerOpen(brickId: Int): Boolean? =
        bricks.firstOrNull { it.id == brickId }?.takeIf(::isSourceAlternatingBlocker)
            ?.let { isSourceAlternatingBlockerOpen() }

    private fun applyInitialImportedFreeze() {
        if (initialImportedFreezeApplied) return
        initialImportedFreezeApplied = true
        bricks.filter { it.health > 0 && sourceKind(it) == "ICE_FREEZE" &&
            it.bounds.y < boardTop && it.bounds.y + it.bounds.height > RETURN_Y }.forEach { caster ->
            bricks.filter { it.health > 0 && sourceKind(it) == "ICE" && turnSpellTouches(caster, it) }
                .forEach { frozenBricks += it.id }
        }
    }

    /** Torch waits three idle seconds, then slides for one second within its authored range. */
    private fun updateTorchMotion(dt: Float) {
        bricks.filter { it.health > 0 && sourceKind(it) == "TORCH" }.forEach { brick ->
            val seed = stage.authoredBricks.getOrNull(brick.id - 1) ?: return@forEach
            val distance = if (seed.slideHorizontal != 0) seed.slideHorizontal else seed.slideVertical
            if (distance == 0) return@forEach
            val before = torchMotion[brick.id] ?: VolleyTorchMotionSnapshot(towardEnd = seed.slideStartsMoved)
            var next = before
            if (before.progress < 1f) {
                val progress = (before.progress + dt).coerceAtMost(1f)
                val eased = if (progress < .5f) 2f * progress * progress else
                    1f - 2f * (1f - progress) * (1f - progress)
                next = before.copy(progress = progress, offset = before.from + (before.target - before.from) * eased)
            } else if (!firing && balls.none { it.alive }) {
                val idle = before.idle + dt
                next = before.copy(idle = idle)
                if (idle >= 3f) {
                    val destinations = (minOf(0, distance)..maxOf(0, distance)).map { it.toFloat() }
                        .filter { abs(it - before.offset) > .01f }
                    val direction = if (distance > 0) 1f else -1f
                    val preferred = destinations.filter {
                        (it - before.offset) * direction * (if (before.towardEnd) 1f else -1f) > 0f
                    }.ifEmpty { destinations }
                    if (preferred.isNotEmpty()) next = before.copy(from = before.offset,
                        target = preferred[random.nextInt(preferred.size)], progress = 0f,
                        idle = 0f, towardEnd = !before.towardEnd)
                }
            }
            val motion = (next.offset - before.offset) * TARGET_SOURCE_BRICK_PITCH
            if (seed.slideHorizontal != 0) brick.bounds.x += motion else brick.bounds.y -= motion
            torchMotion[brick.id] = next
        }
    }

    fun vortexRemaining(ballId: Int): Float = vortexBalls[ballId] ?: 0f

    private fun regenerateBricks() {
        bricks.filter { it.type == BrickType.REGENERATING && it.health in 1 until it.initialHealth }
            .forEach { it.health = min(it.initialHealth, it.health + 1) }
    }

    private fun moveBricks() {
        if (stage.sourceScene != null) {
            bricks.filter { it.health > 0 && sourceKind(it) == "SLIDER" }.forEach { brick ->
                val seed = stage.authoredBricks.getOrNull(brick.id - 1) ?: return@forEach
                if (seed.sourceKind != "SLIDER" || (seed.slideHorizontal == 0 && seed.slideVertical == 0)) return@forEach
                if (brick.movementDirection == 0) {
                    // Some authored obstacles spend their first turn at the origin.
                    brick.movementDirection = 2
                    return@forEach
                }
                val horizontal = seed.slideHorizontal * brick.movementDirection
                val vertical = if (seed.slideHorizontal == 0) seed.slideVertical * brick.movementDirection else 0
                brick.gridCol += horizontal
                brick.gridRow += vertical
                brick.movementDirection *= -1
            }
            return
        }
        val brickWidth = (BOARD_RIGHT - BOARD_LEFT - BRICK_GAP * (stage.columns - 1)) / stage.columns
        val pitch = brickWidth + BRICK_GAP
        bricks.filter { it.health > 0 && (it.type == BrickType.MOVING_HORIZONTAL || it.type == BrickType.MOVING_VERTICAL) }
            .forEach { brick ->
                val horizontal = brick.type == BrickType.MOVING_HORIZONTAL
                var targetRow = brick.gridRow + if (horizontal) 0 else brick.movementDirection
                var targetCol = brick.gridCol + if (horizontal) brick.movementDirection else 0
                fun blocked(row: Int, col: Int): Boolean = row !in 0 until stage.rows || col !in 0 until stage.columns ||
                    bricks.any { it !== brick && it.health > 0 && it.gridRow == row && it.gridCol == col }
                if (blocked(targetRow, targetCol)) {
                    brick.movementDirection *= -1
                    targetRow = brick.gridRow + if (horizontal) 0 else brick.movementDirection
                    targetCol = brick.gridCol + if (horizontal) brick.movementDirection else 0
                }
                if (!blocked(targetRow, targetCol)) {
                    brick.gridRow = targetRow
                    brick.gridCol = targetCol
                    if (horizontal) brick.bounds.x += pitch * brick.movementDirection else brick.bounds.y -= ROW_PITCH * brick.movementDirection
                }
            }
    }

    private fun updateBoardDescent(dt: Float) {
        val speed = stage.descentDistance / if (reduceMotion) .08f else DESCENT_SECONDS
        val step = min(descentRemaining, speed * dt)
        if (stage.descentDistance > 0f) {
            val t = (1f - (descentRemaining - step) / stage.descentDistance).coerceIn(0f, 1f)
            val eased = 1f - (1f - t) * (1f - t) * (1f - t)
            rotatingTargetAngles.forEach { (id, target) ->
                val start = rotatingStartAngles[id] ?: 0f
                val delta = ((target - start + 540f) % 360f) - 180f
                rotatingAngles[id] = start + delta * eased
            }
        }
        if (stage.sourceScene != null && stage.descentDistance > 0f) {
            val before = (1f - descentRemaining / stage.descentDistance).coerceIn(0f, 1f)
            val after = (before + step / stage.descentDistance).coerceIn(0f, 1f)
            fun ease(t: Float): Float = if (t < .5f) 2f * t * t else 1f - 2f * (1f - t) * (1f - t)
            val fraction = ease(after) - ease(before)
            bricks.filter { it.health > 0 && sourceKind(it) == "SLIDER" && it.movementDirection in -1..1 }.forEach { brick ->
                val seed = stage.authoredBricks.getOrNull(brick.id - 1) ?: return@forEach
                val motion = -brick.movementDirection * stage.descentDistance * fraction
                brick.bounds.x += seed.slideHorizontal * motion
                if (seed.slideHorizontal == 0) brick.bounds.y -= seed.slideVertical * motion
            }
        }
        bricks.filter { it.health > 0 }.forEach { it.bounds.y -= step }
        rails.filter { !it.fixed }.forEach {
            it.y1 -= step
            it.y2 -= step
        }
        pickups.filter { it.active }.forEach { it.position.y -= step }
        descentRemaining -= step
        if (descentRemaining <= .001f) finalizeTurnAfterDescent()
    }

    private fun finalizeTurnAfterDescent() {
        phase = VolleyPhase.DANGER_CHECK
        bricks.filter { sourceKind(it) == "SLIDER" && it.movementDirection == 2 }
            .forEach { it.movementDirection = 1 }
        launcherX = resolvedReturnX(pendingReturnX)
        pendingReturnX = null
        // Indestructible blockers do not prevent stage clear, but they are still part of the
        // descending wall and can breach the danger line. Rails/portals/pickups remain non-danger.
        val breached = bricks.any {
            it.health > 0 && (it.countsForClear || it.type == BrickType.INDESTRUCTIBLE) && it.bounds.y <= DANGER_Y
        }
        if (breached) {
            val secondWindOrb = orbFor("SECONDW")
            val secondWindChance = when {
                secondWindOrb == null || orbSecondWindUsed -> 0f
                secondWindOrb.rank >= 4 -> 100f
                else -> 0f
            }
            val orbRescue = secondWindChance > 0f && random.nextFloat() * 100f < secondWindChance
            if (rescueCharges > 0 || orbRescue) {
                if (orbRescue) {
                    orbSecondWindUsed = true
                    if (secondWindOrb != null && secondWindOrb.rank >= 5) {
                        queuedModifiers += VolleyModifier.FIRE
                        emit(VolleyEvent.Feedback("SECOND WIND + FIRE NEXT VOLLEY", 1.4f))
                    }
                } else rescueCharges--
                bricks.filter { it.health > 0 }.forEach { it.bounds.y += stage.descentDistance }
                rails.filter { !it.fixed }.forEach {
                    it.y1 += stage.descentDistance
                    it.y2 += stage.descentDistance
                }
                pickups.filter { it.active }.forEach { it.position.y += stage.descentDistance }
                emit(VolleyEvent.Feedback(if (orbRescue) "SECOND WIND" else "RESCUE WARD SAVED THE RUN", 1.2f))
                emit(VolleyEvent.RescueUsed)
            } else {
                phase = VolleyPhase.GAME_OVER
                emit(VolleyEvent.Feedback("WALL BREACHED", 1f))
                activeModifiers.clear()
                return
            }
        }
        activeModifiers.clear()
        turn++
        phase = VolleyPhase.READY_TO_AIM
    }

    private fun resolvedReturnX(returnX: Float?): Float = when {
        VolleyModifier.STICKY_RETURN in activeModifiers -> launcherX

        VolleyModifier.MAGNETIC_RETURN in activeModifiers ->
            lerp(returnX ?: launcherX, 450f, .75f).coerceIn(BOARD_LEFT + 16f, BOARD_RIGHT - 16f)

        else -> returnX ?: launcherX
    }

    internal fun resolvedReturnXForTest(returnX: Float?): Float = resolvedReturnX(returnX)

    private fun resetFastForward() {
        fastForwardActive = false
    }

    private fun detonateImmediateBombs(): Boolean {
        val targets = bricks.filter { it.type.breakable && it.health > 0 && !(it.type == BrickType.LOCKED && it.locked) }
            .sortedBy { Math.floorMod(it.id * 31 + stage.levelId * 17 + turn, 997) }
            .take(6)
        if (targets.isEmpty()) return false
        val visited = mutableSetOf<Int>()
        targets.forEach { damageBrick(it, max(3, stage.baseHp / 4), VolleyDamageSource.EXPLOSION, visited) }
        emit(VolleyEvent.Feedback("BOMBS DETONATED", 1.05f))
        return true
    }

    private fun checkLevelComplete() {
        if (phase == VolleyPhase.LEVEL_COMPLETE) return
        if (bricks.none { it.countsForClear && it.health > 0 }) {
            phase = VolleyPhase.LEVEL_COMPLETE
            pendingLaunches = 0

            // checkLevelComplete can run from directBrickHit -> damageBrick while updateVolley is
            // iterating the list. Clearing the list there invalidates MutableList's iterator and can
            // throw ConcurrentModificationException when more than one ball is active. Retire the
            // balls non-structurally and let updateVolley purge them after its iteration completes.
            balls.forEach { it.alive = false }
            if (!processingBallUpdates) balls.removeAll { !it.alive }
        }
    }

    private fun abilityTriggered(every: Int): Boolean = every > 0 && abilityHitCounter % every == 0
    private fun centerX(brick: Brick): Float = brick.bounds.x + brick.bounds.width * .5f
    private fun centerY(brick: Brick): Float = brick.bounds.y + brick.bounds.height * .5f
    private fun distanceSquared(x1: Float, y1: Float, x2: Float, y2: Float): Float = (x1 - x2) * (x1 - x2) + (y1 - y2) * (y1 - y2)
    private fun lerp(from: Float, to: Float, progress: Float): Float = from + (to - from) * progress
    private fun emit(event: VolleyEvent) {
        events += event
    }
}
