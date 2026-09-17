/*
 * ملاحظات صيانة الملف:
 * المسار: app/src/main/java/com/example/brick_breaker_ball/GameSession.kt
 * المؤلف: mohamed alromaihi
 * دوال توسعة الويبن: `weaponPaddleActive`، `weaponPaddleBonus`، `paddleTargetWithoutWeaponBonus`، `restoreWeaponPaddleBonus`
 * الدوال الموجودة: `newLevel`، `serve`، `createBall`، `activeElement`، `applyCustomization`، `clampPaddleForActiveLayout`، `action`، `hasAttachedBalls`، `launchAttachedBalls`، `launchServe`، `launchBall`، `movePaddle`، `update`، `applyMagneticAttraction`، `consumeDeathRailZapX`، `consumeEvents`، `simulateBall`، `consider`، `randomizeBallDirection`، `teleportBall`، `reflect`، `paddleBounce`، `clampSpeed`، `speedMultiplier`، `syncBallSpeed`، `ensureVelocityBounds`، `damageBrick`، `activatePuzzleGroup`، `matches`، `updateBricks`، `moveBrickWithoutOverlap`، `applyFallingBricksStep`، `updateCapsules`، `canActivatePowerUp`، `randomCandidates`، `activatePowerUp`، `isZapTarget`، `tryUseInventoryBooster`، `useItem`، `stepPaddleSize`، `paddleCollisionBounds`، `onExpired`، `refreshPaddleMode`، `fillBallsTo`، `fireLasers`، `killSpecificBall`، `isHazardCandidate`، `updateLasers`، `loseLife`، `completeLevel`، `scoreMultiplier`
 */

package com.example.brick_breaker_ball

import com.badlogic.gdx.math.MathUtils
import com.badlogic.gdx.math.Rectangle
import com.badlogic.gdx.math.Vector2
import java.util.Random
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.sqrt

sealed interface BoosterUseResult {
    data object Applied : BoosterUseResult
    data class Rejected(val reason: String) : BoosterUseResult
}

