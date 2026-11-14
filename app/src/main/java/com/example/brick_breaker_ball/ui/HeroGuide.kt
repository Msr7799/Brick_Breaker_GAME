package com.example.brick_breaker_ball.ui

import com.badlogic.gdx.Preferences
import com.badlogic.gdx.graphics.Color
import com.badlogic.gdx.graphics.g2d.SpriteBatch
import com.badlogic.gdx.math.Rectangle
import com.badlogic.gdx.math.MathUtils
import com.badlogic.gdx.utils.Align
import com.example.brick_breaker_ball.assets.GameAssets

/** The story is offered only to a fresh player; each lesson has its own durable checkpoint. */
class HeroGuideProgress(private val prefs: Preferences) {
    val storyComplete: Boolean get() = prefs.getBoolean("story_complete", false)
    val firstShotComplete: Boolean get() = prefs.getBoolean("first_shot_complete", false)
    val shopOrbsComplete: Boolean get() = prefs.getBoolean("shop_orbs_complete", false)

    fun shouldShowStory(hasStartedGame: Boolean, hasPausedGame: Boolean): Boolean =
        !storyComplete && !hasStartedGame && !hasPausedGame

    fun shouldTeachFirstShot(levelId: Int, editorTest: Boolean): Boolean =
        storyComplete && !firstShotComplete && levelId == 1 && !editorTest

    fun shouldTeachShopOrbs(): Boolean = storyComplete && !shopOrbsComplete

    fun completeStory() = prefs.putBoolean("story_complete", true).flush()
    fun completeFirstShot() = prefs.putBoolean("first_shot_complete", true).flush()
    fun completeShopOrbs() = prefs.putBoolean("shop_orbs_complete", true).flush()
}

enum class GuideSpeaker { DRAGON, ORB_SAGE }

/** Foreground clock: no timers, threads, or gameplay simulation time. */
class GuideAnimation {
    var elapsed = 0f
        private set
    var exiting = false
        private set
    private var exitElapsed = 0f
    private var revealed = false
    private var reducedMotion = false
    private var frameDelta = 0f
    private var focus: Rectangle? = null

    val exitFinished: Boolean get() = exiting && (reducedMotion || exitElapsed >= .22f)
    val presence: Float get() {
        val enter = if (reducedMotion) 1f else smootherStep(elapsed / .34f)
        val leave = if (!exiting) 1f else if (reducedMotion) 0f else 1f - smootherStep(exitElapsed / .22f)
        return enter * leave
    }
    val bob: Float get() = if (reducedMotion) 0f else MathUtils.sin(elapsed * 2.6f) * 3f

    fun update(delta: Float, reduceMotion: Boolean) {
        reducedMotion = reduceMotion
        frameDelta = delta.coerceIn(0f, .05f)
        elapsed += frameDelta
        if (exiting) exitElapsed += frameDelta
    }

    fun reset() {
        elapsed = 0f
        exitElapsed = 0f
        revealed = false
        exiting = false
    }

    fun beginExit() { exiting = true }

    private fun characterCount(): Int =
        if (revealed || reducedMotion) Int.MAX_VALUE else ((elapsed - .14f).coerceAtLeast(0f) * 42f).toInt()

    fun textComplete(lines: List<String>): Boolean = characterCount() >= lines.sumOf { it.length }

    /** First tap completes the sentence. A later tap may advance the lesson. */
    fun revealOrContinue(lines: List<String>): Boolean {
        if (exiting || (!reducedMotion && elapsed < .12f)) return false
        if (!textComplete(lines)) { revealed = true; return false }
        return true
    }

    fun visibleLines(lines: List<String>): List<String> {
        var remaining = characterCount()
        return lines.map { line ->
            val count = remaining.coerceIn(0, line.length)
            remaining = (remaining - count).coerceAtLeast(0)
            line.take(count)
        }
    }

