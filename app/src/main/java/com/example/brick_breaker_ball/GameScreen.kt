/*
 * ملاحظات صيانة الملف:
 * المسار: app/src/main/java/com/example/brick_breaker_ball/GameScreen.kt
 * المؤلف: mohamed alromaihi
 * الدوال الموجودة: `render`، `brick`، `paddle`، `ball`، `aimGuide`، `hud`، `activeEffectsHud`، `drawHudCell`، `smallButton`، `pauseOverlay`، `pauseArtButton`، `overlayButton`، `input`، `pauseAndSave`، `resumeGame`، `inventoryOverlay`، `inventoryTab`، `inventoryEntries`، `selectedInventoryType`، `inventoryCardRect`، `drawInventoryCard`، `inventoryMaxScroll`، `beginInventoryClip`، `endInventoryClip`، `handleInventoryInput`، `openShopFromInventory`، `closeInventory`، `draw`، `drawRegionFit`، `drawBallSquare`، `drawBackgroundCover`، `drawDeathRail`، `updateResponsiveLayout`، `finishLevel`، `exitCustomTest`، `pause`، `resize`، `dispose`
 */

package com.example.brick_breaker_ball

import com.badlogic.gdx.Gdx
import com.badlogic.gdx.Input
import com.badlogic.gdx.ScreenAdapter
import com.badlogic.gdx.graphics.Color
import com.badlogic.gdx.graphics.GL20
import com.badlogic.gdx.graphics.OrthographicCamera
import com.badlogic.gdx.graphics.g2d.SpriteBatch
import com.badlogic.gdx.graphics.g2d.TextureRegion
import com.badlogic.gdx.graphics.glutils.ShapeRenderer
import com.badlogic.gdx.math.MathUtils
import com.badlogic.gdx.math.Rectangle
import com.badlogic.gdx.math.Vector2
import com.badlogic.gdx.math.Vector3
import com.badlogic.gdx.utils.Align
import com.badlogic.gdx.utils.viewport.ExtendViewport
import kotlin.math.abs

internal fun requiresGameplayBallSprite(ball: Ball): Boolean =
    ball.abilityFireCharge || ball.element != BallElement.NORMAL || ball.collisionMode == BallCollisionMode.PIERCING

