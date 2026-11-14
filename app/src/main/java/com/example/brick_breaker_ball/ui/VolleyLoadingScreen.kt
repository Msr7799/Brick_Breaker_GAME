/*
 * المسار: app/src/main/java/com/example/brick_breaker_ball/ui/VolleyLoadingScreen.kt
 * الوظيفة: عرض شاشة تحميل Volley.
 * الحسابات: نسبة التقدم والزمن وتحجيم الخلفية.
 */

package com.example.brick_breaker_ball.ui

import com.badlogic.gdx.graphics.Color
import com.badlogic.gdx.math.MathUtils
import com.example.brick_breaker_ball.BrickBreakerGame
import com.example.brick_breaker_ball.BuildConfig
import com.example.brick_breaker_ball.assets.WorldVideoBackgrounds
import com.example.brick_breaker_ball.campaign.LevelDefinition
import com.example.brick_breaker_ball.campaign.VolleyStageRepository
import com.example.brick_breaker_ball.gameplay.GameplayMode
import com.example.brick_breaker_ball.gameplay.VolleySessionSnapshot

internal val VOLLEY_LOADING_BACKGROUND_FILES = BuildConfig.VOLLEY_LOADING_IMAGES.split(';').filter(String::isNotBlank)

/** Portrait transition that displays the supplied loading art while world assets are warmed. */
class VolleyLoadingScreen(
    game: BrickBreakerGame,
    private val level: LevelDefinition,
    private val restoredSnapshot: VolleySessionSnapshot? = null,
    private val restoredRewardedRevivesUsed: Int = 0
) : ForgeScreen(game) {
    private companion object {
        const val MIN_LOADING_SECONDS = 1.2f
        const val LOADER_FRAME_COUNT = 12
        const val LOADER_MAX_WIDTH = 650f
        const val LOADER_MAX_HEIGHT = 176f
        private var previousBackgroundFile: String? = null

        fun takeRandomBackgroundFile(): String {
            val choices = VOLLEY_LOADING_BACKGROUND_FILES.filterNot { it == previousBackgroundFile }
                .ifEmpty { VOLLEY_LOADING_BACKGROUND_FILES }
            return choices.random().also { previousBackgroundFile = it }
        }
    }

    private val stage = VolleyStageRepository.forLevel(level.id)
    private val bossDialogue = if (restoredSnapshot == null) VolleyBossDialogueCatalog.forStage(stage) else null
    private val backgroundFile = takeRandomBackgroundFile()
    private var elapsed = 0f
    private var warmupStep = 0
    private var opened = false
    private var skipRequested = false

    override fun show() {
        game.applyGameplayOrientation(GameplayMode.VOLLEY)
        WorldVideoBackgrounds.hide()
    }

    override fun render(delta: Float) {
        elapsed += delta.coerceAtLeast(0f)
        // Let the entrance veil clear before decoding heavyweight stage resources. Otherwise
        // a slow decode holds a nearly black frame on screen for its entire duration.
        if (!game.uiMotion.active && elapsed >= .08f) {
            when (warmupStep) {
                0 -> {
                    game.assets.volleyStageBackground(stage)
                    warmupStep++
                }

                1 -> {
                    game.assets.volleyWorldArt(stage.world)
                    warmupStep++
                }

                2 -> {
                    if (bossDialogue != null) {
                        game.assets.volleyBossPortrait(stage.world)
                        game.assets.prepareBossDialogueAudio(bossDialogue.moment)
                    }
                    warmupStep++
                }
            }
        }

        beginStaticBackground(game.assets.volleyLoadingBackground(backgroundFile), .06f)
        // Play the twelve extracted GIF frames during the short minimum presentation.
        val loaderFrame = if (game.progress.settings.reduceMotion) {
            0
        } else {
            ((elapsed / MIN_LOADING_SECONDS) * LOADER_FRAME_COUNT)
                .toInt()
                .coerceIn(0, LOADER_FRAME_COUNT - 1)
        }
        val loader = game.assets.volleyLoaderFrame(loaderFrame)
        val pulse = if (game.progress.settings.reduceMotion) 1f else .992f + .008f * MathUtils.sin(elapsed * 5f)
        val sourceAspect = loader.regionWidth.toFloat() / loader.regionHeight.coerceAtLeast(1)
        val loaderWidth = minOf(LOADER_MAX_WIDTH, LOADER_MAX_HEIGHT * sourceAspect) * pulse
        val loaderHeight = loaderWidth / sourceAspect
        batch.color = Color(0f, 0f, 0f, .58f)
        batch.draw(game.assets.solidUiRegion, 112f, 230f, 676f, 340f)
        batch.color = Color.WHITE
        batch.draw(loader, DESIGN_WIDTH / 2f - loaderWidth / 2f, 390f - loaderHeight / 2f, loaderWidth, loaderHeight)
        fittedText(game.assets.titleFont, stage.name, 90f, 535f, 720f, .62f)
        fittedText(
            game.assets.smallFont,
            if (stage.bossStage) "BOSS STAGE  •  PREPARE YOUR ITEMS" else "BUILDING VOLLEY ${stage.stage}/${VolleyStageRepository.worldLevelCount(stage.world)}",
            110f,
            270f,
            680f,
            .70f
        )
        val skip = button("SKIP", 698f, 1460f, 154f, 70f, ForgeUiRenderer.GradientStyle.NEUTRAL)
        end()

        if (tapped(skip)) skipRequested = true
        if (warmupStep >= 3 && (elapsed >= MIN_LOADING_SECONDS || skipRequested) && !opened) {
            opened = true
            val next = if (bossDialogue != null) {
                VolleyBossDialogueScreen(game, level, restoredSnapshot)
            } else {
                VolleyGameScreen(game, level, restoredSnapshot, restoredRewardedRevivesUsed)
            }
            game.navigate(next, ScreenTransitionStyle.FADE_THROUGH)
        }
    }
}