    fun focusBounds(target: Rectangle): Rectangle {
        val current = focus ?: Rectangle(target).also { focus = it }
        val blend = if (reducedMotion) 1f else (1f - kotlin.math.exp(-frameDelta * 18f))
        current.set(current.x + (target.x - current.x) * blend,
            current.y + (target.y - current.y) * blend,
            current.width + (target.width - current.width) * blend,
            current.height + (target.height - current.height) * blend)
        return current
    }
}

/** A shared speech bubble for the dragon and the talisman guide. */
object HeroGuideRenderer {
    fun drawCompanion(
        batch: SpriteBatch,
        assets: GameAssets,
        bounds: Rectangle,
        speaker: GuideSpeaker = GuideSpeaker.DRAGON,
        opacity: Float = 1f
    ) {
        batch.setColor(1f, 1f, 1f, opacity.coerceIn(0f, 1f))
        val sprite = if (speaker == GuideSpeaker.ORB_SAGE) assets.orbSageRegion else assets.heroGuideRegion
        val scale = minOf(bounds.width / sprite.regionWidth, bounds.height / sprite.regionHeight)
        val width = sprite.regionWidth * scale
        val height = sprite.regionHeight * scale
        batch.draw(sprite, bounds.x + (bounds.width - width) / 2f,
            bounds.y + (bounds.height - height) / 2f, width, height)
        batch.color = Color.WHITE
    }

    fun drawDialogue(
        batch: SpriteBatch,
        assets: GameAssets,
        screen: Rectangle,
        lines: List<String>,
        bottom: Float,
        progress: String = "TAP ANYWHERE TO CONTINUE",
        speaker: GuideSpeaker = GuideSpeaker.DRAGON,
        animation: GuideAnimation? = null,
        focus: Rectangle? = null,
        reduceMotion: Boolean = false
    ) {
        val alpha = animation?.presence ?: 1f
        val target = focus?.let { animation?.focusBounds(it) ?: it }
        drawSpotlight(batch, assets, screen, target, .69f * alpha)
        target?.let { drawFocus(batch, assets, it, animation?.elapsed ?: 0f, reduceMotion, alpha) }
        val side = (screen.width * .255f).coerceIn(155f, 240f)
        val groupWidth = minOf(screen.width - 32f, 1040f)
        val groupX = screen.x + (screen.width - groupWidth) * .5f
        val y = bottom.coerceIn(screen.y + 14f, maxOf(screen.y + 14f, screen.y + screen.height - 280f))
        val hero = Rectangle(groupX - (1f - alpha) * 80f,
            y - 5f + (animation?.bob ?: 0f) - (1f - alpha) * 18f, side, side)
        val bubble = Rectangle(
            groupX + side - 14f + (1f - alpha) * 25f,
            y - (1f - alpha) * 14f,
            groupWidth - side + 14f,
            260f
        )
        val sage = speaker == GuideSpeaker.ORB_SAGE
        val accent = if (sage) Color(.82f, .67f, 1f, alpha) else Color(1f, .43f, .35f, alpha)
        batch.setColor(.06f, .075f, .12f, .97f * alpha)
        batch.draw(assets.solidUiRegion, bubble.x - 18f, bubble.y + 64f,
            15f, 15f, 30f, 30f, 1f, 1f, 45f)
        batch.color = accent
        batch.draw(assets.solidUiRegion, bubble.x, bubble.y, bubble.width, bubble.height)
        batch.setColor(.06f, .075f, .12f, .97f * alpha)
        batch.draw(assets.solidUiRegion, bubble.x + 3f, bubble.y + 3f, bubble.width - 6f, bubble.height - 6f)
        drawCompanion(batch, assets, hero, speaker, alpha)

        val oldColor = Color(assets.smallFont.color)
        val oldX = assets.smallFont.data.scaleX
        val oldY = assets.smallFont.data.scaleY
        assets.smallFont.color = Color(1f, 1f, 1f, alpha)
        assets.smallFont.data.setScale(.73f)
        assets.smallFont.draw(batch,
            if (speaker == GuideSpeaker.ORB_SAGE) "SAGE" else "EMBER",
            bubble.x + 24f, bubble.y + bubble.height - 30f)
        assets.smallFont.data.setScale(.66f)
        (animation?.visibleLines(lines) ?: lines).take(3).forEachIndexed { i, line ->
            assets.smallFont.draw(
                batch, line,
                bubble.x + 22f,
                bubble.y + bubble.height - 80f - i * 48f,
                bubble.width - 44f,
                Align.left,
                true
            )
        }
        assets.smallFont.data.setScale(.48f)
        assets.smallFont.color = Color(accent).also { it.a = alpha }
        val footer = if (animation != null && !animation.textComplete(lines)) "TAP TO REVEAL TEXT" else progress
        assets.smallFont.draw(batch, footer, bubble.x + 22f, bubble.y + 27f, bubble.width - 44f, Align.left, true)
        assets.smallFont.data.setScale(oldX, oldY)
        assets.smallFont.color = oldColor
        batch.color = Color.WHITE
    }

