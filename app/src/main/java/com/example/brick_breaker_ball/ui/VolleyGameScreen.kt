/*
 * المسار: app/src/main/java/com/example/brick_breaker_ball/ui/VolleyGameScreen.kt
 * الوظيفة: رسم جولة Volley وإدخال اللاعب.
 * الحسابات: إسقاط إحداثيات المحاكاة إلى الشاشة واللمس.
 */

package com.example.brick_breaker_ball.ui

import com.badlogic.gdx.Gdx
import com.badlogic.gdx.Input
import com.badlogic.gdx.InputAdapter
import com.badlogic.gdx.InputMultiplexer
import com.badlogic.gdx.InputProcessor
import com.badlogic.gdx.graphics.Color
import com.badlogic.gdx.graphics.g2d.TextureRegion
import com.badlogic.gdx.math.MathUtils
import com.badlogic.gdx.math.Rectangle
import com.badlogic.gdx.math.Vector2
import com.badlogic.gdx.math.Vector3
import com.badlogic.gdx.scenes.scene2d.utils.ScissorStack
import com.badlogic.gdx.utils.Align
import com.example.brick_breaker_ball.BrickBreakerGame
import com.example.brick_breaker_ball.assets.VolleyAssetCatalog
import com.example.brick_breaker_ball.assets.VolleySourceBrickArt
import com.example.brick_breaker_ball.assets.VolleyEnvironment
import com.example.brick_breaker_ball.assets.VolleyVfx
import com.example.brick_breaker_ball.assets.WorldVideoBackgrounds
import com.example.brick_breaker_ball.assets.volleyGameplayEffectsEnabled
import com.example.brick_breaker_ball.campaign.GraphicsQuality
import com.example.brick_breaker_ball.campaign.LevelDefinition
import com.example.brick_breaker_ball.campaign.VolleyStageRepository
import com.example.brick_breaker_ball.cosmetics.BallAbilityCatalog
import com.example.brick_breaker_ball.cosmetics.BallAbilityProfile
import com.example.brick_breaker_ball.cosmetics.VolleyBallSelection
import com.example.brick_breaker_ball.economy.AdState
import com.example.brick_breaker_ball.economy.RewardedAdResult
import com.example.brick_breaker_ball.economy.SHOP_ELIGIBLE_TYPES
import com.example.brick_breaker_ball.gameplay.BrickType
import com.example.brick_breaker_ball.gameplay.GameplayMode
import com.example.brick_breaker_ball.gameplay.PowerUpType
import com.example.brick_breaker_ball.gameplay.VolleyAimGesture
import com.example.brick_breaker_ball.gameplay.VolleyAimRelease
import com.example.brick_breaker_ball.gameplay.VolleyBonusNodeType
import com.example.brick_breaker_ball.gameplay.VolleyEditorDraft
import com.example.brick_breaker_ball.campaign.VolleyPlayerProgress
import com.example.brick_breaker_ball.gameplay.VolleyEvent
import com.example.brick_breaker_ball.gameplay.VolleyGeneratorFeature
import com.example.brick_breaker_ball.gameplay.VolleyImpactPitch
import com.example.brick_breaker_ball.gameplay.VolleyItemController
import com.example.brick_breaker_ball.gameplay.VolleyItemDescriptions
import com.example.brick_breaker_ball.gameplay.VolleyItemUseResult
import com.example.brick_breaker_ball.gameplay.VolleyLayout
import com.example.brick_breaker_ball.gameplay.VolleyModifier
import com.example.brick_breaker_ball.gameplay.VolleyOrbDefinition
import com.example.brick_breaker_ball.gameplay.VolleyPhase
import com.example.brick_breaker_ball.gameplay.VolleyRailState
import com.example.brick_breaker_ball.gameplay.VolleySession
import com.example.brick_breaker_ball.gameplay.VolleySessionSnapshot
import com.example.brick_breaker_ball.gameplay.VolleyVfxKind
import com.example.brick_breaker_ball.gameplay.itemType
import java.util.UUID
import kotlin.math.abs
import kotlin.math.atan2
import kotlin.math.ceil
import kotlin.math.max
import kotlin.math.min
import kotlin.math.sqrt

