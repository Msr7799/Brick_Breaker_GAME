/*
 * المسار: app/src/main/java/com/example/brick_breaker_ball/ui/UiMotion.kt
 * الوظيفة: حركة عناصر الواجهة وانتقالاتها.
 * الحسابات: استيفاء الحركة والشفافية عبر الزمن.
 */

package com.example.brick_breaker_ball.ui

import com.badlogic.gdx.Gdx
import com.badlogic.gdx.graphics.Color
import com.badlogic.gdx.graphics.Pixmap
import com.badlogic.gdx.graphics.Texture
import com.badlogic.gdx.graphics.g2d.SpriteBatch
import com.badlogic.gdx.utils.Disposable

/** The only transition style in the app: fade out, swap screens, fade in. */
enum class ScreenTransitionStyle(val duration: Float) { FADE_THROUGH(.28f) }

internal fun smootherStep(value: Float): Float {
    val t = value.coerceIn(0f, 1f)
    return t * t * (3f - 2f * t)
}

/** Fade to fully opaque and then back to transparent, without screenshots or camera moves. */
internal fun navigationVeilAlpha(progress: Float): Float = 1f - smootherStep(progress)

data class UiEntranceFrame(
    val alpha: Float,
    val progress: Float,
    val style: ScreenTransitionStyle,
    val active: Boolean,
    val readyToSwitch: Boolean = false
)

/** A single testable clock shared by all navigation paths. */
class UiMotionState {
    private enum class Phase { IDLE, FADE_OUT, FADE_IN }
    private var phase = Phase.IDLE
    private var elapsed = 0f
    private var duration = ScreenTransitionStyle.FADE_THROUGH.duration

    val active: Boolean get() = phase != Phase.IDLE
    val exiting: Boolean get() = phase == Phase.FADE_OUT

    /** Returns false when a transition is already in progress. */
    fun startExit(reduceMotion: Boolean = false): Boolean {
        if (phase == Phase.FADE_OUT || reduceMotion) return false
        // The menu is already interactive while its entrance veil is fading out.
        // Reverse that fade when the player taps Continue immediately, instead of
        // silently dropping the first tap and making the second one work.
        val reverseProgress = if (phase == Phase.FADE_IN) {
            (1f - elapsed / duration).coerceIn(0f, 1f)
        } else {
            0f
        }
        phase = Phase.FADE_OUT
        duration = ScreenTransitionStyle.FADE_THROUGH.duration
        elapsed = duration * reverseProgress
        return true
    }

    /** Called ONLY after the destination screen has replaced the old one under full cover. */
    fun startEntrance(
        duration: Float = ScreenTransitionStyle.FADE_THROUGH.duration,
        style: ScreenTransitionStyle = ScreenTransitionStyle.FADE_THROUGH
    ) {
        @Suppress("UNUSED_VARIABLE")
        val ignoredStyle = style // compatibility with older unit tests
        this.duration = duration.coerceAtLeast(.05f)
        elapsed = 0f
        phase = Phase.FADE_IN
    }

    fun sampleEntrance(delta: Float, reduceMotion: Boolean): UiEntranceFrame {
        if (phase == Phase.IDLE) return UiEntranceFrame(0f, 1f, ScreenTransitionStyle.FADE_THROUGH, false)
        if (reduceMotion) {
            val swap = phase == Phase.FADE_OUT
            phase = Phase.IDLE
            elapsed = 0f
            return UiEntranceFrame(if (swap) 1f else 0f, 1f, ScreenTransitionStyle.FADE_THROUGH, false, swap)
        }
        // A slow destination frame must not leave the opaque veil covering a ready screen.
        // Keep the exit controlled, but let the entrance catch up with real elapsed time.
        val step = if (phase == Phase.FADE_OUT) delta.coerceIn(0f, .05f) else delta.coerceAtLeast(0f)
        elapsed = (elapsed + step).coerceAtMost(duration)
        val p = (elapsed / duration).coerceIn(0f, 1f)
        val fadeOut = phase == Phase.FADE_OUT
        val alpha = if (fadeOut) smootherStep(p) else 1f - smootherStep(p)
        val switch = fadeOut && p >= 1f
        if (!fadeOut && p >= 1f) phase = Phase.IDLE
        return UiEntranceFrame(alpha, p, ScreenTransitionStyle.FADE_THROUGH, active, switch)
    }

    fun entranceAlpha(delta: Float, reduceMotion: Boolean): Float = sampleEntrance(delta, reduceMotion).alpha
    fun dispose() {
        phase = Phase.IDLE
    }
}

/** Short translucent blue transition; avoids a full black screen during image preparation. */
class NavigationVeil : Disposable {
    private val batch = SpriteBatch()
    private val pixel: Texture = Pixmap(1, 1, Pixmap.Format.RGBA8888).let { pixmap ->
        try {
            pixmap.setColor(Color.WHITE)
            pixmap.fill()
            Texture(pixmap)
        } finally {
            pixmap.dispose()
        }
    }

    /** Returns true when the pending screen can be committed without exposing a frame jump. */
    fun render(motion: UiMotionState, delta: Float, reduceMotion: Boolean): Boolean {
        if (!motion.active) return false
        val frame = motion.sampleEntrance(delta, reduceMotion)
        if (frame.alpha > .001f) {
            val width = Gdx.graphics.width
            val height = Gdx.graphics.height
            if (width > 0 && height > 0) {
                batch.projectionMatrix.setToOrtho2D(0f, 0f, width.toFloat(), height.toFloat())
                batch.begin()
                // A dark translucent veil is intentional: avoid the old 100% black flash.
                batch.setColor(.025f, .055f, .085f, frame.alpha * .72f)
                batch.draw(pixel, 0f, 0f, width.toFloat(), height.toFloat())
                batch.end()
            }
        }
        return frame.readyToSwitch
    }

    override fun dispose() {
        batch.dispose()
        pixel.dispose()
    }
}