class GameSession(
    val seed: Long = 0xB12C_BA11L,
    val level: LevelDefinition = LevelRepository.level(1),
    var baseBallSize: BallSize = BallSize.DEFAULT,
    var selectedBallGroupName: String = CosmeticDefaults.BALL_GROUP,
    var selectedBallSpriteName: String = CosmeticDefaults.BALL_SPRITE,
    var paddleAbility: PaddleAbilityProfile = PaddleAbilityCatalog.default,
    var ballAbility: BallAbilityProfile = BallAbilityCatalog.default
) {
    companion object {
        const val WIDTH = 900f
        const val HEIGHT = 1600f
        const val MIN_SPEED = 520f
        const val MAX_SPEED = 1060f
        const val MIN_HORIZONTAL = 90f
        const val MIN_VERTICAL = 180f
        const val MAX_BALLS = 8
        const val ABSOLUTE_MAX_BALLS = 15
        const val MAX_LASER_SHOTS = 8
        const val BASE_PADDLE_WIDTH = 244f
        const val PADDLE_EXPAND_STEP = 72f
        const val MAX_EXPAND_STACKS = 4
        const val MAX_REWARDED_REVIVES = 3
        const val REWARDED_REVIVE_LIVES = 3
        val PADDLE_SIZE_LEVELS = floatArrayOf(110f, 145f, 208f, 244f, 280f)
    }

    var phase = GamePhase.SERVING
    val paddle = Paddle()
    val balls = mutableListOf<Ball>()
    val ball get() = balls.first()
    val bricks = mutableListOf<Brick>()
    val fallingPowerUps = mutableListOf<FallingPowerUp>()
    val laserShots = mutableListOf<LaserShot>()
    val powerUps = PowerUpManager()
    val replay = ReplayRecorder(seed, 1)
    private val dropDirector = PowerUpDropDirector(seed, level.world)
    private val effectRandom = Random(seed xor 0x50A3_E77L)
    private val events = ArrayDeque<GameplayEvent>()
    internal var nextBallId = 1
    internal var nextPowerUpId = 1
    private var elapsed = 0f
    internal var laserCooldown = 0f
    private var pendingBrickCrush = false
    var lives = level.lives
    var score = 0
    var bottomShield = false
    var explosionExpansion = 1
    var fallingBricksMode = false
    var expandPaddleStacks = 0
    var deathRailTop = 0f
    var levelCompletionCount = 0
        private set
    private var pendingDeathRailZapX: Float? = null
    var itemRewardSink: (PowerUpType) -> Unit = {}
    internal var paddleAbilityHitCount = 0
    internal var ballAbilityHitCount = 0
    internal var ballAbilityBreakCount = 0
    internal var abilityTapWindowRemaining = 0f
    var rewardedRevivesUsed = 0
        internal set
    val rewardedRevivesRemaining: Int get() = (MAX_REWARDED_REVIVES - rewardedRevivesUsed).coerceAtLeast(0)

    init {
        newLevel()
    }

    /** ملاحظة صيانة: الدالة `newLevel` تنشئ الكائنات أو البيانات اللازمة لهذه المسؤولية؛ راجع استدعاءاتها واختباراتها قبل تعديلها. */
    fun newLevel() {
        bricks.clear()
        fallingPowerUps.clear()
        laserShots.clear()
        events.clear()
        powerUps.clear()
        bottomShield = false
        explosionExpansion = 1
        fallingBricksMode = false
        expandPaddleStacks = 0
        paddleAbilityHitCount = 0
        ballAbilityHitCount = 0
        ballAbilityBreakCount = 0
        abilityTapWindowRemaining = 0f
        rewardedRevivesUsed = 0
        paddle.targetWidth = BASE_PADDLE_WIDTH
        paddle.width = BASE_PADDLE_WIDTH
        paddle.mode = PaddleMode.NORMAL
        var id = 1
        val gapX = 4f
        val gapY = 8f
        val brickWidth = (WIDTH - 40f - (level.columns - 1) * gapX) / level.columns
        val brickHeight = 58f
        val boardWidth = level.columns * brickWidth + (level.columns - 1) * gapX
        val boardLeft = (WIDTH - boardWidth) / 2f
        val boardBottom = 620f
        level.layout.forEachIndexed { sourceRow, row ->
            row.forEachIndexed { sourceCol, symbol ->
                val key = BrickCodec.key(sourceRow, sourceCol)
                val type = level.brickIds[key]?.let { runCatching { BrickType.valueOf(it) }.getOrNull() }
                    ?: BrickCodec.type(symbol) ?: return@forEachIndexed
                val visualRow = level.rows - 1 - sourceRow
                val brickId = id++
                bricks += Brick(
                    brickId,
                    Rectangle(
                        boardLeft + sourceCol * (brickWidth + gapX),
                        boardBottom + visualRow * (brickHeight + gapY),
                        brickWidth,
                        brickHeight
                    ),
                    type,
                    if (type == BrickType.BOSS_CORE) level.bossHealth else type.maxHealth,
                    (sourceRow * .1f + sourceCol * .018f) % 1f,
                    groupId = level.brickGroups[key] ?: 0
                )
            }
        }
        serve()
    }

    /** ملاحظة صيانة: الدالة `serve` تنفّذ مسؤولية محلية يعتمد عليها هذا الجزء من اللعبة؛ راجع استدعاءاتها واختباراتها قبل تعديلها. */
    fun serve() {
        phase = GamePhase.SERVING
        balls.clear()
        val newBall = createBall(Vector2(paddle.x, 0f), Vector2())
        newBall.position.y = paddle.y + paddle.height / 2f + newBall.radius + 2f
        newBall.previousPosition.set(newBall.position)
        balls += newBall
    }

    /** ملاحظة صيانة: الدالة `createBall` تنشئ الكائنات أو البيانات اللازمة لهذه المسؤولية؛ راجع استدعاءاتها واختباراتها قبل تعديلها. */
    private fun createBall(position: Vector2, velocity: Vector2, source: Ball? = null): Ball {
        val initialSize = source?.size ?: when {
            PowerUpType.SHRINK_BALL in powerUps -> baseBallSize.smaller()
            PowerUpType.MEGA_BALL in powerUps -> baseBallSize.megaBoosted()
            else -> baseBallSize
        }
        val ball = Ball(
            id = nextBallId++, position = position, previousPosition = Vector2(position), velocity = velocity,
            size = initialSize,
            baseSize = source?.baseSize ?: baseBallSize,
            cosmeticGroupName = source?.cosmeticGroupName ?: selectedBallGroupName,
            cosmeticSpriteName = source?.cosmeticSpriteName ?: selectedBallSpriteName,
            element = source?.element ?: activeElement(),
            collisionMode = source?.collisionMode ?: when {
                PowerUpType.GHOST_BALL in powerUps -> BallCollisionMode.GHOST
                PowerUpType.PIERCING_BALL in powerUps -> BallCollisionMode.PIERCING
                else -> BallCollisionMode.NORMAL
            },
            baseSpeed = source?.baseSpeed ?: velocity.len(),
            abilityFireCharge = false
        )
        return ball
    }

    /** ملاحظة صيانة: الدالة `activeElement` تنفّذ مسؤولية محلية يعتمد عليها هذا الجزء من اللعبة؛ راجع استدعاءاتها واختباراتها قبل تعديلها. */
    private fun activeElement() = when {
        PowerUpType.FIRE_BALL in powerUps -> BallElement.FIRE
        PowerUpType.EXPLOSIVE_BALL in powerUps -> BallElement.EXPLOSIVE
        else -> BallElement.NORMAL
    }

    /** ملاحظة صيانة: الدالة `applyCustomization` تعالج الحدث أو الطلب وتحدّث الحالة المرتبطة به؛ راجع استدعاءاتها واختباراتها قبل تعديلها. */
    fun applyCustomization(settings: GameSettings) {
        baseBallSize = settings.selectedBallBaseSize
        selectedBallGroupName = settings.selectedBallGroupName
        selectedBallSpriteName = settings.selectedBallSpriteName
        val nextBallAbility = BallAbilityCatalog.profileForSprite(settings.selectedBallSpriteName)
        if (nextBallAbility != ballAbility) {
            ballAbility = nextBallAbility
            ballAbilityHitCount = 0
            ballAbilityBreakCount = 0
        }
        val nextAbility = PaddleAbilityCatalog.profileForNormalPaddle(settings.selectedPaddleId)
        if (nextAbility != paddleAbility) {
            paddleAbility = nextAbility
            paddleAbilityHitCount = 0
            abilityTapWindowRemaining = 0f
            balls.forEach { it.abilityFireCharge = false }
        }
        balls.forEach { ball ->
            ball.baseSize = baseBallSize
            ball.cosmeticGroupName = selectedBallGroupName
            ball.cosmeticSpriteName = selectedBallSpriteName
            ball.size = when {
                PowerUpType.SHRINK_BALL in powerUps -> baseBallSize.smaller()
                PowerUpType.MEGA_BALL in powerUps -> baseBallSize.megaBoosted()
                else -> baseBallSize
            }
        }
        clampPaddleForActiveLayout()
    }

    /** ملاحظة صيانة: الدالة `clampPaddleForActiveLayout` تنفّذ مسؤولية محلية يعتمد عليها هذا الجزء من اللعبة؛ راجع استدعاءاتها واختباراتها قبل تعديلها. */
    fun clampPaddleForActiveLayout() {
        val halfExtent = if (PowerUpType.DUAL_PADDLE in powerUps) paddle.width + 3f else paddle.width / 2f
        paddle.x = paddle.x.coerceIn(halfExtent, WIDTH - halfExtent)
    }

    /** ملاحظة صيانة: الدالة `action` تعالج الحدث أو الطلب وتحدّث الحالة المرتبطة به؛ راجع استدعاءاتها واختباراتها قبل تعديلها. */
    fun action() {
        when (phase) {
            GamePhase.SERVING -> launchBall(ball, 280f)

            GamePhase.PLAYING -> {
                val stuck = balls.filter { it.stuckOffset != null }
                if (stuck.isNotEmpty()) {
                    launchAttachedBalls(0f)
                } else if (PowerUpType.LASER_PADDLE in powerUps || PowerUpType.LASER_AUTO_CHARGE in powerUps) {
                    fireLasers()
                } else {
                    armPaddleAbility()
                }
            }

            GamePhase.LEVEL_COMPLETE, GamePhase.GAME_OVER -> {
                lives = level.lives
                score = 0
                levelCompletionCount = 0
                newLevel()
            }

            GamePhase.LOADING, GamePhase.READY, GamePhase.RESOLVING, GamePhase.PAUSED -> Unit
        }
        replay.record(elapsed, paddle.x, "ACTION")
    }

    /** Arms timing-based paddle abilities without stealing Sticky/Laser actions. */
    private fun armPaddleAbility() {
        if (paddleAbility.kind == PaddleAbilityKind.IMPACT_BOOST) {
            abilityTapWindowRemaining = paddleAbility.perfectWindowSeconds
        }
    }

    fun paddleAbilityStatus(): String = when (paddleAbility.kind) {
        PaddleAbilityKind.PRECISION_CORE -> "STEADY +${((paddleAbility.moveResponseMultiplier - 1f) * 100f).toInt()}%"
        PaddleAbilityKind.HYPER_GLIDE -> "MOVE +${((paddleAbility.moveResponseMultiplier - 1f) * 100f).toInt()}%"
        PaddleAbilityKind.IMPACT_BOOST -> if (abilityTapWindowRemaining > 0f) "PERFECT WINDOW" else "TAP ON IMPACT"
        PaddleAbilityKind.INFERNO_RHYTHM -> "CHARGE ${paddleAbilityHitCount % paddleAbility.fireEveryHits.coerceAtLeast(1)}/${paddleAbility.fireEveryHits}"
    }

    /** ملاحظة صيانة: الدالة `hasAttachedBalls` تتحقق من الشرط المطلوب وتعيد نتيجة يمكن لبقية النظام الاعتماد عليها؛ راجع استدعاءاتها واختباراتها قبل تعديلها. */
    fun hasAttachedBalls(): Boolean = phase == GamePhase.SERVING || balls.any { it.stuckOffset != null }

    /** ملاحظة صيانة: الدالة `launchAttachedBalls` تعالج الحدث أو الطلب وتحدّث الحالة المرتبطة به؛ راجع استدعاءاتها واختباراتها قبل تعديلها. */
    fun launchAttachedBalls(horizontalIntent: Float) {
        val intent = horizontalIntent.coerceIn(-1f, 1f)
        when (phase) {
            GamePhase.SERVING -> balls.toList().forEachIndexed { index, attachedBall ->
                val spread = (index - balls.lastIndex / 2f) * 70f
                launchBall(attachedBall, intent * 760f + spread)
            }

            GamePhase.PLAYING -> {
                val stuck = balls.filter { it.stuckOffset != null }
                if (stuck.isEmpty()) return
                stuck.forEachIndexed { index, attachedBall ->
                    attachedBall.stuckOffset = null
                    val spread = (index - stuck.lastIndex / 2f) * 80f
                    launchBall(attachedBall, intent * 760f + spread)
                }
            }

            else -> return
        }
        replay.record(elapsed, paddle.x, "ACTION")
    }

    /** ملاحظة صيانة: الدالة `launchServe` تعالج الحدث أو الطلب وتحدّث الحالة المرتبطة به؛ راجع استدعاءاتها واختباراتها قبل تعديلها. */
    fun launchServe(horizontalIntent: Float) {
        if (phase == GamePhase.SERVING) launchAttachedBalls(horizontalIntent)
    }

    /** ملاحظة صيانة: الدالة `launchBall` تعالج الحدث أو الطلب وتحدّث الحالة المرتبطة به؛ راجع استدعاءاتها واختباراتها قبل تعديلها. */
    private fun launchBall(ball: Ball, xSpeed: Float) {
        phase = GamePhase.PLAYING
        ball.baseSpeed = (level.ballSpeed * 1.10f).coerceIn(MIN_SPEED, MAX_SPEED)
        ball.velocity.set(xSpeed, 560f).nor()
        syncBallSpeed(ball)
    }

    /** ملاحظة صيانة: الدالة `movePaddle` تحدّث الحالة المتغيرة خلال دورة التشغيل أو المحاكاة؛ راجع استدعاءاتها واختباراتها قبل تعديلها. */
    fun movePaddle(rawTargetX: Float, dt: Float) {
        val targetX = if (PowerUpType.INVERT_CONTROLS in powerUps) WIDTH - rawTargetX else rawTargetX
        val oldX = paddle.x
        val baseResponse = if (PowerUpType.SLIPPERY_PADDLE in powerUps) 5f else 18f
        val response = baseResponse * paddleAbility.moveResponseMultiplier
        val halfExtent = if (PowerUpType.DUAL_PADDLE in powerUps) paddle.width + 3f else paddle.width / 2f
        val wanted = targetX.coerceIn(halfExtent, WIDTH - halfExtent)
        paddle.x += (wanted - paddle.x) * (dt * response).coerceAtMost(1f)
        paddle.animate(dt)
        val animatedHalfExtent = if (PowerUpType.DUAL_PADDLE in powerUps) paddle.width + 3f else paddle.width / 2f
        paddle.x = paddle.x.coerceIn(animatedHalfExtent, WIDTH - animatedHalfExtent)
        paddle.velocityX = (paddle.x - oldX) / max(dt, .0001f)
        balls.filter { it.stuckOffset != null || phase == GamePhase.SERVING }.forEach {
            it.position.x = (paddle.x + (it.stuckOffset ?: 0f)).coerceIn(it.radius, WIDTH - it.radius)
            it.position.y = paddle.y + paddle.height / 2f + it.radius + 1f
        }
        replay.record(elapsed, paddle.x)
    }

    /** ملاحظة صيانة: الدالة `update` تحدّث الحالة المتغيرة خلال دورة التشغيل أو المحاكاة؛ راجع استدعاءاتها واختباراتها قبل تعديلها. */
    fun update(dt: Float) {
        if (phase == GamePhase.PAUSED) return
        elapsed += dt
        laserCooldown = (laserCooldown - dt).coerceAtLeast(0f)
        abilityTapWindowRemaining = (abilityTapWindowRemaining - dt).coerceAtLeast(0f)
        if (phase == GamePhase.PLAYING && PowerUpType.LASER_AUTO_CHARGE in powerUps) fireLasers()
        dropDirector.update(dt)
        updateBricks(dt)
        powerUps.update(dt).forEach(::onExpired)
        // A dropped talisman belongs to the playfield, not to the current ball. Keep it
        // falling while the replacement ball is waiting on the paddle after a life loss.
        if (phase == GamePhase.PLAYING || phase == GamePhase.SERVING) updateCapsules(dt)
        if (phase != GamePhase.PLAYING) return
        balls.toList().forEach { ball ->
            ball.previousPosition.set(ball.position)
            if (ball.stuckOffset == null) {
                if (ball.baseSpeed <= 0f) ball.baseSpeed = ball.velocity.len().coerceIn(MIN_SPEED, MAX_SPEED)
                val expectedSpeed = (ball.baseSpeed * speedMultiplier()).coerceIn(MIN_SPEED, MAX_SPEED)
                if (abs(ball.velocity.len() - expectedSpeed) > 2f) {
                    ball.baseSpeed = (ball.velocity.len() / speedMultiplier()).coerceIn(MIN_SPEED, MAX_SPEED)
                }
                ball.baseSpeed = (ball.baseSpeed * (1f + dt * .008f)).coerceIn(MIN_SPEED, MAX_SPEED)
                syncBallSpeed(ball)
                applyMagneticAttraction(ball, dt)
                simulateBall(ball, dt)
            }
        }
        if (pendingBrickCrush) {
            pendingBrickCrush = false
            loseLife(force = true, feedback = "BRICKS CRUSHED THE PADDLE")
            return
        }
        updateLasers(dt)
        val lost = balls.filter { it.position.y - it.radius <= deathRailTop }
        if (lost.isNotEmpty()) pendingDeathRailZapX = lost.last().position.x
        balls.removeAll(lost.toSet())
        if (balls.isEmpty()) loseLife(force = false)
        if (phase == GamePhase.PLAYING && bricks.none {
                (it.type.breakable || it.temporaryOriginalType?.breakable == true) &&
                    it.type !in setOf(BrickType.KEY_BRICK, BrickType.SWITCH) &&
                    !(it.type == BrickType.GHOST && !it.ghostVisible)
            }
        ) {
            completeLevel()
        }
    }

    /** ملاحظة صيانة: الدالة `applyMagneticAttraction` تعالج الحدث أو الطلب وتحدّث الحالة المرتبطة به؛ راجع استدعاءاتها واختباراتها قبل تعديلها. */
    private fun applyMagneticAttraction(ball: Ball, dt: Float) {
        if (PowerUpType.MAGNETIC_PADDLE !in powerUps || ball.velocity.y >= 0f) return

        // Dual Paddle gets two real magnetic targets. Aim at whichever active physical slot
        // is closest to this ball instead of always steering toward the overall paddle center.
        val targetBounds = paddleCollisionBounds().minByOrNull { bounds ->
            abs((bounds.x + bounds.width / 2f) - ball.position.x)
        } ?: return
        val targetX = targetBounds.x + targetBounds.width / 2f
        val targetY = targetBounds.y + targetBounds.height + ball.radius + 4f
        val dx = targetX - ball.position.x
        val dy = ball.position.y - targetY
        if (dy < 0f) return

        // Physics and rendering use the same ellipse: almost no pull at the edge and a
        // clearly readable assist as the ball approaches the paddle.
        val nx = dx / GameplayTuning.MAGNET_HORIZONTAL_RANGE
        val ny = dy / GameplayTuning.MAGNET_RANGE
        val radialSquared = nx * nx + ny * ny
        if (radialSquared > 1f) return
        val radial = sqrt(radialSquared.coerceAtLeast(0f))
        val edgeInfluence = (1f - radial).coerceIn(0f, 1f)
        val influence = edgeInfluence * edgeInfluence * (3f - 2f * edgeInfluence)

        val speed = ball.velocity.len().coerceIn(MIN_SPEED, MAX_SPEED)
        val currentDirection = Vector2(ball.velocity).nor()
        val targetDirection = Vector2(dx, targetY - ball.position.y).nor()
        val blend = (GameplayTuning.MAGNET_STEER_RATE * influence * dt).coerceIn(0f, GameplayTuning.MAGNET_MAX_BLEND)
        currentDirection.lerp(targetDirection, blend).nor()

        // Keep the result descending and preserve speed: Magnet assists a catch; it does
        // not teleport, accelerate, or create an orbit around the paddle.
        if (currentDirection.y > -.18f) currentDirection.y = -.18f
        currentDirection.nor()
        ball.velocity.set(currentDirection.scl(speed))
    }

    /** ملاحظة صيانة: الدالة `consumeDeathRailZapX` تنفّذ مسؤولية محلية يعتمد عليها هذا الجزء من اللعبة؛ راجع استدعاءاتها واختباراتها قبل تعديلها. */
    fun consumeDeathRailZapX(): Float? = pendingDeathRailZapX.also { pendingDeathRailZapX = null }

    /** ملاحظة صيانة: الدالة `consumeEvents` تنفّذ مسؤولية محلية يعتمد عليها هذا الجزء من اللعبة؛ راجع استدعاءاتها واختباراتها قبل تعديلها. */
    fun consumeEvents(): List<GameplayEvent> = buildList { while (events.isNotEmpty()) add(events.removeFirst()) }

    /** ملاحظة صيانة: الدالة `simulateBall` تنفّذ مسؤولية محلية يعتمد عليها هذا الجزء من اللعبة؛ راجع استدعاءاتها واختباراتها قبل تعديلها. */
    private fun simulateBall(ball: Ball, dt: Float) {
        val visitedBrickIds = mutableSetOf<Int>()
        var remaining = dt
        repeat(8) {
            if (remaining <= .000001f) return
            val delta = Vector2(ball.velocity).scl(remaining)
            var best: CollisionResult? = null

            /** ملاحظة صيانة: الدالة `consider` تنفّذ مسؤولية محلية يعتمد عليها هذا الجزء من اللعبة؛ راجع استدعاءاتها واختباراتها قبل تعديلها. */
            fun consider(hit: CollisionResult?) {
                if (hit != null && (best == null || hit.time < best!!.time)) best = hit
            }
            consider(SweptCollision.circleVsAabb(ball.position, delta, ball.radius, Rectangle(0f, 0f, 18f, HEIGHT), -10))
            consider(SweptCollision.circleVsAabb(ball.position, delta, ball.radius, Rectangle(WIDTH - 18f, 0f, 18f, HEIGHT), -11))
            consider(SweptCollision.circleVsAabb(ball.position, delta, ball.radius, Rectangle(0f, HEIGHT - 18f, WIDTH, 18f), -12))
            if (ball.velocity.y < 0f) {
                paddleCollisionBounds().forEachIndexed { index, bounds ->
                    consider(SweptCollision.circleVsAabb(ball.position, delta, ball.radius, bounds, -20 - index))
                }
            }
            bricks.asSequence().filter { it.id !in visitedBrickIds && (it.type != BrickType.GHOST || it.ghostVisible) }.forEach {
                consider(SweptCollision.circleVsAabb(ball.position, delta, ball.radius, it.bounds, it.id))
            }
            val hit = best
            if (hit == null) {
                ball.position.mulAdd(ball.velocity, remaining)
                return
            }
            ball.position.mulAdd(delta, (hit.time - .0001f).coerceAtLeast(0f))
            remaining *= (1f - hit.time).coerceAtLeast(0f)
            when {
                hit.targetId == -20 || hit.targetId == -21 -> paddleBounce(ball)

                hit.targetId > 0 -> {
                    val brick = bricks.firstOrNull { it.id == hit.targetId }
                    if (brick != null) {
                        visitedBrickIds += brick.id
                        when {
                            brick.type == BrickType.BLACK_HOLE_TELEPORTER -> {
                                teleportBall(ball, brick)
                                remaining = 0f
                            }

                            ball.collisionMode == BallCollisionMode.GHOST -> ball.position.mulAdd(ball.velocity, .002f)

                            brick.type == BrickType.LIGHTNING_SPEED_PASS_THROUGH -> {
                                // Supplied rule: pass through, consume the brick, then speed the ball up.
                                damageBrick(brick, ball)
                                ball.baseSpeed = (ball.baseSpeed * GameplayTuning.LIGHTNING_SPEED_MULTIPLIER).coerceAtMost(MAX_SPEED)
                                syncBallSpeed(ball)
                                ball.position.mulAdd(ball.velocity, .002f)
                            }

                            brick.type == BrickType.TRANSPARENT_SLOW_PASS_THROUGH -> {
                                // Supplied rule: pass through, consume the brick, then slow the ball down.
                                damageBrick(brick, ball)
                                ball.baseSpeed = (ball.baseSpeed * GameplayTuning.TRANSPARENT_SLOW_MULTIPLIER).coerceAtLeast(MIN_SPEED)
                                syncBallSpeed(ball)
                                ball.position.mulAdd(ball.velocity, .002f)
                            }

                            brick.type == BrickType.ROUGH_STONE -> {
                                // Supplied rule: rough stone breaks and sends the ball away
                                // at an irregular/randomized angle.
                                damageBrick(brick, ball)
                                randomizeBallDirection(ball)
                            }

                            brick.type == BrickType.SPIKED_HAZARD && GameplayTuning.SPIKED_HAZARD_KILLS_BALL -> {
                                killSpecificBall(ball)
                                remaining = 0f
                            }

                            else -> {
                                val piercing = ball.collisionMode == BallCollisionMode.PIERCING
                                val piercesSteel = piercing && brick.type == BrickType.INDESTRUCTIBLE
                                val passThrough = piercing && (brick.type.breakable || piercesSteel)
                                damageBrick(brick, ball, forceBreak = piercesSteel)
                                if (!passThrough) reflect(ball, hit) else ball.position.mulAdd(ball.velocity, .002f)
                            }
                        }
                    }
                }

                else -> reflect(ball, hit)
            }
            ball.position.add(hit.normalX * .08f, hit.normalY * .08f)
        }
        ensureVelocityBounds(ball)
    }

    /** ملاحظة صيانة: الدالة `randomizeBallDirection` تنفّذ مسؤولية محلية يعتمد عليها هذا الجزء من اللعبة؛ راجع استدعاءاتها واختباراتها قبل تعديلها. */
    private fun randomizeBallDirection(ball: Ball) {
        val speed = ball.velocity.len().coerceIn(MIN_SPEED, MAX_SPEED)
        val angle = effectRandom.nextFloat() * 360f
        ball.velocity.set(MathUtils.cosDeg(angle), MathUtils.sinDeg(angle)).scl(speed)
        ensureVelocityBounds(ball)
    }

    /** ملاحظة صيانة: الدالة `teleportBall` تنفّذ مسؤولية محلية يعتمد عليها هذا الجزء من اللعبة؛ راجع استدعاءاتها واختباراتها قبل تعديلها. */
    private fun teleportBall(ball: Ball, source: Brick) {
        val safeTop = HEIGHT - ball.radius - 40f
        repeat(GameplayTuning.TELEPORT_ATTEMPTS) {
            val candidate = Vector2(
                ball.radius + 24f + effectRandom.nextFloat() * (WIDTH - 2f * ball.radius - 48f),
                260f + effectRandom.nextFloat() * (safeTop - 260f)
            )
            val blocked = bricks.any { it !== source && it.bounds.contains(candidate) }
            if (!blocked) {
                ball.position.set(candidate)
                ball.previousPosition.set(candidate)
                val speed = ball.velocity.len().coerceIn(MIN_SPEED, MAX_SPEED)
                val angle = effectRandom.nextFloat() * 360f
                ball.velocity.set(MathUtils.cosDeg(angle), MathUtils.sinDeg(angle)).scl(speed)
                ensureVelocityBounds(ball)
                events += GameplayEvent.Feedback("BLACK HOLE TELEPORT")
                return
            }
        }
        ball.position.set(WIDTH / 2f, 360f)
        ball.previousPosition.set(ball.position)
    }

    /** ملاحظة صيانة: الدالة `reflect` تنفّذ مسؤولية محلية يعتمد عليها هذا الجزء من اللعبة؛ راجع استدعاءاتها واختباراتها قبل تعديلها. */
    private fun reflect(ball: Ball, hit: CollisionResult) {
        val dot = ball.velocity.x * hit.normalX + ball.velocity.y * hit.normalY
        ball.velocity.add(-2f * dot * hit.normalX, -2f * dot * hit.normalY)
    }

    /** ملاحظة صيانة: الدالة `paddleBounce` تنفّذ مسؤولية محلية يعتمد عليها هذا الجزء من اللعبة؛ راجع استدعاءاتها واختباراتها قبل تعديلها. */
    private fun paddleBounce(ball: Ball) {
        if (PowerUpType.STICKY_PADDLE in powerUps) {
            val stickyHalfWidth = if (PowerUpType.DUAL_PADDLE in powerUps) paddle.width else paddle.width * .42f
            ball.stuckOffset = (ball.position.x - paddle.x).coerceIn(-stickyHalfWidth, stickyHalfWidth)
            ball.velocity.setZero()
            return
        }
        val bounceHalfWidth = if (PowerUpType.DUAL_PADDLE in powerUps) paddle.width + 3f else paddle.width / 2f
        val offset = ((ball.position.x - paddle.x) / bounceHalfWidth).coerceIn(-1f, 1f)
        val speed = ball.velocity.len().coerceIn(MIN_SPEED, MAX_SPEED)
        val angle = MathUtils.lerp(150f, 30f, (offset + 1f) / 2f)
        ball.velocity.set(MathUtils.cosDeg(angle), MathUtils.sinDeg(angle)).scl(speed)
        ball.velocity.x += paddle.velocityX.coerceIn(-500f, 500f) * .12f
        ensureVelocityBounds(ball)
        paddleAbilityHitCount++
        when (paddleAbility.kind) {
            PaddleAbilityKind.IMPACT_BOOST -> if (abilityTapWindowRemaining > 0f) {
                val currentBase = ball.baseSpeed.coerceAtLeast(ball.velocity.len() / speedMultiplier())
                ball.baseSpeed = (currentBase * paddleAbility.perfectSpeedMultiplier).coerceIn(MIN_SPEED, MAX_SPEED)
                syncBallSpeed(ball)
                abilityTapWindowRemaining = 0f
                events += GameplayEvent.Feedback("PERFECT +${((paddleAbility.perfectSpeedMultiplier - 1f) * 100f).toInt()}%")
            }

            PaddleAbilityKind.INFERNO_RHYTHM -> {
                val every = paddleAbility.fireEveryHits.coerceAtLeast(1)
                if (paddleAbilityHitCount % every == 0) {
                    ball.abilityFireCharge = true
                    events += GameplayEvent.Feedback("FIRE CHARGED")
                }
            }

            else -> Unit
        }
        if (fallingBricksMode) applyFallingBricksStep()
    }

    /** ملاحظة صيانة: الدالة `clampSpeed` تنفّذ مسؤولية محلية يعتمد عليها هذا الجزء من اللعبة؛ راجع استدعاءاتها واختباراتها قبل تعديلها. */
    fun clampSpeed(ball: Ball) {
        if (ball.velocity.isZero(.001f)) return
        ball.baseSpeed = ball.velocity.len().coerceIn(MIN_SPEED, MAX_SPEED)
        syncBallSpeed(ball)
        ensureVelocityBounds(ball)
    }

    /** ملاحظة صيانة: الدالة `speedMultiplier` تنفّذ مسؤولية محلية يعتمد عليها هذا الجزء من اللعبة؛ راجع استدعاءاتها واختباراتها قبل تعديلها. */
    private fun speedMultiplier(): Float {
        val effectMultiplier = when {
            PowerUpType.FAST_BALL in powerUps -> 1.25f
            PowerUpType.SLOW_BALL in powerUps -> GameplayTuning.SLOW_BALL_MULTIPLIER
            else -> 1f
        }
        return effectMultiplier * ballAbility.speedMultiplier
    }

    /** ملاحظة صيانة: الدالة `syncBallSpeed` تنفّذ مسؤولية محلية يعتمد عليها هذا الجزء من اللعبة؛ راجع استدعاءاتها واختباراتها قبل تعديلها. */
    private fun syncBallSpeed(ball: Ball) {
        if (ball.velocity.isZero(.001f)) return
        ball.velocity.setLength((ball.baseSpeed * speedMultiplier()).coerceIn(MIN_SPEED, MAX_SPEED))
        ensureVelocityBounds(ball)
    }

    /** ملاحظة صيانة: الدالة `ensureVelocityBounds` تنفّذ مسؤولية محلية يعتمد عليها هذا الجزء من اللعبة؛ راجع استدعاءاتها واختباراتها قبل تعديلها. */
    private fun ensureVelocityBounds(ball: Ball) {
        var speed = ball.velocity.len()
        if (speed < .001f) return
        speed = speed.coerceIn(MIN_SPEED, MAX_SPEED)
        ball.velocity.setLength(speed)
        if (abs(ball.velocity.x) < MIN_HORIZONTAL) {
            ball.velocity.x = if (ball.velocity.x < 0f) -MIN_HORIZONTAL else MIN_HORIZONTAL
            ball.velocity.y = (if (ball.velocity.y < 0f) -1f else 1f) * sqrt(speed * speed - MIN_HORIZONTAL * MIN_HORIZONTAL)
        }
        if (abs(ball.velocity.y) < MIN_VERTICAL) {
            ball.velocity.y = if (ball.velocity.y < 0f) -MIN_VERTICAL else MIN_VERTICAL
            ball.velocity.x = (if (ball.velocity.x < 0f) -1f else 1f) * sqrt(speed * speed - MIN_VERTICAL * MIN_VERTICAL)
        }
    }

    /** ملاحظة صيانة: الدالة `damageBrick` تنفّذ مسؤولية محلية يعتمد عليها هذا الجزء من اللعبة؛ راجع استدعاءاتها واختباراتها قبل تعديلها. */
    private fun damageBrick(
        brick: Brick,
        ball: Ball? = null,
        chain: MutableSet<Int> = mutableSetOf(),
        forceBreak: Boolean = false,
        fireSplashOverride: Boolean = false
    ): Boolean {
        if (!chain.add(brick.id) || brick !in bricks || (!brick.type.breakable && !forceBreak) || brick.locked ||
            (brick.type == BrickType.GHOST && !brick.ghostVisible)
        ) {
            return false
        }
        if (brick.type == BrickType.SPIKED_HAZARD) return false

        val directBallHit = ball != null
        if (directBallHit) ballAbilityHitCount++
        val abilityTriggered = directBallHit && ballAbility.triggerEveryHits > 0 &&
            ballAbilityHitCount % ballAbility.triggerEveryHits == 0

        if (ball != null && PowerUpType.TIMED_BOMB_BRICKS in powerUps && brick.timedBombSeconds == null) {
            brick.timedBombSeconds = GameplayTuning.TIMED_BOMB_DURATION
            events += GameplayEvent.Feedback("TIMED BOMB ARMED")
            return true
        }
        val sourceType = brick.type
        val directFire = ball?.element == BallElement.FIRE || fireSplashOverride
        val paddleAbilityFire = ball?.abilityFireCharge == true
        val catalogInferno = abilityTriggered && ballAbility.kind == BallAbilityKind.INFERNO_BURST
        val abilityFire = paddleAbilityFire || catalogInferno
        val voidPhase = abilityTriggered && ballAbility.kind == BallAbilityKind.VOID_PHASE
        val armorBreak = abilityTriggered && ballAbility.kind == BallAbilityKind.ARMOR_BREAKER
        val oneHit = forceBreak || abilityFire || voidPhase || (ball != null && (
            PowerUpType.ONE_HIT_ANY_BRICK in powerUps ||
                PowerUpType.MEGA_BALL in powerUps
            ))
        val normalDamage = 1 + if (armorBreak) ballAbility.bonusDamage else 0
        brick.health -= if (directFire || oneHit) brick.health else normalDamage
        if (paddleAbilityFire) ball?.abilityFireCharge = false
        if (abilityFire) events += GameplayEvent.Feedback(if (catalogInferno) "INFERNO BURST" else "INFERNO HIT")
        if (armorBreak) events += GameplayEvent.Feedback("ARMOR BREAK +${ballAbility.bonusDamage}")
        if (voidPhase) events += GameplayEvent.Feedback("VOID PHASE")
        if (brick.health > 0) return true
        val cx = brick.bounds.x + brick.bounds.width / 2f
        val cy = brick.bounds.y + brick.bounds.height / 2f
        bricks.remove(brick)
        val baseBrickScore = 100 * scoreMultiplier()
        val impactBonus = if (ball != null && ballAbility.kind == BallAbilityKind.IMPACT_CORE) {
            (baseBrickScore * ballAbility.scoreBonusPercent) / 100
        } else {
            0
        }
        score += baseBrickScore + impactBonus
        if (ball != null) {
            ballAbilityBreakCount++
            if (ballAbility.kind == BallAbilityKind.FORTUNE_CORE &&
                ballAbility.fortuneEveryBreaks > 0 &&
                ballAbilityBreakCount % ballAbility.fortuneEveryBreaks == 0
            ) {
                score += ballAbility.fortuneBonusScore
                events += GameplayEvent.Feedback("FORTUNE +${ballAbility.fortuneBonusScore}")
            }
        }
        val forced = sourceType == BrickType.POWERUP_CARRIER
        dropDirector.choose(lives, fallingPowerUps.size, forced, level.dropRate)?.let {
            fallingPowerUps += FallingPowerUp(nextPowerUpId++, it, Vector2(cx, brick.bounds.y))
        }
        if (sourceType == BrickType.KEY_BRICK || sourceType == BrickType.SWITCH) activatePuzzleGroup(brick.groupId, sourceType)
        if (sourceType == BrickType.CHAIN_BRICK) {
            bricks.toList().filter { candidate -> candidate.type == BrickType.CHAIN_BRICK && candidate.groupId == brick.groupId }
                .forEach { damageBrick(it, null, chain) }
        }
        if (sourceType == BrickType.RANDOM_INVENTORY_POWERUP) {
            val rewards = SHOP_ELIGIBLE_TYPES.filter { PowerUpCatalog.definitions.getValue(it).category == PowerUpCategory.GOOD }
            if (rewards.isNotEmpty()) {
                val reward = rewards[effectRandom.nextInt(rewards.size)]
                itemRewardSink(reward)
                events += GameplayEvent.Feedback("${PowerUpInfoRepository.info(reward).shortName} ADDED TO BAG")
            }
        }
        if (ball != null && abilityTriggered && ballAbility.kind == BallAbilityKind.ARC_CHAIN) {
            val target = bricks.asSequence()
                .filter { it.type.breakable && !it.locked && it.type != BrickType.SPIKED_HAZARD }
                .minByOrNull { candidate ->
                    Vector2.dst(cx, cy, candidate.bounds.x + candidate.bounds.width / 2f, candidate.bounds.y + candidate.bounds.height / 2f)
                }
            if (target != null) {
                events += GameplayEvent.Feedback("ARC CHAIN")
                damageBrick(target, null, chain)
            }
        }
        val explosion = sourceType == BrickType.EXPLOSIVE || ball?.element == BallElement.EXPLOSIVE
        val fireSplash = directFire || abilityFire
        if (explosion || fireSplash) {
            events += GameplayEvent.Explosion
            val normalRadius = when {
                fireSplash && ball?.size in setOf(BallSize.MEGA, BallSize.LARGE) -> GameplayTuning.LARGE_FIRE_BLAST_RADIUS
                fireSplash -> GameplayTuning.FIRE_BLAST_RADIUS
                else -> GameplayTuning.TIMED_BOMB_RADIUS
            }
            val blastRadius = if (explosionExpansion > 1) normalRadius * 1.57f else normalRadius
            var targets = bricks.toList().filter { candidate ->
                candidate.type.breakable && Vector2.dst(cx, cy, candidate.bounds.x + candidate.bounds.width / 2f, candidate.bounds.y + candidate.bounds.height / 2f) < blastRadius
            }.sortedBy { candidate -> Vector2.dst(cx, cy, candidate.bounds.x + candidate.bounds.width / 2f, candidate.bounds.y + candidate.bounds.height / 2f) }
            if (fireSplash && ball?.size in setOf(BallSize.MEGA, BallSize.LARGE)) targets = targets.take(GameplayTuning.LARGE_FIRE_MAX_TARGETS - 1)
            targets.forEach { damageBrick(it, null, chain) }
        }
        return true
    }

    /** ملاحظة صيانة: الدالة `activatePuzzleGroup` تعالج الحدث أو الطلب وتحدّث الحالة المرتبطة به؛ راجع استدعاءاتها واختباراتها قبل تعديلها. */
    private fun activatePuzzleGroup(groupId: Int, source: BrickType) {
        /** ملاحظة صيانة: الدالة `matches` تنفّذ مسؤولية محلية يعتمد عليها هذا الجزء من اللعبة؛ راجع استدعاءاتها واختباراتها قبل تعديلها. */
        fun matches(brick: Brick) = groupId == 0 || brick.groupId == groupId
        var changed = false
        bricks.filter { matches(it) && it.type == BrickType.LOCKED && it.locked }.forEach {
            it.locked = false
            changed = true
        }
        if (source == BrickType.SWITCH) {
            bricks.filter { matches(it) && it.type == BrickType.GHOST }.forEach {
                it.ghostVisible = !it.ghostVisible
                changed = true
            }
        }
        if (changed) {
            events += GameplayEvent.Feedback(
                if (source == BrickType.KEY_BRICK) "LOCKED BRICKS UNLOCKED" else "SWITCH GROUP CHANGED"
            )
        }
    }

    /** ملاحظة صيانة: الدالة `updateBricks` تحدّث الحالة المتغيرة خلال دورة التشغيل أو المحاكاة؛ راجع استدعاءاتها واختباراتها قبل تعديلها. */
    private fun updateBricks(dt: Float) {
        bricks.forEach { brick ->
            brick.age += dt
            brick.timedBombSeconds = brick.timedBombSeconds?.minus(dt)
            when (brick.type) {
                BrickType.MOVING_HORIZONTAL -> moveBrickWithoutOverlap(
                    brick,
                    targetX = (brick.originX + MathUtils.sin(brick.age * 1.4f) * 42f)
                        .coerceIn(18f, WIDTH - 18f - brick.bounds.width),
                    targetY = brick.bounds.y
                )

                BrickType.MOVING_VERTICAL -> moveBrickWithoutOverlap(
                    brick,
                    targetX = brick.bounds.x,
                    targetY = brick.originY + MathUtils.sin(brick.age * 1.2f) * 30f
                )

                BrickType.REGENERATING -> if (brick.health < brick.type.maxHealth && brick.age > 8f) {
                    brick.health++
                    brick.age = 0f
                }

                else -> Unit
            }
        }
        bricks.filter { (it.timedBombSeconds ?: Float.MAX_VALUE) <= 0f }.toList().forEach { bomb ->
            bomb.timedBombSeconds = null
            val chain = mutableSetOf<Int>()
            // An armed brick is consumed by its own detonation even when it originally had multiple HP.
            bomb.health = 1
            damageBrick(bomb, null, chain)
            val cx = bomb.bounds.x + bomb.bounds.width / 2f
            val cy = bomb.bounds.y + bomb.bounds.height / 2f
            bricks.toList().filter { candidate ->
                candidate.type.breakable &&
                    Vector2.dst(cx, cy, candidate.bounds.x + candidate.bounds.width / 2f, candidate.bounds.y + candidate.bounds.height / 2f) < GameplayTuning.TIMED_BOMB_RADIUS
            }
                .forEach { damageBrick(it, null, chain) }
            events += GameplayEvent.Explosion
        }
    }

    /** ملاحظة صيانة: الدالة `moveBrickWithoutOverlap` تحدّث الحالة المتغيرة خلال دورة التشغيل أو المحاكاة؛ راجع استدعاءاتها واختباراتها قبل تعديلها. */
    private fun moveBrickWithoutOverlap(brick: Brick, targetX: Float, targetY: Float) {
        var safeX = targetX
        if (targetX > brick.bounds.x) {
            bricks.asSequence().filter { other ->
                other !== brick &&
                    other.bounds.y < brick.bounds.y + brick.bounds.height &&
                    other.bounds.y + other.bounds.height > brick.bounds.y &&
                    other.bounds.x >= brick.bounds.x + brick.bounds.width
            }.forEach { other -> safeX = minOf(safeX, other.bounds.x - brick.bounds.width) }
        } else if (targetX < brick.bounds.x) {
            bricks.asSequence().filter { other ->
                other !== brick &&
                    other.bounds.y < brick.bounds.y + brick.bounds.height &&
                    other.bounds.y + other.bounds.height > brick.bounds.y &&
                    other.bounds.x + other.bounds.width <= brick.bounds.x
            }.forEach { other -> safeX = maxOf(safeX, other.bounds.x + other.bounds.width) }
        }
        brick.bounds.x = safeX

        var safeY = targetY
        if (targetY > brick.bounds.y) {
            bricks.asSequence().filter { other ->
                other !== brick &&
                    other.bounds.x < brick.bounds.x + brick.bounds.width &&
                    other.bounds.x + other.bounds.width > brick.bounds.x &&
                    other.bounds.y >= brick.bounds.y + brick.bounds.height
            }.forEach { other -> safeY = minOf(safeY, other.bounds.y - brick.bounds.height) }
        } else if (targetY < brick.bounds.y) {
            bricks.asSequence().filter { other ->
                other !== brick &&
                    other.bounds.x < brick.bounds.x + brick.bounds.width &&
                    other.bounds.x + other.bounds.width > brick.bounds.x &&
                    other.bounds.y + other.bounds.height <= brick.bounds.y
            }.forEach { other -> safeY = maxOf(safeY, other.bounds.y + other.bounds.height) }
        }
        brick.bounds.y = safeY
    }

    /** ملاحظة صيانة: الدالة `applyFallingBricksStep` تعالج الحدث أو الطلب وتحدّث الحالة المرتبطة به؛ راجع استدعاءاتها واختباراتها قبل تعديلها. */
    private fun applyFallingBricksStep() {
        bricks.filter { it.type.breakable }.forEach { brick ->
            brick.bounds.y -= 42f
            brick.originY -= 42f
        }
        events += GameplayEvent.FallingWarning
        val dangerTop = maxOf(deathRailTop, paddle.bounds.y + paddle.bounds.height + 8f)
        if (bricks.any { it.type.breakable && it.bounds.y <= dangerTop }) pendingBrickCrush = true
    }

    /** ملاحظة صيانة: الدالة `updateCapsules` تحدّث الحالة المتغيرة خلال دورة التشغيل أو المحاكاة؛ راجع استدعاءاتها واختباراتها قبل تعديلها. */
    private fun updateCapsules(dt: Float) {
        val iterator = fallingPowerUps.iterator()
        while (iterator.hasNext()) {
            val capsule = iterator.next()
            if (PowerUpType.POWERUP_MAGNET in powerUps) {
                // Strong but controlled attraction: pull each falling talisman toward the nearest
                // physical paddle slot, damp sideways oscillation, and cap horizontal velocity.
                val magnetTargetX = paddleCollisionBounds().minByOrNull { bounds ->
                    abs((bounds.x + bounds.width / 2f) - capsule.position.x)
                }?.let { it.x + it.width / 2f } ?: paddle.x
                val dx = magnetTargetX - capsule.position.x
                capsule.velocity.x += dx * dt * GameplayTuning.POWERUP_MAGNET_PULL
                capsule.velocity.x *= (1f - dt * GameplayTuning.POWERUP_MAGNET_DAMPING).coerceAtLeast(0f)
                capsule.velocity.x = capsule.velocity.x.coerceIn(
                    -GameplayTuning.POWERUP_MAGNET_MAX_HORIZONTAL_SPEED,
                    GameplayTuning.POWERUP_MAGNET_MAX_HORIZONTAL_SPEED,
                )
            }
            capsule.position.mulAdd(capsule.velocity, dt)
            val bounds = Rectangle(
                capsule.position.x - GameplayTuning.POWERUP_PICKUP_WIDTH / 2f,
                capsule.position.y - GameplayTuning.POWERUP_PICKUP_HEIGHT / 2f,
                GameplayTuning.POWERUP_PICKUP_WIDTH,
                GameplayTuning.POWERUP_PICKUP_HEIGHT,
            )
            if (paddleCollisionBounds().any(bounds::overlaps)) {
                val collectedType = capsule.type
                iterator.remove()
                activatePowerUp(collectedType)
            } else {
                val passedPaddle = capsule.position.y < paddle.y - paddle.height / 2f - GameplayTuning.POWERUP_PADDLE_CLEARANCE
                val leftPlayfield = capsule.position.y < GameplayTuning.POWERUP_DESPAWN_Y
                if (passedPaddle && leftPlayfield) iterator.remove()
            }
        }
    }

    /** ملاحظة صيانة: الدالة `canActivatePowerUp` تتحقق من الشرط المطلوب وتعيد نتيجة يمكن لبقية النظام الاعتماد عليها؛ راجع استدعاءاتها واختباراتها قبل تعديلها. */
    fun canActivatePowerUp(requested: PowerUpType): Boolean {
        if (phase == GamePhase.LEVEL_COMPLETE || phase == GamePhase.GAME_OVER) return false
        if (requested == PowerUpType.RANDOM_GOOD || requested == PowerUpType.RANDOM_BAD) return randomCandidates(requested).isNotEmpty()
        val definition = PowerUpCatalog.definitions.getValue(requested)
        if (PowerUpType.POWERUP_JAM in powerUps && definition.category == PowerUpCategory.GOOD) return false
        return when (requested) {
            PowerUpType.EXTRA_LIFE -> lives < 9

            PowerUpType.SET_OFF_EXPLODING -> bricks.any { it.type == BrickType.EXPLOSIVE }

            PowerUpType.LEVEL_WARP -> !level.modifiers.contains("BOSS") && bricks.none { it.type == BrickType.BOSS_CORE }

            PowerUpType.ZAP_BRICKS -> bricks.any(::isZapTarget)

            PowerUpType.EIGHT_BALL, PowerUpType.MULTI_BALL, PowerUpType.TRIPLE_BALL -> balls.size < MAX_BALLS

            PowerUpType.MULTIBALL_PLUS_4, PowerUpType.MULTIBALL_15 -> balls.size < ABSOLUTE_MAX_BALLS

            PowerUpType.INSTANT_KILL_BALL ->
                PowerUpType.INSTANT_KILL_BALL !in powerUps &&
                    bricks.count(::isHazardCandidate) >= GameplayTuning.TEMPORARY_SPIKE_COUNT

            PowerUpType.EXPAND_PADDLE -> expandPaddleStacks < MAX_EXPAND_STACKS

            PowerUpType.SHRINK_PADDLE -> paddleTargetWithoutWeaponBonus() > PADDLE_SIZE_LEVELS.first()

            PowerUpType.SUPER_SHRINK -> paddleTargetWithoutWeaponBonus() > PADDLE_SIZE_LEVELS.first()

            PowerUpType.SHRINK_BALL -> PowerUpType.SHRINK_BALL !in powerUps && balls.any { it.size != BallSize.SMALL }

            PowerUpType.MEGA_BALL -> PowerUpType.MEGA_BALL !in powerUps && balls.any { it.size != it.baseSize.megaBoosted() }

            PowerUpType.FAST_BALL -> PowerUpType.SLOW_BALL in powerUps || balls.any { it.velocity.isZero(.001f) || it.velocity.len() < MAX_SPEED - 1f }

            PowerUpType.SLOW_BALL -> PowerUpType.FAST_BALL in powerUps || balls.any { it.velocity.isZero(.001f) || it.velocity.len() > MIN_SPEED + 1f }

            PowerUpType.EXPAND_EXPLODING -> explosionExpansion == 1

            PowerUpType.FALLING_BRICKS -> !fallingBricksMode

            PowerUpType.KILL_PADDLE -> lives > 0

            PowerUpType.BOTTOM_SHIELD, PowerUpType.PADDLE_SHIELD -> !bottomShield

            PowerUpType.POWERUP_JAM -> PowerUpType.POWERUP_JAM !in powerUps

            PowerUpType.RANDOM_GOOD, PowerUpType.RANDOM_BAD -> false

            PowerUpType.STICKY_PADDLE, PowerUpType.LASER_PADDLE, PowerUpType.LASER_AUTO_CHARGE,
            PowerUpType.MAGNETIC_PADDLE, PowerUpType.DUAL_PADDLE,
            PowerUpType.ONE_HIT_ANY_BRICK, PowerUpType.GHOST_BALL,
            PowerUpType.FIRE_BALL, PowerUpType.PIERCING_BALL, PowerUpType.EXPLOSIVE_BALL,
            PowerUpType.SCORE_X2, PowerUpType.SCORE_X3, PowerUpType.INVERT_CONTROLS,
            PowerUpType.SLIPPERY_PADDLE, PowerUpType.POWERUP_MAGNET -> true

            PowerUpType.TIMED_BOMB_BRICKS -> bricks.any { it.type.breakable && !it.locked && it.type != BrickType.SPIKED_HAZARD }
        }
    }

    /** ملاحظة صيانة: الدالة `randomCandidates` تنفّذ مسؤولية محلية يعتمد عليها هذا الجزء من اللعبة؛ راجع استدعاءاتها واختباراتها قبل تعديلها. */
    private fun randomCandidates(randomType: PowerUpType): List<PowerUpType> {
        val category = if (randomType == PowerUpType.RANDOM_GOOD) PowerUpCategory.GOOD else PowerUpCategory.BAD
        return PowerUpCatalog.definitions.values.asSequence()
            .filter { it.category == category && it.type !in setOf(PowerUpType.RANDOM_GOOD, PowerUpType.RANDOM_BAD) }
            .map { it.type }.filter(::canActivatePowerUp).toList()
    }

    /** ملاحظة صيانة: الدالة `activatePowerUp` تعالج الحدث أو الطلب وتحدّث الحالة المرتبطة به؛ راجع استدعاءاتها واختباراتها قبل تعديلها. */
    fun activatePowerUp(requested: PowerUpType): Boolean {
        val type = if (requested == PowerUpType.RANDOM_GOOD || requested == PowerUpType.RANDOM_BAD) {
            val candidates = randomCandidates(requested)
            if (candidates.isEmpty()) return false
            candidates[effectRandom.nextInt(candidates.size)]
        } else {
            requested
        }
        if (!canActivatePowerUp(type)) return false
        val weaponPaddleWasActive = weaponPaddleActive()
        powerUps.activate(type)
        if (!weaponPaddleWasActive && weaponPaddleActive()) {
            paddle.targetWidth += PADDLE_EXPAND_STEP
        }
        when (type) {
            PowerUpType.EXPAND_PADDLE -> {
                expandPaddleStacks = (expandPaddleStacks + 1).coerceAtMost(MAX_EXPAND_STACKS)
                paddle.targetWidth = BASE_PADDLE_WIDTH + PADDLE_EXPAND_STEP * expandPaddleStacks + weaponPaddleBonus()
            }

            PowerUpType.SHRINK_PADDLE -> {
                expandPaddleStacks = 0
                stepPaddleSize(-1)
            }

            PowerUpType.SUPER_SHRINK -> {
                expandPaddleStacks = 0
                paddle.targetWidth = PADDLE_SIZE_LEVELS.first() + weaponPaddleBonus()
            }

            PowerUpType.STICKY_PADDLE, PowerUpType.LASER_PADDLE, PowerUpType.LASER_AUTO_CHARGE,
            PowerUpType.MAGNETIC_PADDLE, PowerUpType.DUAL_PADDLE -> refreshPaddleMode()

            PowerUpType.MULTI_BALL -> fillBallsTo((balls.size * 2).coerceAtMost(MAX_BALLS))

            PowerUpType.TRIPLE_BALL -> fillBallsTo((balls.size * 3).coerceAtMost(MAX_BALLS))

            PowerUpType.EIGHT_BALL -> fillBallsTo(MAX_BALLS)

            PowerUpType.MULTIBALL_PLUS_4 -> fillBallsTo((balls.size + 4).coerceAtMost(ABSOLUTE_MAX_BALLS))

            PowerUpType.MULTIBALL_15 -> fillBallsTo(ABSOLUTE_MAX_BALLS)

            PowerUpType.EXTRA_LIFE -> lives++

            PowerUpType.SLOW_BALL, PowerUpType.FAST_BALL -> balls.forEach(::syncBallSpeed)

            PowerUpType.FIRE_BALL -> balls.forEach { it.element = BallElement.FIRE }

            PowerUpType.PIERCING_BALL -> balls.forEach { it.collisionMode = BallCollisionMode.PIERCING }

            PowerUpType.GHOST_BALL -> balls.forEach { it.collisionMode = BallCollisionMode.GHOST }

            PowerUpType.EXPLOSIVE_BALL -> balls.forEach { it.element = BallElement.EXPLOSIVE }

            PowerUpType.KILL_PADDLE -> loseLife(force = true, feedback = "PLAYER KILLED")

            PowerUpType.INSTANT_KILL_BALL -> bricks.filter(::isHazardCandidate)
                .sortedBy { effectRandom.nextInt() }.take(GameplayTuning.TEMPORARY_SPIKE_COUNT).forEach { brick ->
                    brick.temporaryOriginalType = brick.type
                    brick.temporaryOriginalHealth = brick.health
                    brick.temporaryOriginalInitialHealth = brick.initialHealth
                    brick.type = BrickType.SPIKED_HAZARD
                    brick.health = 1
                    brick.initialHealth = 1
                }

            PowerUpType.SET_OFF_EXPLODING -> {
                val chain = mutableSetOf<Int>()
                bricks.toList().filter { it.type == BrickType.EXPLOSIVE }.forEach { damageBrick(it, null, chain) }
            }

            PowerUpType.LEVEL_WARP -> completeLevel()

            PowerUpType.SHRINK_BALL -> balls.forEach { it.size = it.baseSize.smaller() }

            PowerUpType.ZAP_BRICKS -> bricks.filter(::isZapTarget).forEach {
                it.type = BrickType.NORMAL_ONE_HIT
                it.health = 1
            }

            PowerUpType.MEGA_BALL -> balls.forEach { it.size = it.baseSize.megaBoosted() }

            PowerUpType.EXPAND_EXPLODING -> explosionExpansion = 2

            PowerUpType.FALLING_BRICKS -> fallingBricksMode = true

            PowerUpType.TIMED_BOMB_BRICKS -> Unit

            PowerUpType.ONE_HIT_ANY_BRICK -> Unit

            PowerUpType.BOTTOM_SHIELD, PowerUpType.PADDLE_SHIELD -> bottomShield = true

            PowerUpType.SCORE_X2, PowerUpType.SCORE_X3, PowerUpType.INVERT_CONTROLS,
            PowerUpType.SLIPPERY_PADDLE, PowerUpType.POWERUP_MAGNET, PowerUpType.POWERUP_JAM -> Unit

            PowerUpType.RANDOM_GOOD, PowerUpType.RANDOM_BAD -> error("Random effects must resolve before dispatch")
        }
        refreshPaddleMode()
        events += GameplayEvent.Feedback("${PowerUpInfoRepository.info(type).shortName} ACTIVATED")
        return true
    }

    /** ملاحظة صيانة: الدالة `isZapTarget` تتحقق من الشرط المطلوب وتعيد نتيجة يمكن لبقية النظام الاعتماد عليها؛ راجع استدعاءاتها واختباراتها قبل تعديلها. */
    private fun isZapTarget(brick: Brick) = brick.type in setOf(
        BrickType.INDESTRUCTIBLE,
        BrickType.ARMORED_TWO_HIT,
        BrickType.ARMORED_THREE_HIT,
        BrickType.REGENERATING,
        BrickType.GHOST
    )

    /** ملاحظة صيانة: الدالة `tryUseInventoryBooster` تنفّذ مسؤولية محلية يعتمد عليها هذا الجزء من اللعبة؛ راجع استدعاءاتها واختباراتها قبل تعديلها. */
    fun tryUseInventoryBooster(type: PowerUpType): BoosterUseResult {
        if (type !in SHOP_ELIGIBLE_TYPES) return BoosterUseResult.Rejected("NOT AVAILABLE")
        if (!canActivatePowerUp(type)) return BoosterUseResult.Rejected("BOOSTER HAS NO EFFECT NOW")
        return if (activatePowerUp(type)) BoosterUseResult.Applied else BoosterUseResult.Rejected("BOOSTER COULD NOT START")
    }

    /** ملاحظة صيانة: الدالة `useItem` تنفّذ مسؤولية محلية يعتمد عليها هذا الجزء من اللعبة؛ راجع استدعاءاتها واختباراتها قبل تعديلها. */
    fun useItem(type: PowerUpType, items: BoosterInventoryStore): BoosterUseResult {
        if (items.count(type) <= 0) return BoosterUseResult.Rejected("NO CHARMS OWNED")
        val result = tryUseInventoryBooster(type)
        if (result is BoosterUseResult.Applied) check(items.consume(type)) { "Bag count changed during activation" }
        return result
    }

    /** ملاحظة صيانة: الدالة `stepPaddleSize` تحدّث الحالة المتغيرة خلال دورة التشغيل أو المحاكاة؛ راجع استدعاءاتها واختباراتها قبل تعديلها. */
    private fun stepPaddleSize(direction: Int) {
        val bonus = weaponPaddleBonus()
        val baseTargetWidth = paddleTargetWithoutWeaponBonus()
        val index = PADDLE_SIZE_LEVELS.indices.minBy { abs(PADDLE_SIZE_LEVELS[it] - baseTargetWidth) }
        paddle.targetWidth = PADDLE_SIZE_LEVELS[(index + direction).coerceIn(PADDLE_SIZE_LEVELS.indices)] + bonus
    }

    /** الويبن بادل يأخذ خطوة Expand إضافية طوال بقاء الليزر أو Auto Laser فعالاً. */
    private fun weaponPaddleActive(): Boolean =
        PowerUpType.LASER_PADDLE in powerUps || PowerUpType.LASER_AUTO_CHARGE in powerUps

    private fun weaponPaddleBonus(): Float = if (weaponPaddleActive()) PADDLE_EXPAND_STEP else 0f

    private fun paddleTargetWithoutWeaponBonus(): Float =
        (paddle.targetWidth - weaponPaddleBonus()).coerceAtLeast(PADDLE_SIZE_LEVELS.first())

    /** يرقّي الحفظ القديم الذي كان يخزن الويبن بادل من دون زيادة العرض الجديدة. */
    internal fun restoreWeaponPaddleBonus(savedWidthIncludesBonus: Boolean) {
        if (savedWidthIncludesBonus || !weaponPaddleActive()) return
        paddle.width += PADDLE_EXPAND_STEP
        paddle.targetWidth += PADDLE_EXPAND_STEP
        clampPaddleForActiveLayout()
    }

    /** ملاحظة صيانة: الدالة `paddleCollisionBounds` تنفّذ مسؤولية محلية يعتمد عليها هذا الجزء من اللعبة؛ راجع استدعاءاتها واختباراتها قبل تعديلها. */
    internal fun paddleCollisionBounds(): List<Rectangle> = PaddleVisualLayout
        .slots(paddle.x, paddle.width, PowerUpType.DUAL_PADDLE in powerUps)
        .map { slot -> Rectangle(slot.centerX - slot.width / 2f, paddle.y - paddle.height / 2f, slot.width, paddle.height) }

    /** ملاحظة صيانة: الدالة `onExpired` تعالج الحدث أو الطلب وتحدّث الحالة المرتبطة به؛ راجع استدعاءاتها واختباراتها قبل تعديلها. */
    private fun onExpired(type: PowerUpType) {
        when (type) {
            PowerUpType.STICKY_PADDLE, PowerUpType.LASER_PADDLE, PowerUpType.LASER_AUTO_CHARGE,
            PowerUpType.MAGNETIC_PADDLE, PowerUpType.DUAL_PADDLE -> {
                if ((type == PowerUpType.LASER_PADDLE || type == PowerUpType.LASER_AUTO_CHARGE) && !weaponPaddleActive()) {
                    paddle.targetWidth = (paddle.targetWidth - PADDLE_EXPAND_STEP)
                        .coerceAtLeast(PADDLE_SIZE_LEVELS.first())
                }
                refreshPaddleMode()
            }

            PowerUpType.FIRE_BALL -> balls.forEach { if (it.element == BallElement.FIRE) it.element = activeElement() }

            PowerUpType.EXPLOSIVE_BALL -> balls.forEach { if (it.element == BallElement.EXPLOSIVE) it.element = activeElement() }

            PowerUpType.PIERCING_BALL -> balls.forEach { it.collisionMode = BallCollisionMode.NORMAL }

            PowerUpType.GHOST_BALL -> balls.forEach { if (it.collisionMode == BallCollisionMode.GHOST) it.collisionMode = BallCollisionMode.NORMAL }

            PowerUpType.FAST_BALL, PowerUpType.SLOW_BALL -> balls.forEach(::syncBallSpeed)

            PowerUpType.MEGA_BALL -> balls.forEach { it.size = it.baseSize }

            PowerUpType.MULTIBALL_PLUS_4, PowerUpType.MULTIBALL_15 -> if (balls.size > 1) {
                val survivor = balls.first()
                balls.clear()
                balls += survivor
            }

            PowerUpType.INSTANT_KILL_BALL -> bricks.filter { it.temporaryOriginalType != null }.forEach { brick ->
                brick.type = requireNotNull(brick.temporaryOriginalType)
                brick.health = brick.temporaryOriginalHealth
                brick.initialHealth = brick.temporaryOriginalInitialHealth
                brick.temporaryOriginalType = null
            }

            PowerUpType.SCORE_X2, PowerUpType.SCORE_X3, PowerUpType.INVERT_CONTROLS,
            PowerUpType.SLIPPERY_PADDLE, PowerUpType.POWERUP_MAGNET, PowerUpType.POWERUP_JAM -> Unit

            PowerUpType.ONE_HIT_ANY_BRICK, PowerUpType.TIMED_BOMB_BRICKS -> Unit

            PowerUpType.EXPAND_PADDLE, PowerUpType.SHRINK_PADDLE, PowerUpType.SUPER_SHRINK, PowerUpType.SHRINK_BALL,
            PowerUpType.EXPAND_EXPLODING, PowerUpType.FALLING_BRICKS, PowerUpType.MULTI_BALL,
            PowerUpType.TRIPLE_BALL, PowerUpType.EXTRA_LIFE, PowerUpType.RANDOM_GOOD,
            PowerUpType.RANDOM_BAD, PowerUpType.KILL_PADDLE, PowerUpType.SET_OFF_EXPLODING,
            PowerUpType.LEVEL_WARP, PowerUpType.ZAP_BRICKS,
            PowerUpType.EIGHT_BALL, PowerUpType.BOTTOM_SHIELD,
            PowerUpType.PADDLE_SHIELD -> error("Non-timed power-up expired: $type")
        }
    }

    /** ملاحظة صيانة: الدالة `refreshPaddleMode` تنفّذ مسؤولية محلية يعتمد عليها هذا الجزء من اللعبة؛ راجع استدعاءاتها واختباراتها قبل تعديلها. */
    private fun refreshPaddleMode() {
        paddle.mode = when {
            PowerUpType.LASER_PADDLE in powerUps || PowerUpType.LASER_AUTO_CHARGE in powerUps -> PaddleMode.LASER
            PowerUpType.STICKY_PADDLE in powerUps -> PaddleMode.STICKY
            PowerUpType.MAGNETIC_PADDLE in powerUps -> PaddleMode.MAGNETIC
            PowerUpType.EXPAND_PADDLE in powerUps -> PaddleMode.EXPANDED
            PowerUpType.SHRINK_PADDLE in powerUps || PowerUpType.SUPER_SHRINK in powerUps -> PaddleMode.SHRUNK
            else -> PaddleMode.NORMAL
        }
    }

    /** ملاحظة صيانة: الدالة `fillBallsTo` تنفّذ مسؤولية محلية يعتمد عليها هذا الجزء من اللعبة؛ راجع استدعاءاتها واختباراتها قبل تعديلها. */
    private fun fillBallsTo(targetTotal: Int) {
        if (balls.isEmpty()) return
        val originals = balls.toList()
        val angleOffsets = floatArrayOf(-42f, 42f, -28f, 28f, -14f, 14f, 56f, -56f)
        var cloneIndex = 0
        while (balls.size < targetTotal.coerceAtMost(ABSOLUTE_MAX_BALLS)) {
            val source = originals[cloneIndex % originals.size]
            val velocity = Vector2(source.velocity)
            if (velocity.isZero(.001f)) velocity.set(0f, 1f)
            velocity.rotateDeg(angleOffsets[cloneIndex % angleOffsets.size])
            val clone = createBall(Vector2(source.position), velocity, source)
            clone.position.x = (clone.position.x + (if (cloneIndex % 2 == 0) -1f else 1f) * source.radius * .45f)
                .coerceIn(clone.radius, WIDTH - clone.radius)
            syncBallSpeed(clone)
            balls += clone
            cloneIndex++
        }
    }

    /** ملاحظة صيانة: الدالة `fireLasers` تعالج الحدث أو الطلب وتحدّث الحالة المرتبطة به؛ راجع استدعاءاتها واختباراتها قبل تعديلها. */
    private fun fireLasers() {
        if (laserCooldown > 0f || laserShots.size > MAX_LASER_SHOTS - 2) return
        val cannonTipY = paddle.y + 50f
        val fire = PowerUpType.FIRE_BALL in powerUps
        val piercing = PowerUpType.PIERCING_BALL in powerUps
        laserShots += LaserShot(Vector2(paddle.x - paddle.width * .36f, cannonTipY), fire = fire, piercing = piercing)
        laserShots += LaserShot(Vector2(paddle.x + paddle.width * .36f, cannonTipY), fire = fire, piercing = piercing)
        laserCooldown = if (PowerUpType.LASER_AUTO_CHARGE in powerUps) GameplayTuning.LASER_AUTO_CHARGE_COOLDOWN else GameplayTuning.LASER_COOLDOWN
        events += GameplayEvent.LaserFired
    }

    /** ملاحظة صيانة: الدالة `killSpecificBall` تنفّذ مسؤولية محلية يعتمد عليها هذا الجزء من اللعبة؛ راجع استدعاءاتها واختباراتها قبل تعديلها. */
    private fun killSpecificBall(ball: Ball) {
        if (balls.size > 1) {
            balls.remove(ball)
            events += GameplayEvent.Feedback("BALL DESTROYED", "explosion")
        } else {
            loseLife(force = true, feedback = "BALL DESTROYED")
        }
    }

    /** ملاحظة صيانة: الدالة `isHazardCandidate` تتحقق من الشرط المطلوب وتعيد نتيجة يمكن لبقية النظام الاعتماد عليها؛ راجع استدعاءاتها واختباراتها قبل تعديلها. */
    private fun isHazardCandidate(brick: Brick) = brick.type.breakable && !brick.locked &&
        brick.type !in setOf(
            BrickType.SPIKED_HAZARD,
            BrickType.BOSS_CORE,
            BrickType.KEY_BRICK,
            BrickType.SWITCH,
            BrickType.CHAIN_BRICK,
            BrickType.POWERUP_CARRIER,
            BrickType.RANDOM_INVENTORY_POWERUP
        )

    /** ملاحظة صيانة: الدالة `updateLasers` تحدّث الحالة المتغيرة خلال دورة التشغيل أو المحاكاة؛ راجع استدعاءاتها واختباراتها قبل تعديلها. */
    private fun updateLasers(dt: Float) {
        laserShots.forEach { shot ->
            shot.previousPosition.set(shot.position)
            val delta = Vector2(0f, 820f * dt)
            val hit = bricks.asSequence()
                .filter { it.id !in shot.hitBrickIds && (it.type != BrickType.GHOST || it.ghostVisible) }
                .mapNotNull { brick -> SweptCollision.circleVsAabb(shot.position, delta, 4f, brick.bounds, brick.id) }
                .minByOrNull { it.time }
            if (hit != null) {
                shot.position.mulAdd(delta, hit.time)
                shot.hitBrickIds += hit.targetId
                bricks.firstOrNull { it.id == hit.targetId }?.let { brick ->
                    val piercesSteel = shot.piercing && brick.type == BrickType.INDESTRUCTIBLE
                    damageBrick(
                        brick,
                        forceBreak = piercesSteel,
                        fireSplashOverride = shot.fire
                    )
                }
                if (shot.piercing) {
                    // Keep the projectile moving after the hit and make sure it starts
                    // just beyond the collision plane on the next sweep.
                    shot.position.y += 6f
                } else {
                    shot.alive = false
                }
                events += GameplayEvent.LaserHit
            } else {
                shot.position.add(delta)
            }
            if (shot.position.y > HEIGHT) shot.alive = false
        }
        laserShots.removeAll { !it.alive }
    }

    /** Restores all temporary gameplay state that the supplied rules say is lost with a life. */
    private fun resetEffectsAfterLifeLoss() {
        // Restore temporary spike conversions before clearing their source power-up.
        bricks.filter { it.temporaryOriginalType != null }.forEach { brick ->
            brick.type = requireNotNull(brick.temporaryOriginalType)
            brick.health = brick.temporaryOriginalHealth
            brick.initialHealth = brick.temporaryOriginalInitialHealth
            brick.temporaryOriginalType = null
        }
        bricks.forEach { it.timedBombSeconds = null }

        powerUps.clear()
        fallingPowerUps.clear()
        laserShots.clear()
        bottomShield = false
        explosionExpansion = 1
        fallingBricksMode = false
        expandPaddleStacks = 0
        laserCooldown = 0f
        paddleAbilityHitCount = 0
        ballAbilityHitCount = 0
        ballAbilityBreakCount = 0
        abilityTapWindowRemaining = 0f
        paddle.targetWidth = BASE_PADDLE_WIDTH
        paddle.width = BASE_PADDLE_WIDTH
        paddle.mode = PaddleMode.NORMAL
    }

    /** ملاحظة صيانة: الدالة `loseLife` تنفّذ مسؤولية محلية يعتمد عليها هذا الجزء من اللعبة؛ راجع استدعاءاتها واختباراتها قبل تعديلها. */
    private fun loseLife(force: Boolean, feedback: String = "") {
        if (!force && bottomShield) {
            bottomShield = false
            powerUps.deactivate(PowerUpType.BOTTOM_SHIELD)
            powerUps.deactivate(PowerUpType.PADDLE_SHIELD)
            events += GameplayEvent.Feedback("BOTTOM SHIELD SAVED THE BALL")
            serve()
            return
        }
        balls.clear()
        lives = (lives - 1).coerceAtLeast(0)
        resetEffectsAfterLifeLoss()
        events += GameplayEvent.Feedback(feedback, if (force) "explosion" else "wall_hit")
        if (lives <= 0) phase = GamePhase.GAME_OVER else serve()
    }

    /**
     * Grants a rewarded continue without rebuilding the level. The ad layer must call this only
     * after [RewardedAdResult.Earned]. Each level attempt can use at most three rewarded continues.
     */
    fun reviveFromRewardedAd(): Boolean {
        if (phase != GamePhase.GAME_OVER || lives > 0 || rewardedRevivesUsed >= MAX_REWARDED_REVIVES) return false
        rewardedRevivesUsed++
        lives = REWARDED_REVIVE_LIVES
        pendingDeathRailZapX = null
        abilityTapWindowRemaining = 0f
        events += GameplayEvent.Feedback("CONTINUE • $REWARDED_REVIVE_LIVES LIVES RESTORED")
        serve()
        return true
    }

    /** ملاحظة صيانة: الدالة `completeLevel` تنفّذ مسؤولية محلية يعتمد عليها هذا الجزء من اللعبة؛ راجع استدعاءاتها واختباراتها قبل تعديلها. */
    private fun completeLevel() {
        if (phase == GamePhase.LEVEL_COMPLETE) return
        phase = GamePhase.LEVEL_COMPLETE
        levelCompletionCount++
    }

    /** ملاحظة صيانة: الدالة `scoreMultiplier` تنفّذ مسؤولية محلية يعتمد عليها هذا الجزء من اللعبة؛ راجع استدعاءاتها واختباراتها قبل تعديلها. */
    private fun scoreMultiplier() = when {
        PowerUpType.SCORE_X3 in powerUps -> 3
        PowerUpType.SCORE_X2 in powerUps -> 2
        else -> 1
    }
}