/** Volley presentation/input adapter. All mutable gameplay state lives in [VolleySession]. */
class VolleyGameScreen(
    game: BrickBreakerGame,
    private val level: LevelDefinition,
    restoredSnapshot: VolleySessionSnapshot? = null,
    restoredRewardedRevivesUsed: Int = 0,
    private val editorTestDraft: VolleyEditorDraft? = null,
    private val onExitEditorTest: (() -> Unit)? = null
) : ForgeScreen(game) {
    private data class DebrisFx(val x: Float, val y: Float, var age: Float = 0f)
    private data class VfxFx(val kind: VolleyVfxKind, val x: Float, val y: Float, var age: Float = 0f)
    val musicWorld: Int get() = level.world

    private companion object {
        const val CANCEL_STRIP_HEIGHT = 94f
        const val CANCEL_HIT_PADDING = 30f
        const val AIM_CLEARANCE_ABOVE_CANNON = 80f
    }
    private val editorTest = editorTestDraft != null
    private val stage = editorTestDraft?.toStage() ?: VolleyGeneratorFeature.stageForLevel(level.id)
    private val earnedOrbs = restoredSnapshot?.earnedOrbUids.orEmpty()
        .mapNotNull(game.progress::orbCopy).toMutableList()
    private var orbRunId = restoredSnapshot?.orbRunId?.takeIf { it.isNotBlank() }
        ?: UUID.randomUUID().toString()
    private val testExitButton = Rectangle(660f, 1530f, 210f, 60f)
    private val assetCatalog = VolleyAssetCatalog(game.assets.volley)
    private val worldArt = game.assets.volleyWorldArt(stage.world)
    private val sourceBrickArt = if (stage.sourceScene != null) VolleySourceBrickArt() else null
    private val aimDirection = Vector2(0f, 1f)
    private val aimGuideOrigin = Vector2()
    private val aimGestureStart = Vector2()
    private val aimGesture = VolleyAimGesture(0f)
    private val touch = Vector2()
    private val inputPoint = Vector3()
    private val cancelStrip = Rectangle()
    private val helpButton = Rectangle()
    private val powerHelpFocus = Rectangle()
    private val visualHelp = VisualHelpOverlay()
    private var helpInputBlocked = false
    private val bagButton = Rectangle()
    private val pauseButton = Rectangle()
    private val pauseSettingsButton = Rectangle()
    private val fastForwardButton = Rectangle()
    private val recallButton = Rectangle()
    private val itemsButton = Rectangle()
    private val armedCharmBadge = Rectangle()
    private val disarmCharmButton = Rectangle()
    private val resumeButton = Rectangle()
    private val ballSelectButton = Rectangle()
    private val pauseBagButton = Rectangle()
    private val pauseShopButton = Rectangle()
    private val restartButton = Rectangle()
    private val homeButton = Rectangle()
    private val confirmation = ConfirmationCoordinator()
    private val confirmationYes = Rectangle()
    private val confirmationNo = Rectangle()
    private var confirmationPressedButton = 0
    private var pauseBoardTime = 0f
    private val rewardedContinueButton = Rectangle()
    private val itemUseButton = Rectangle()
    private val itemCloseButton = Rectangle()
    private var previousInputProcessor: InputProcessor? = null
    private var installedInputProcessor: InputProcessor? = null
    private var capturedAimPointer = -1
    private var aimInputConsumed = false
    private var speedHeldPointer = -1
    private enum class HeroLesson { NONE, INTRO, AIM, WAIT_RETURN, SPEED_HINT, RECALL_HINT, RETURN_HINT, DANGER_HINT }
    private val controlTutorial by lazy {
        val seen = editorTest || heroLesson != HeroLesson.NONE ||
            Gdx.app.getPreferences("brickbreakerball-volley-controls-v1").getBoolean("learned", false)
        VolleyControlTutorial(seen)
    }
    private val charmTutorial = CharmTutorial()
    private val controlGuideAnimation = GuideAnimation()
    private var lastControlGuideStep = VolleyControlTutorial.Step.NONE
    private var resumeAfterCharmLesson = false

    // لم تعد اللعبة تعرض درس Boosts؛ Orbs هي نظام التمائم النشط وحده.


    private val aimInputProcessor = object : InputAdapter() {
        override fun touchDown(screenX: Int, screenY: Int, pointer: Int, button: Int): Boolean {
            if (rewardedContinueInFlight || game.monetization.rewardedAdInProgress) return true
            if (visualHelp.active || helpInputBlocked || heroLessonModal() || charmTutorial.active || controlTutorial.active || game.uiMotion.active) return true
            worldPoint(screenX, screenY, touch)
            if (editorTest && testExitButton.contains(touch.x, touch.y)) return true
            if (speedHeldPointer < 0 && session.firing && fastForwardButton.contains(touch.x, touch.y)) {
                if (session.setFastForwardHeld(true)) {
                    speedHeldPointer = pointer
                    return true
                }
            }
            if (capturedAimPointer >= 0 || aiming || itemsOpen || session.gameOver || session.levelComplete ||
                session.phase != VolleyPhase.READY_TO_AIM
            ) {
                return false
            }
            worldPoint(screenX, screenY, touch)
            if (touch.y <= aimTouchMinimumY() || isActionControl(touch.x, touch.y)) return false
            if (!session.beginAiming()) return false

            capturedAimPointer = pointer
            aimInputConsumed = true
            aiming = true
            aimGestureStart.set(touch)
            aimGesture.begin(touch.x, touch.y)
            updateAim(touch.x, touch.y)
            return true
        }

        override fun touchDragged(screenX: Int, screenY: Int, pointer: Int): Boolean {
            if (rewardedContinueInFlight || game.monetization.rewardedAdInProgress) return true
            if (pointer != capturedAimPointer || !aiming) return false
            worldPoint(screenX, screenY, touch)
            aimInputConsumed = true
            aimGesture.update(touch.x, touch.y)
            updateAim(touch.x, touch.y)
            return true
        }

        override fun touchUp(screenX: Int, screenY: Int, pointer: Int, button: Int): Boolean {
            if (rewardedContinueInFlight || game.monetization.rewardedAdInProgress) return true
            if (pointer == speedHeldPointer) {
                speedHeldPointer = -1
                session.setFastForwardHeld(false)
                return true
            }
            if (pointer != capturedAimPointer) return false
            worldPoint(screenX, screenY, touch)
            capturedAimPointer = -1
            aimInputConsumed = true
            finishAimGesture(touch.x, touch.y)
            return true
        }
    }

    private val selectedBallRegion: TextureRegion by lazy {
        val settings = game.progress.settings
        game.assets.cosmetics.selectedBall(settings.selectedVolleyBallGroupName, settings.selectedVolleyBallSpriteName)
            ?.let(game.assets.cosmetics::ballRegion)
            ?: game.assets.cosmetics.requireBallRegion(VolleyBallSelection.STARTER_GROUP, VolleyBallSelection.STARTER_SPRITE)
    }

    // دليل التصويب من أطلس Volley نفسه؛ لا يحتاج تحميل spritesheet القديم عند بدء اللعب.
    private val aimGuideBallRegion: TextureRegion by lazy {
        game.assets.cosmetics.requireBallRegion(VolleyBallSelection.STARTER_GROUP, VolleyBallSelection.STARTER_SPRITE)
    }
    private val bossPortraitRegion: TextureRegion? by lazy {
        if (stage.bossStage) game.assets.volleyBossPortrait(stage.world) else null
    }
    private val selectedBallAbility: BallAbilityProfile by lazy {
        val settings = game.progress.settings
        game.assets.cosmetics.selectedBall(settings.selectedVolleyBallGroupName, settings.selectedVolleyBallSpriteName)
            ?.let(BallAbilityCatalog::profileForBall) ?: BallAbilityCatalog.default
    }
    private var session = VolleySession(stage, level.world, selectedBallAbility,
        reduceMotion = game.progress.settings.reduceMotion,
        difficulty = game.progress.selectedDifficulty,
        shotgunEnabled = game.progress.isShotgunEnabled(game.progress.settings.selectedVolleyBallSpriteName),
        equippedOrbs = game.progress.equippedOrbs,
        playerLevel = game.progress.combatPlayerLevel, combatRunId = orbRunId).also {
        restoredSnapshot?.let(it::restore)
        it.restoreRewardedRevivesUsed(restoredRewardedRevivesUsed)
    }
    private var heroLesson = if (game.heroGuideProgress.shouldTeachFirstShot(level.id, editorTest) &&
        !session.gameOver && !session.levelComplete) {
        when {
            (restoredSnapshot?.turn ?: 1) > 1 -> HeroLesson.RETURN_HINT
            restoredSnapshot != null &&
                (session.firing || session.descending || restoredSnapshot.launchedThisVolley > 0) -> HeroLesson.WAIT_RETURN
            else -> HeroLesson.INTRO
        }
    } else HeroLesson.NONE
    private val heroGuideAnimation = GuideAnimation()
    private var heroSkipAfterExit = false
    private var heroAimLeaving = false
    private var heroFlightSeconds = 0f
    private var heroFlightControlsShown = false
    private var heroReturnExplained = false
    private var aiming = false
    private var itemsOpen = false
    private var selectedItemIndex = 0
    private var feedbackText = ""
    private var mechanicHintActive = false
    private var feedbackTime = 0f
    private var hitFlash = 0f
    private var hitX = DESIGN_WIDTH / 2f
    private var hitY = DESIGN_HEIGHT / 2f
    private var completionCommitted = false
    private val debrisFx = mutableListOf<DebrisFx>()
    private val vfxFx = mutableListOf<VfxFx>()
    private val soundCooldowns = mutableMapOf<String, Float>()

    // Final Eclipse suppresses incidental effects but retains the source cannon shot.
    private val gameplayEffectsEnabled = volleyGameplayEffectsEnabled(stage.world, stage.bossStage)
    private val impactPitch = VolleyImpactPitch()
    private var cannonCharge = 0f
    private var muzzleFlash = 0f
    private var shakeTime = 0f
    private var stageAnimationTime = 0f
    private var displayedBossHealthRatio = session.bricks.firstOrNull { it.type == BrickType.BOSS_CORE }
        ?.let { it.health.toFloat() / it.initialHealth.coerceAtLeast(1) } ?: 0f
    private var lastBossHealthRatio = displayedBossHealthRatio
    private var bossHealthDamageFlash = 0f
    private var rewardedContinueInFlight = false
    private var screenVisible = false
    private var rewardedContinueMessage = ""
    private var rewardRecoveryCountdown = 0f

    override fun show() {
        screenVisible = true
        WorldVideoBackgrounds.hide()
        installAimInputProcessor()
        if (!editorTest) game.monetization.rewardedReviveAdGateway.preload()
    }

    override fun resize(w: Int, h: Int) {
        super.resize(w, h)
        session.alignBoardTop(visibleBottom + visibleHeight - safeInsets.top - 3f)
    }

    override fun render(delta: Float) {
        val frameDelta = delta.coerceAtMost(.05f)
        rewardRecoveryCountdown -= frameDelta
        if (!editorTest && !rewardedContinueInFlight && session.gameOver && rewardRecoveryCountdown <= 0f) {
            rewardRecoveryCountdown = 1f
            val context = reviveRewardContext()
            game.monetization.rewardedReviveAdGateway.pendingRewards(context).firstOrNull()?.let(::applyEarnedContinue)
        }
        helpInputBlocked = visualHelp.active
        visualHelp.update(frameDelta, game.progress.settings.reduceMotion)
        val dt = if (helpInputBlocked || rewardedContinueInFlight || game.monetization.rewardedAdInProgress) 0f else frameDelta
        if (heroLesson != HeroLesson.NONE) {
            if (!game.uiMotion.active && !charmTutorial.active)
                heroGuideAnimation.update(dt, game.progress.settings.reduceMotion)
            if (heroGuideAnimation.exitFinished) {
                if (heroLessonModal()) finishHeroLessonStep()
                else heroAimLeaving = false
            }
        }
        feedbackTime = (feedbackTime - dt).coerceAtLeast(0f)
        hitFlash = (hitFlash - dt * 4.8f).coerceAtLeast(0f)
        debrisFx.forEach { it.age += dt }
        debrisFx.removeAll { it.age >= .48f }
        vfxFx.forEach { it.age += dt }
        vfxFx.removeAll { it.age >= .42f }
        soundCooldowns.replaceAll { _, value -> (value - dt).coerceAtLeast(0f) }
        impactPitch.update(dt)
        cannonCharge = if (session.phase == VolleyPhase.FIRING_SEQUENCE) {
            min(1f, cannonCharge + dt / .25f)
        } else {
            max(0f, cannonCharge - dt / .25f)
        }
        muzzleFlash = (muzzleFlash - dt).coerceAtLeast(0f)
        shakeTime = (shakeTime - dt).coerceAtLeast(0f)
        if (!controlTutorial.active && !charmTutorial.active) stageAnimationTime += dt
        updateControlLayout()
        if (session.phase == VolleyPhase.PAUSED) layoutPauseBoard()
        pauseBoardTime = if (session.phase == VolleyPhase.PAUSED) (pauseBoardTime + dt).coerceAtMost(.24f) else 0f
        // Also release after an Android touch cancellation, if touchUp was never delivered.
        if (speedHeldPointer >= 0 && !Gdx.input.isTouched(speedHeldPointer)) releaseHeldSpeed()
        // Tutorial runs on foreground elapsed time and NEVER advances the gameplay simulation.
        controlTutorial.observe(dt, session.firing && !itemsOpen && !charmTutorial.active &&
            !session.gameOver && !game.uiMotion.active && heroLesson == HeroLesson.NONE)
        if (controlTutorial.active && speedHeldPointer >= 0) releaseHeldSpeed()
        controlTutorial.animate(dt)
        if (controlTutorial.step != lastControlGuideStep) {
            lastControlGuideStep = controlTutorial.step
            controlGuideAnimation.reset()
        }
        if (controlTutorial.active && !charmTutorial.active && !game.uiMotion.active) {
            controlGuideAnimation.update(dt, game.progress.settings.reduceMotion)
            if (controlGuideAnimation.exitFinished) {
                if (controlTutorial.skip()) Gdx.app.getPreferences("brickbreakerball-volley-controls-v1")
                    .putBoolean("learned", true).flush()
                lastControlGuideStep = controlTutorial.step
                controlGuideAnimation.reset()
            }
        }
        charmTutorial.animate(dt)
        handleInput()
        aimInputConsumed = false
        if (!itemsOpen && !controlTutorial.active && !charmTutorial.active &&
            !heroLessonModal() && !visualHelp.active && !helpInputBlocked && !game.uiMotion.active) session.advance(dt)
        if (heroLesson == HeroLesson.WAIT_RETURN && !charmTutorial.active) {
            if (session.firing && !heroFlightControlsShown) heroFlightSeconds += dt
            when {
                session.gameOver || session.levelComplete -> completeHeroLesson()
                !heroFlightControlsShown && heroFlightSeconds >= .9f -> showHeroLesson(HeroLesson.SPEED_HINT)
                session.turn > 1 && session.phase == VolleyPhase.READY_TO_AIM -> showHeroLesson(HeroLesson.RETURN_HINT)
            }
        }
        // Daily rewards are claimed from the calendar popup, not by launching Volley balls.
        consumeEvents()
        if (stage.bossStage) {
            val core = session.bricks.firstOrNull { it.type == BrickType.BOSS_CORE }
            val actual = core?.let { it.health.toFloat() / it.initialHealth.coerceAtLeast(1) }?.coerceIn(0f, 1f) ?: 0f
            if (actual < lastBossHealthRatio - .001f) bossHealthDamageFlash = .22f
            displayedBossHealthRatio = if (actual <= 0f) {
                0f
            } else {
                displayedBossHealthRatio +
                    (actual - displayedBossHealthRatio) *
                    (if (game.progress.settings.reduceMotion) 1f else (dt * 13f).coerceIn(0f, 1f))
            }
            lastBossHealthRatio = actual
            bossHealthDamageFlash = (bossHealthDamageFlash - dt).coerceAtLeast(0f)
        }

        if (
            shakeTime > 0f &&
            !game.progress.settings.reduceMotion &&
            game.progress.settings.quality != GraphicsQuality.LOW
        ) {
            camera.position.set(DESIGN_WIDTH / 2f + MathUtils.random(-4f, 4f), DESIGN_HEIGHT / 2f + MathUtils.random(-3f, 3f), 0f)
            camera.update()
        }
        beginStaticBackground(game.assets.volleyStageBackground(stage), 0f)
        drawArena()
        drawWorldDecor()
        drawRails()
        drawStageAmbientEffects()
        drawPlayfieldContent()
        drawAimGuide()
        drawLauncher()
        drawHud()
        drawBossHealth()
        if (aiming) drawCancelZone()
        if (session.phase == VolleyPhase.PAUSED) {
            if (editorTest) drawEditorTestOverlay() else drawPauseOverlay()
        }
        if (itemsOpen) drawItemsOverlay()
        if (session.gameOver || (editorTest && session.levelComplete)) {
            if (editorTest) drawEditorTestOverlay() else drawGameOverOverlay()
        }
        if (editorTest && !session.gameOver && !session.levelComplete && session.phase != VolleyPhase.PAUSED) {
            button(
                "EXIT TEST",
                testExitButton.x,
                testExitButton.y,
                testExitButton.width,
                testExitButton.height,
                ForgeUiRenderer.GradientStyle.CRIMSON
            )
        }
        confirmation.pending?.let {
            val hitboxes = BoardConfirmationRenderer.draw(
                batch,
                game.assets,
                game.assets.smallFont,
                Rectangle(visibleLeft, visibleBottom, visibleWidth, visibleHeight),
                it
            )
            confirmationYes.set(hitboxes.yes)
            confirmationNo.set(hitboxes.no)
        }
        if (controlTutorial.active && !charmTutorial.active) drawControlTutorial()
        charmTutorial.type?.let {
            CharmTutorialRenderer.draw(
                batch, game.assets, it, charmTutorial.status ?: CharmLessonStatus.ACTIVATED, charmTutorial.elapsed,
                game.progress.settings.reduceMotion, visibleLeft, visibleBottom, visibleWidth, visibleHeight
            )
        }
        if (heroLesson != HeroLesson.NONE && !charmTutorial.active) drawHeroLesson()
        visualHelp.focus.set(when (visualHelp.page) {
            0 -> Rectangle(session.launcherX - 100f, VolleySession.CANNON_PIVOT_Y - 25f,
                200f, VolleySession.CANNON_BARREL_LENGTH + 130f)
            1 -> session.bricks.firstOrNull { it.health > 0 && it.type.breakable }?.bounds ?: powerHelpFocus
            2 -> powerHelpFocus
            else -> Rectangle(minOf(fastForwardButton.x, recallButton.x), fastForwardButton.y,
                maxOf(fastForwardButton.x + fastForwardButton.width, recallButton.x + recallButton.width) -
                    minOf(fastForwardButton.x, recallButton.x), maxOf(fastForwardButton.height, recallButton.height))
        })
        visualHelp.draw(batch, game.assets, Rectangle(visibleLeft, visibleBottom, visibleWidth, visibleHeight),
            game.progress.settings.reduceMotion,
            game.progress.equippedOrbs.firstOrNull()?.takeIf { game.assets.orbIconsLoaded() }?.let { game.assets.orbIcon(it.id) },
            VisualGameUi.orbEffect(game.progress.equippedOrbs.firstOrNull()?.family ?: ""),
            balls = session.ballCount, damage = session.currentHitDamage)
        if (rewardedContinueInFlight || game.monetization.rewardedAdInProgress)
            drawRewardedAdWaitingOverlay(game.monetization.rewardedReviveAdGateway.preparingPresentation)
        end()
        if (camera.position.x != DESIGN_WIDTH / 2f || camera.position.y != DESIGN_HEIGHT / 2f) {
            camera.position.set(DESIGN_WIDTH / 2f, DESIGN_HEIGHT / 2f, 0f)
            camera.update()
        }

        if (session.levelComplete && !completionCommitted && !charmTutorial.active) commitCompletion()
    }

    private fun updateControlLayout() {
        val layout = VolleyLayout.actionBar(
            visibleBottom,
            safeInsets,
            game.progress.settings.leftHanded,
            visibleLeft,
            visibleWidth,
            isLandscape
        )
        bagButton.set(layout.bag)
        itemsButton.set(layout.items)
        armedCharmBadge.set(visibleLeft + safeInsets.left + 48f, 340f, 372f, 88f)
        disarmCharmButton.set(armedCharmBadge.x + armedCharmBadge.width - 72f, armedCharmBadge.y + 9f, 62f, 70f)
        fastForwardButton.set(layout.fastForward)
        recallButton.set(layout.recall)
        pauseButton.set(layout.pause)
        helpButton.set(visibleLeft + safeInsets.left + 16f,
            layout.bag.y + layout.bag.height + 12f, 80f, 80f)
        cancelStrip.set(
            VolleySession.BOARD_LEFT,
            visibleBottom + safeInsets.bottom + 6f,
            VolleySession.BOARD_RIGHT - VolleySession.BOARD_LEFT,
            CANCEL_STRIP_HEIGHT
        )
        aimGesture.updateCancelZoneTop(cancelStrip.y + cancelStrip.height + CANCEL_HIT_PADDING)
        if (isLandscape) {
            val width = 520f
            val height = 112f
            val gap = 24f
            val left = DESIGN_WIDTH / 2f - width - gap / 2f
            val right = DESIGN_WIDTH / 2f + gap / 2f
            resumeButton.set(left, 770f, width, height)
            ballSelectButton.set(right, 770f, width, height)
            restartButton.set(left, 620f, width, height)
            homeButton.set(right, 620f, width, height)
            pauseSettingsButton.set(DESIGN_WIDTH / 2f - 260f, 470f, 520f, height)
            rewardedContinueButton.set(DESIGN_WIDTH / 2f - 310f, 742f, 620f, height)
        } else {
            resumeButton.set(190f, 855f, 520f, 96f)
            ballSelectButton.set(190f, 745f, 520f, 96f)
            pauseSettingsButton.set(190f, 635f, 520f, 96f)
            restartButton.set(190f, 525f, 520f, 96f)
            homeButton.set(190f, 415f, 520f, 96f)
            rewardedContinueButton.set(190f, 745f, 520f, 96f)
        }
        itemUseButton.set(180f, 225f, 250f, 78f)
        itemCloseButton.set(470f, 225f, 250f, 78f)
    }

    private fun handleInput() {
        if (rewardedContinueInFlight || game.monetization.rewardedAdInProgress) {
            cancelAim()
            releaseHeldSpeed()
            return
        }
        if (visualHelp.active || helpInputBlocked) {
            if (Gdx.input.isKeyJustPressed(Input.Keys.BACK) || Gdx.input.isKeyJustPressed(Input.Keys.ESCAPE)) visualHelp.tap(true)
            else if (Gdx.input.justTouched()) visualHelp.tap(VisualHelpOverlay.closeBounds(
                Rectangle(visibleLeft, visibleBottom, visibleWidth, visibleHeight)).contains(touchPoint().x, touchPoint().y))
            return
        }

        if (editorTest) {
            val testBack = Gdx.input.isKeyJustPressed(Input.Keys.BACK) || Gdx.input.isKeyJustPressed(Input.Keys.ESCAPE)
            if (testBack) {
                onExitEditorTest?.invoke()
                return
            }
            if (Gdx.input.justTouched()) {
                val point = touchPoint()
                if (session.gameOver || session.levelComplete || session.phase == VolleyPhase.PAUSED) {
                    when {
                        homeButton.contains(point.x, point.y) -> onExitEditorTest?.invoke()
                        restartButton.contains(point.x, point.y) -> restart()
                        session.phase == VolleyPhase.PAUSED && resumeButton.contains(point.x, point.y) -> resumePausedRun()
                    }
                    return
                }
                if (testExitButton.contains(point.x, point.y)) {
                    onExitEditorTest?.invoke()
                    return
                }
            }
        }
        if (heroLesson != HeroLesson.NONE && !heroLessonModal() && !charmTutorial.active &&
            !itemsOpen && session.phase != VolleyPhase.PAUSED && !game.uiMotion.active) {
            val screen = Rectangle(visibleLeft, visibleBottom, visibleWidth, visibleHeight)
            val point = touchPoint()
            if (Gdx.input.isKeyJustPressed(Input.Keys.BACK) || Gdx.input.isKeyJustPressed(Input.Keys.ESCAPE) ||
                (Gdx.input.justTouched() && HeroGuideRenderer.skipBounds(screen).contains(point.x, point.y))) {
                cancelAim()
                heroSkipAfterExit = true
                completeHeroLesson()
                return
            }
        }
        if (heroLessonModal() && !charmTutorial.active) {
            if (heroGuideAnimation.exiting || game.uiMotion.active) return
            val screen = Rectangle(visibleLeft, visibleBottom, visibleWidth, visibleHeight)
            val point = touchPoint()
            if (Gdx.input.isKeyJustPressed(Input.Keys.BACK) || Gdx.input.isKeyJustPressed(Input.Keys.ESCAPE) ||
                (Gdx.input.justTouched() && HeroGuideRenderer.skipBounds(screen).contains(point.x, point.y))) {
                heroSkipAfterExit = true
                heroGuideAnimation.beginExit()
            } else if (Gdx.input.justTouched() && heroGuideAnimation.revealOrContinue(heroLessonLines())) {
                heroGuideAnimation.beginExit()
            }
            return
        }
        if (charmTutorial.active) {
            if (Gdx.input.isKeyJustPressed(Input.Keys.BACK) ||
                (
                    Gdx.input.justTouched() &&
                        CharmTutorialRenderer.skipButton(visibleLeft, visibleBottom, visibleWidth, visibleHeight)
                            .contains(touchPoint().x, touchPoint().y)
                    )
            ) {
                if (charmTutorial.skip() && resumeAfterCharmLesson && session.phase == VolleyPhase.PAUSED) {
                    resumeAfterCharmLesson = false
                    resumePausedRun()
                }
            }
            return
        }
        if (controlTutorial.active) {
            val point = touchPoint()
            val screen = Rectangle(visibleLeft, visibleBottom, visibleWidth, visibleHeight)
            if (Gdx.input.isKeyJustPressed(Input.Keys.BACK) ||
                Gdx.input.isKeyJustPressed(Input.Keys.ESCAPE) ||
                (Gdx.input.justTouched() && HeroGuideRenderer.skipBounds(screen).contains(point.x, point.y))) {
                controlTutorial.skipAll()
                Gdx.app.getPreferences("brickbreakerball-volley-controls-v1")
                    .putBoolean("learned", true).flush()
                controlGuideAnimation.reset()
            } else if (Gdx.input.justTouched() &&
                controlGuideAnimation.revealOrContinue(controlGuideLines())) {
                controlGuideAnimation.beginExit()
            }
            return
        }
        if (game.uiMotion.active) return
        val back = Gdx.input.isKeyJustPressed(Input.Keys.BACK) || Gdx.input.isKeyJustPressed(Input.Keys.ESCAPE)
        if (confirmation.pending != null) {
            if (back) {
                confirmationPressedButton = 0
                confirmation.cancel()
                game.uiFeedback.emit(UiFeedbackKind.BACK)
            } else if (Gdx.input.justTouched()) {
                val point = touchPoint()
                confirmationPressedButton = when {
                    confirmationNo.contains(point.x, point.y) -> 1
                    confirmationYes.contains(point.x, point.y) -> 2
                    else -> 0
                }
            } else if (!Gdx.input.isTouched && confirmationPressedButton != 0) {
                val point = touchPoint()
                val pressedButton = confirmationPressedButton
                confirmationPressedButton = 0
                when {
                    pressedButton == 1 && confirmationNo.contains(point.x, point.y) -> {
                        confirmation.cancel()
                        game.uiFeedback.emit(UiFeedbackKind.BACK)
                    }

                    pressedButton == 2 && confirmationYes.contains(point.x, point.y) -> {
                        game.uiFeedback.emit(UiFeedbackKind.CONFIRM)
                        confirmation.confirm()
                    }
                }
            }
            return
        }
        confirmationPressedButton = 0
        if (back) {
            when {
                itemsOpen -> itemsOpen = false
                session.phase == VolleyPhase.PAUSED -> resumePausedRun()
                !session.gameOver && !session.levelComplete -> pauseAndSave()
            }
            return
        }
        if (itemsOpen) {
            if (Gdx.input.justTouched()) {
                val p = touchPoint()
                touch.set(p.x, p.y)
                when {
                    itemUseButton.contains(touch.x, touch.y) -> useSelectedItem()
                    itemCloseButton.contains(touch.x, touch.y) -> itemsOpen = false
                    else -> SHOP_ELIGIBLE_TYPES.indices.firstOrNull { itemCardRect(it).contains(touch.x, touch.y) }?.let { selectedItemIndex = it }
                }
            }
            return
        }
        if (aimInputConsumed || aiming) return
        if (Gdx.input.justTouched()) {
            val p = touchPoint()
            touch.set(p.x, p.y)
            when {
                session.gameOver && rewardedContinueButton.contains(touch.x, touch.y) -> requestRewardedContinue()

                session.gameOver && restartButton.contains(touch.x, touch.y) -> requestRestartConfirmation()

                session.gameOver && homeButton.contains(touch.x, touch.y) -> requestHomeConfirmation(clearSavedSession = true)

                !editorTest && session.phase == VolleyPhase.PAUSED && pauseBagButton.contains(touch.x, touch.y) -> openCharmsBag()

                !editorTest && session.phase == VolleyPhase.PAUSED && pauseShopButton.contains(touch.x, touch.y) ->
                    game.navigate(ShopScreen(game, ShopReturnDestination.PAUSED_GAME, GameplayMode.VOLLEY))

                !editorTest && session.phase == VolleyPhase.PAUSED && ballSelectButton.contains(touch.x, touch.y) -> openBallSelection()

                !editorTest && session.phase == VolleyPhase.PAUSED && pauseSettingsButton.contains(touch.x, touch.y) -> openSettings()

                session.phase == VolleyPhase.PAUSED && resumeButton.contains(touch.x, touch.y) -> resumePausedRun()

                session.phase == VolleyPhase.PAUSED && restartButton.contains(touch.x, touch.y) -> requestRestartConfirmation()

                session.phase == VolleyPhase.PAUSED && homeButton.contains(touch.x, touch.y) -> requestHomeConfirmation()

                !session.gameOver && !session.levelComplete && session.phase != VolleyPhase.PAUSED &&
                    helpButton.contains(touch.x, touch.y) -> {
                    cancelAim()
                    releaseHeldSpeed()
                    visualHelp.open(VisualHelpOverlay.Topic.GAME)
                }

                !session.gameOver && !session.levelComplete && pauseButton.contains(touch.x, touch.y) -> pauseAndSave()

                !editorTest && !session.gameOver && !session.levelComplete && bagButton.contains(touch.x, touch.y) -> openCharmsBag()

                !editorTest && session.phase == VolleyPhase.READY_TO_AIM && itemsButton.contains(touch.x, touch.y) -> openCharmsBag()

                session.phase == VolleyPhase.READY_TO_AIM && disarmCharmButton.contains(touch.x, touch.y) -> {
                    displayedCharm()?.takeIf { it in session.cancellableModifiers }?.let { modifier ->
                        if (VolleyItemController(session, game.volleyItemInventory).disarmQueuedItem(modifier)) {
                            feedbackText = "${modifier.name.replace('_', ' ')} DISARMED"
                            feedbackTime = 1.25f
                        }
                    }
                }

                // Fast-forward touch starts and ends in InputAdapter, never from justTouched().
                session.firing && fastForwardButton.contains(touch.x, touch.y) -> Unit

                session.firing && recallButton.contains(touch.x, touch.y) -> session.recall()
            }
        }
    }

    private fun finishAimGesture(releaseX: Float, releaseY: Float) {
        aiming = false
        when (aimGesture.finish(releaseX, releaseY)) {
            VolleyAimRelease.CANCEL -> {
                session.cancelAiming()
                feedbackText = "LAUNCH CANCELLED"
                feedbackTime = .65f
            }

            VolleyAimRelease.LAUNCH -> {
                updateAim(releaseX, releaseY)
                session.setShotgunEnabled(game.progress.isShotgunEnabled(game.progress.settings.selectedVolleyBallSpriteName))
                if (!session.startVolley(aimDirection)) session.cancelAiming()
                else if (heroLesson == HeroLesson.AIM) {
                    heroLesson = HeroLesson.WAIT_RETURN
                    heroAimLeaving = true
                    heroGuideAnimation.beginExit()
                }
            }
        }
    }

    private fun cancelAim() {
        aiming = false
        capturedAimPointer = -1
        aimGesture.cancel()
        session.cancelAiming()
    }

    private fun aimTouchMinimumY(): Float = max(
        VolleySession.CANNON_PIVOT_Y + AIM_CLEARANCE_ABOVE_CANNON,
        cancelStrip.y + cancelStrip.height + CANCEL_HIT_PADDING
    )

    private fun isActionControl(x: Float, y: Float): Boolean = helpButton.contains(x, y) || bagButton.contains(x, y) || itemsButton.contains(x, y) ||
        fastForwardButton.contains(x, y) || recallButton.contains(x, y) || pauseButton.contains(x, y) ||
        (heroLesson != HeroLesson.NONE && HeroGuideRenderer.skipBounds(
            Rectangle(visibleLeft, visibleBottom, visibleWidth, visibleHeight)).contains(x, y)) ||
        (displayedCharm()?.let { it in session.cancellableModifiers } == true && disarmCharmButton.contains(x, y))

    private fun worldPoint(screenX: Int, screenY: Int, out: Vector2) {
        inputPoint.set(screenX.toFloat(), screenY.toFloat(), 0f)
        viewport.unproject(inputPoint)
        out.set(inputPoint.x, inputPoint.y)
    }

    private fun installAimInputProcessor() {
        removeAimInputProcessor()
        previousInputProcessor = Gdx.input.inputProcessor
        installedInputProcessor = previousInputProcessor?.let { existing ->
            InputMultiplexer(aimInputProcessor, existing)
        } ?: aimInputProcessor
        Gdx.input.inputProcessor = installedInputProcessor
    }

    private fun removeAimInputProcessor() {
        if (Gdx.input.inputProcessor === installedInputProcessor) {
            Gdx.input.inputProcessor = previousInputProcessor
        }
        installedInputProcessor = null
        previousInputProcessor = null
        capturedAimPointer = -1
        releaseHeldSpeed()
    }

    private fun updateAim(x: Float, y: Float) {
        touch.set(x, y)
        if (aimGesture.cancelArmed) return
        if (y <= VolleySession.CANNON_PIVOT_Y + 30f) return
        val sensitivity = game.progress.settings.sensitivity.coerceIn(.5f, 1.8f)
        if (game.progress.settings.relativeControl) {
            val horizontal = (x - aimGestureStart.x) * sensitivity * 1.8f
            val vertical = 420f + (y - aimGestureStart.y).coerceAtLeast(0f) * .45f
            aimDirection.set(horizontal, vertical)
        } else {
            aimDirection.set((x - session.launcherX) * sensitivity, y - VolleySession.CANNON_PIVOT_Y)
        }
        if (aimDirection.len2() < 1f) aimDirection.set(0f, 1f)
        aimDirection.nor()
        if (aimDirection.y < .30f) {
            aimDirection.y = .30f
            aimDirection.nor()
        }
    }

    private fun consumeEvents() {
        session.drainEvents().forEach { event ->
            when (event) {
                is VolleyEvent.Feedback -> {
                    mechanicHintActive = false
                    feedbackText = event.text
                    feedbackTime = event.duration
                }

                is VolleyEvent.Impact -> {
                    hitX = event.x
                    hitY = event.y
                    hitFlash = 1f
                }

                is VolleyEvent.BrickHit -> {
                    if (!editorTest && event.damage > 0 && feedbackTime <= 0f) {
                        val seed = stage.authoredBricks.getOrNull(event.brickId - 1)
                        val kind = seed?.sourceKind
                        val applicable = kind != "BLOCKER" || seed?.sourceScene?.contains("/Alt_Blocker/") == true
                        if (kind != null && applicable && !game.heroGuideProgress.mechanicLearned(kind)) {
                            VolleyMechanicHints.all[kind]?.let { hint ->
                                feedbackText = hint
                                mechanicHintActive = true
                                feedbackTime = 3f
                                game.heroGuideProgress.completeMechanic(kind)
                            }
                        }
                    }
                    if (stage.sourceScene == null && gameplayEffectsEnabled &&
                        (soundCooldowns["impact_pop"] ?: 0f) <= 0f) {
                        game.assets.play(
                            "volley_ball",
                            game.progress.settings.masterVolume * game.progress.settings.sfxVolume * .50f,
                            impactPitch.nextPitch()
                        )
                        soundCooldowns["impact_pop"] = .028f
                    }
                }

                is VolleyEvent.CombatExperience -> if (!editorTest) {
                    game.progress.recordCombatExperience(event.runId, event.killSequence,
                        VolleyPlayerProgress.enemyExperience(event.enemyLevel, event.difficulty,
                            event.playerLevel))
                }

                is VolleyEvent.BrickBroken -> Unit

                is VolleyEvent.PickupCollected -> Unit

                is VolleyEvent.BonusNodeCollected -> Unit

                is VolleyEvent.WallDescending -> Unit

                VolleyEvent.RescueUsed -> Unit

                VolleyEvent.VolleyComplete -> Unit

                is VolleyEvent.Sound -> if (event.name == "volley_ball") {
                    // Do not throttle launches: RPG calls shoot_sound.play() for every ball.
                    game.assets.playVolleyLaunch(game.progress.settings.masterVolume * game.progress.settings.sfxVolume)
                    muzzleFlash = .09f
                } else if (event.name == "source_volley_return_pop") {
                    game.assets.playVolleyReturnPop(game.progress.settings.masterVolume * game.progress.settings.sfxVolume)
                } else if ((gameplayEffectsEnabled || event.name == "source_ball_hit") &&
                    !(stage.sourceScene != null && event.name in setOf("wall_hit", "metal_hit")) &&
                    (soundCooldowns[event.name] ?: 0f) <= 0f) {
                    game.assets.play(event.name, game.progress.settings.masterVolume * game.progress.settings.sfxVolume, event.pitch)
                    soundCooldowns[event.name] = when (event.name) {
                        "source_ball_hit" -> .08f // RPG does not restart its 80 ms hit player.
                        "brick_hit", "wall_hit", "metal_hit" -> .045f
                        else -> .10f
                    }
                }

                is VolleyEvent.Haptic -> if (game.progress.settings.haptics) Gdx.input.vibrate(event.milliseconds)

                is VolleyEvent.Debris -> if (debrisFx.size < 28) debrisFx += DebrisFx(event.x, event.y)

                is VolleyEvent.Vfx -> if (!game.progress.settings.reduceMotion && vfxFx.size < if (game.progress.settings.quality == GraphicsQuality.LOW) 12 else 36) {
                    vfxFx += VfxFx(event.kind, event.x, event.y)
                    if (event.kind in setOf(VolleyVfxKind.SMOKE, VolleyVfxKind.FIRE, VolleyVfxKind.HAZARD)) shakeTime = .10f
                }

                is VolleyEvent.GrantRandomItem -> if (!editorTest) {
                    val bossEvent = event.turn == 0 && !event.naturalDrop &&
                        stage.authoredBricks.getOrNull(event.sourceId - 1)?.sourceKind == "BOSS"
                    val receiptRunId = if (bossEvent) "" else orbRunId
                    val alreadyAwarded = game.progress.hasStageOrbDropReceipt(stage.levelId, event.sourceId,
                        event.turn, event.naturalDrop, receiptRunId)
                    val firstDrop = game.progress.creditStageOrbDrop(stage.levelId, event.sourceId, event.turn,
                        event.naturalDrop, event.orbRank, receiptRunId)
                    val bossReplay = alreadyAwarded && game.progress.stars(level.id) > 0 && bossEvent
                    val awarded = if (bossReplay) game.progress.creditRepeatBossOrbDrops(
                        stage.levelId, event.sourceId, session.rollRepeatBossOrbRanks()) else listOfNotNull(firstDrop)
                    earnedOrbs += awarded.map { it.orb }
                    if (awarded.isEmpty() && !alreadyAwarded &&
                        game.progress.hasStageOrbDropReceipt(stage.levelId, event.sourceId,
                            event.turn, event.naturalDrop, receiptRunId)) {
                        feedbackText = "ORB BAG FULL: +1 FORGE POINT"
                        feedbackTime = 1.8f
                    }
                    awarded.lastOrNull()?.let { drop ->
                        val soundVolume = game.progress.settings.masterVolume * game.progress.settings.sfxVolume
                        game.assets.play("source_orb_found", soundVolume * .62f)
                        if (drop.orb.rank >= 4) game.assets.play("source_orb_legendary", soundVolume * .91f)
                        feedbackText = "ORB FOUND: ${drop.orb.family} R${drop.orb.rank}"
                        feedbackTime = 1.8f
                    }
                }

                // Defensive migration for any legacy gem reward events.
                is VolleyEvent.GrantGems -> if (!editorTest) game.economy.addCoins(event.amount * 100)

                is VolleyEvent.GrantCoins -> if (!editorTest) game.economy.addCoins(event.amount)
            }
        }
    }

    private fun useSelectedItem() {
        val paidType = SHOP_ELIGIBLE_TYPES[selectedItemIndex.coerceIn(SHOP_ELIGIBLE_TYPES.indices)]
        when (val result = VolleyItemController(session, game.volleyItemInventory).tryUseVolleyItem(paidType)) {
            is VolleyItemUseResult.Applied -> {
                itemsOpen = false
                charmTutorial.show(result.applied)
                feedbackText = "${VolleyItemDescriptions.info(result.applied).shortName} ARMED"
                feedbackTime = 1.25f
                if (gameplayEffectsEnabled) game.assets.play("powerup", game.progress.settings.masterVolume * game.progress.settings.sfxVolume)
                if (!game.progress.settings.reduceMotion) {
                    vfxFx += VfxFx(VolleyVfxKind.PORTAL, session.launcherX, VolleySession.LAUNCH_Y)
                }
                if (game.progress.settings.haptics) Gdx.input.vibrate(18)
            }

            is VolleyItemUseResult.Rejected -> {
                feedbackText = result.reason
                feedbackTime = .9f
            }
        }
    }

    private fun openCharmsBag() {
        pauseAndSave()
        game.navigate(
            VolleyOrbsScreen(game, ShopReturnDestination.PAUSED_GAME, GameplayMode.VOLLEY, returnToShop = false),
            ScreenTransitionStyle.FADE_THROUGH
        )
    }

    /** Going to Settings saves Volley, and Settings returns to this same paused run. */
    private fun openSettings() {
        releaseHeldSpeed()
        pauseAndSave()
        game.navigate(SettingsScreen(game, returnToPausedGame = true), ScreenTransitionStyle.FADE_THROUGH)
    }

    private fun releaseHeldSpeed() {
        speedHeldPointer = -1
        session.setFastForwardHeld(false)
    }

    private fun openBallSelection() {
        pauseAndSave()
        game.navigate(
            CustomizationScreen(
                game,
                returnToPausedGame = true
            ),
            ScreenTransitionStyle.FADE_THROUGH
        )
    }

    private fun drawArena() {
        // Use the authored cyan wire at the danger boundary indicated by the player.
        batch.color = Color.WHITE
        val wireHeight = 46f
        batch.draw(
            game.assets.arenaFloorWireRegion,
            visibleLeft,
            VolleySession.DANGER_Y - wireHeight * .5f,
            visibleWidth,
            wireHeight
        )
        fittedText(game.assets.smallFont, "DANGER LINE", VolleySession.BOARD_LEFT + 8f, VolleySession.DANGER_Y + 25f, 190f, .50f)
    }

    /** Frames the playable field with environment crops without creating collisions. */
    private fun drawWorldDecor() {
        val pair = when (stage.world) {
            1 -> VolleyEnvironment.RUIN_PILLAR_LARGE to VolleyEnvironment.FLOATING_ISLAND_GRASS
            2 -> VolleyEnvironment.CRYSTAL_BLUE_LEFT to VolleyEnvironment.CRYSTAL_BLUE_RIGHT
            3 -> VolleyEnvironment.STONE_FRAGMENT_SMALL to VolleyEnvironment.PILLAR_FRAGMENT
            4 -> VolleyEnvironment.TORCH_FIRE to VolleyEnvironment.BANNER_RED_LARGE
            5 -> VolleyEnvironment.BANNER_BLUE to VolleyEnvironment.CHAIN_LONG_RIGHT
            6 -> VolleyEnvironment.CRYSTAL_BLUE_LEFT to VolleyEnvironment.TORCH_BLUE
            7 -> VolleyEnvironment.CRYSTAL_PURPLE to VolleyEnvironment.RUBBLE_CRYSTAL_BLUE
            8 -> VolleyEnvironment.PILLAR_FRAGMENT to VolleyEnvironment.CHAIN_MID_A
            9 -> VolleyEnvironment.CRYSTAL_RED to VolleyEnvironment.CRYSTAL_BLUE_RIGHT
            else -> VolleyEnvironment.CRYSTAL_PURPLE to VolleyEnvironment.BANNER_RED_SMALL
        }
        val pulse = if (game.progress.settings.reduceMotion) 1f else .94f + .06f * MathUtils.sin(stageAnimationTime * .72f)
        batch.color = Color(1f, 1f, 1f, .60f)
        val componentScale = if (isLandscape) 1.22f else 1f
        drawRegionFitCentered(assetCatalog.environment(pair.first), VolleySession.BOARD_LEFT + 38f, 1050f, 112f * pulse * componentScale, 210f * pulse * componentScale)
        drawRegionFitCentered(assetCatalog.environment(pair.second), VolleySession.BOARD_RIGHT - 38f, 1015f, 118f * pulse * componentScale, 205f * pulse * componentScale)
        batch.color = Color.WHITE
    }

    /** Authored rows above the arena remain hidden until each board descent reveals them. */
    private fun drawPlayfieldContent() {
        val bounds = Rectangle(
            VolleySession.BOARD_LEFT,
            VolleySession.RETURN_Y,
            VolleySession.BOARD_RIGHT - VolleySession.BOARD_LEFT,
            session.boardTop - VolleySession.RETURN_Y
        )
        val scissors = Rectangle()
        batch.flush()
        ScissorStack.calculateScissors(camera, batch.transformMatrix, bounds, scissors)
        if (ScissorStack.pushScissors(scissors)) {
            try {
                drawBricks()
                drawDebris()
                drawPickups()
                drawBalls()
                batch.flush()
            } finally {
                ScissorStack.popScissors()
            }
        }
    }

    private fun drawRails() {
        if (session.rails.isEmpty()) return
        if (stage.levelId == 11) {
            val tile = worldArt.stageEightBarrier()
            session.rails.forEach { rail -> drawStageEightBarrierRail(rail, tile) }
            return
        }
        session.rails.forEach { rail ->
            drawBarrierRail(rail.x1, rail.y1, rail.x2, rail.y2, rail.thickness, rail.fixed)
        }
    }

    /** Tile the selected square along the exact collision segment without stretching it into a strip. */
    private fun drawStageEightBarrierRail(rail: VolleyRailState, tile: TextureRegion) {
        val dx = rail.x2 - rail.x1
        val dy = rail.y2 - rail.y1
        val length = sqrt(dx * dx + dy * dy).coerceAtLeast(1f)
        val angle = MathUtils.radiansToDegrees * atan2(dy, dx)
        val tileSize = max(38f, rail.thickness * 1.6f)
        val pieces = ceil(length / tileSize).toInt().coerceAtLeast(1)
        val step = length / pieces
        repeat(pieces) { index ->
            val distance = (index + .5f) * step / length
            val cx = rail.x1 + dx * distance
            val cy = rail.y1 + dy * distance
            batch.draw(
                tile, cx - step / 2f, cy - tileSize / 2f,
                step / 2f, tileSize / 2f, step + 1f, tileSize, 1f, 1f, angle
            )
        }
    }

    /**
     * Ricochet barriers use reinforced/locked brick pieces from the Volley atlas. They are
     * intentionally static and opaque: the shot should read from geometry, not from animated glow.
     */
    private fun drawBarrierRail(x1: Float, y1: Float, x2: Float, y2: Float, thickness: Float, fixed: Boolean) {
        val region = worldArt.rail(fixed)
        val cap = worldArt.railCap()
        val dx = x2 - x1
        val dy = y2 - y1
        val length = sqrt(dx * dx + dy * dy).coerceAtLeast(1f)
        val angle = MathUtils.radiansToDegrees * atan2(dy, dx)
        val pieceLength = max(46f, thickness * 2.20f)
        val pieces = ceil(length / pieceLength).toInt().coerceAtLeast(1)
        val step = length / pieces
        val ux = dx / length
        val uy = dy / length
        val drawThickness = max(27f, thickness * 1.42f)

        batch.color = Color(1f, 1f, 1f, if (fixed) 1f else .97f)
        repeat(pieces) { piece ->
            val distance = (piece + .5f) * step
            val cx = x1 + ux * distance
            val cy = y1 + uy * distance
            batch.draw(
                region,
                cx - step / 2f,
                cy - drawThickness / 2f,
                step / 2f,
                drawThickness / 2f,
                step + 1f,
                drawThickness,
                1f,
                1f,
                angle
            )
        }

        val capSize = drawThickness * 1.12f
        batch.draw(cap, x1 - capSize / 2f, y1 - capSize / 2f, capSize, capSize)
        batch.draw(cap, x2 - capSize / 2f, y2 - capSize / 2f, capSize, capSize)
        batch.color = Color.WHITE
    }

    /** Tiles a narrow source vertically along a segment so no rail texture is stretched. */
    private fun drawEnergyRail(
        x1: Float,
        y1: Float,
        x2: Float,
        y2: Float,
        thickness: Float,
        pulseKey: Int,
        bottom: Boolean = false
    ) {
        val base = assetCatalog.energyBar(white = false, bottom = bottom)
        val dx = x2 - x1
        val dy = y2 - y1
        val length = sqrt(dx * dx + dy * dy).coerceAtLeast(1f)
        val angle = MathUtils.radiansToDegrees * atan2(dy, dx)
        val sourceAspect = base.regionHeight.toFloat() / base.regionWidth.coerceAtLeast(1)
        val targetLength = thickness * sourceAspect
        val pieces = ceil(length / targetLength).toInt().coerceAtLeast(1)
        val step = length / pieces
        val drawThickness = step / sourceAspect
        val ux = dx / length
        val uy = dy / length

        batch.color = Color(1f, 1f, 1f, .94f)
        repeat(pieces) { index ->
            val distance = (index + .5f) * step
            val cx = x1 + ux * distance
            val cy = y1 + uy * distance
            batch.draw(
                base,
                cx - drawThickness / 2f,
                cy - (step + 1.5f) / 2f,
                drawThickness / 2f,
                (step + 1.5f) / 2f,
                drawThickness,
                step + 1.5f,
                1f,
                1f,
                angle - 90f
            )
        }

        if (!game.progress.settings.reduceMotion) {
            val pulse = (stageAnimationTime * .18f + pulseKey * .137f).let { it - kotlin.math.floor(it) }
            val white = assetCatalog.energyBar(white = true, bottom = bottom)
            val pulseLength = thickness * white.regionHeight.toFloat() / white.regionWidth.coerceAtLeast(1)
            val cx = x1 + dx * pulse
            val cy = y1 + dy * pulse
            batch.color = Color(1f, 1f, 1f, .72f)
            batch.draw(
                white,
                cx - thickness / 2f,
                cy - pulseLength / 2f,
                thickness / 2f,
                pulseLength / 2f,
                thickness,
                pulseLength,
                1f,
                1f,
                angle - 90f
            )
        }
        batch.color = Color.WHITE
    }

    private fun drawStageAmbientEffects() {
        session.bricks.asSequence()
            .filter { it.health > 0 }
            .mapNotNull { brick ->
                val sourceKind = stage.authoredBricks.getOrNull(brick.id - 1)?.sourceKind
                val asset = when (brick.type) {
                    BrickType.EXPLOSIVE -> VolleyVfx.SPARK_ORANGE
                    BrickType.BLACK_HOLE_TELEPORTER ->
                        if (sourceKind == "VORTEX") null else VolleyVfx.PORTAL_RING_PURPLE
                    BrickType.SWITCH, BrickType.GHOST -> VolleyVfx.MAGIC_SHARDS
                    BrickType.SPIKED_HAZARD ->
                        if (sourceKind == "SPIKE_TRAP") null else VolleyVfx.GROUND_SPIKES_RED
                    BrickType.BOSS_CORE -> VolleyVfx.BEAM_RED
                    BrickType.POWERUP_CARRIER, BrickType.RANDOM_INVENTORY_POWERUP -> VolleyVfx.GROUND_RING_BLUE
                    else -> null
                }
                asset?.let { brick to it }
            }
            .take(if (game.progress.settings.quality == GraphicsQuality.LOW) 4 else 8)
            .forEachIndexed { index, (brick, asset) ->
                val phase = if (game.progress.settings.reduceMotion) {
                    .5f
                } else {
                    .5f + .5f * MathUtils.sin(stageAnimationTime * 1.45f + index * 1.1f)
                }
                val size = max(brick.bounds.width, brick.bounds.height) * (1.05f + phase * .18f)
                batch.color = Color(1f, 1f, 1f, .08f + phase * .10f)
                drawRegionFitCentered(
                    assetCatalog.vfx(asset),
                    brick.bounds.x + brick.bounds.width / 2f,
                    brick.bounds.y + brick.bounds.height / 2f,
                    size,
                    size
                )
            }
        batch.color = Color.WHITE
    }

    private fun drawRegionFitCentered(region: TextureRegion, centerX: Float, centerY: Float, maxWidth: Float, maxHeight: Float) {
        val aspect = region.regionWidth.toFloat() / region.regionHeight.coerceAtLeast(1)
        val width = min(maxWidth, maxHeight * aspect)
        val height = min(maxHeight, maxWidth / aspect)
        batch.draw(region, centerX - width / 2f, centerY - height / 2f, width, height)
    }

    /** Keep the dragon frame fixed and reveal the fill from its bottom edge. */
    private fun drawBossHealth() {
        if (!stage.bossStage) return
        val core = session.bricks.firstOrNull { it.type == BrickType.BOSS_CORE } ?: return
        val atlas = game.assets.boards2
        val frame = atlas.requireRegion("boss_hp_frame")
        val track = atlas.requireRegion("boss_hp_bar_track")
        val fill = atlas.requireRegion("boss_hp_fill")
        val scale = if (isLandscape) 1.05f else .82f
        val x = (VolleySession.BOARD_LEFT - frame.regionWidth * scale * .35f)
            .coerceAtLeast(visibleLeft + safeInsets.left + 8f)
        val y = VolleySession.DANGER_Y + 78f
        val trackX = x + 24f * scale
        val trackY = y + 24f * scale
        batch.color = Color.WHITE
        batch.draw(track, trackX, trackY, track.regionWidth * scale, track.regionHeight * scale)
        val visiblePixels = (fill.regionHeight * displayedBossHealthRatio.coerceIn(0f, 1f)).toInt()
        if (visiblePixels > 0) {
            val visibleFill = TextureRegion(fill, 0, fill.regionHeight - visiblePixels, fill.regionWidth, visiblePixels)
            batch.color = if (bossHealthDamageFlash > 0f && !game.progress.settings.reduceMotion) Color(1f, .7f, .7f, 1f) else Color.WHITE
            batch.draw(visibleFill, trackX, trackY, track.regionWidth * scale, visiblePixels * scale)
        }
        batch.color = Color.WHITE
        batch.draw(frame, x, y, frame.regionWidth * scale, frame.regionHeight * scale)
        val guards = session.bricks.count { it.volleyShieldLock && it.health > 0 }
        session.bossWarning?.let { warning ->
            fittedText(game.assets.smallFont, warning, x + frame.regionWidth * scale + 12f,
                y + 35f * scale, 410f, .36f)
        }
        if (guards > 0 && core.health > 0) {
            fittedText(game.assets.smallFont, "LOCKS $guards", x - 8f, y + 16f * scale, frame.regionWidth * scale + 16f, .35f)
        }
    }

    private fun drawBricks() {
        session.bricks.filter { it.health > 0 }.forEach { brick ->
            batch.color = when {
                brick.type == BrickType.GHOST && !brick.ghostVisible -> Color(1f, 1f, 1f, .15f)
                brick.type == BrickType.LOCKED && brick.locked -> Color(.62f, .68f, .75f, 1f)
                else -> Color.WHITE
            }
            val sourceSeed = stage.authoredBricks.getOrNull(brick.id - 1)
            val importedArt = sourceSeed?.let { seed ->
                seed.sourceScene?.let { sourceBrickArt?.visual(it, stage.world, seed.sourceKind, brick.volleyPieceShape) }
            }
            if (sourceSeed?.sourceKind == "BURN" && session.isBurnDepleted(brick.id)) {
                batch.color = Color(.52f, .52f, .58f, 1f)
            }
            if (sourceSeed?.sourceScene?.contains("/Blocker/Alt_Blocker/") == true &&
                session.sourceAlternatingBlockerOpen(brick.id) == false) {
                batch.color = Color(.58f, .64f, .70f, 1f)
            }
            if (brick.type == BrickType.BOSS_CORE && stage.bossStage) {
                // One boss is both the visible world character and the physical target.
                // Its portrait follows the core through every descent and saved run.
                val pulse = if (game.progress.settings.reduceMotion) 0f
                    else MathUtils.sin(stageAnimationTime * 2.4f) * 3f
                drawRegionFitCentered(
                    requireNotNull(bossPortraitRegion),
                    brick.bounds.x + brick.bounds.width / 2f,
                    brick.bounds.y + brick.bounds.height / 2f,
                    brick.bounds.width * 1.12f + pulse,
                    brick.bounds.height * 1.12f + pulse
                )
            } else if (sourceSeed?.sourceKind == "BOSS_GUARD") {
                // Use the supplied skull stone as a distinct, solid guard target.
                val skull = game.assets.arcadeIcons.findRegion("assets_9cd24f93_009")
                batch.draw(skull, brick.bounds.x, brick.bounds.y, brick.bounds.width, brick.bounds.height)
                val lock = game.assets.arcadeIcons.findRegion("assets_9cd24f93_033")
                val size = brick.bounds.width * .34f
                batch.draw(lock, brick.bounds.x + brick.bounds.width - size,
                    brick.bounds.y + brick.bounds.height - size * .7f, size, size)
            } else if (importedArt != null) {
                // The sprite is centered on the collision body at scale .25. Match its
                // full art extent, including the border below square and triangle tiles.
                val centerX = brick.bounds.x + brick.bounds.width / 2f
                val centerY = brick.bounds.y + brick.bounds.height / 2f
                importedArt.layers.forEach { layer ->
                    batch.draw(layer.region,
                        centerX + layer.offsetX - layer.width / 2f,
                        centerY + layer.offsetY - layer.height / 2f,
                        layer.width / 2f - layer.offsetX, layer.height / 2f - layer.offsetY,
                        layer.width, layer.height, 1f, 1f, session.brickRotation(brick.id))
                }
            } else {
                val visual = worldArt.brickVisual(brick)
                val overhang = VolleyLayout.BRICK_VISUAL_OVERHANG
                val artWidth = brick.bounds.width + overhang * 2f
                val artHeight = brick.bounds.height + overhang * 2f
                batch.draw(visual.region, brick.bounds.x - overhang, brick.bounds.y - overhang,
                    artWidth / 2f, artHeight / 2f, artWidth, artHeight, 1f, 1f,
                    visual.rotationDegrees)
            }
            // Stage sprites are unchanged; draw the existing original-game VFX at
            // EVERY authored hazard, not the global 4/8-ambient-effect cap.
            if (sourceSeed?.sourceKind == "SPIKE_TRAP") {
                val strength = session.sourceSpikeVisualStrength(brick.id)
                if (strength > .001f) {
                    batch.color = Color(1f, .78f, .76f, strength)
                    drawRegionFitCentered(
                        assetCatalog.vfx(VolleyVfx.GROUND_SPIKES_RED),
                        brick.bounds.x + brick.bounds.width / 2f,
                        brick.bounds.y + brick.bounds.height / 2f,
                        brick.bounds.width * 1.13f, brick.bounds.height * strength
                    )
                }
            }
            if (sourceSeed?.sourceKind == "VORTEX") {
                val active = session.sourceVortexActive(brick.id)
                val pulse = if (game.progress.settings.reduceMotion) .75f
                    else .75f + .12f * MathUtils.sin(stageAnimationTime * 5f)
                batch.color = Color(1f, 1f, 1f, if (active) pulse else .12f)
                drawRegionFitCentered(
                    assetCatalog.vfx(VolleyVfx.PORTAL_RING_PURPLE),
                    brick.bounds.x + brick.bounds.width / 2f,
                    brick.bounds.y + brick.bounds.height / 2f,
                    brick.bounds.width * .93f, brick.bounds.height * .93f
                )
            }
            batch.color = Color.WHITE
            if (brick.type.breakable) {
                val hp = if (brick.type == BrickType.LOCKED && brick.locked) "LOCK"
                    else formatVolleyBrickHealth(session.displayedBrickHealth(brick))
                val textScale = if (isLandscape) 1.22f else 1f
                fittedText(game.assets.hudValueFont, hp, brick.bounds.x,
                    brick.bounds.y + brick.bounds.height * (importedArt?.healthLabelYFraction ?: .67f),
                    brick.bounds.width, (if (hp.length > 3) .39f else .50f) * textScale)
            }
            if (session.bossMarked(brick.id)) {
                fittedText(game.assets.smallFont, "!", brick.bounds.x,
                    brick.bounds.y + brick.bounds.height - 4f, brick.bounds.width, .45f)
            }
            if (game.progress.settings.colorBlind) {
                fittedText(game.assets.smallFont, assetCatalog.accessibilityMarker(brick), brick.bounds.x + 3f, brick.bounds.y + brick.bounds.height - 5f, 30f, .28f)
            }
        }
        if (hitFlash > 0f) {
            val size = 82f + (1f - hitFlash) * 26f
            batch.color = Color(1f, .9f, .6f, hitFlash * .42f)
            batch.draw(game.assets.softGlowRegion, hitX - size / 2f, hitY - size / 2f, size, size)
            batch.color = Color.WHITE
        }
    }

    private fun drawPickups() {
        session.pickups.filter { it.active }.forEach { pickup ->
            val pulse = if (game.progress.settings.reduceMotion) .5f else .5f + .5f * MathUtils.sin(Gdx.graphics.frameId * .05f + pickup.id)
            val size = (64f + pulse * 7f) * if (isLandscape) 1.25f else 1f
            batch.color = Color(.42f, .92f, 1f, .18f)
            batch.draw(game.assets.softGlowRegion, pickup.position.x - size * .66f, pickup.position.y - size * .66f, size * 1.32f, size * 1.32f)
            batch.color = Color.WHITE
            drawRegionFitCentered(assetCatalog.bonusNode(pickup.type), pickup.position.x, pickup.position.y, size, size * .78f)
            fittedText(
                game.assets.hudValueFont,
                when (pickup.type) {
                    VolleyBonusNodeType.CROWN -> "★"
                    VolleyBonusNodeType.COINS, VolleyBonusNodeType.GEMS -> "+${pickup.amount * 100}"
                    else -> "+${pickup.amount}"
                },
                pickup.position.x - 34f,
                pickup.position.y + 43f,
                68f,
                .36f
            )
        }
    }

    private fun drawDebris() {
        debrisFx.forEach { fx ->
            val progress = (fx.age / .48f).coerceIn(0f, 1f)
            val travel = if (game.progress.settings.reduceMotion) 10f else 34f
            batch.color = Color(1f, 1f, 1f, 1f - progress)
            batch.draw(worldArt.debris(true), fx.x - 34f - progress * travel, fx.y - progress * 26f, 34f, 22f)
            batch.draw(worldArt.debris(false), fx.x + progress * travel, fx.y - progress * 29f, 34f, 22f)
        }
        vfxFx.forEach { fx ->
            val progress = (fx.age / .42f).coerceIn(0f, 1f)
            val asset = when (fx.kind) {
                VolleyVfxKind.SPARK -> VolleyVfx.SPARK_BLUE
                VolleyVfxKind.SMOKE -> VolleyVfx.SMOKE_B
                VolleyVfxKind.BEAM -> VolleyVfx.BEAM_CYAN
                VolleyVfxKind.FIRE -> VolleyVfx.GROUND_BURST_ORANGE
                VolleyVfxKind.PORTAL -> VolleyVfx.PORTAL_RING_PURPLE
                VolleyVfxKind.HAZARD -> VolleyVfx.GROUND_SPIKES_RED
            }
            val size = 54f + progress * 42f
            batch.color = Color(1f, 1f, 1f, 1f - progress)
            batch.draw(assetCatalog.vfx(asset), fx.x - size / 2f, fx.y - size / 2f, size, size)
        }
        batch.color = Color.WHITE
    }

    private fun drawBalls() {
        session.balls.forEach { ball ->
            val vortexRemaining = session.vortexRemaining(ball.id)
            val vortexScale = if (vortexRemaining > 0f) (vortexRemaining / .4f).coerceIn(.1f, 1f) else 1f
            // Volley ball physics are 15 units in radius. The selected cosmetic sprite has
            // transparent margins, so render it larger without changing collision geometry.
            val diameter = ball.radius * 2f * 1.45f * vortexScale
            val x = ball.position.x - diameter / 2f
            val y = ball.position.y - diameter / 2f
            batch.color = Color(1f, 1f, 1f, if (vortexRemaining > 0f) .2f + vortexScale * .8f else 1f)
            val direction = MathUtils.radiansToDegrees * atan2(ball.velocity.y, ball.velocity.x) - 90f
            batch.draw(selectedBallRegion, x, y, diameter / 2f, diameter / 2f, diameter, diameter, 1f, 1f, direction)
        }
        batch.color = Color.WHITE
    }

    private fun drawLauncher() {
        val charged = VolleyModifier.FIRE in session.queuedModifiers || VolleyModifier.LASER in session.queuedModifiers ||
            VolleyModifier.FIRE in session.activeModifiers || VolleyModifier.LASER in session.activeModifiers
        val region = worldArt.cannon()
        val angle = MathUtils.radiansToDegrees * atan2(aimDirection.y, aimDirection.x) - 90f
        val maxWidth = if (isLandscape) 265f else 205f
        val maxHeight = if (isLandscape) 430f else 340f
        val aspect = region.regionWidth.toFloat() / region.regionHeight.coerceAtLeast(1)
        val drawWidth = if (aspect >= maxWidth / maxHeight) maxWidth else maxHeight * aspect
        val drawHeight = if (aspect >= maxWidth / maxHeight) maxWidth / aspect else maxHeight
        val originX = drawWidth / 2f
        // Volley atlas regions are already trimmed by TexturePacker, so only a tiny base offset is
        // needed. Keeping the full packed region prevents the barrel/body from being clipped.
        val originY = drawHeight * .035f
        if (charged) {
            val glowSize = max(drawWidth, drawHeight) * 1.05f
            batch.color = Color(.24f, .92f, 1f, .20f)
            batch.draw(game.assets.softGlowRegion, session.launcherX - glowSize / 2f, VolleySession.CANNON_PIVOT_Y - glowSize * .18f, glowSize, glowSize)
        }
        batch.color = Color.WHITE
        batch.draw(
            region,
            session.launcherX - originX,
            VolleySession.CANNON_PIVOT_Y - originY,
            originX,
            originY,
            drawWidth,
            drawHeight,
            1f,
            1f,
            angle
        )
        // The source scene scales its cannon-head orb from .09 to .10 over .25 seconds.
        val headSize = 30f * (1f + cannonCharge / 9f)
        batch.draw(selectedBallRegion, session.launcherX - headSize / 2f,
            VolleySession.CANNON_PIVOT_Y + 18f - headSize / 2f, headSize, headSize)
        if (muzzleFlash > 0f && !game.progress.settings.reduceMotion) {
            val muzzle = session.launchOrigin(aimDirection, aimGuideOrigin)
            val flashSize = 54f * (muzzleFlash / .09f)
            batch.color = Color(1f, .82f, .48f, .64f)
            batch.draw(game.assets.softGlowRegion, muzzle.x - flashSize / 2f, muzzle.y - flashSize / 2f, flashSize, flashSize)
            batch.color = Color.WHITE
        }
        if (!session.firing) {
            fittedText(game.assets.smallFont, "x${session.ballCount}", session.launcherX + 44f, VolleySession.LAUNCH_Y + 42f, 104f, .54f)
        }
    }

    private fun drawAimGuide() {
        if (!aiming || !aimGesture.active || aimGesture.cancelArmed || session.phase != VolleyPhase.AIMING) return
        session.launchOrigin(aimDirection, aimGuideOrigin)
        var x = aimGuideOrigin.x
        var y = aimGuideOrigin.y
        var dx = aimDirection.x * 43f
        var dy = aimDirection.y * 43f
        val guideDots = if (VolleyModifier.SHRINK_GUIDE in session.queuedModifiers || VolleyModifier.SHRINK_GUIDE in session.activeModifiers) 18
            else if (session.experimentalLaunchStyle == com.example.brick_breaker_ball.gameplay.VolleyLaunchStyle.RICOCHET_GUIDE) 48 else 32
        repeat(guideDots) { index ->
            x += dx
            y += dy
            if (x < VolleySession.BOARD_LEFT + VolleySession.DEFAULT_BALL_RADIUS) {
                x = VolleySession.BOARD_LEFT + VolleySession.DEFAULT_BALL_RADIUS
                dx = abs(dx)
            } else if (x > VolleySession.BOARD_RIGHT - VolleySession.DEFAULT_BALL_RADIUS) {
                x = VolleySession.BOARD_RIGHT - VolleySession.DEFAULT_BALL_RADIUS
                dx = -abs(dx)
            }
            if (y > session.boardTop - VolleySession.DEFAULT_BALL_RADIUS) {
                y = session.boardTop - VolleySession.DEFAULT_BALL_RADIUS
                dy = -abs(dy)
            }
            val size = if (index % 6 == 0) 32f else 24f
            val alpha = (1f - index * .019f).coerceAtLeast(.38f)
            // Clean white trajectory dots match the readable reference style. No large blue halo or
            // translucent shadow is drawn around every point.
            batch.color = Color(1f, 1f, 1f, alpha)
            batch.draw(aimGuideBallRegion, x - size / 2f, y - size / 2f, size, size)
        }
        batch.color = Color.WHITE
    }

    private fun drawHud() {
        // المرحلة تظهر في القائمة وفي شريط المعلومات السفلي؛ لا نحجب صفوف البركس العلوية.

        val bottom = bagButton.y
        val statsRect = if (isLandscape) {
            Rectangle(visibleLeft + safeInsets.left + 72f, bottom, 720f, VolleyLayout.LANDSCAPE_ACTION_HEIGHT)
        } else {
            Rectangle(30f, bottom, bagButton.x - 42f, VolleyLayout.ACTION_HEIGHT)
        }
        drawModernPanel(statsRect)
        val powerWidth = statsRect.width - 16f
        powerHelpFocus.set(statsRect.x + 8f, statsRect.y + statsRect.height * .49f,
            powerWidth - 10f, statsRect.height * .43f)
        VisualGameUi.stats(batch, game.assets, powerHelpFocus,
            session.ballCount, session.currentHitDamage)
        VisualGameUi.icon(batch, game.assets, VisualGameUi.Icon.STAR,
            Rectangle(statsRect.x + 8f, statsRect.y + 6f, statsRect.height * .30f, statsRect.height * .30f))
        VisualGameUi.number(batch, game.assets, "${session.score}",
            Rectangle(statsRect.x + statsRect.height * .35f, statsRect.y + 5f,
                (powerWidth - statsRect.height * .35f).coerceAtLeast(1f), statsRect.height * .30f), .49f)
        VisualGameUi.helpButton(batch, game.assets, helpButton)
        if (!editorTest) {
            drawIconButton(bagButton, "bag", ForgeUiRenderer.GradientStyle.NEUTRAL)
            drawIconButton(itemsButton, "orb", ForgeUiRenderer.GradientStyle.NEUTRAL)
        }
        drawFastForwardButton()
        drawIconButton(recallButton, "recall", if (session.firing) ForgeUiRenderer.GradientStyle.PRIMARY else ForgeUiRenderer.GradientStyle.DISABLED)
        drawIconButton(pauseButton, "pause", ForgeUiRenderer.GradientStyle.CRIMSON)
        drawArmedCharmBadge()
        if (session.rescueCharges > 0) {
            fittedText(game.assets.smallFont, "WARD x${session.rescueCharges}", 55f, 453f, 360f, .52f)
        }
        if (VolleyModifier.LASER in session.activeModifiers || VolleyModifier.LASER in session.queuedModifiers) {
            fittedText(game.assets.smallFont, "LASER ${session.laserGauge}/${VolleySession.LASER_CHARGE_HITS}", 590f, 1390f, 250f, .44f)
        }
        if (feedbackTime > 0f) {
            if (mechanicHintActive) HeroGuideRenderer.drawCompanion(batch, game.assets,
                Rectangle(18f, 440f, 70f, 70f), opacity = 1f)
            fittedText(game.assets.hudLabelFont, feedbackText,
                if (mechanicHintActive) 100f else 260f, 485f, if (mechanicHintActive) 720f else 380f, .56f)
        }
    }

    private fun displayedCharm(): VolleyModifier? = session.queuedModifiers.firstOrNull { it == VolleyModifier.FIRE }
        ?: session.queuedModifiers.firstOrNull { it.itemType() != null }
        ?: session.activeModifiers.firstOrNull { it == VolleyModifier.FIRE }
        ?: session.activeModifiers.firstOrNull { it.itemType() != null }

    private fun drawArmedCharmBadge() {
        val modifier = displayedCharm() ?: return
        val item = modifier.itemType() ?: return
        val queued = modifier in session.queuedModifiers
        val cancellable = queued && modifier in session.cancellableModifiers && session.phase == VolleyPhase.READY_TO_AIM
        drawModernPanel(armedCharmBadge, emphasized = true)
        drawRegionFitCentered(assetCatalog.itemIcon(item), armedCharmBadge.x + 46f, armedCharmBadge.y + 44f, 70f, 70f)
        val labelWidth = if (cancellable) 210f else 272f
        val otherCharms = (session.queuedModifiers + session.activeModifiers).count { it != modifier && it.itemType() != null }
        val state = if (queued) "ARMED - NEXT VOLLEY" else "ACTIVE THIS VOLLEY"
        fittedText(game.assets.hudLabelFont, VolleyItemDescriptions.info(item).shortName, armedCharmBadge.x + 86f, armedCharmBadge.y + 57f, labelWidth, .56f)
        fittedText(game.assets.smallFont, if (otherCharms > 0) "$state  +$otherCharms" else state, armedCharmBadge.x + 86f, armedCharmBadge.y + 27f, labelWidth, .46f)
        if (cancellable) {
            game.assets.uiRenderer.drawRoundedGradientButton(batch, disarmCharmButton, ForgeUiRenderer.GradientStyle.CRIMSON)
            fittedText(game.assets.buttonFont, "X", disarmCharmButton.x, disarmCharmButton.y + disarmCharmButton.height * .61f, disarmCharmButton.width, .72f)
        }
    }

    private fun drawFastForwardButton() {
        val style = when {
            session.fastForwardActive -> ForgeUiRenderer.GradientStyle.SUCCESS
            session.firing -> ForgeUiRenderer.GradientStyle.PRIMARY
            else -> ForgeUiRenderer.GradientStyle.DISABLED
        }
        game.assets.uiRenderer.drawRoundedGradientButton(batch, fastForwardButton, style)
        // Large distinct fast-forward arrow from the user's atlas; hold behavior is unchanged.
        game.assets.atlasUiIcon("speed")?.let { arrow ->
            val size = min(fastForwardButton.height * .54f, fastForwardButton.width * .40f)
            val scale = min(size / arrow.regionWidth, size / arrow.regionHeight)
            val w = arrow.regionWidth * scale
            val h = arrow.regionHeight * scale
            batch.color = Color.WHITE
            batch.draw(
                arrow,
                fastForwardButton.x + 5f,
                fastForwardButton.y + (fastForwardButton.height - h) / 2f,
                w,
                h
            )
        }
        fittedText(
            game.assets.buttonFont,
            "${VolleySession.FAST_FORWARD_MULTIPLIER}X",
            fastForwardButton.x + fastForwardButton.width * .43f,
            fastForwardButton.y + fastForwardButton.height * .64f,
            fastForwardButton.width * .55f,
            if (isLandscape) .80f else .70f
        )
    }

    private fun drawCancelZone() {
        val armed = aimGesture.cancelArmed
        batch.color = if (armed) Color(.58f, .035f, .055f, .92f) else Color(.035f, .018f, .11f, .84f)
        batch.draw(game.assets.solidUiRegion, cancelStrip.x, cancelStrip.y, cancelStrip.width, cancelStrip.height)
        batch.color = if (armed) Color(1f, .12f, .22f, .34f) else Color(.44f, .12f, 1f, .30f)
        batch.draw(
            game.assets.softGlowRegion,
            cancelStrip.x + cancelStrip.width * .21f,
            cancelStrip.y - cancelStrip.height * .34f,
            cancelStrip.width * .58f,
            cancelStrip.height * 1.70f
        )
        drawEnergyRail(
            cancelStrip.x,
            cancelStrip.y + cancelStrip.height - 5f,
            cancelStrip.x + cancelStrip.width,
            cancelStrip.y + cancelStrip.height - 5f,
            thickness = 13f,
            pulseKey = 19,
            bottom = true
        )
        batch.color = Color.WHITE
        fittedText(
            game.assets.smallFont,
            if (armed) "RELEASE TO CANCEL" else "DRAG BACK HERE TO CANCEL",
            cancelStrip.x + 10f,
            cancelStrip.y + cancelStrip.height * .57f,
            cancelStrip.width - 20f,
            .64f
        )
    }

    private fun drawItemsOverlay() {
        drawOverlayPanel("VOLLEY ITEMS")
        fittedText(game.assets.smallFont, "POSITIVE ITEMS - ARM BETWEEN VOLLEYS", 115f, 1205f, 670f, .54f)
        SHOP_ELIGIBLE_TYPES.forEachIndexed { index, type ->
            val rect = itemCardRect(index)
            game.assets.uiRenderer.drawCompactRoundedGradientBorderPanel(batch, rect, if (index == selectedItemIndex) ForgeUiRenderer.GradientStyle.PRIMARY else ForgeUiRenderer.GradientStyle.NEUTRAL, 2f)
            drawRegionFitCentered(assetCatalog.itemIcon(type), rect.x + 44f, rect.y + 50f, 76f, 76f)
            fittedText(game.assets.smallFont, VolleyItemDescriptions.info(type).shortName, rect.x + 88f, rect.y + 67f, rect.width - 142f, .48f)
            fittedText(game.assets.hudValueFont, "x${game.volleyItemInventory.count(type)}", rect.x + rect.width - 58f, rect.y + 60f, 50f, .44f)
        }
        val selectedType = SHOP_ELIGIBLE_TYPES[selectedItemIndex.coerceIn(SHOP_ELIGIBLE_TYPES.indices)]
        val check = VolleyItemController(session, game.volleyItemInventory).canUseVolleyItem(selectedType)
        val itemInfo = Rectangle(125f, 310f, 650f, 78f)
        game.assets.uiRenderer.drawCompactRoundedGradientBorderPanel(batch, itemInfo, ForgeUiRenderer.GradientStyle.NEUTRAL, 2f)
        fittedText(
            game.assets.smallFont,
            VolleyItemDescriptions.description(selectedType),
            itemInfo.x + 10f,
            if (check.usable) 350f else 358f,
            itemInfo.width - 20f,
            .52f
        )
        if (!check.usable) {
            fittedText(
                game.assets.smallFont,
                check.reason,
                itemInfo.x + 10f,
                327f,
                itemInfo.width - 20f,
                .46f
            )
        }
        drawWideButton(itemUseButton, "use", "ARM", if (check.usable) ForgeUiRenderer.GradientStyle.SUCCESS else ForgeUiRenderer.GradientStyle.DISABLED)
        drawWideButton(itemCloseButton, "back", "CLOSE", ForgeUiRenderer.GradientStyle.NEUTRAL)
    }

    private fun itemCardRect(index: Int) = Rectangle(112f + (index % 2) * 344f, 1075f - (index / 2) * 112f, 330f, 100f)

    private fun pauseBoardRect(): Rectangle = game.assets.boards2.contain(
        "pause_menu_board",
        Rectangle(
            visibleLeft + visibleWidth * .12f,
            visibleBottom + visibleHeight * .20f,
            visibleWidth * .76f,
            visibleHeight * .72f
        )
    )

    private fun layoutPauseBoard() {
        val board = pauseBoardRect()
        fun part(name: String): Rectangle = game.assets.boards2.placement("pause_menu_board", name, board)
        ballSelectButton.set(part("pause_menu_balls_button"))
        pauseBagButton.set(part("pause_menu_bag_button"))
        pauseShopButton.set(game.assets.boards2.placementNearby("pause_menu_board", "shop-btn-place-in-pause-board", board))
        resumeButton.set(part("pause_menu_resume_button"))
        pauseSettingsButton.set(part("pause_menu_settings_button"))
        restartButton.set(part("pause_menu_restart_button"))
        homeButton.set(part("pause_menu_quit_game_button"))
    }

    private fun drawPauseOverlay() {
        batch.color = Color(0f, 0f, 0f, .55f)
        batch.draw(game.assets.solidUiRegion, visibleLeft, visibleBottom, visibleWidth, visibleHeight)
        val authoredBoard = pauseBoardRect()
        val progress = if (game.progress.settings.reduceMotion) 1f else (pauseBoardTime / .23f).coerceIn(0f, 1f)
        val eased = 1f - (1f - progress) * (1f - progress)
        val scale = .94f + .06f * eased
        val board = Rectangle(
            authoredBoard.x + authoredBoard.width * (1f - scale) * .5f,
            authoredBoard.y + authoredBoard.height * (1f - scale) * .5f,
            authoredBoard.width * scale,
            authoredBoard.height * scale
        )
        batch.color = Color(1f, 1f, 1f, eased)
        batch.draw(game.assets.boards2.requireRegion("pause_menu_board"), board.x, board.y, board.width, board.height)
        batch.color = Color.WHITE
        pauseShopButton.set(game.assets.boards2.placementNearby("pause_menu_board", "shop-btn-place-in-pause-board", board))
        val point = touchPoint()
        val shopPressed = Gdx.input.isTouched && pauseShopButton.contains(point.x, point.y)
        batch.color = if (shopPressed) Color(.72f, .72f, .72f, eased) else Color(1f, 1f, 1f, eased)
        batch.draw(
            game.assets.boards2.requireRegion("shop-btn"),
            pauseShopButton.x,
            pauseShopButton.y,
            pauseShopButton.width,
            pauseShopButton.height
        )
        batch.color = Color.WHITE
        fittedText(
            game.assets.smallFont,
            "Round saved for Continue",
            authoredBoard.x,
            authoredBoard.y - 24f,
            authoredBoard.width,
            .42f
        )
    }
    private fun drawGameOverOverlay() {
        drawModalBase("WALL BREACHED")
        val remaining = session.rewardedRevivesRemaining
        val gateway = game.monetization.rewardedReviveAdGateway
        val state = gateway.state
        fittedText(
            game.assets.smallFont,
            if (remaining > 0) "CONTINUE: PUSH BACK ${VolleySession.REWARDED_REVIVE_ROWS} ROWS"
            else "RETRY WITH A NEW AIM OR ORB LOADOUT",
            155f,
            930f,
            590f,
            .50f
        )
        fittedText(
            game.assets.smallFont,
            "TURN ${session.turn}  |  SCORE ${session.score}  |  $remaining CONTINUES LEFT",
            190f,
            885f,
            520f,
            .46f
        )
        val label = when {
            remaining <= 0 -> "NO CONTINUES LEFT"
            gateway.preparingPresentation -> "OPENING VIDEO…"
            rewardedContinueInFlight || state == AdState.SHOWING -> "RETURNING TO GAME…"
            state == AdState.READY -> "WATCH • PUSH BACK"
            state == AdState.LOADING -> "LOADING VIDEO…"
            state == AdState.UNAVAILABLE && gateway.retryInSeconds > 0 -> "RETRY IN ${gateway.retryInSeconds}s"
            state == AdState.UNAVAILABLE -> "RETRY VIDEO"
            else -> "VIDEO UNAVAILABLE"
        }
        drawPauseActionButton(
            rewardedContinueButton,
            "play",
            label,
            emphasized = remaining > 0 && state == AdState.READY
        )
        drawPauseActionButton(restartButton, "restart", "RETRY", emphasized = remaining <= 0 || state != AdState.READY)
        drawPauseActionButton(homeButton, "home", "HOME")
        val adMessage = rewardedContinueMessage.ifBlank {
            if (state == AdState.UNAVAILABLE) gateway.availabilityMessage else ""
        }
        if (adMessage.isNotBlank()) {
            fittedText(game.assets.smallFont, adMessage, 155f, 345f, 590f, .42f)
        }
    }

    private fun drawEditorTestOverlay() {
        val title = when {
            session.levelComplete -> "TEST COMPLETE"
            session.gameOver -> "TEST OVER"
            else -> "TEST PAUSED"
        }
        drawModalBase(title)
        fittedText(
            game.assets.smallFont,
            "EDITOR TEST - CAMPAIGN PROGRESS IS NOT CHANGED",
            130f,
            925f,
            640f,
            .60f
        )
        if (session.phase == VolleyPhase.PAUSED) drawPauseActionButton(resumeButton, "play", "RESUME")
        drawPauseActionButton(restartButton, "restart", "RETRY")
        drawPauseActionButton(homeButton, "home", "EXIT TO EDITOR")
    }
    private fun drawOverlayPanel(title: String) {
        batch.color = Color(0f, 0f, 0f, .34f)
        batch.draw(game.assets.solidUiRegion, visibleLeft, visibleBottom, visibleWidth, visibleHeight)
        batch.color = Color(ForgeUiPalette.navy).also { it.a = .96f }
        batch.draw(game.assets.solidUiRegion, 90f, 185f, 720f, 1180f)
        batch.color = Color.WHITE
        fittedText(game.assets.pauseTitleFont, title, 100f, 1305f, 700f)
    }
    private fun drawModalBase(title: String) {
        batch.color = Color(0f, 0f, 0f, .48f)
        batch.draw(game.assets.solidUiRegion, visibleLeft, visibleBottom, visibleWidth, visibleHeight)
        val panel = if (isLandscape) Rectangle(DESIGN_WIDTH / 2f - 590f, 400f, 1180f, 730f) else Rectangle(125f, 325f, 650f, 945f)
        game.assets.uiRenderer.drawSquareGradientBorderPanel(batch, panel, ForgeUiRenderer.GradientStyle.PRIMARY, 4f)
        batch.color = Color.WHITE
        fittedText(game.assets.pauseTitleFont, title, panel.x + 10f, panel.y + panel.height - 72f, panel.width - 20f)
    }

    private fun heroLessonModal(): Boolean = heroLesson != HeroLesson.NONE &&
        heroLesson != HeroLesson.AIM && heroLesson != HeroLesson.WAIT_RETURN

    private fun showHeroLesson(lesson: HeroLesson) {
        heroLesson = lesson
        heroAimLeaving = false
        heroGuideAnimation.reset()
    }

    private fun finishHeroLessonStep() {
        if (heroSkipAfterExit) { completeHeroLesson(); return }
        when (heroLesson) {
            HeroLesson.INTRO -> showHeroLesson(HeroLesson.AIM)
            HeroLesson.SPEED_HINT -> showHeroLesson(HeroLesson.RECALL_HINT)
            HeroLesson.RECALL_HINT -> {
                heroFlightControlsShown = true
                showHeroLesson(if (session.phase == VolleyPhase.READY_TO_AIM) {
                    if (heroReturnExplained) HeroLesson.DANGER_HINT else HeroLesson.RETURN_HINT
                } else HeroLesson.WAIT_RETURN)
            }
            HeroLesson.RETURN_HINT -> {
                heroReturnExplained = true
                showHeroLesson(if (heroFlightControlsShown) HeroLesson.DANGER_HINT else HeroLesson.SPEED_HINT)
            }
            HeroLesson.DANGER_HINT -> completeHeroLesson()
            else -> Unit
        }
    }

    private fun heroLessonLines(): List<String> = when (heroLesson) {
        HeroLesson.INTRO -> listOf("HIT BRICKS DIRECTLY OR BOUNCE OFF WALLS.", "DRAG BACK TO AIM; RELEASE TO FIRE.")
        HeroLesson.SPEED_HINT -> listOf("HOLD 3X; RELEASE FOR NORMAL SPEED.", "YOUR BALL DAMAGE STAYS THE SAME.")
        HeroLesson.RECALL_HINT -> listOf("RECALL ENDS THE SHOT EARLY.", "IT ALSO ADVANCES THE BRICKS.")
        HeroLesson.RETURN_HINT -> listOf("THE BALLS HAVE RETURNED.", "SURVIVING BRICKS DROP ONE ROW.")
        HeroLesson.DANGER_HINT -> listOf("KEEP BRICKS ABOVE THIS LINE.", "CROSSING IT CAN END YOUR RUN.")
        else -> listOf("DRAG BACK IN THE ARENA", "AIM, THEN RELEASE TO FIRE")
    }

    private fun completeHeroLesson() {
        if (heroLesson == HeroLesson.NONE) return
        game.heroGuideProgress.completeFirstShot()
        if (heroFlightControlsShown || heroSkipAfterExit) {
            Gdx.app.getPreferences("brickbreakerball-volley-controls-v1")
                .putBoolean("learned", true).flush()
        }
        heroLesson = HeroLesson.NONE
        heroAimLeaving = false
    }

    private fun drawHeroLesson() {
        if (!heroLessonModal() && (session.phase == VolleyPhase.PAUSED || itemsOpen || confirmation.pending != null)) return
        if (heroLesson == HeroLesson.AIM || (heroLesson == HeroLesson.WAIT_RETURN && heroAimLeaving)) {
            val alpha = heroGuideAnimation.presence
            val bottom = visibleBottom + safeInsets.bottom + 280f
            val strip = Rectangle(visibleLeft + 110f, bottom, visibleWidth - 150f, 100f)
            batch.setColor(.10f, .025f, .035f, .88f * alpha)
            batch.draw(game.assets.solidUiRegion, strip.x, strip.y, strip.width, strip.height)
            batch.color = Color.WHITE
            HeroGuideRenderer.drawCompanion(batch, game.assets,
                Rectangle(visibleLeft + 8f - (1f - alpha) * 65f, bottom - 8f + heroGuideAnimation.bob, 125f, 125f),
                opacity = alpha)
            val lines = heroGuideAnimation.visibleLines(heroLessonLines())
            val old = Color(game.assets.smallFont.color)
            game.assets.smallFont.color = Color(1f, 1f, 1f, alpha)
            fittedText(game.assets.smallFont, lines[0], strip.x + 18f,
                strip.y + 70f, strip.width - 36f, .66f)
            fittedText(game.assets.smallFont, lines[1], strip.x + 18f,
                strip.y + 35f, strip.width - 36f, .62f)
            game.assets.smallFont.color = old
            if (!aiming && heroLesson == HeroLesson.AIM) {
                HeroGuideRenderer.drawFocus(batch, game.assets,
                    Rectangle(session.launcherX - 65f, VolleySession.CANNON_PIVOT_Y - 20f, 130f, 190f),
                    heroGuideAnimation.elapsed, game.progress.settings.reduceMotion, alpha)
                if (!game.progress.settings.reduceMotion) {
                    val swipe = (heroGuideAnimation.elapsed * .65f) % 1f
                    batch.setColor(1f, .85f, .42f, .7f * alpha * MathUtils.sin(swipe * MathUtils.PI))
                    batch.draw(game.assets.solidUiRegion, session.launcherX + 65f, 650f - swipe * 170f, 14f, 14f)
                    batch.color = Color.WHITE
                }
            }
            HeroGuideRenderer.drawSkip(batch, game.assets,
                Rectangle(visibleLeft, visibleBottom, visibleWidth, visibleHeight))
            return
        }
        if (!heroLessonModal()) {
            if (heroLesson != HeroLesson.NONE) HeroGuideRenderer.drawSkip(batch, game.assets,
                Rectangle(visibleLeft, visibleBottom, visibleWidth, visibleHeight))
            return
        }
        val focus = when (heroLesson) {
            HeroLesson.INTRO -> Rectangle(session.launcherX - 80f, VolleySession.CANNON_PIVOT_Y - 25f,
                160f, VolleySession.CANNON_BARREL_LENGTH + 50f)
            HeroLesson.SPEED_HINT -> fastForwardButton
            HeroLesson.RECALL_HINT -> recallButton
            HeroLesson.RETURN_HINT -> session.bricks.firstOrNull { it.health > 0 }?.bounds
            HeroLesson.DANGER_HINT -> Rectangle(VolleySession.BOARD_LEFT + 8f, VolleySession.DANGER_Y - 8f,
                VolleySession.BOARD_RIGHT - VolleySession.BOARD_LEFT - 16f, 44f)
            else -> null
        }
        val screen = Rectangle(visibleLeft, visibleBottom, visibleWidth, visibleHeight)
        HeroGuideRenderer.drawDialogue(batch, game.assets,
            screen, heroLessonLines(), visibleBottom + visibleHeight * .38f,
            animation = heroGuideAnimation, focus = focus, reduceMotion = game.progress.settings.reduceMotion)
        HeroGuideRenderer.drawSkip(batch, game.assets, screen)
    }

    private fun controlGuideLines(): List<String> = if (controlTutorial.step == VolleyControlTutorial.Step.SPEED)
        listOf("HOLD 3X WHILE BALLS ARE FLYING.", "RELEASE FOR NORMAL SPEED.")
    else listOf("RECALL ENDS THIS SHOT EARLY.", "THE BRICKS ADVANCE TO THE NEXT TURN.")

    /** Screen-space coach overlay. Dim the arena, spotlight exactly one target and type the hint. */
    private fun drawControlTutorial() {
        val target = when (controlTutorial.step) {
            VolleyControlTutorial.Step.SPEED -> fastForwardButton
            VolleyControlTutorial.Step.RECALL -> recallButton
            VolleyControlTutorial.Step.NONE -> return
        }
        HeroGuideRenderer.drawDialogue(batch, game.assets,
            Rectangle(visibleLeft, visibleBottom, visibleWidth, visibleHeight), controlGuideLines(),
            target.y + target.height + 70f, animation = controlGuideAnimation,
            focus = target, reduceMotion = game.progress.settings.reduceMotion)
        HeroGuideRenderer.drawSkip(batch, game.assets,
            Rectangle(visibleLeft, visibleBottom, visibleWidth, visibleHeight))
    }

    private fun drawIconButton(rect: Rectangle, iconName: String, style: ForgeUiRenderer.GradientStyle) {
        game.assets.uiRenderer.drawRoundedGradientButton(batch, rect, style)
        val icon = game.assets.atlasUiIcon(iconName) ?: TextureRegion(game.assets.gameIcon(iconName))
        val size = min(rect.width, rect.height) * VolleyLayout.ACTION_ICON_FILL
        val iconScale = min(size / icon.regionWidth, size / icon.regionHeight)
        val iconWidth = icon.regionWidth * iconScale
        val iconHeight = icon.regionHeight * iconScale
        batch.color = Color.WHITE
        batch.draw(
            icon,
            rect.x + (rect.width - iconWidth) / 2f,
            rect.y + (rect.height - iconHeight) / 2f,
            iconWidth,
            iconHeight
        )
    }

    private fun drawWideButton(rect: Rectangle, iconName: String, label: String, style: ForgeUiRenderer.GradientStyle) {
        game.assets.uiRenderer.drawCompactRoundedGradientButton(batch, rect, style)
        val size = rect.height * .64f
        val iconX = rect.x + 14f
        batch.color = Color.WHITE
        batch.draw(
            game.assets.atlasUiIcon(iconName) ?: TextureRegion(game.assets.gameIcon(iconName)),
            iconX,
            rect.y + (rect.height - size) / 2f,
            size,
            size
        )
        val textX = iconX + size + 12f
        fittedText(
            game.assets.buttonFont,
            label,
            textX,
            rect.y + rect.height * .62f,
            (rect.x + rect.width - textX - 12f).coerceAtLeast(30f),
            .68f
        )
    }

    private fun drawModernPanel(rect: Rectangle, emphasized: Boolean = false) {
        game.assets.uiRenderer.drawRoundedGradientBorderPanel(
            batch,
            rect,
            if (emphasized) ForgeUiRenderer.GradientStyle.PRIMARY else ForgeUiRenderer.GradientStyle.NEUTRAL,
            if (emphasized) 4f else 2f
        )
        batch.color = Color.WHITE
    }

    private fun drawPauseActionButton(rect: Rectangle, iconName: String, label: String, emphasized: Boolean = false) {
        game.assets.uiRenderer.drawSquareGradientBorderPanel(
            batch,
            rect,
            if (emphasized) ForgeUiRenderer.GradientStyle.PRIMARY else ForgeUiRenderer.GradientStyle.NEUTRAL,
            if (emphasized) 4f else 2f
        )
        val icon = game.assets.atlasUiIcon(iconName) ?: TextureRegion(game.assets.gameIcon(iconName))
        val size = rect.height * .64f
        val iconX = rect.x + 26f
        batch.color = Color.WHITE
        batch.draw(icon, iconX, rect.y + (rect.height - size) / 2f, size, size)
        val textX = iconX + size + 18f
        game.assets.buttonFont.color = ForgeUiPalette.textPrimary
        fittedText(
            game.assets.buttonFont,
            label,
            textX,
            rect.y + rect.height * .64f,
            (rect.x + rect.width - textX - 22f).coerceAtLeast(30f),
            if (isLandscape) .82f else .68f
        )
        game.assets.buttonFont.color = Color.WHITE
    }

    /** Pause Volley and persist the exact resumable state before leaving or showing pause UI. */
    private fun pauseAndSave() {
        releaseHeldSpeed()
        cancelAim()
        itemsOpen = false
        if (session.gameOver || session.levelComplete) return
        session.pause()
        if (!editorTest) game.volleyPausedSession.save(level.id,
            session.snapshot().copy(earnedOrbUids = earnedOrbs.mapNotNull { it.uid },
                orbRunId = orbRunId),
            session.rewardedRevivesUsed)
    }

    /** Resume the in-memory run and invalidate the persisted pause point so Continue never goes stale. */
    private fun resumePausedRun() {
        session.resume()
        if (!editorTest) game.volleyPausedSession.clear()
    }

    /** Leaving a paused run intentionally keeps its snapshot; terminal runs must never be resumable. */
    private fun returnHome(clearSavedSession: Boolean = false) {
        if (editorTest) {
            onExitEditorTest?.invoke()
            return
        }
        if (clearSavedSession) {
            game.volleyPausedSession.clear()
        } else if (!session.gameOver && !session.levelComplete) {
            pauseAndSave()
        }
        game.openMenu()
    }

    private fun requestRestartConfirmation() {
        confirmation.request(
            UiConfirmation(
                UiConfirmationPurpose.RESTART,
                "Restart this Volley stage? Current round progress will be lost.",
                onConfirm = { restart() }
            )
        )
    }

    private fun requestHomeConfirmation(clearSavedSession: Boolean = false) {
        confirmation.request(
            UiConfirmation(
                UiConfirmationPurpose.QUIT,
                if (clearSavedSession) {
                    "Return to the menu? This finished run cannot be continued."
                } else {
                    "Return to the menu? Your paused Volley round is saved for Continue."
                },
                onConfirm = { returnHome(clearSavedSession) }
            )
        )
    }

    private fun restart() {
        releaseHeldSpeed()
        if (!editorTest) game.volleyPausedSession.clear()
        earnedOrbs.clear()
        orbRunId = UUID.randomUUID().toString()
        session = VolleySession(stage, level.world, selectedBallAbility, reduceMotion = game.progress.settings.reduceMotion, difficulty = game.progress.selectedDifficulty, equippedOrbs = game.progress.equippedOrbs,
            shotgunEnabled = game.progress.isShotgunEnabled(game.progress.settings.selectedVolleyBallSpriteName),
            playerLevel = game.progress.combatPlayerLevel, combatRunId = orbRunId)
        displayedBossHealthRatio = session.bricks.firstOrNull { it.type == BrickType.BOSS_CORE }
            ?.let { it.health.toFloat() / it.initialHealth.coerceAtLeast(1) } ?: 0f
        lastBossHealthRatio = displayedBossHealthRatio
        bossHealthDamageFlash = 0f
        aimDirection.set(0f, 1f)
        cancelAim()
        itemsOpen = false
        completionCommitted = false
    }

    private fun requestRewardedContinue() {
        if (rewardedContinueInFlight || session.rewardedRevivesRemaining <= 0) return
        val gateway = game.monetization.rewardedReviveAdGateway
        gateway.pendingRewards(reviveRewardContext()).firstOrNull()?.let {
            applyEarnedContinue(it)
            return
        }
        when (gateway.state) {
            AdState.READY -> {
                rewardedContinueInFlight = true
                rewardedContinueMessage = "Opening rewarded video…"
                // Preserve the defeated run before leaving for an ad; it can receive a delayed reward.
                saveRewardedContinueCheckpoint()
                val context = reviveRewardContext()
                gateway.showForContext(context) { result ->
                    rewardedContinueInFlight = false
                    if (!screenVisible) return@showForContext
                    when (result) {
                        is RewardedAdResult.Earned -> {
                            if (context == reviveRewardContext()) applyEarnedContinue(result.rewardToken)
                        }

                        RewardedAdResult.ClosedWithoutReward -> rewardedContinueMessage = "Finish the video to continue."

                        is RewardedAdResult.Failed -> rewardedContinueMessage = result.message
                    }
                }
            }

            AdState.UNAVAILABLE -> {
                rewardedContinueMessage = gateway.availabilityMessage
                gateway.preload()
            }

            AdState.LOADING -> rewardedContinueMessage = "Rewarded video is still loading…"

            AdState.SHOWING -> rewardedContinueMessage = "Rewarded video is already playing…"

            AdState.DISABLED -> rewardedContinueMessage = "Rewarded videos are unavailable in this build."
        }
    }

    private fun reviveRewardContext() = "$orbRunId:revive:${session.rewardedRevivesUsed}"

    private fun saveRewardedContinueCheckpoint() {
        if (!editorTest) game.volleyPausedSession.save(level.id,
            session.snapshot().copy(earnedOrbUids = earnedOrbs.mapNotNull { it.uid }, orbRunId = orbRunId),
            session.rewardedRevivesUsed)
    }

    private fun applyEarnedContinue(token: String) {
        if (!session.reviveFromRewardedAd()) return
        // Persist the incremented continue index BEFORE acknowledging delivery. Recovery can never
        // apply this token to the next continue or another run, even if the process dies here.
        saveRewardedContinueCheckpoint()
        game.monetization.rewardedReviveAdGateway.acknowledgeReward(token, "revive")
        rewardedContinueMessage = ""
        feedbackText = "WALL PUSHED BACK • ${session.rewardedRevivesRemaining} CONTINUES LEFT"
        feedbackTime = 2.2f
        if (game.progress.settings.haptics) Gdx.input.vibrate(35)
        if (gameplayEffectsEnabled) game.assets.play("powerup", game.progress.settings.masterVolume * game.progress.settings.sfxVolume)
    }

    private fun commitCompletion() {
        completionCommitted = true
        if (editorTest) return
        game.volleyPausedSession.clear()
        val turnsUsed = max(1, session.turn - 1)
        val stars = when {
            session.score >= stage.threeStarScore || turnsUsed <= stage.parTurns -> 3
            session.score >= stage.twoStarScore || turnsUsed <= stage.parTurns + 3 -> 2
            else -> 1
        }
        val previousStars = game.progress.stars(level.id)
        game.progress.complete(level.id, session.score, stars)
        val reward = game.economy.rewardCampaignClear(
            levelId = level.id,
            world = level.world,
            stars = stars,
            score = session.score,
            previousStars = previousStars,
            isBossStage = stage.bossStage,
            coinMultiplier = session.orbCoinRewardMultiplier
        )
        game.navigate(
            ResultsScreen(
                game = game,
                level = level,
                score = session.score,
                stars = stars,
                unlocks = emptyList(),
                economyReward = reward,
                campaignMode = GameplayMode.VOLLEY,
                orbLoot = earnedOrbs.toList()
            ),
            ScreenTransitionStyle.FADE_THROUGH
        )
    }

    override fun pause() {
        pauseAndSave()
    }
    override fun hide() {
        screenVisible = false
        releaseHeldSpeed()
        removeAimInputProcessor()
        WorldVideoBackgrounds.hide()
    }
    override fun dispose() {
        screenVisible = false
        removeAimInputProcessor()
        sourceBrickArt?.dispose()
        super.dispose()
    }
}

/** Mirrors the original brick label's k/m notation without formatting every frame with Locale. */
private fun formatVolleyBrickHealth(health: Int): String {
    if (health < 1000) return health.toString()
    val unit = if (health < 1_000_000) 1000 else 1_000_000
    val suffix = if (unit == 1000) "k" else "m"
    val whole = health / unit
    return when {
        whole < 10 -> {
            val hundredths = ((health.toLong() * 100 + unit / 2) / unit).toInt()
            "${hundredths / 100}.${(hundredths % 100).toString().padStart(2, '0')}$suffix"
        }
        whole < 100 -> {
            val tenths = ((health.toLong() * 10 + unit / 2) / unit).toInt()
            "${tenths / 10}.${tenths % 10}$suffix"
        }
        else -> "$whole$suffix"
    }
}
