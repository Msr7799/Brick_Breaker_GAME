/*
 * المسار: app/src/main/java/com/example/brick_breaker_ball/ui/UiMetrics.kt
 * الوظيفة: قياسات الواجهة ومساحات الأمان.
 * الحسابات: تحويل أبعاد الجهاز إلى إحداثيات LibGDX.
 */

package com.example.brick_breaker_ball.ui

import com.badlogic.gdx.Gdx
import com.badlogic.gdx.graphics.g2d.BitmapFont
import com.badlogic.gdx.math.Rectangle
import com.badlogic.gdx.utils.viewport.Viewport
import kotlin.math.max

enum class UiOrientation { PORTRAIT, LANDSCAPE }

enum class UiTextRole(val baseMultiplier: Float) {
    TITLE_XL(1.14f),
    TITLE(1.08f),
    SECTION(1.04f),
    BUTTON_LARGE(1.08f),
    BUTTON(1f),
    BODY(1f),
    CAPTION(.96f)
}

data class UiSafeInsets(
    val left: Float = 0f,
    val right: Float = 0f,
    val top: Float = 0f,
    val bottom: Float = 0f
)

/**
 * Central production UI metrics.
 *
 * The game uses logical world units, so physical Android safe insets are converted back to
 * viewport-world units. Reflection intentionally keeps this compatible with LibGDX versions
 * where safe-inset accessors may not be present on every backend.
 */
object UiMetrics {
    private data class SafeInsetMethods(
        val graphicsClass: Class<*>,
        val left: java.lang.reflect.Method?,
        val right: java.lang.reflect.Method?,
        val top: java.lang.reflect.Method?,
        val bottom: java.lang.reflect.Method?
    )

    private var cachedSafeInsetMethods: SafeInsetMethods? = null

    private fun safeInsetMethods(graphics: Any): SafeInsetMethods {
        cachedSafeInsetMethods?.takeIf { it.graphicsClass == graphics.javaClass }?.let { return it }
        val methods = graphics.javaClass.methods
        fun lookup(name: String) = methods.firstOrNull { it.name == name && it.parameterCount == 0 }
        return SafeInsetMethods(
            graphicsClass = graphics.javaClass,
            left = lookup("getSafeInsetLeft"),
            right = lookup("getSafeInsetRight"),
            top = lookup("getSafeInsetTop"),
            bottom = lookup("getSafeInsetBottom")
        ).also { cachedSafeInsetMethods = it }
    }

    fun orientation(): UiOrientation = if (Gdx.graphics.width > Gdx.graphics.height) UiOrientation.LANDSCAPE else UiOrientation.PORTRAIT

    fun textScale(orientation: UiOrientation = orientation()): Float = if (orientation == UiOrientation.LANDSCAPE) 1.29f else 1.08f

    fun touchMinSize(orientation: UiOrientation = orientation()): Float = if (orientation == UiOrientation.LANDSCAPE) 82f else 70f

    fun expandedTouchRect(rect: Rectangle, minSize: Float = touchMinSize()): Rectangle {
        val w = max(rect.width, minSize)
        val h = max(rect.height, minSize)
        return Rectangle(rect.x - (w - rect.width) / 2f, rect.y - (h - rect.height) / 2f, w, h)
    }

    /** Allocation-free hit test for render/input hot paths. */
    fun containsExpanded(rect: Rectangle, x: Float, y: Float, minSize: Float = touchMinSize()): Boolean {
        val w = max(rect.width, minSize)
        val h = max(rect.height, minSize)
        val left = rect.x - (w - rect.width) / 2f
        val bottom = rect.y - (h - rect.height) / 2f
        return x >= left && x <= left + w && y >= bottom && y <= bottom + h
    }

    fun safeInsets(viewport: Viewport): UiSafeInsets {
        val graphics = Gdx.graphics
        val methods = safeInsetMethods(graphics)
        fun invoke(method: java.lang.reflect.Method?): Int = try {
            (method?.invoke(graphics) as? Number)?.toInt() ?: 0
        } catch (_: Throwable) {
            0
        }
        val pxW = graphics.width.coerceAtLeast(1).toFloat()
        val pxH = graphics.height.coerceAtLeast(1).toFloat()
        val sx = viewport.worldWidth / pxW
        val sy = viewport.worldHeight / pxH
        return UiSafeInsets(
            left = invoke(methods.left) * sx,
            right = invoke(methods.right) * sx,
            top = invoke(methods.top) * sy,
            bottom = invoke(methods.bottom) * sy
        )
    }

    inline fun <T> withFontScale(font: BitmapFont, scale: Float, block: () -> T): T {
        val oldX = font.data.scaleX
        val oldY = font.data.scaleY
        font.data.setScale(oldX * scale, oldY * scale)
        return try {
            block()
        } finally {
            font.data.setScale(oldX, oldY)
        }
    }
}