class GameScreen(
    private val game: BrickBreakerGame,
    private val level: LevelDefinition,
    private val session: GameSession = GameSession(
        seed = kotlin.random.Random.Default.nextLong(),
        level = level,
        baseBallSize = game.progress.settings.selectedBallBaseSize,
        selectedBallGroupName = game.progress.settings.selectedBallGroupName,
        selectedBallSpriteName = game.progress.settings.selectedBallSpriteName,
        paddleAbility = PaddleAbilityCatalog.profileForNormalPaddle(game.progress.settings.selectedPaddleId),
        ballAbility = BallAbilityCatalog.profileForSprite(game.progress.settings.selectedBallSpriteName)
    ),
    startPaused: Boolean = false,
    private val customTestEditor: LevelEditorState? = null
) : ScreenAdapter() {
    internal val musicWorld: Int = level.world
    private val camera = OrthographicCamera()
    private val viewport = ExtendViewport(GameSession.WIDTH, GameSession.HEIGHT, camera)
    private val batch = SpriteBatch()
    private val aimRenderer = ShapeRenderer()
    private val loop = FixedStepLoop()
    private val pointer = Vector3()
    private var targetX = session.paddle.x
    private var hitCount = 0
    private var feedbackText = ""
    private var feedbackTimer = 0f
    private var aimHintTimer = AIM_HINT_DURATION_SECONDS
    private var aimHintDismissed = false
    private var levelCompleteTransitionTime = 0f
    private var levelCompleteFinishStarted = false
    private var resumePhase = session.phase
    private var visibleLeft = 0f
    private var visibleBottom = 0f
    private var visibleWidth = GameSession.WIDTH
    private var visibleHeight = GameSession.HEIGHT
    private val hudPanelRect = Rectangle()
    private val actionBarRect = Rectangle()
    private val pauseButton = Rectangle()
    private val shopButton = Rectangle()
    private val inventoryButton = Rectangle()
    private val customExitButton = Rectangle()
    private val customizeButton = Rectangle()
    // Legacy overlay state remains private while all player-facing BAG entry points now route
    // to CharmsBagScreen. Keeping these fields temporarily avoids mixing a large mechanical
    // extraction with the navigation change in this already-modified gameplay file.
    private var inventoryOpen = false
    private var selectedBooster = 0
    private var selectedGuidePowerUp = 0
    private var inventoryGuideMode = false
    private var inventoryScrollOffset = 0f
    private var inventoryTouchActive = false
    private var inventoryTouchMoved = false
    private var inventoryTouchStartX = 0f
    private var inventoryTouchStartY = 0f
    private var inventoryTouchLastX = 0f
    private var inventoryTouchLastY = 0f
    private val deathRailRect = Rectangle()
    private var deathRailTime = 0f
    private var deathRailZapTimer = 0f
    private var deathRailZapX = GameSession.WIDTH / 2f
    private var aimDragActive = false
    private var aimDragStartX = 0f
    private var aimDragCurrentX = 0f
    private var aimIntent = 0f
    private var controlTouchActive = false
    private var controlLastX = targetX
    private var gameOverOverlayTime = 0f
    private var rewardedContinueInFlight = false
    private var rewardedContinueMessage = ""
    private val rewardedContinueButton = Rectangle(150f, 655f, 600f, 100f)
    private val gameOverRestartButton = Rectangle(150f, 525f, 600f, 88f)
    private val gameOverMenuButton = Rectangle(150f, 410f, 600f, 82f)

    // A menu tap can still be down during the first gameplay frame. Wait for
    // that pointer to be released so it cannot accidentally launch the serve.
    private var aimInputArmed = false
    private val overlayResume = Rectangle(150f, 1010f, 600f, 76f)
    private val overlaySettings = Rectangle(150f, 865f, 600f, 76f)
    private val overlayBag = Rectangle(150f, 720f, 600f, 76f)
    private val overlayShop = Rectangle(150f, 575f, 600f, 76f)
    private val overlayCustomize = Rectangle(150f, 430f, 600f, 76f)
    private val overlayRestart = Rectangle(150f, 285f, 600f, 76f)
    private val overlayMenu = Rectangle(150f, 140f, 600f, 76f)
    private val assets get() = game.assets

    init {
        if (customTestEditor == null) {
            session.itemRewardSink = { game.boosterInventory.add(it, 1) }
            game.monetization.rewardedReviveAdGateway.preload()
        }
        if (startPaused) {
            resumePhase = session.phase
            session.phase = GamePhase.PAUSED
        }
    }

    /** ملاحظة صيانة: الدالة `render` تنفّذ العقد الموروث وتربط دورة حياة المكوّن بسلوك هذا الملف؛ راجع استدعاءاتها واختباراتها قبل تعديلها. */
    override fun render(delta: Float) {
        if (game.progress.settings.quality == GraphicsQuality.LOW) {
            WorldVideoBackgrounds.hide()
        } else {
            WorldVideoBackgrounds.show(level.world)
        }
        input()
        deathRailTime += delta
        deathRailZapTimer = (deathRailZapTimer - delta).coerceAtLeast(0f)
        feedbackTimer = (feedbackTimer - delta).coerceAtLeast(0f)
        if (session.phase == GamePhase.GAME_OVER) {
            gameOverOverlayTime = (gameOverOverlayTime + delta.coerceAtMost(.1f)).coerceAtMost(10f)
        } else if (!rewardedContinueInFlight) {
            gameOverOverlayTime = 0f
        }
        if (!aimHintDismissed) {
            if (session.phase == GamePhase.PLAYING) {
                aimHintDismissed = true
            } else if (session.phase == GamePhase.SERVING) {
                aimHintTimer = (aimHintTimer - delta).coerceAtLeast(0f)
                if (aimHintTimer <= 0f) aimHintDismissed = true
            }
        }
        val before = session.bricks.size
        loop.advance(delta) { dt ->
            if (session.phase != GamePhase.PAUSED) session.movePaddle(targetX, dt)
            session.update(dt)
        }
        if (session.phase == GamePhase.LEVEL_COMPLETE) {
            levelCompleteTransitionTime = (levelCompleteTransitionTime + delta.coerceAtMost(.1f))
                .coerceAtMost(LEVEL_COMPLETE_TRANSITION_SECONDS)
        } else if (!levelCompleteFinishStarted) {
            levelCompleteTransitionTime = 0f
        }
        session.consumeDeathRailZapX()?.let {
            deathRailZapX = it
            deathRailZapTimer = .14f
        }
        session.consumeEvents().forEach { event ->
            when (event) {
                is GameplayEvent.Feedback -> {
                    if (event.text.isNotBlank()) {
                        feedbackText = event.text
                        feedbackTimer = 1.8f
                    } else {
                        // Normal life loss intentionally has sound feedback only.
                        feedbackText = ""
                        feedbackTimer = 0f
                    }
                    assets.play(event.sound, game.progress.settings.masterVolume * game.progress.settings.sfxVolume)
                }

                GameplayEvent.Explosion -> assets.play("explosion", game.progress.settings.masterVolume * game.progress.settings.sfxVolume)

                GameplayEvent.LaserFired -> assets.play("wall_hit", game.progress.settings.masterVolume * game.progress.settings.sfxVolume * .7f)

                GameplayEvent.LaserHit -> assets.play("brick_hit", game.progress.settings.masterVolume * game.progress.settings.sfxVolume)

                GameplayEvent.FallingWarning -> {
                    feedbackText = "BRICKS ARE FALLING"
                    feedbackTimer = 1.5f
                    assets.play("wall_hit", game.progress.settings.masterVolume * game.progress.settings.sfxVolume)
                }
            }
        }
        if (session.bricks.size < before) {
            hitCount++
            assets.play(
                if (hitCount % 5 == 0) "explosion" else "brick_hit",
                game.progress.settings.masterVolume * game.progress.settings.sfxVolume
            )
            if (game.progress.settings.haptics) Gdx.input.vibrate(18)
        }
        val videoVisible = WorldVideoBackgrounds.isVideoVisible()
        Gdx.gl.glClearColor(.004f, .008f, .03f, if (videoVisible) 0f else 1f)
        Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT)
        viewport.apply()
        batch.projectionMatrix = camera.combined
        batch.begin()
        if (!videoVisible) drawBackgroundCover(assets.worldFallbackBg(level.world), Color(.66f, .74f, .9f, .9f))
        val playfieldBottom = 0f
        val playfieldTop = actionBarRect.y - 8f
        draw(assets.ui.findRegion("panel"), 0f, playfieldBottom, 900f, playfieldTop - playfieldBottom, Color(0f, .006f, .02f, .46f))
        if (level.deathRailEnabled) drawDeathRail()
        if (session.fallingBricksMode) {
            val pulse = .35f + .2f * MathUtils.sin(deathRailTime * 7f)
            draw(assets.particles.findRegion("glow"), 18f, deathRailRect.y + 16f, 864f, 30f, Color(1f, .18f, .04f, pulse))
        }
        session.bricks.forEach(::brick)
        paddle()
        aimGuide()
        session.balls.forEach(::ball)
        session.laserShots.forEach { draw(assets.gameplayAtlas.laserProjectile, it.position.x - 8f, it.position.y, 16f, 42f, Color.MAROON) }
        session.fallingPowerUps.forEach { power ->
            val wave = if (game.progress.settings.reduceMotion) 0f else MathUtils.sin(deathRailTime * 5.2f + power.id * .73f)
            val size = GameplayTuning.POWERUP_VISUAL_SIZE * (1f + wave * .045f)
            val bob = wave * 3.5f
            val glowSize = size * 1.28f
            draw(
                assets.particles.findRegion("glow"),
                power.position.x - glowSize / 2f,
                power.position.y + bob - glowSize / 2f,
                glowSize,
                glowSize,
                Color(.35f, .78f, 1f, .18f + (wave + 1f) * .035f),
            )
            draw(
                assets.gameplayAtlas.powerUpIcon(power.type),
                power.position.x - size / 2f,
                power.position.y + bob - size / 2f,
                size,
                size,
                Color.WHITE,
            )
        }
        if (session.bottomShield) repeat(10) { draw(assets.particles.findRegion("glow"), it * 90f, 12f, 110f, 24f, Color(.1f, .9f, 1f, .7f)) }
        hud()
        if (feedbackTimer > 0f) {
            draw(assets.ui.findRegion("panel"), 150f, 360f, 600f, 64f, Color(.01f, .06f, .1f, .88f))
            assets.hudLabelFont.draw(batch, feedbackText, 160f, 402f, 580f, Align.center, false)
        }
        if (session.phase == GamePhase.GAME_OVER) drawGameOverOverlay()
        if (session.phase == GamePhase.LEVEL_COMPLETE) drawLevelCompleteTransition()
        if (session.phase == GamePhase.PAUSED) {
            pauseOverlay()
        }
        batch.end()
        if (
            session.phase == GamePhase.LEVEL_COMPLETE &&
            levelCompleteTransitionTime >= LEVEL_COMPLETE_TRANSITION_SECONDS &&
            !levelCompleteFinishStarted
        ) {
            levelCompleteFinishStarted = true
            finishLevel()
        }
    }

    /** Short in-game victory hold so the result screen never appears as a hard cut. */
    private fun drawLevelCompleteTransition() {
        val raw = (levelCompleteTransitionTime / LEVEL_COMPLETE_TRANSITION_SECONDS).coerceIn(0f, 1f)
        val eased = raw * raw * (3f - 2f * raw)
        val panelWidth = 720f
        val panelHeight = 170f
        val panelX = (GameSession.WIDTH - panelWidth) / 2f
        val panelY = 690f
        draw(
            assets.ui.findRegion("panel"),
            panelX,
            panelY,
            panelWidth,
            panelHeight,
            Color(ForgeUiPalette.glassPanel).also { it.a *= eased },
        )

        val font = assets.pauseTitleFont
        val oldColor = Color(font.color)
        val oldScaleX = font.data.scaleX
        val oldScaleY = font.data.scaleY
        val scale = .92f + .08f * eased
        font.data.setScale(oldScaleX * scale, oldScaleY * scale)
        font.color = Color(oldColor.r, oldColor.g, oldColor.b, eased)
        font.draw(batch, "LEVEL COMPLETE", panelX, panelY + 108f, panelWidth, Align.center, false)
        font.data.setScale(oldScaleX, oldScaleY)
        font.color = oldColor
    }

    /** ملاحظة صيانة: الدالة `brick` تنفّذ مسؤولية محلية يعتمد عليها هذا الجزء من اللعبة؛ راجع استدعاءاتها واختباراتها قبل تعديلها. */
    private fun brick(brick: Brick) {
        if (brick.type == BrickType.GHOST && !brick.ghostVisible) return
        val campaignTint =
            if (customTestEditor == null) CampaignBrickPalette.tintFor(brick.type, level.world, game.progress.settings.colorBlind)
            else Color.WHITE
        draw(
            assets.gameplayAtlas.brick(
                brick,
                level.world,
                useCampaignPalette = customTestEditor == null,
                colorBlind = game.progress.settings.colorBlind,
                levelId = level.id,
            ),
            brick.bounds.x,
            brick.bounds.y,
            brick.bounds.width,
            brick.bounds.height,
            campaignTint
        )
        if (brick.locked) draw(assets.ui.findRegion("panel"), brick.bounds.x, brick.bounds.y, brick.bounds.width, brick.bounds.height, Color(.05f, .1f, .18f, .68f))
        if (brick.type == BrickType.BOSS_CORE) {
            val ratio = brick.health.toFloat() / level.bossHealth.coerceAtLeast(1)
            draw(assets.ui.findRegion("panel"), brick.bounds.x, brick.bounds.y + brick.bounds.height + 4f, brick.bounds.width, 8f, Color(.08f, .08f, .1f, .95f))
            draw(assets.particles.findRegion("glow"), brick.bounds.x, brick.bounds.y + brick.bounds.height + 4f, brick.bounds.width * ratio, 8f, Color(1f, .15f, .2f, .95f))
        }
        brick.timedBombSeconds?.let { remaining ->
            val pulse = .45f + .35f * MathUtils.sin(deathRailTime * 12f)
            draw(assets.particles.findRegion("glow"), brick.bounds.x, brick.bounds.y, brick.bounds.width, brick.bounds.height, Color(1f, .12f, .03f, pulse))
            assets.smallFont.draw(batch, kotlin.math.ceil(remaining).toInt().toString(), brick.bounds.x, brick.bounds.y + brick.bounds.height * .7f, brick.bounds.width, Align.center, false)
        }
    }

    /** ملاحظة صيانة: الدالة `paddle` تنفّذ مسؤولية محلية يعتمد عليها هذا الجزء من اللعبة؛ راجع استدعاءاتها واختباراتها قبل تعديلها. */
    private fun paddle() {
        val paddle = session.paddle
        val laserActive = PowerUpType.LASER_PADDLE in session.powerUps || PowerUpType.LASER_AUTO_CHARGE in session.powerUps
        val stickyActive = PowerUpType.STICKY_PADDLE in session.powerUps
        val magneticActive = PowerUpType.MAGNETIC_PADDLE in session.powerUps
        val activePaddleId = CosmeticPaddleSelection.idForState(
            game.progress.settings.selectedPaddleId,
            game.progress.settings.selectedWeaponPaddleId,
            game.progress.settings.selectedStickyPaddleId,
            laserActive,
            stickyActive
        )
        val selected = assets.cosmetics.selectedPaddle(activePaddleId)
        val region = selected?.let(assets.cosmetics::paddleRegion) ?: assets.gameplayAtlas.paddleNormal.whole
        val drawY = paddle.y - paddle.height / 2f
        val dual = PowerUpType.DUAL_PADDLE in session.powerUps
        if (magneticActive) {
            val fieldWidth = GameplayTuning.MAGNET_HORIZONTAL_RANGE * 2f
            draw(
                assets.particles.findRegion("glow"),
                paddle.x - fieldWidth / 2f,
                paddle.y,
                fieldWidth,
                GameplayTuning.MAGNET_RANGE,
                Color(.12f, .58f, 1f, .14f)
            )
        }
        PaddleVisualLayout.slots(paddle.x, paddle.width, dual).forEach { slot ->
            val visualHeight = (slot.width * region.regionHeight.toFloat() / region.regionWidth.toFloat()).coerceAtMost(92f)
            draw(region, slot.centerX - slot.width / 2f, drawY, slot.width, visualHeight, Color.WHITE)
            if (laserActive) {
                val glow = assets.particles.findRegion("glow")
                val glowSize = 24f
                draw(glow, slot.centerX - slot.width * .38f - glowSize / 2f, drawY + visualHeight - 10f, glowSize, glowSize, Color(.12f, .9f, 1f, .82f))
                draw(glow, slot.centerX + slot.width * .38f - glowSize / 2f, drawY + visualHeight - 10f, glowSize, glowSize, Color(.12f, .9f, 1f, .82f))
            }
            if (magneticActive) {
                draw(
                    assets.particles.findRegion("glow"),
                    slot.centerX - slot.width * .48f,
                    drawY - 8f,
                    slot.width * .96f,
                    visualHeight + 16f,
                    Color(.48f, .34f, 1f, .42f)
                )
            }
        }
    }

    /** ملاحظة صيانة: الدالة `ball` تنفّذ مسؤولية محلية يعتمد عليها هذا الجزء من اللعبة؛ راجع استدعاءاتها واختباراتها قبل تعديلها. */
    private fun ball(ball: Ball) {
        val r = ball.radius
        val diameter = ball.visualDiameter
        val selected = assets.cosmetics.selectedBall(ball.cosmeticGroupName, ball.cosmeticSpriteName)
        // Element/collision transformations have dedicated art. Size power-ups keep the
        // currently equipped cosmetic and scale it to the ball's real collision diameter.
        val region = if (requiresGameplayBallSprite(ball)) {
            assets.gameplayAtlas.ball(ball)
        } else {
            selected?.let(assets.cosmetics::ballRegion) ?: assets.gameplayAtlas.ball(ball)
        }
        val ghost = ball.collisionMode == BallCollisionMode.GHOST
        val trailTint = when {
            ghost -> Color(.72f, .48f, 1f, 1f)
            ball.abilityFireCharge || ball.element == BallElement.FIRE -> Color(1f, .3f, .05f, 1f)
            else -> Color(.45f, .9f, 1f, 1f)
        }
        if (!game.progress.settings.reduceMotion) {
            for (i in 1..3) {
                val t = i / 4f
                val trailR = r * .82f
                val trailX = MathUtils.lerp(ball.position.x, ball.previousPosition.x, t)
                val trailY = MathUtils.lerp(ball.position.y, ball.previousPosition.y, t)
                drawBallSquare(region, trailX - trailR, trailY - trailR, trailR * 2f, Color(trailTint.r, trailTint.g, trailTint.b, .16f * (1f - t)))
            }
        }
        val ballTint = when {
            game.progress.settings.highContrastBall -> Color.YELLOW
            ghost -> Color(.72f, .72f, 1f, .58f)
            else -> Color.WHITE
        }
        drawBallSquare(region, ball.position.x - r, ball.position.y - r, diameter, ballTint)
    }

    /** ملاحظة صيانة: الدالة `aimGuide` تنفّذ مسؤولية محلية يعتمد عليها هذا الجزء من اللعبة؛ راجع استدعاءاتها واختباراتها قبل تعديلها. */
    private fun aimGuide() {
        if (!session.hasAttachedBalls()) return
        val direction = Vector2(aimIntent * 760f, 560f).nor()
        batch.end()
        Gdx.gl.glEnable(GL20.GL_BLEND)
        Gdx.gl.glBlendFunc(GL20.GL_SRC_ALPHA, GL20.GL_ONE_MINUS_SRC_ALPHA)
        aimRenderer.projectionMatrix = camera.combined
        aimRenderer.begin(ShapeRenderer.ShapeType.Filled)
        session.balls.filter { session.phase == GamePhase.SERVING || it.stuckOffset != null }.forEach { attachedBall ->
            repeat(7) { index ->
                val distance = attachedBall.radius + 36f + index * 43f
                val x = attachedBall.position.x + direction.x * distance
                val y = attachedBall.position.y + direction.y * distance
                if (x in 24f..876f && y < actionBarRect.y - 22f) {
                    val radius = (attachedBall.radius * .44f - index * .35f).coerceAtLeast(3f)
                    val alpha = .82f - index * .075f
                    aimRenderer.color = Color(.86f, .88f, .90f, alpha)
                    aimRenderer.circle(x, y, radius, 18)
                }
            }
        }
        aimRenderer.end()
        batch.begin()
    }

    /** ملاحظة صيانة: الدالة `hud` تنفّذ مسؤولية محلية يعتمد عليها هذا الجزء من اللعبة؛ راجع استدعاءاتها واختباراتها قبل تعديلها. */
    private fun hud() {
        draw(assets.ui.findRegion("panel"), hudPanelRect.x, hudPanelRect.y, hudPanelRect.width, hudPanelRect.height, Color(.01f, .04f, .09f, .96f))
        draw(assets.ui.findRegion("panel"), actionBarRect.x, actionBarRect.y, actionBarRect.width, actionBarRect.height, Color(.01f, .04f, .09f, .96f))
        drawHudCell("WORLD", if (customTestEditor != null) "CUSTOM" else level.world.toString(), 24f, 110f)
        drawHudCell("LEVEL", if (customTestEditor != null) "TEST" else level.id.toString(), 150f, 110f)
        drawHudCell("SCORE", session.score.toString(), 282f, 255f)
        drawHudCell("LIVES", session.lives.toString(), 492f, 92f)
        if (customTestEditor != null) {
            smallButton(customExitButton, "EXIT TEST", ForgeUiRenderer.GradientStyle.CRIMSON)
        } else {
            // Top gameplay action bar:
            // - utility buttons stay dark/neutral
            // - SHOP gets the main burnt-orange accent
            // - PAUSE gets the deep-crimson accent
            smallButton(inventoryButton, "BAG", ForgeUiRenderer.GradientStyle.NEUTRAL)
            smallButton(shopButton, "SHOP", ForgeUiRenderer.GradientStyle.PRIMARY)
        }
        smallButton(pauseButton, "PAUSE", ForgeUiRenderer.GradientStyle.CRIMSON)
        if (customTestEditor == null) {
            smallButton(customizeButton, "CUSTOMIZE", ForgeUiRenderer.GradientStyle.NEUTRAL)
        }
        activeEffectsHud()
        val message = when {
            !aimHintDismissed && aimHintTimer > 0f && session.phase == GamePhase.SERVING && session.hasAttachedBalls() ->
                if (aimDragActive) "RELEASE TO LAUNCH" else "DRAG TO AIM - RELEASE TO LAUNCH"
            else -> ""
        }
        val launchTextY = session.paddle.y + 170f
        if (message.isNotEmpty()) assets.bodyFont.draw(batch, message, 0f, launchTextY, 900f, Align.center, false)
    }

    /** ملاحظة صيانة: الدالة `activeEffectsHud` تنفّذ مسؤولية محلية يعتمد عليها هذا الجزء من اللعبة؛ راجع استدعاءاتها واختباراتها قبل تعديلها. */
    private fun activeEffectsHud() {
        val abilityY = actionBarRect.y - 52f
        draw(assets.ui.findRegion("panel"), 18f, abilityY, 864f, 44f, Color(.01f, .055f, .1f, .9f))
        assets.hudLabelFont.color = ForgeUiPalette.primaryLight
        assets.hudLabelFont.draw(
            batch,
            "PADDLE • ${session.paddleAbility.title} • ${session.paddleAbilityStatus()}",
            32f,
            abilityY + 31f,
            836f,
            Align.center,
            false,
        )
        assets.hudLabelFont.color = Color.WHITE

        val effects = session.powerUps.activeEffects().take(4)
        effects.forEachIndexed { index, (type, remaining) ->
            val x = 18f + (index % 2) * 432f
            val y = actionBarRect.y - 100f - (index / 2) * 48f
            draw(assets.ui.findRegion("panel"), x, y, 424f, 44f, Color(.01f, .055f, .1f, .88f))
            draw(assets.gameplayAtlas.powerUpIcon(type), x + 4f, y + 4f, 36f, 36f, Color.WHITE)
            val suffix = remaining?.let { " ${kotlin.math.ceil(it).toInt()}s" } ?: ""
            assets.hudLabelFont.draw(batch, PowerUpInfoRepository.info(type).shortName + suffix, x + 44f, y + 31f, 370f, Align.left, false)
        }
    }

    /** ملاحظة صيانة: الدالة `drawHudCell` ترسم العناصر المطلوبة مع الحفاظ على ترتيب طبقات العرض؛ راجع استدعاءاتها واختباراتها قبل تعديلها. */
    private fun drawHudCell(label: String, value: String, x: Float, width: Float) {
        val labelBaseline = hudPanelRect.y + 91f
        val valueBaseline = hudPanelRect.y + 43f
        assets.hudLabelFont.draw(batch, label, x, labelBaseline, width, Align.center, false)
        assets.hudValueFont.draw(batch, value, x, valueBaseline, width, Align.center, false)
    }

    /** ملاحظة صيانة: الدالة `smallButton` تنفّذ مسؤولية محلية يعتمد عليها هذا الجزء من اللعبة؛ راجع استدعاءاتها واختباراتها قبل تعديلها. */
    private fun smallButton(
        rect: Rectangle,
        label: String,
        style: ForgeUiRenderer.GradientStyle = ForgeUiRenderer.GradientStyle.NEUTRAL,
    ) {
        // Real gradient: no color-multiplication against the pre-coloured button texture.
        assets.uiRenderer.drawGradientButton(batch, rect, style)

        assets.hudLabelFont.color = ForgeUiPalette.textPrimary
        assets.hudLabelFont.draw(
            batch,
            label,
            rect.x,
            rect.y + rect.height * .67f,
            rect.width,
            Align.center,
            false,
        )
        assets.hudLabelFont.color = Color.WHITE
    }

    /** Animated game-over surface with a rewarded continue path capped at three uses per level attempt. */
    private fun drawGameOverOverlay() {
        val raw = if (game.progress.settings.reduceMotion) 1f else (gameOverOverlayTime / .34f).coerceIn(0f, 1f)
        val eased = raw * raw * (3f - 2f * raw)
        draw(assets.ui.findRegion("panel"), 0f, visibleBottom, 900f, visibleHeight, Color(0f, 0f, 0f, .68f * eased))

        val base = Rectangle(95f, 365f, 710f, 710f)
        val scale = if (game.progress.settings.reduceMotion) 1f else .92f + .08f * eased
        val panel = scaledRect(base, scale)
        draw(assets.ui.findRegion("panel"), panel.x, panel.y, panel.width, panel.height, Color(.015f, .035f, .075f, .97f * eased))

        val titleColor = Color(ForgeUiPalette.textPrimary).also { it.a *= eased }
        assets.pauseTitleFont.color = titleColor
        assets.pauseTitleFont.draw(batch, if (customTestEditor != null) "TEST OVER" else "OUT OF LIVES", 0f, 1005f, 900f, Align.center, false)
        assets.pauseTitleFont.color = Color.WHITE

        assets.smallFont.color = Color(ForgeUiPalette.textSecondary).also { it.a *= eased }
        val subtitle = if (customTestEditor != null) {
            "Restart the test to try again"
        } else {
            "Watch a rewarded video to continue this stage with ${GameSession.REWARDED_REVIVE_LIVES} lives"
        }
        assets.smallFont.draw(batch, subtitle, 135f, 928f, 630f, Align.center, true)
        assets.smallFont.color = Color.WHITE

        if (customTestEditor == null) {
            val remaining = session.rewardedRevivesRemaining
            assets.hudValueFont.color = ForgeUiPalette.primaryLight
            assets.hudValueFont.draw(batch, "+${GameSession.REWARDED_REVIVE_LIVES} LIVES", 0f, 842f, 900f, Align.center, false)
            assets.hudValueFont.color = Color.WHITE
            assets.hudLabelFont.color = ForgeUiPalette.textSecondary
            assets.hudLabelFont.draw(
                batch,
                "CONTINUES ${session.rewardedRevivesUsed}/${GameSession.MAX_REWARDED_REVIVES}  •  $remaining LEFT",
                0f,
                792f,
                900f,
                Align.center,
                false,
            )
            assets.hudLabelFont.color = Color.WHITE

            val pulse = if (
                game.progress.settings.reduceMotion ||
                game.monetization.rewardedReviveAdGateway.state != AdState.READY ||
                remaining <= 0
            ) {
                1f
            } else {
                1f + MathUtils.sin(gameOverOverlayTime * 5.2f) * .024f
            }
            val watchRect = scaledRect(rewardedContinueButton, pulse)
            val canContinue = remaining > 0
            val state = game.monetization.rewardedReviveAdGateway.state
            val label = when {
                !canContinue -> "REVIVE LIMIT REACHED"
                rewardedContinueInFlight || state == AdState.SHOWING -> "PLAYING VIDEO…"
                state == AdState.READY -> "WATCH VIDEO • CONTINUE"
                state == AdState.LOADING -> "LOADING REWARD…"
                state == AdState.UNAVAILABLE -> "RETRY REWARDED VIDEO"
                else -> "REWARDED VIDEO UNAVAILABLE"
            }
            smallButton(
                watchRect,
                label,
                if (canContinue && state == AdState.READY) ForgeUiRenderer.GradientStyle.PRIMARY else ForgeUiRenderer.GradientStyle.NEUTRAL,
            )
            if (rewardedContinueMessage.isNotBlank()) {
                assets.smallFont.color = ForgeUiPalette.primaryLight
                assets.smallFont.draw(batch, rewardedContinueMessage, 135f, 635f, 630f, Align.center, true)
                assets.smallFont.color = Color.WHITE
            }
        }

        smallButton(
            gameOverRestartButton,
            if (customTestEditor != null) "RESTART TEST" else "RESTART LEVEL",
            ForgeUiRenderer.GradientStyle.CRIMSON,
        )
        smallButton(
            gameOverMenuButton,
            if (customTestEditor != null) "EXIT TEST" else "MAIN MENU",
            ForgeUiRenderer.GradientStyle.NEUTRAL,
        )
        assets.smallFont.color = ForgeUiPalette.textSecondary
        assets.smallFont.draw(batch, "FORGEPULSE GAMES", 0f, 392f, 900f, Align.center, false)
        assets.smallFont.color = Color.WHITE
    }

    private fun scaledRect(rect: Rectangle, scale: Float): Rectangle {
        val width = rect.width * scale
        val height = rect.height * scale
        return Rectangle(rect.x + (rect.width - width) / 2f, rect.y + (rect.height - height) / 2f, width, height)
    }

    private fun handleGameOverInput(back: Boolean) {
        aimDragActive = false
        controlTouchActive = false
        if (back) {
            game.pausedSession.clear()
            if (customTestEditor != null) exitCustomTest() else game.openMenu()
            return
        }
        if (!Gdx.input.justTouched()) return

        when {
            customTestEditor == null && rewardedContinueButton.contains(pointer.x, pointer.y) -> requestRewardedContinue()

            gameOverRestartButton.contains(pointer.x, pointer.y) -> {
                game.pausedSession.clear()
                if (customTestEditor != null) game.playCustom(customTestEditor) else game.play(level.id)
            }

            gameOverMenuButton.contains(pointer.x, pointer.y) -> {
                game.pausedSession.clear()
                if (customTestEditor != null) exitCustomTest() else game.openMenu()
            }
        }
    }

    private fun requestRewardedContinue() {
        if (rewardedContinueInFlight || session.rewardedRevivesRemaining <= 0) return
        val gateway = game.monetization.rewardedReviveAdGateway
        when (gateway.state) {
            AdState.READY -> {
                rewardedContinueInFlight = true
                rewardedContinueMessage = "Opening rewarded video…"
                gateway.show { result ->
                    rewardedContinueInFlight = false
                    when (result) {
                        is RewardedAdResult.Earned -> {
                            if (session.reviveFromRewardedAd()) {
                                game.pausedSession.clear()
                                targetX = session.paddle.x
                                aimInputArmed = false
                                aimDragActive = false
                                controlTouchActive = false
                                rewardedContinueMessage = ""
                                feedbackText = "REVIVED • ${GameSession.REWARDED_REVIVE_LIVES} LIVES"
                                feedbackTimer = 2.2f
                                if (game.progress.settings.haptics) Gdx.input.vibrate(35)
                                assets.play("powerup", game.progress.settings.masterVolume * game.progress.settings.sfxVolume)
                            } else {
                                rewardedContinueMessage = "Continue could not be applied."
                            }
                        }

                        RewardedAdResult.ClosedWithoutReward ->
                            rewardedContinueMessage = "Finish the video to continue."

                        is RewardedAdResult.Failed -> rewardedContinueMessage = result.message
                    }
                }
            }

            AdState.UNAVAILABLE -> {
                rewardedContinueMessage = "Reloading rewarded video…"
                gateway.preload()
            }

            AdState.LOADING -> rewardedContinueMessage = "Rewarded video is still loading…"
            AdState.SHOWING -> rewardedContinueMessage = "Rewarded video is already playing…"
            AdState.DISABLED -> rewardedContinueMessage = "Rewarded videos are unavailable in this build."
        }
    }

    /** ملاحظة صيانة: الدالة `pauseOverlay` تنفّذ مسؤولية محلية يعتمد عليها هذا الجزء من اللعبة؛ راجع استدعاءاتها واختباراتها قبل تعديلها. */
    private fun pauseOverlay() {
        draw(assets.ui.findRegion("panel"), 0f, visibleBottom, 900f, visibleHeight, ForgeUiPalette.glassOverlay)
        draw(assets.ui.findRegion("panel"), 10f, 55f, 880f, 1265f, ForgeUiPalette.glassPanel)
        assets.pauseTitleFont.draw(batch, if (customTestEditor != null) "TEST PAUSED" else "PAUSED", 0f, 1200f, 900f, Align.center, false)
        if (customTestEditor != null) {
            overlayButton(overlayResume, "RESUME")
            overlayButton(overlaySettings, "RESTART TEST")
            overlayButton(overlayRestart, "EXIT TEST")
            assets.smallFont.draw(batch, "Test results never change campaign or paid Items", 0f, 390f, 900f, Align.center, false)
        } else {
            pauseArtButton(overlayResume, "resume")
            pauseArtButton(overlaySettings, "setting")
            pauseArtButton(overlayBag, "charms-bag")
            pauseArtButton(overlayShop, "shop")
            pauseArtButton(overlayCustomize, "customise")
            pauseArtButton(overlayRestart, "restart-level")
            pauseArtButton(overlayMenu, "main-menu")
            assets.smallFont.draw(batch, "Progress is saved while paused", 0f, 90f, 900f, Align.center, false)
        }
    }

    /** ملاحظة صيانة: الدالة `pauseArtButton` تنفّذ مسؤولية محلية يعتمد عليها هذا الجزء من اللعبة؛ راجع استدعاءاتها واختباراتها قبل تعديلها. */
    private fun pauseArtButton(rect: Rectangle, assetName: String) {
        batch.color = Color.WHITE
        val texture = assets.pauseMenuTexture(assetName)

        // Pause artwork is authored at 1500x350. Keep its native aspect ratio so the
        // frame/text never looks vertically crushed on tall phones.
        val drawWidth = 720f
        val drawHeight = drawWidth * texture.height.toFloat() / texture.width.toFloat()
        val drawX = (GameSession.WIDTH - drawWidth) * .5f
        val drawY = rect.y + (rect.height - drawHeight) * .5f
        batch.draw(texture, drawX, drawY, drawWidth, drawHeight)
    }

    /** ملاحظة صيانة: الدالة `overlayButton` تنفّذ مسؤولية محلية يعتمد عليها هذا الجزء من اللعبة؛ راجع استدعاءاتها واختباراتها قبل تعديلها. */
    private fun overlayButton(rect: Rectangle, text: String) {
        val tint = if (rect === overlayResume) ForgeUiPalette.purple else ForgeUiPalette.button
        draw(assets.ui.findRegion("button_primary"), rect.x, rect.y, rect.width, rect.height, tint)
        assets.buttonFont.draw(batch, text, rect.x, rect.y + rect.height * .64f, rect.width, Align.center, false)
    }

    /** ملاحظة صيانة: الدالة `input` تنفّذ مسؤولية محلية يعتمد عليها هذا الجزء من اللعبة؛ راجع استدعاءاتها واختباراتها قبل تعديلها. */
    private fun input() {
        if (Gdx.input.isTouched || Gdx.input.justTouched()) {
            pointer.set(Gdx.input.x.toFloat(), Gdx.input.y.toFloat(), 0f)
            viewport.unproject(pointer)
        }
        val back = Gdx.input.isKeyJustPressed(Input.Keys.ESCAPE) || Gdx.input.isKeyJustPressed(Input.Keys.BACK)
        if (session.phase == GamePhase.GAME_OVER) {
            handleGameOverInput(back)
            return
        }
        if (session.phase == GamePhase.PAUSED) {
            if (Gdx.input.justTouched()) {
                when {
                    overlayResume.contains(pointer.x, pointer.y) -> resumeGame()

                    overlaySettings.contains(pointer.x, pointer.y) -> if (customTestEditor != null) game.playCustom(customTestEditor) else game.setScreen(SettingsScreen(game, returnToPausedGame = true))

                    customTestEditor == null && overlayBag.contains(pointer.x, pointer.y) -> game.setScreen(CharmsBagScreen(game, ShopReturnDestination.PAUSED_GAME))

                    customTestEditor == null && overlayCustomize.contains(pointer.x, pointer.y) -> game.setScreen(CustomizationScreen(game, returnToPausedGame = true))

                    customTestEditor == null && overlayShop.contains(pointer.x, pointer.y) -> game.setScreen(ShopScreen(game, ShopReturnDestination.PAUSED_GAME))

                    overlayRestart.contains(pointer.x, pointer.y) -> if (customTestEditor != null) {
                        exitCustomTest()
                    } else {
                        game.pausedSession.clear()
                        game.play(level.id)
                    }

                    customTestEditor == null && overlayMenu.contains(pointer.x, pointer.y) -> game.openMenu()
                }
            }
            if (back) resumeGame()
            return
        }
        if (session.phase == GamePhase.LEVEL_COMPLETE) {
            aimDragActive = false
            controlTouchActive = false
            return
        }
        if (!aimInputArmed) {
            if (!Gdx.input.isTouched) aimInputArmed = true
            return
        }
        if (Gdx.input.justTouched()) {
            when {
                pauseButton.contains(pointer.x, pointer.y) -> {
                    pauseAndSave()
                    return
                }

                customTestEditor != null && customExitButton.contains(pointer.x, pointer.y) -> {
                    exitCustomTest()
                    return
                }

                inventoryButton.contains(pointer.x, pointer.y) -> {
                    pauseAndSave()
                    game.setScreen(CharmsBagScreen(game, ShopReturnDestination.PAUSED_GAME))
                    return
                }

                shopButton.contains(pointer.x, pointer.y) -> {
                    pauseAndSave()
                    game.setScreen(ShopScreen(game, ShopReturnDestination.PAUSED_GAME))
                    return
                }

                customTestEditor == null && customizeButton.contains(pointer.x, pointer.y) -> {
                    pauseAndSave()
                    game.setScreen(CustomizationScreen(game, returnToPausedGame = true))
                    return
                }

                actionBarRect.contains(pointer.x, pointer.y) || hudPanelRect.contains(pointer.x, pointer.y) -> return

                else -> if (session.hasAttachedBalls()) {
                    aimDragActive = true
                    aimDragStartX = pointer.x
                    aimDragCurrentX = pointer.x
                    aimIntent = 0f
                } else {
                    session.action()
                }
            }
        }
        if (Gdx.input.isTouched) {
            if (aimDragActive) {
                aimDragCurrentX = pointer.x
                aimIntent = ((aimDragCurrentX - aimDragStartX) / 220f).coerceIn(-1f, 1f)
            } else if (game.progress.settings.relativeControl) {
                if (!controlTouchActive) {
                    controlTouchActive = true
                    controlLastX = pointer.x
                } else {
                    val sensitivity = game.progress.settings.sensitivity.coerceIn(.5f, 2f)
                    targetX += (pointer.x - controlLastX) * sensitivity
                    controlLastX = pointer.x
                }
            } else {
                controlTouchActive = false
                targetX = pointer.x
            }
        } else {
            controlTouchActive = false
            if (aimDragActive) {
                aimDragActive = false
                session.launchAttachedBalls(aimIntent)
                aimIntent = 0f
            }
        }
        val distance = 700f * Gdx.graphics.deltaTime
        if (Gdx.input.isKeyPressed(Input.Keys.LEFT)) targetX -= distance
        if (Gdx.input.isKeyPressed(Input.Keys.RIGHT)) targetX += distance
        targetX = targetX.coerceIn(0f, 900f)
        if (Gdx.input.isKeyJustPressed(Input.Keys.SPACE)) session.action()
        if (back) pauseAndSave()
    }

    /** ملاحظة صيانة: الدالة `pauseAndSave` تنفّذ مسؤولية محلية يعتمد عليها هذا الجزء من اللعبة؛ راجع استدعاءاتها واختباراتها قبل تعديلها. */
    private fun pauseAndSave() {
        if (session.phase == GamePhase.PAUSED) return
        resumePhase = session.phase
        if (customTestEditor == null) game.pausedSession.save(level, session)
        session.phase = GamePhase.PAUSED
    }

    /** ملاحظة صيانة: الدالة `resumeGame` تنفّذ انتقالًا أو تعرض التدفق المطلوب للمستخدم؛ راجع استدعاءاتها واختباراتها قبل تعديلها. */
    private fun resumeGame() {
        session.phase = resumePhase
        if (customTestEditor == null) game.pausedSession.clear()
    }

    /** Large scrollable Items / power-up guide overlay. */
    private fun inventoryItemsTabRect(): Rectangle = Rectangle(82f, INVENTORY_TAB_Y, 345f, INVENTORY_TAB_HEIGHT)

    private fun inventoryGuideTabRect(): Rectangle = Rectangle(473f, INVENTORY_TAB_Y, 345f, INVENTORY_TAB_HEIGHT)

    private fun inventoryUseButtonRect(): Rectangle = Rectangle(90f, INVENTORY_ACTION_Y, 220f, INVENTORY_ACTION_HEIGHT)

    private fun inventoryCloseButtonRect(): Rectangle = Rectangle(340f, INVENTORY_ACTION_Y, 220f, INVENTORY_ACTION_HEIGHT)

    private fun inventoryShopButtonRect(): Rectangle = Rectangle(590f, INVENTORY_ACTION_Y, 220f, INVENTORY_ACTION_HEIGHT)

    /** ملاحظة صيانة: الدالة `inventoryOverlay` ترسم شاشة "العناصر / دليل الباور-أب" الكاملة بما فيها التابات وبطاقة التفاصيل والأزرار؛ راجع استدعاءاتها واختباراتها قبل تعديلها. */
    private fun inventoryOverlay() {
        assets.uiRenderer.drawGradientPanel(
            batch,
            Rectangle(42f, 235f, 816f, 1145f),
            ForgeUiRenderer.GradientStyle.NEUTRAL,
        )

        // Header: keep the title clear of the accent rule and leave a stronger gap before the tabs.
        assets.pauseTitleFont.color = ForgeUiPalette.textPrimary
        assets.pauseTitleFont.draw(
            batch,
            if (inventoryGuideMode) "POWER-UP GUIDE" else "MY ITEMS",
            0f,
            INVENTORY_TITLE_BASELINE,
            900f,
            Align.center,
            false,
        )
        assets.pauseTitleFont.color = Color.WHITE
        assets.uiRenderer.drawGradientProgress(
            batch,
            Rectangle(150f, INVENTORY_TITLE_RULE_Y, 600f, 4f),
            ForgeUiRenderer.GradientStyle.PRIMARY,
        )

        val itemsTab = inventoryItemsTabRect()
        val guideTab = inventoryGuideTabRect()
        inventoryTab(itemsTab, "MY ITEMS 14", !inventoryGuideMode)
        inventoryTab(guideTab, "GUIDE 20", inventoryGuideMode)

        val entries = inventoryEntries()
        inventoryScrollOffset = inventoryScrollOffset.coerceIn(0f, inventoryMaxScroll(entries.size))
        beginInventoryClip()
        entries.forEachIndexed { index, type -> drawInventoryCard(index, type) }
        endInventoryClip()

        // Selected-item details are separated from the scroll list so text never feels squeezed.
        val selectedType = selectedInventoryType(entries)
        val selectedCategory = PowerUpCatalog.definitions.getValue(selectedType).category
        val info = PowerUpInfoRepository.info(selectedType)
        val detailRect = Rectangle(82f, INVENTORY_DETAIL_Y, 736f, INVENTORY_DETAIL_HEIGHT)
        assets.uiRenderer.drawGradientBorderPanel(
            batch,
            detailRect,
            ForgeUiRenderer.GradientStyle.PRIMARY,
            4f,
        )

        val detailIconPanel = Rectangle(detailRect.x + 14f, detailRect.y + 23f, 104f, 104f)
        assets.uiRenderer.drawGradientPanel(batch, detailIconPanel, ForgeUiRenderer.GradientStyle.NEUTRAL)
        draw(
            assets.gameplayAtlas.powerUpIcon(selectedType),
            detailIconPanel.x + 5f,
            detailIconPanel.y + 5f,
            94f,
            94f,
            Color.WHITE,
        )

        val detailTextX = detailRect.x + 138f
        val detailTextWidth = detailRect.width - 158f
        assets.hudLabelFont.color = ForgeUiPalette.primaryLight
        assets.hudLabelFont.draw(
            batch,
            info.fullName,
            detailTextX,
            detailRect.y + 118f,
            detailTextWidth,
            Align.left,
            false,
        )
        assets.hudLabelFont.color = Color.WHITE

        assets.smallFont.color = ForgeUiPalette.textSecondary
        drawInventoryDescription(
            info.description,
            detailTextX,
            detailRect.y + 84f,
            detailTextWidth,
            .80f,
        )
        assets.smallFont.color = Color.WHITE

        if (inventoryGuideMode) {
            val availability = if (selectedType in SHOP_ELIGIBLE_TYPES) {
                "POSITIVE ITEM • CAN BE OWNED / USED"
            } else {
                when (selectedCategory) {
                    PowerUpCategory.BAD -> "HAZARD DROP • NOT SOLD"
                    PowerUpCategory.SPECIAL -> "SPECIAL DROP • NOT SOLD"
                    PowerUpCategory.GOOD -> "GAMEPLAY DROP"
                }
            }
            assets.smallFont.color = when {
                selectedType in SHOP_ELIGIBLE_TYPES -> ForgeUiPalette.successLight
                selectedCategory == PowerUpCategory.BAD -> ForgeUiPalette.crimsonLight
                selectedCategory == PowerUpCategory.SPECIAL -> ForgeUiPalette.primaryLight
                else -> ForgeUiPalette.textSecondary
            }
            drawInventoryDescription(
                availability,
                detailTextX,
                detailRect.y + 42f,
                detailTextWidth,
                .68f,
            )
            assets.smallFont.color = Color.WHITE
        }

        val useRect = inventoryUseButtonRect()
        if (!inventoryGuideMode) {
            val enabled = game.boosterInventory.count(selectedType) > 0 && session.canActivatePowerUp(selectedType)
            assets.uiRenderer.drawGradientButton(
                batch,
                useRect,
                if (enabled) ForgeUiRenderer.GradientStyle.SUCCESS else ForgeUiRenderer.GradientStyle.DISABLED,
            )
            assets.buttonFont.color = ForgeUiPalette.textPrimary
            assets.buttonFont.draw(batch, "USE", useRect.x, useRect.y + 55f, useRect.width, Align.center, false)
            assets.buttonFont.color = Color.WHITE
        } else {
            assets.uiRenderer.drawGradientButton(batch, useRect, ForgeUiRenderer.GradientStyle.NEUTRAL)
            assets.hudLabelFont.color = ForgeUiPalette.textSecondary
            assets.hudLabelFont.draw(batch, "READ ONLY", useRect.x, useRect.y + 53f, useRect.width, Align.center, false)
            assets.hudLabelFont.color = Color.WHITE
        }

        val closeRect = inventoryCloseButtonRect()
        assets.uiRenderer.drawGradientButton(batch, closeRect, ForgeUiRenderer.GradientStyle.NEUTRAL)
        assets.buttonFont.draw(batch, "CLOSE", closeRect.x, closeRect.y + 55f, closeRect.width, Align.center, false)

        val shopRect = inventoryShopButtonRect()
        assets.uiRenderer.drawGradientButton(batch, shopRect, ForgeUiRenderer.GradientStyle.PRIMARY)
        assets.buttonFont.draw(batch, "SHOP", shopRect.x, shopRect.y + 55f, shopRect.width, Align.center, false)

        assets.smallFont.color = ForgeUiPalette.muted
        drawInventoryDescription(
            "DRAG THE LIST TO SCROLL",
            0f,
            INVENTORY_SCROLL_HINT_BASELINE,
            900f,
            .72f,
            Align.center,
        )
        assets.smallFont.color = Color.WHITE
    }

    /** ملاحظة صيانة: الدالة `inventoryTab` ترسم زر تبويب واحد ("MY ITEMS"/"GUIDE") وتموّنه حسب حالة التحديد؛ راجع استدعاءاتها واختباراتها قبل تعديلها. */
    private fun inventoryTab(rect: Rectangle, label: String, selected: Boolean) {
        assets.uiRenderer.drawGradientButton(
            batch,
            rect,
            if (selected) ForgeUiRenderer.GradientStyle.PRIMARY else ForgeUiRenderer.GradientStyle.NEUTRAL,
        )
        assets.hudLabelFont.color = if (selected) ForgeUiPalette.textPrimary else ForgeUiPalette.textSecondary
        assets.hudLabelFont.draw(batch, label, rect.x, rect.y + 47f, rect.width, Align.center, false)
        assets.hudLabelFont.color = Color.WHITE
    }

    /** ملاحظة صيانة: الدالة `inventoryEntries` تُرجع قائمة أنواع الباور-أب المناسبة حسب الوضع الحالي (عناصر مملوكة أو الدليل الكامل)؛ راجع استدعاءاتها واختباراتها قبل تعديلها. */
    private fun inventoryEntries(): List<PowerUpType> = if (inventoryGuideMode) PowerUpCatalog.classicOrderedTypes else SHOP_ELIGIBLE_TYPES

    /** ملاحظة صيانة: الدالة `selectedInventoryType` تُرجع نوع الباور-أب المحدد حاليًا ضمن القائمة المعروضة؛ راجع استدعاءاتها واختباراتها قبل تعديلها. */
    private fun selectedInventoryType(entries: List<PowerUpType> = inventoryEntries()): PowerUpType {
        val index = if (inventoryGuideMode) selectedGuidePowerUp else selectedBooster
        return entries[index.coerceIn(entries.indices)]
    }

    /** ملاحظة صيانة: الدالة `inventoryCardRect` تحسب مستطيل الموضع والحجم الخاص ببطاقة عنصر معيّن ضمن القائمة القابلة للتمرير؛ راجع استدعاءاتها واختباراتها قبل تعديلها. */
    private fun inventoryCardRect(index: Int): Rectangle = Rectangle(
        80f,
        ITEMS_LIST_TOP - ITEMS_CARD_HEIGHT - ITEMS_LIST_INSET - index * ITEMS_CARD_STEP + inventoryScrollOffset,
        740f,
        ITEMS_CARD_HEIGHT,
    )

    /** ملاحظة صيانة: الدالة `drawInventoryCard` ترسم بطاقة عنصر واحد ضمن القائمة القابلة للتمرير مع شريط التصنيف اللوني وشارة الملكية؛ راجع استدعاءاتها واختباراتها قبل تعديلها. */
    private fun drawInventoryCard(index: Int, type: PowerUpType) {
        val rect = inventoryCardRect(index)
        val selectedIndex = if (inventoryGuideMode) selectedGuidePowerUp else selectedBooster
        val selected = index == selectedIndex
        val category = PowerUpCatalog.definitions.getValue(type).category
        val stripeStyle = when (category) {
            PowerUpCategory.BAD -> ForgeUiRenderer.GradientStyle.CRIMSON
            PowerUpCategory.SPECIAL -> ForgeUiRenderer.GradientStyle.PRIMARY
            PowerUpCategory.GOOD -> ForgeUiRenderer.GradientStyle.SUCCESS
        }

        if (selected) {
            assets.uiRenderer.drawGradientBorderPanel(batch, rect, ForgeUiRenderer.GradientStyle.PRIMARY, 5f)
        } else {
            assets.uiRenderer.drawGradientPanel(batch, rect, ForgeUiRenderer.GradientStyle.NEUTRAL)
        }

        assets.uiRenderer.drawGradientProgress(batch, Rectangle(rect.x, rect.y, 9f, rect.height), stripeStyle)

        // Icon gets a little more breathing room without changing the existing visual style.
        val iconPanel = Rectangle(rect.x + 18f, rect.y + 24f, 100f, 100f)
        assets.uiRenderer.drawGradientPanel(batch, iconPanel, ForgeUiRenderer.GradientStyle.NEUTRAL)
        draw(
            assets.gameplayAtlas.powerUpIcon(type),
            iconPanel.x + 5f,
            iconPanel.y + 5f,
            90f,
            90f,
            Color.WHITE,
        )

        // Reserve a fixed right-side status column so every card lines up identically.
        val chipWidth = 102f
        val chipX = rect.x + rect.width - chipWidth - 18f
        val textX = rect.x + 136f
        val textWidth = chipX - textX - 14f

        val info = PowerUpInfoRepository.info(type)
        assets.hudLabelFont.color = if (selected) ForgeUiPalette.primaryLight else ForgeUiPalette.textPrimary
        assets.hudLabelFont.draw(
            batch,
            info.shortName,
            textX,
            rect.y + 122f,
            textWidth,
            Align.left,
            false,
        )
        assets.hudLabelFont.color = Color.WHITE

        assets.smallFont.color = ForgeUiPalette.textSecondary
        drawInventoryDescription(
            info.description,
            textX,
            rect.y + 86f,
            textWidth,
            .78f,
        )
        assets.smallFont.color = Color.WHITE

        val chipRect = Rectangle(chipX, rect.y + 34f, chipWidth, 80f)
        assets.uiRenderer.drawGradientPanel(batch, chipRect, ForgeUiRenderer.GradientStyle.NEUTRAL)
        assets.uiRenderer.drawGradientProgress(
            batch,
            Rectangle(chipRect.x, chipRect.y, chipRect.width, 4f),
            stripeStyle,
        )

        if (type in SHOP_ELIGIBLE_TYPES) {
            val owned = game.boosterInventory.count(type)
            assets.hudLabelFont.color = if (owned > 0) ForgeUiPalette.successLight else ForgeUiPalette.muted
            assets.hudLabelFont.draw(batch, "x$owned", chipRect.x, rect.y + 84f, chipRect.width, Align.center, false)
            assets.hudLabelFont.color = Color.WHITE
            assets.smallFont.color = ForgeUiPalette.muted
            drawInventoryDescription("OWNED", chipRect.x, rect.y + 53f, chipRect.width, .64f, Align.center)
            assets.smallFont.color = Color.WHITE
        } else {
            assets.smallFont.color = when (category) {
                PowerUpCategory.BAD -> ForgeUiPalette.crimsonLight
                PowerUpCategory.SPECIAL -> ForgeUiPalette.primaryLight
                PowerUpCategory.GOOD -> ForgeUiPalette.textSecondary
            }
            drawInventoryDescription(
                if (category == PowerUpCategory.BAD) "HAZARD" else "DROP",
                chipRect.x,
                rect.y + 78f,
                chipRect.width,
                .66f,
                Align.center,
            )
            assets.smallFont.color = ForgeUiPalette.muted
            drawInventoryDescription("ONLY", chipRect.x, rect.y + 49f, chipRect.width, .60f, Align.center)
            assets.smallFont.color = Color.WHITE
        }
    }

    /** ملاحظة صيانة: الدالة `inventoryMaxScroll` تحسب أقصى مسافة تمرير مسموحة لقائمة العناصر حسب عددها؛ راجع استدعاءاتها واختباراتها قبل تعديلها. */
    private fun inventoryMaxScroll(count: Int): Float {
        if (count <= 0) return 0f
        val contentHeight = ITEMS_LIST_INSET * 2f + ITEMS_CARD_HEIGHT + (count - 1) * ITEMS_CARD_STEP
        return (contentHeight - (ITEMS_LIST_TOP - ITEMS_LIST_BOTTOM)).coerceAtLeast(0f)
    }

    /** ملاحظة صيانة: الدالة `beginInventoryClip` تُفعّل قصّ الرسم (scissor) لحصر رسم القائمة داخل حدودها المرئية؛ راجع استدعاءاتها واختباراتها قبل تعديلها. */
    private fun beginInventoryClip() {
        batch.flush()
        val bottom = Vector3(0f, ITEMS_LIST_BOTTOM, 0f)
        val top = Vector3(GameSession.WIDTH, ITEMS_LIST_TOP, 0f)
        viewport.project(bottom)
        viewport.project(top)
        Gdx.gl.glEnable(GL20.GL_SCISSOR_TEST)
        Gdx.gl.glScissor(
            bottom.x.toInt(),
            bottom.y.toInt(),
            (top.x - bottom.x).toInt().coerceAtLeast(1),
            (top.y - bottom.y).toInt().coerceAtLeast(1),
        )
    }

    /** ملاحظة صيانة: الدالة `endInventoryClip` تُلغي قصّ الرسم بعد الانتهاء من رسم القائمة؛ راجع استدعاءاتها واختباراتها قبل تعديلها. */
    private fun endInventoryClip() {
        batch.flush()
        Gdx.gl.glDisable(GL20.GL_SCISSOR_TEST)
    }

    /** ملاحظة صيانة: الدالة `handleInventoryInput` تعالج لمسات المستخدم داخل شاشة العناصر (التابات، التمرير، الأزرار) وتحدّث الحالة تبعًا لذلك؛ راجع استدعاءاتها واختباراتها قبل تعديلها. */
    private fun handleInventoryInput(back: Boolean) {
        if (back) {
            closeInventory()
            return
        }

        // Use the exact same rectangles used by drawing so touch targets can never drift out of alignment.
        val itemsTab = inventoryItemsTabRect()
        val guideTab = inventoryGuideTabRect()
        val useRect = inventoryUseButtonRect()
        val closeRect = inventoryCloseButtonRect()
        val shopRect = inventoryShopButtonRect()

        if (Gdx.input.justTouched()) {
            when {
                itemsTab.contains(pointer.x, pointer.y) -> {
                    inventoryGuideMode = false
                    inventoryScrollOffset = 0f
                    inventoryTouchActive = false
                    return
                }

                guideTab.contains(pointer.x, pointer.y) -> {
                    inventoryGuideMode = true
                    inventoryScrollOffset = 0f
                    inventoryTouchActive = false
                    return
                }

                !inventoryGuideMode && useRect.contains(pointer.x, pointer.y) -> {
                    val type = selectedInventoryType()
                    val result = session.useItem(type, game.boosterInventory)
                    if (result is BoosterUseResult.Applied) {
                        closeInventory()
                    } else if (result is BoosterUseResult.Rejected) {
                        feedbackText = result.reason
                        feedbackTimer = 1.8f
                    }
                    return
                }

                closeRect.contains(pointer.x, pointer.y) -> {
                    closeInventory()
                    return
                }

                shopRect.contains(pointer.x, pointer.y) -> {
                    openShopFromInventory()
                    return
                }

                pointer.y in ITEMS_LIST_BOTTOM..ITEMS_LIST_TOP -> {
                    inventoryTouchActive = true
                    inventoryTouchMoved = false
                    inventoryTouchStartX = pointer.x
                    inventoryTouchStartY = pointer.y
                    inventoryTouchLastX = pointer.x
                    inventoryTouchLastY = pointer.y
                }
            }
        }

        if (inventoryTouchActive && Gdx.input.isTouched) {
            val dy = pointer.y - inventoryTouchLastY
            inventoryScrollOffset = (inventoryScrollOffset + dy).coerceIn(0f, inventoryMaxScroll(inventoryEntries().size))
            inventoryTouchLastX = pointer.x
            inventoryTouchLastY = pointer.y
            if (abs(pointer.y - inventoryTouchStartY) > 20f || abs(pointer.x - inventoryTouchStartX) > 20f) inventoryTouchMoved = true
        } else if (inventoryTouchActive) {
            if (!inventoryTouchMoved) {
                val index = inventoryEntries().indices.firstOrNull {
                    inventoryCardRect(it).contains(inventoryTouchLastX, inventoryTouchLastY)
                }
                if (index != null) {
                    if (inventoryGuideMode) selectedGuidePowerUp = index else selectedBooster = index
                }
            }
            inventoryTouchActive = false
        }
    }

    /** ملاحظة صيانة: الدالة `openShopFromInventory` تحفظ حالة الجلسة وتنتقل إلى شاشة المتجر مباشرة من شاشة العناصر؛ راجع استدعاءاتها واختباراتها قبل تعديلها. */
    private fun openShopFromInventory() {
        session.phase = resumePhase
        game.pausedSession.save(level, session)
        session.phase = GamePhase.PAUSED
        inventoryOpen = false
        game.setScreen(ShopScreen(game, ShopReturnDestination.PAUSED_GAME))
    }

    /** ملاحظة صيانة: الدالة `closeInventory` تُغلق شاشة العناصر وتُعيد اللعبة إلى الحالة التي كانت عليها قبل فتحها؛ راجع استدعاءاتها واختباراتها قبل تعديلها. */
    private fun closeInventory() {
        inventoryOpen = false
        inventoryTouchActive = false
        session.phase = resumePhase
    }

    private companion object {
        const val INVENTORY_TITLE_BASELINE = 1340f
        const val INVENTORY_TITLE_RULE_Y = 1278f
        const val INVENTORY_TAB_Y = 1185f
        const val INVENTORY_TAB_HEIGHT = 70f

        const val ITEMS_LIST_BOTTOM = 555f
        const val ITEMS_LIST_TOP = 1158f
        const val ITEMS_LIST_INSET = 12f
        const val ITEMS_CARD_HEIGHT = 148f
        const val ITEMS_CARD_STEP = 160f

        const val INVENTORY_SCROLL_HINT_BASELINE = 536f
        const val INVENTORY_DETAIL_Y = 360f
        const val INVENTORY_DETAIL_HEIGHT = 150f
        const val INVENTORY_ACTION_Y = 260f
        const val INVENTORY_ACTION_HEIGHT = 82f

        const val LEVEL_COMPLETE_TRANSITION_SECONDS = 1.0f
        const val AIM_HINT_DURATION_SECONDS = 4f
    }

    private fun drawInventoryDescription(
        text: String,
        x: Float,
        y: Float,
        width: Float,
        scale: Float,
        align: Int = Align.left,
    ) {
        val oldX = assets.smallFont.data.scaleX
        val oldY = assets.smallFont.data.scaleY
        assets.smallFont.data.setScale(scale)
        assets.smallFont.draw(batch, text, x, y, width, align, true)
        assets.smallFont.data.setScale(oldX, oldY)
    }

    /** ملاحظة صيانة: الدالة `draw` ترسم العناصر المطلوبة مع الحفاظ على ترتيب طبقات العرض؛ راجع استدعاءاتها واختباراتها قبل تعديلها. */
    private fun draw(region: TextureRegion, x: Float, y: Float, width: Float, height: Float, color: Color) {
        batch.color = color
        batch.draw(region, x, y, width, height)
        batch.color = Color.WHITE
    }

    /** ملاحظة صيانة: الدالة `drawRegionFit` ترسم العناصر المطلوبة مع الحفاظ على ترتيب طبقات العرض؛ راجع استدعاءاتها واختباراتها قبل تعديلها. */
    private fun drawRegionFit(region: TextureRegion, x: Float, y: Float, width: Float, height: Float, color: Color) {
        val scale = minOf(width / region.regionWidth, height / region.regionHeight)
        val drawWidth = region.regionWidth * scale
        val drawHeight = region.regionHeight * scale
        draw(region, x + (width - drawWidth) / 2f, y + (height - drawHeight) / 2f, drawWidth, drawHeight, color)
    }

    /** Ball physics are circular, so cosmetic source rectangles are normalized to a square at draw time. */

    /** ملاحظة صيانة: الدالة `drawBallSquare` ترسم العناصر المطلوبة مع الحفاظ على ترتيب طبقات العرض؛ راجع استدعاءاتها واختباراتها قبل تعديلها. */
    private fun drawBallSquare(region: TextureRegion, x: Float, y: Float, diameter: Float, color: Color) {
        val scale = minOf(diameter / region.regionWidth, diameter / region.regionHeight)
        val drawWidth = region.regionWidth * scale
        val drawHeight = region.regionHeight * scale
        draw(region, x + (diameter - drawWidth) / 2f, y + (diameter - drawHeight) / 2f, drawWidth, drawHeight, color)
    }

    /** ملاحظة صيانة: الدالة `drawBackgroundCover` ترسم العناصر المطلوبة مع الحفاظ على ترتيب طبقات العرض؛ راجع استدعاءاتها واختباراتها قبل تعديلها. */
    private fun drawBackgroundCover(region: TextureRegion, tint: Color) {
        val sourceAspect = region.regionWidth.toFloat() / region.regionHeight.toFloat()
        val targetAspect = visibleWidth / visibleHeight
        val drawWidth: Float
        val drawHeight: Float
        if (sourceAspect > targetAspect) {
            drawHeight = visibleHeight
            drawWidth = drawHeight * sourceAspect
        } else {
            drawWidth = visibleWidth
            drawHeight = drawWidth / sourceAspect
        }
        val drawX = visibleLeft + (visibleWidth - drawWidth) / 2f
        val drawY = visibleBottom + (visibleHeight - drawHeight) / 2f
        draw(region, drawX, drawY, drawWidth, drawHeight, tint)
    }

    /** ملاحظة صيانة: الدالة `drawDeathRail` ترسم العناصر المطلوبة مع الحفاظ على ترتيب طبقات العرض؛ راجع استدعاءاتها واختباراتها قبل تعديلها. */
    private fun drawDeathRail() {
        val energy = assets.gameplayAtlas.electricFloorWire
        val spark = assets.gameplayAtlas.sparkFrames[((deathRailTime * 16f).toInt()).mod(8)]
        val pulse = .72f + .18f * MathUtils.sin(deathRailTime * 5.4f)
        draw(energy, deathRailRect.x - 8f, deathRailRect.y - 7f, deathRailRect.width + 16f, deathRailRect.height + 14f, Color(.05f, .55f, 1f, .28f * pulse))
        draw(energy, deathRailRect.x, deathRailRect.y, deathRailRect.width, deathRailRect.height, Color(.2f, .9f, 1f, .98f))
        draw(spark, deathRailRect.x - 10f, deathRailRect.y - 2f, 24f, 24f, Color(.2f, .92f, 1f, .9f))
        draw(spark, deathRailRect.x + deathRailRect.width - 14f, deathRailRect.y - 2f, 24f, 24f, Color(.2f, .92f, 1f, .9f))
        repeat(3) { index ->
            val travel = (deathRailTime * (.34f + index * .07f) + index * .31f) % 1f
            val x = deathRailRect.x + 16f + travel * (deathRailRect.width - 32f)
            val flicker = .36f + .28f * MathUtils.sin(deathRailTime * 13f + index * 2.1f)
            draw(spark, x - 7f, deathRailRect.y + 2f, 14f, 14f, Color(.5f, .98f, 1f, flicker))
        }
        if (deathRailZapTimer > 0f) {
            val strength = (deathRailZapTimer / .14f).coerceIn(0f, 1f)
            draw(spark, deathRailZapX - 30f, deathRailRect.y - 20f, 60f, 60f, Color(.65f, .98f, 1f, strength))
        }
    }

    /** ملاحظة صيانة: الدالة `updateResponsiveLayout` تحدّث الحالة المتغيرة خلال دورة التشغيل أو المحاكاة؛ راجع استدعاءاتها واختباراتها قبل تعديلها. */
    private fun updateResponsiveLayout() {
        val visibleTop = visibleBottom + visibleHeight
        val hudTop = visibleTop - 18f
        hudPanelRect.set(12f, hudTop - 108f, 876f, 108f)
        actionBarRect.set(12f, hudPanelRect.y - 80f, 876f, 70f)
        if (game.progress.settings.leftHanded && customTestEditor == null) {
            pauseButton.set(24f, actionBarRect.y, 188f, actionBarRect.height)
            customizeButton.set(220f, actionBarRect.y, 290f, actionBarRect.height)
            shopButton.set(518f, actionBarRect.y, 160f, actionBarRect.height)
            inventoryButton.set(686f, actionBarRect.y, 190f, actionBarRect.height)
        } else {
            inventoryButton.set(24f, actionBarRect.y, 190f, actionBarRect.height)
            shopButton.set(222f, actionBarRect.y, 160f, actionBarRect.height)
            customizeButton.set(390f, actionBarRect.y, 290f, actionBarRect.height)
            pauseButton.set(688f, actionBarRect.y, 188f, actionBarRect.height)
        }
        customExitButton.set(24f, actionBarRect.y, 372f, actionBarRect.height)
        // Full-width kill rail between the two playfield walls, matching the
        // visible boundary instead of appearing as a short centered bar.
        deathRailRect.set(18f, visibleBottom + 54f, GameSession.WIDTH - 36f, 12f)
        session.deathRailTop = if (level.deathRailEnabled) deathRailRect.y + deathRailRect.height else -200f
    }

    /** ملاحظة صيانة: الدالة `finishLevel` تنفّذ مسؤولية محلية يعتمد عليها هذا الجزء من اللعبة؛ راجع استدعاءاتها واختباراتها قبل تعديلها. */
    private fun finishLevel() {
        val stars = when {
            session.score >= level.threeStarScore -> 3
            session.score >= level.threeStarScore / 2 -> 2
            else -> 1
        }
        if (customTestEditor != null) {
            game.setScreen(CustomTestResultScreen(game, customTestEditor, session.score, stars))
            return
        }
        game.pausedSession.clear()
        val previousStars = game.progress.stars(level.id)
        game.progress.complete(level.id, session.score, stars)
        val cosmeticUnlocks = game.cosmeticProgression.onCampaignResult(
            level.id, previousStars, game.progress, game.developmentAccess.enabled
        )
        game.setScreen(ResultsScreen(game, level, session.score, stars, cosmeticUnlocks))
    }

    /** ملاحظة صيانة: الدالة `exitCustomTest` تنفّذ مسؤولية محلية يعتمد عليها هذا الجزء من اللعبة؛ راجع استدعاءاتها واختباراتها قبل تعديلها. */
    private fun exitCustomTest() {
        customTestEditor?.let { game.setScreen(LevelEditorScreen(game, it)) }
    }

    /** ملاحظة صيانة: الدالة `pause` تنفّذ العقد الموروث وتربط دورة حياة المكوّن بسلوك هذا الملف؛ راجع استدعاءاتها واختباراتها قبل تعديلها. */
    override fun pause() {
        // A full-screen rewarded ad can pause the Android activity. Keep GAME_OVER intact so
        // the earned-reward callback can legally revive the same run when the ad finishes.
        if (rewardedContinueInFlight && session.phase == GamePhase.GAME_OVER) {
            if (customTestEditor == null) game.pausedSession.save(level, session)
            return
        }
        if (session.phase != GamePhase.PAUSED) pauseAndSave()
    }

    /** ملاحظة صيانة: الدالة `resize` تنفّذ العقد الموروث وتربط دورة حياة المكوّن بسلوك هذا الملف؛ راجع استدعاءاتها واختباراتها قبل تعديلها. */
    override fun resize(width: Int, height: Int) {
        viewport.update(width, height, false)
        camera.position.set(GameSession.WIDTH / 2f, GameSession.HEIGHT / 2f, 0f)
        camera.update()
        visibleWidth = viewport.worldWidth
        visibleHeight = viewport.worldHeight
        visibleLeft = camera.position.x - visibleWidth / 2f
        visibleBottom = camera.position.y - visibleHeight / 2f
        updateResponsiveLayout()
    }

    /** ملاحظة صيانة: الدالة `dispose` تنفّذ العقد الموروث وتربط دورة حياة المكوّن بسلوك هذا الملف؛ راجع استدعاءاتها واختباراتها قبل تعديلها. */
    override fun dispose() {
        WorldVideoBackgrounds.hide()
        batch.dispose()
        aimRenderer.dispose()
    }
}