    internal fun drawSpotlight(batch: SpriteBatch, assets: GameAssets, screen: Rectangle, target: Rectangle?, alpha: Float) {
        batch.setColor(0f, 0f, 0f, alpha)
        if (target == null) {
            batch.draw(assets.solidUiRegion, screen.x, screen.y, screen.width, screen.height)
        } else {
            val left = (target.x - 9f).coerceIn(screen.x, screen.x + screen.width)
            val right = (target.x + target.width + 9f).coerceIn(left, screen.x + screen.width)
            val bottom = (target.y - 9f).coerceIn(screen.y, screen.y + screen.height)
            val top = (target.y + target.height + 9f).coerceIn(bottom, screen.y + screen.height)
            batch.draw(assets.solidUiRegion, screen.x, screen.y, screen.width, bottom - screen.y)
            batch.draw(assets.solidUiRegion, screen.x, top, screen.width, screen.y + screen.height - top)
            batch.draw(assets.solidUiRegion, screen.x, bottom, left - screen.x, top - bottom)
            batch.draw(assets.solidUiRegion, right, bottom, screen.x + screen.width - right, top - bottom)
        }
        batch.color = Color.WHITE
    }

    fun drawFocus(batch: SpriteBatch, assets: GameAssets, target: Rectangle, elapsed: Float,
                  reduceMotion: Boolean, opacity: Float = 1f) {
        val pulse = if (reduceMotion) .5f else .5f + .5f * MathUtils.sin(elapsed * 5.2f)
        val padding = 7f + pulse * 4f
        val rect = Rectangle(target.x - padding, target.y - padding,
            target.width + padding * 2f, target.height + padding * 2f)
        val stroke = 3f + pulse
        batch.setColor(1f, .80f, .30f, opacity * (.65f + .35f * pulse))
        batch.draw(assets.solidUiRegion, rect.x, rect.y, rect.width, stroke)
        batch.draw(assets.solidUiRegion, rect.x, rect.y + rect.height - stroke, rect.width, stroke)
        batch.draw(assets.solidUiRegion, rect.x, rect.y, stroke, rect.height)
        batch.draw(assets.solidUiRegion, rect.x + rect.width - stroke, rect.y, stroke, rect.height)
        batch.color = Color.WHITE
    }

    fun skipBounds(screen: Rectangle): Rectangle = Rectangle(screen.x + screen.width - 174f,
        screen.y + screen.height - 125f, 144f, 60f)

    fun drawSkip(batch: SpriteBatch, assets: GameAssets, screen: Rectangle) {
        val rect = skipBounds(screen)
        batch.setColor(.06f, .075f, .12f, .94f)
        batch.draw(assets.solidUiRegion, rect.x, rect.y, rect.width, rect.height)
        val font = assets.smallFont
        val oldColor = Color(font.color)
        val oldX = font.data.scaleX
        val oldY = font.data.scaleY
        font.color = Color.WHITE
        font.data.setScale(.54f)
        font.draw(batch, "SKIP", rect.x, rect.y + 40f, rect.width, Align.center, false)
        font.data.setScale(oldX, oldY)
        font.color = oldColor
        batch.color = Color.WHITE
    }
}
