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

class GameScreen(
    private val game: BrickBreakerGame,
    private val level: LevelDefinition,
    private val session: GameSession = GameSession(
        level = level,
        baseBallSize = game.progress.settings.selectedBallBaseSize,
        selectedBallGroupName = game.progress.settings.selectedBallGroupName,
        selectedBallSpriteName = game.progress.settings.selectedBallSpriteName,
    ),
    startPaused: Boolean = false,
    private val customTestEditor: LevelEditorState? = null,
) : ScreenAdapter() {
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
    private var resumePhase = session.phase
    private var visibleLeft = 0f
    private var visibleBottom = 0f
    private var visibleWidth = GameSession.WIDTH
    private var visibleHeight = GameSession.HEIGHT
    private val hudPanelRect = Rectangle()
    private val actionBarRect=Rectangle()
    private val pauseButton = Rectangle()
    private val settingsButton = Rectangle()
    private val shopButton=Rectangle()
    private val inventoryButton=Rectangle()
    private val customExitButton=Rectangle()
    private val customizeButton=Rectangle()
    private var inventoryOpen=false
    private var selectedBooster=0
    private val deathRailRect = Rectangle()
    private var deathRailTime = 0f
    private var deathRailZapTimer = 0f
    private var deathRailZapX = GameSession.WIDTH / 2f
    private var aimDragActive = false
    private var aimDragStartX = 0f
    private var aimDragCurrentX = 0f
    private var aimIntent = 0f
    // A menu tap can still be down during the first gameplay frame. Wait for
    // that pointer to be released so it cannot accidentally launch the serve.
    private var aimInputArmed = false
    private val overlayResume = Rectangle(175f, 830f, 550f, 106f)
    private val overlaySettings = Rectangle(175f, 694f, 550f, 100f)
    private val overlayCustomize = Rectangle(175f, 564f, 550f, 100f)
    private val overlayRestart = Rectangle(175f, 434f, 550f, 100f)
    private val overlayMenu = Rectangle(175f, 304f, 550f, 100f)
    private val assets get() = game.assets

    init {
        if (customTestEditor == null) session.itemRewardSink = { game.boosterInventory.add(it, 1) }
        if (startPaused) {
            resumePhase = session.phase
            session.phase = GamePhase.PAUSED
        }
    }

    override fun render(delta: Float) {
        WorldVideoBackgrounds.show(level.world)
        input()
        deathRailTime += delta
        deathRailZapTimer = (deathRailZapTimer - delta).coerceAtLeast(0f)
        feedbackTimer = (feedbackTimer - delta).coerceAtLeast(0f)
        val before = session.bricks.size
        loop.advance(delta) { dt ->
            if (session.phase != GamePhase.PAUSED) session.movePaddle(targetX, dt)
            session.update(dt)
        }
        session.consumeDeathRailZapX()?.let {
            deathRailZapX = it
            deathRailZapTimer = .14f
        }
        session.consumeEvents().forEach { event ->
            when (event) {
                is GameplayEvent.Feedback -> { feedbackText = event.text; feedbackTimer = 1.8f; assets.play(event.sound, game.progress.settings.masterVolume * game.progress.settings.sfxVolume) }
                GameplayEvent.Explosion -> assets.play("explosion", game.progress.settings.masterVolume * game.progress.settings.sfxVolume)
                GameplayEvent.LaserFired -> assets.play("wall_hit", game.progress.settings.masterVolume * game.progress.settings.sfxVolume * .7f)
                GameplayEvent.LaserHit -> assets.play("brick_hit", game.progress.settings.masterVolume * game.progress.settings.sfxVolume)
                GameplayEvent.FallingWarning -> { feedbackText = "BRICKS ARE FALLING"; feedbackTimer = 1.5f; assets.play("wall_hit", game.progress.settings.masterVolume * game.progress.settings.sfxVolume) }
            }
        }
        if (session.bricks.size < before) {
            hitCount++
            assets.play(if (hitCount % 5 == 0) "explosion" else "brick_hit",
                game.progress.settings.masterVolume * game.progress.settings.sfxVolume)
            if (game.progress.settings.haptics) Gdx.input.vibrate(18)
        }
        val videoVisible = WorldVideoBackgrounds.isVideoVisible()
        Gdx.gl.glClearColor(.004f, .008f, .03f, if (videoVisible) 0f else 1f)
        Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT)
        viewport.apply()
        batch.projectionMatrix = camera.combined
        batch.begin()
        if (!videoVisible) drawBackgroundCover(assets.worldFallbackBg(level.world), Color(.66f, .74f, .9f, .9f))
        val playfieldBottom = 150f
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
        session.laserShots.forEach { draw(assets.gameplayAtlas.laserProjectile, it.position.x - 8f, it.position.y, 16f, 42f, Color.CYAN) }
        session.fallingPowerUps.forEach { power ->
            draw(assets.gameplayAtlas.powerUpIcon(power.type), power.position.x - 36f, power.position.y - 36f, 72f, 72f, Color.WHITE)
        }
        if (session.bottomShield) repeat(10) { draw(assets.particles.findRegion("glow"), it * 90f, 12f, 110f, 24f, Color(.1f, .9f, 1f, .7f)) }
        hud()
        if (feedbackTimer > 0f) {
            draw(assets.ui.findRegion("panel"), 150f, 360f, 600f, 64f, Color(.01f, .06f, .1f, .88f))
            assets.hudLabelFont.draw(batch, feedbackText, 160f, 402f, 580f, Align.center, false)
        }
        if(inventoryOpen)inventoryOverlay() else if (session.phase == GamePhase.PAUSED) pauseOverlay()
        batch.end()
        if (session.phase == GamePhase.LEVEL_COMPLETE) finishLevel()
    }

    private fun brick(brick: Brick) {
        if (brick.type == BrickType.GHOST && !brick.ghostVisible) return
        draw(assets.gameplayAtlas.brick(brick, level.world, useCampaignPalette = customTestEditor == null),
            brick.bounds.x, brick.bounds.y, brick.bounds.width, brick.bounds.height, Color.WHITE)
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
            stickyActive,
        )
        val selected = assets.cosmetics.selectedPaddle(activePaddleId)
        val region = selected?.let(assets.cosmetics::paddleRegion) ?: assets.gameplayAtlas.paddleNormal.whole
        val drawY = paddle.y - paddle.height / 2f
        val dual = PowerUpType.DUAL_PADDLE in session.powerUps
        if (magneticActive) {
            val fieldWidth = GameplayTuning.MAGNET_HORIZONTAL_RANGE * 2f
            draw(assets.particles.findRegion("glow"), paddle.x - fieldWidth / 2f, paddle.y,
                fieldWidth, GameplayTuning.MAGNET_RANGE, Color(.1f, 1f, .55f, .13f))
        }
        PaddleVisualLayout.slots(paddle.x, paddle.width, dual).forEach { slot ->
            val visualHeight = (slot.width * region.regionHeight.toFloat() / region.regionWidth.toFloat()).coerceAtMost(92f)
            val glow = assets.particles.findRegion("glow")
            draw(glow, slot.centerX - slot.width * .56f, drawY - 15f,
                slot.width * 1.12f, visualHeight + 30f, Color(.08f, .78f, 1f, .24f))
            draw(region, slot.centerX - slot.width / 2f, drawY - 8f,
                slot.width, visualHeight, Color(.005f, .012f, .02f, .9f))
            draw(region, slot.centerX - slot.width / 2f, drawY, slot.width, visualHeight, Color.WHITE)
            if (laserActive) {
                val glowSize = 24f
                draw(glow, slot.centerX - slot.width * .38f - glowSize / 2f, drawY + visualHeight - 10f, glowSize, glowSize, Color(.12f, .9f, 1f, .82f))
                draw(glow, slot.centerX + slot.width * .38f - glowSize / 2f, drawY + visualHeight - 10f, glowSize, glowSize, Color(.12f, .9f, 1f, .82f))
            }
            if (stickyActive) draw(assets.particles.findRegion("glow"), slot.centerX - slot.width * .46f, drawY + visualHeight - 16f, slot.width * .92f, 20f, Color(.35f, 1f, .72f, .58f))
            if (magneticActive) draw(assets.particles.findRegion("glow"), slot.centerX - slot.width * .48f,
                drawY - 8f, slot.width * .96f, visualHeight + 16f, Color(.08f, 1f, .42f, .38f))
        }
    }

    private fun ball(ball: Ball) {
        val r = ball.radius
        val diameter = ball.visualDiameter
        val selected = assets.cosmetics.selectedBall(ball.cosmeticGroupName, ball.cosmeticSpriteName)
        val region = selected?.let(assets.cosmetics::ballRegion) ?: assets.gameplayAtlas.ball(ball)
        val ghost = ball.collisionMode == BallCollisionMode.GHOST
        val trailTint = when {
            ghost -> Color(.72f, .48f, 1f, 1f)
            ball.element == BallElement.FIRE -> Color(1f, .3f, .05f, 1f)
            else -> Color(.45f, .9f, 1f, 1f)
        }
        if (!game.progress.settings.reduceMotion) for (i in 1..3) {
            val t = i / 4f
            val trailR = r * .82f
            val trailX = MathUtils.lerp(ball.position.x, ball.previousPosition.x, t)
            val trailY = MathUtils.lerp(ball.position.y, ball.previousPosition.y, t)
            drawBallSquare(region, trailX - trailR, trailY - trailR, trailR * 2f, Color(trailTint.r, trailTint.g, trailTint.b, .16f * (1f - t)))
        }
        val ballTint = when {
            game.progress.settings.highContrastBall -> Color.YELLOW
            ghost -> Color(.72f, .72f, 1f, .58f)
            else -> Color.WHITE
        }
        val ballGlow = when {
            game.progress.settings.highContrastBall -> Color(1f, .92f, .08f, .38f)
            ghost -> Color(.65f, .42f, 1f, .3f)
            ball.element == BallElement.FIRE -> Color(1f, .28f, .04f, .36f)
            else -> Color(.82f, .94f, 1f, .24f)
        }
        val glowDiameter = diameter * 1.42f
        draw(assets.particles.findRegion("glow"), ball.position.x - glowDiameter / 2f,
            ball.position.y - glowDiameter / 2f, glowDiameter, glowDiameter, ballGlow)
        drawBallSquare(region, ball.position.x - r, ball.position.y - r, diameter, ballTint)
    }

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
                    aimRenderer.color = Color(.12f, 1f, .38f, alpha)
                    aimRenderer.circle(x, y, radius, 18)
                }
            }
        }
        aimRenderer.end()
        batch.begin()
    }

    private fun hud() {
        draw(assets.ui.findRegion("panel"), hudPanelRect.x, hudPanelRect.y, hudPanelRect.width, hudPanelRect.height, Color(.01f, .04f, .09f, .96f))
        draw(assets.ui.findRegion("panel"),actionBarRect.x,actionBarRect.y,actionBarRect.width,actionBarRect.height,Color(.01f,.04f,.09f,.96f))
        drawHudCell("WORLD", if (customTestEditor != null) "CUSTOM" else level.world.toString(), 24f, 110f)
        drawHudCell("LEVEL", if (customTestEditor != null) "TEST" else level.id.toString(), 150f, 110f)
        drawHudCell("SCORE", session.score.toString(), 282f, 255f)
        drawHudCell("LIVES", session.lives.toString(), 492f, 92f)
        if (customTestEditor != null) smallButton(customExitButton,"EXIT TEST") else {
            smallButton(inventoryButton,"ITEMS")
            smallButton(shopButton,"SHOP")
        }
        smallButton(pauseButton, "II")
        smallButton(settingsButton, "SET")
        if (customTestEditor == null) smallButton(customizeButton, "CUSTOMIZE")
        activeEffectsHud()
        val message = when {
            session.phase == GamePhase.GAME_OVER -> "GAME OVER - TAP TO RETRY"
            session.hasAttachedBalls() -> if (aimDragActive) "RELEASE TO LAUNCH" else "DRAG TO AIM - RELEASE TO LAUNCH"
            else -> ""
        }
        val launchTextY = session.paddle.y + 170f
        if (message.isNotEmpty()) assets.bodyFont.draw(batch, message, 0f, launchTextY, 900f, Align.center, false)
    }

    private fun activeEffectsHud() {
        val effects = session.powerUps.activeEffects().take(4)
        effects.forEachIndexed { index, (type, remaining) ->
            val x = 18f + (index % 2) * 432f
            val y = actionBarRect.y - 52f - (index / 2) * 48f
            draw(assets.ui.findRegion("panel"), x, y, 424f, 44f, Color(.01f, .055f, .1f, .88f))
            draw(assets.gameplayAtlas.powerUpIcon(type), x + 4f, y + 4f, 36f, 36f, Color.WHITE)
            val suffix = remaining?.let { " ${kotlin.math.ceil(it).toInt()}s" } ?: ""
            assets.hudLabelFont.draw(batch, PowerUpInfoRepository.info(type).shortName + suffix, x + 44f, y + 31f, 370f, Align.left, false)
        }
    }

    private fun drawHudCell(label: String, value: String, x: Float, width: Float) {
        val labelBaseline = hudPanelRect.y + 91f
        val valueBaseline = hudPanelRect.y + 43f
        assets.hudLabelFont.draw(batch, label, x, labelBaseline, width, Align.center, false)
        assets.hudValueFont.draw(batch, value, x, valueBaseline, width, Align.center, false)
    }

    private fun smallButton(rect: Rectangle, label: String) {
        draw(assets.ui.findRegion("button_primary"), rect.x, rect.y, rect.width, rect.height, Color(.25f, .85f, 1f, .95f))
        assets.hudLabelFont.draw(batch, label, rect.x, rect.y + rect.height*.67f, rect.width, Align.center, false)
    }

    private fun pauseOverlay() {
        draw(assets.ui.findRegion("panel"), 92f, 240f, 716f, 894f, Color(.055f, .012f, .016f, .97f))
        assets.pauseTitleFont.draw(batch, if (customTestEditor != null) "TEST PAUSED" else "PAUSED", 0f, 1060f, 900f, Align.center, false)
        if (customTestEditor != null) {
            overlayButton(overlayResume, "RESUME")
            overlayButton(overlaySettings, "RESTART TEST")
            overlayButton(overlayRestart, "EXIT TEST")
            assets.smallFont.draw(batch, "Test results never change campaign or paid Items", 0f, 390f, 900f, Align.center, false)
        } else {
            pauseArtButton(overlayResume, "resume")
            pauseArtButton(overlaySettings, "setting")
            pauseArtButton(overlayCustomize, "customise")
            pauseArtButton(overlayRestart, "restart-level")
            pauseArtButton(overlayMenu, "main-menu")
            assets.smallFont.draw(batch, "Progress is saved while paused", 0f, 292f, 900f, Align.center, false)
        }
    }

    private fun pauseArtButton(rect: Rectangle, assetName: String) {
        batch.color = Color.WHITE
        batch.draw(assets.pauseMenuTexture(assetName), 90f, rect.y - 32f, 720f, 168f)
    }

    private fun overlayButton(rect: Rectangle, text: String) {
        val tint = if (rect === overlayResume) Color(.2f, .95f, 1f, 1f) else Color(.72f, .9f, 1f, .96f)
        draw(assets.ui.findRegion("button_primary"), rect.x, rect.y, rect.width, rect.height, tint)
        assets.buttonFont.draw(batch, text, rect.x, rect.y + rect.height * .64f, rect.width, Align.center, false)
    }

    private fun input() {
        if (Gdx.input.isTouched || Gdx.input.justTouched()) {
            pointer.set(Gdx.input.x.toFloat(), Gdx.input.y.toFloat(), 0f)
            viewport.unproject(pointer)
        }
        val back = Gdx.input.isKeyJustPressed(Input.Keys.ESCAPE) || Gdx.input.isKeyJustPressed(Input.Keys.BACK)
        if(inventoryOpen){if(Gdx.input.justTouched())handleInventoryTap();if(back)closeInventory();return}
        if (session.phase == GamePhase.PAUSED) {
            if (Gdx.input.justTouched()) when {
                overlayResume.contains(pointer.x, pointer.y) -> resumeGame()
                overlaySettings.contains(pointer.x, pointer.y) -> if (customTestEditor != null) game.playCustom(customTestEditor) else game.setScreen(SettingsScreen(game, returnToPausedGame = true))
                customTestEditor == null && overlayCustomize.contains(pointer.x, pointer.y) -> game.setScreen(CustomizationScreen(game, returnToPausedGame = true))
                overlayRestart.contains(pointer.x, pointer.y) -> if (customTestEditor != null) exitCustomTest() else { game.pausedSession.clear(); game.play(level.id) }
                customTestEditor == null && overlayMenu.contains(pointer.x, pointer.y) -> game.openMenu()
            }
            if (back) resumeGame()
            return
        }
        if (!aimInputArmed) {
            if (!Gdx.input.isTouched) aimInputArmed = true
            return
        }
        if (Gdx.input.justTouched()) {
            when {
                pauseButton.contains(pointer.x, pointer.y) -> { pauseAndSave(); return }
                customTestEditor != null && customExitButton.contains(pointer.x, pointer.y) -> { exitCustomTest(); return }
                inventoryButton.contains(pointer.x,pointer.y)->{resumePhase=session.phase;session.phase=GamePhase.PAUSED;inventoryOpen=true;return}
                shopButton.contains(pointer.x,pointer.y)->{pauseAndSave();game.setScreen(ShopScreen(game,ShopReturnDestination.PAUSED_GAME));return}
                settingsButton.contains(pointer.x, pointer.y) -> { pauseAndSave(); game.setScreen(SettingsScreen(game, returnToPausedGame = true)); return }
                customTestEditor == null && customizeButton.contains(pointer.x, pointer.y) -> { pauseAndSave(); game.setScreen(CustomizationScreen(game, returnToPausedGame = true)); return }
                actionBarRect.contains(pointer.x,pointer.y)||hudPanelRect.contains(pointer.x,pointer.y)->return
                else -> if (session.hasAttachedBalls()) {
                    aimDragActive = true
                    aimDragStartX = pointer.x
                    aimDragCurrentX = pointer.x
                    aimIntent = 0f
                } else session.action()
            }
        }
        if (Gdx.input.isTouched) {
            targetX = pointer.x
            if (aimDragActive) {
                aimDragCurrentX = pointer.x
                aimIntent = ((aimDragCurrentX - aimDragStartX) / 220f).coerceIn(-1f, 1f)
            }
        } else if (aimDragActive) {
            aimDragActive = false
            session.launchAttachedBalls(aimIntent)
            aimIntent = 0f
        }
        val distance = 700f * Gdx.graphics.deltaTime
        if (Gdx.input.isKeyPressed(Input.Keys.LEFT)) targetX -= distance
        if (Gdx.input.isKeyPressed(Input.Keys.RIGHT)) targetX += distance
        targetX = targetX.coerceIn(0f, 900f)
        if (Gdx.input.isKeyJustPressed(Input.Keys.SPACE)) session.action()
        if (back) pauseAndSave()
    }

    private fun pauseAndSave() {
        if (session.phase == GamePhase.PAUSED) return
        resumePhase = session.phase
        if (customTestEditor == null) game.pausedSession.save(level, session)
        session.phase = GamePhase.PAUSED
    }

    private fun resumeGame() {
        session.phase = resumePhase
        if (customTestEditor == null) game.pausedSession.clear()
    }

    private fun inventoryOverlay(){
        draw(assets.ui.findRegion("panel"),42f,235f,816f,1145f,Color(.005f,.025f,.07f,.98f));assets.pauseTitleFont.draw(batch,"MY ITEMS",0f,1310f,900f,Align.center,false)
        SHOP_ELIGIBLE_TYPES.forEachIndexed{i,type->val col=i%2;val row=i/2;val x=105f+col*360f;val y=1130f-row*105f
            val tint=if(i==selectedBooster)Color(.25f,.95f,1f,1f)else Color(.3f,.65f,.8f,.7f);draw(assets.ui.findRegion("button_primary"),x,y,330f,82f,tint)
            draw(assets.gameplayAtlas.powerUpIcon(type),x+8f,y+15f,52f,52f,Color.WHITE);assets.hudLabelFont.draw(batch,PowerUpInfoRepository.info(type).shortName,x+68f,y+51f,205f,Align.left,false)
            draw(assets.ui.findRegion("panel"),x+276f,y+13f,47f,54f,Color(.01f,.03f,.08f,.95f));assets.hudLabelFont.draw(batch,"x${game.boosterInventory.count(type)}",x+276f,y+50f,47f,Align.center,false)
        }
        val chosen=SHOP_ELIGIBLE_TYPES[selectedBooster];val info=PowerUpInfoRepository.info(chosen);draw(assets.ui.findRegion("panel"),105f,390f,690f,96f,Color(.03f,.1f,.17f,.96f));draw(assets.gameplayAtlas.powerUpIcon(chosen),118f,408f,60f,60f,Color.WHITE);assets.hudLabelFont.draw(batch,info.fullName,190f,456f,580f,Align.left,false);assets.smallFont.draw(batch,info.description,190f,421f,580f,Align.left,false)
        val useRect=Rectangle(110f,300f,210f,85f);draw(assets.ui.findRegion("button_primary"),useRect.x,useRect.y,useRect.width,useRect.height,if(game.boosterInventory.count(chosen)>0)Color(.2f,.95f,1f,1f)else Color(.3f,.38f,.45f,.65f));assets.buttonFont.draw(batch,"USE",useRect.x,useRect.y+55f,useRect.width,Align.center,false)
        overlayButton(Rectangle(345f,300f,210f,85f),"CLOSE");overlayButton(Rectangle(580f,300f,210f,85f),"SHOP")
    }
    private fun handleInventoryTap(){
        SHOP_ELIGIBLE_TYPES.indices.firstOrNull{i->val x=105f+(i%2)*360f;val y=1130f-(i/2)*105f;Rectangle(x,y,330f,82f).contains(pointer.x,pointer.y)}?.let{selectedBooster=it;return}
        when{Rectangle(110f,300f,210f,85f).contains(pointer.x,pointer.y)->{val type=SHOP_ELIGIBLE_TYPES[selectedBooster];if(session.useItem(type,game.boosterInventory) is BoosterUseResult.Applied)closeInventory()}
            Rectangle(345f,300f,210f,85f).contains(pointer.x,pointer.y)->closeInventory()
            Rectangle(580f,300f,210f,85f).contains(pointer.x,pointer.y)->{session.phase=resumePhase;game.pausedSession.save(level,session);session.phase=GamePhase.PAUSED;game.setScreen(ShopScreen(game,ShopReturnDestination.PAUSED_GAME))}}
    }
    private fun closeInventory(){inventoryOpen=false;session.phase=resumePhase}

    private fun draw(region: TextureRegion, x: Float, y: Float, width: Float, height: Float, color: Color) {
        batch.color = color; batch.draw(region, x, y, width, height); batch.color = Color.WHITE
    }

    private fun drawRegionFit(region: TextureRegion, x: Float, y: Float, width: Float, height: Float, color: Color) {
        val scale = minOf(width / region.regionWidth, height / region.regionHeight)
        val drawWidth = region.regionWidth * scale
        val drawHeight = region.regionHeight * scale
        draw(region, x + (width - drawWidth) / 2f, y + (height - drawHeight) / 2f, drawWidth, drawHeight, color)
    }

    /** Ball physics are circular, so cosmetic source rectangles are normalized to a square at draw time. */
    private fun drawBallSquare(region: TextureRegion, x: Float, y: Float, diameter: Float, color: Color) =
        draw(region, x, y, diameter, diameter, color)

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

    private fun updateResponsiveLayout() {
        val visibleTop = visibleBottom + visibleHeight
        val hudTop = visibleTop - 18f
        hudPanelRect.set(12f,hudTop-108f,876f,108f);actionBarRect.set(12f,hudPanelRect.y-72f,876f,62f)
        inventoryButton.set(24f,actionBarRect.y+7f,190f,48f);shopButton.set(226f,actionBarRect.y+7f,170f,48f)
        customizeButton.set(408f,actionBarRect.y+7f,284f,48f)
        customExitButton.set(24f, actionBarRect.y + 7f, 372f, 48f)
        pauseButton.set(704f,actionBarRect.y+7f,72f,48f);settingsButton.set(788f,actionBarRect.y+7f,88f,48f)
        // Full-width kill rail between the two playfield walls, matching the
        // visible boundary instead of appearing as a short centered bar.
        deathRailRect.set(18f, visibleBottom + 54f, GameSession.WIDTH - 36f, 12f)
        session.deathRailTop = if (level.deathRailEnabled) deathRailRect.y + deathRailRect.height else -200f
    }

    private fun finishLevel() {
        val stars = when { session.score >= level.threeStarScore -> 3; session.score >= level.threeStarScore / 2 -> 2; else -> 1 }
        if (customTestEditor != null) {
            game.setScreen(CustomTestResultScreen(game, customTestEditor, session.score, stars)); return
        }
        game.pausedSession.clear()
        game.progress.complete(level.id, session.score, stars)
        game.setScreen(ResultsScreen(game, level, session.score, stars))
    }

    private fun exitCustomTest() { customTestEditor?.let { game.setScreen(LevelEditorScreen(game, it)) } }

    override fun pause() { if (session.phase != GamePhase.PAUSED) pauseAndSave() }
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
    override fun dispose() { WorldVideoBackgrounds.hide(); batch.dispose(); aimRenderer.dispose() }
}
