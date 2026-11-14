/*
 * المسار: app/src/main/java/com/example/brick_breaker_ball/gameplay/VolleyLayout.kt
 * الوظيفة: أبعاد ساحة Volley وأماكن عناصرها.
 * الحسابات: تحجيم المستطيلات حسب أبعاد الشاشة.
 */

package com.example.brick_breaker_ball.gameplay

import com.badlogic.gdx.math.Rectangle
import com.example.brick_breaker_ball.ui.ForgeScreen
import com.example.brick_breaker_ball.ui.UiSafeInsets
import kotlin.math.max

data class VolleyActionBarLayout(
    val bag: Rectangle,
    val items: Rectangle,
    val fastForward: Rectangle,
    val recall: Rectangle,
    val pause: Rectangle
)

object VolleyLayout {
    const val ACTION_SIZE = 110f
    const val ACTION_HEIGHT = 100f
    const val LANDSCAPE_ACTION_SIZE = 156f
    const val LANDSCAPE_ACTION_HEIGHT = 120f
    const val PORTRAIT_BOTTOM_CLEARANCE = 68f

    // Enlarged brick grid uses tighter 2-unit seams; prevent sprites from obscuring neighbouring HP labels.
    const val BRICK_VISUAL_OVERHANG = VolleySession.BRICK_GAP * .30f
    const val ACTION_ICON_FILL = .68f

    fun actionBar(
        visibleBottom: Float,
        insets: UiSafeInsets,
        leftHanded: Boolean,
        visibleLeft: Float = 0f,
        visibleWidth: Float = ForgeScreen.DESIGN_WIDTH,
        landscape: Boolean = false
    ): VolleyActionBarLayout {
        val y = visibleBottom + insets.bottom + if (landscape) 28f else PORTRAIT_BOTTOM_CLEARANCE
        val size = if (landscape) LANDSCAPE_ACTION_SIZE else ACTION_SIZE
        val height = if (landscape) LANDSCAPE_ACTION_HEIGHT else ACTION_HEIGHT
        val gap = if (landscape) 16f else 4f
        // Anchor portrait controls to the right edge while preserving a readable two-row stats panel.
        // Keep the logical 900-wide playfield on tall Android screens; landscape uses right gutter.
        val endX = if (landscape) {
            visibleLeft + visibleWidth - insets.right - 72f
        } else {
            minOf(ForgeScreen.DESIGN_WIDTH - 14f, visibleLeft + visibleWidth - insets.right - 14f)
        }
        val startX = endX - (size * 5f + gap * 4f)
        val slots = List(5) { index -> Rectangle(startX + index * (size + gap), y, size, height) }
        return if (leftHanded) {
            VolleyActionBarLayout(slots[4], slots[3], slots[2], slots[1], slots[0])
        } else {
            VolleyActionBarLayout(slots[0], slots[1], slots[2], slots[3], slots[4])
        }
    }

    /** Mirrors ExtendViewport's portrait expansion for static layout regression tests. */
    fun portraitWorldSize(pixelWidth: Int, pixelHeight: Int): Pair<Float, Float> {
        val aspect = pixelWidth.coerceAtLeast(1).toFloat() / pixelHeight.coerceAtLeast(1)
        val worldWidth = max(ForgeScreen.DESIGN_WIDTH, ForgeScreen.DESIGN_HEIGHT * aspect)
        val worldHeight = max(ForgeScreen.DESIGN_HEIGHT, ForgeScreen.DESIGN_WIDTH / aspect)
        return worldWidth to worldHeight
    }
}
