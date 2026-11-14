/*
 * المسار: app/src/main/java/com/example/brick_breaker_ball/ui/ForgeUiRenderer.kt
 * الوظيفة: رسم لوحات وأزرار الواجهة.
 * الحسابات: تدرجات اللون وتحجيم المستطيلات.
 */

package com.example.brick_breaker_ball.ui

import com.badlogic.gdx.graphics.Color
import com.badlogic.gdx.graphics.Pixmap
import com.badlogic.gdx.graphics.Texture
import com.badlogic.gdx.graphics.g2d.SpriteBatch
import com.badlogic.gdx.math.Rectangle

/**
 * Generated UI gradients for the Crimson / Ivory / Navy production theme.
 * The renderer only consumes ForgeUiPalette, so generated panels, selected states and buttons
 * stay inside the three-color family supplied for the game.
 *
 * The renderer stays inside the active SpriteBatch and uses tiny generated gradient textures,
 * avoiding ShapeRenderer batch switching and per-frame helper Rectangle allocations.
 */
class ForgeUiRenderer {
    enum class GradientStyle {
        PRIMARY,
        CRIMSON,
        SUCCESS,
        EQUIPPED_BALL_GREEN,
        NEUTRAL,
        DISABLED,

        // Compatibility styles for older call sites while the project is being cleaned up.
        PURPLE,
        MAROON,
        GOLD
    }

    /** Dark text on ivory gradients; ivory text on dark or crimson actions. */
    fun labelColor(style: GradientStyle): Color = when (style) {
        GradientStyle.SUCCESS, GradientStyle.CRIMSON, GradientStyle.MAROON -> ForgeUiPalette.neutralBlack
        GradientStyle.EQUIPPED_BALL_GREEN -> ForgeUiPalette.textPrimary
        else -> ForgeUiPalette.textPrimary
    }

    private val gradients = GradientStyle.entries.associateWith { style ->
        createGradientTexture(colorsFor(style))
    }

    fun drawGradientButton(batch: SpriteBatch, rect: Rectangle, style: GradientStyle) {
        drawSquareGradientButton(batch, rect, style)
    }

    /** Square-cornered button for overlays where rounded texture scaling produces artifacts. */
    fun drawSquareGradientButton(batch: SpriteBatch, rect: Rectangle, style: GradientStyle) {
        drawGradient(batch, rect.x, rect.y - 3f, rect.width, rect.height + 3f, GradientStyle.NEUTRAL, .96f)
        drawGradient(batch, rect.x + 2f, rect.y + 2f, rect.width - 4f, rect.height - 5f, style)
    }

    fun drawRoundedGradientButton(batch: SpriteBatch, rect: Rectangle, style: GradientStyle) {
        drawGradientButton(batch, rect, style)
    }

    /** Settings/footer action button with a clear forged border and square corners. */
    fun drawCompactRoundedGradientButton(batch: SpriteBatch, rect: Rectangle, style: GradientStyle) {
        drawGradientButton(batch, rect, style)
    }

    fun drawGradientPanel(batch: SpriteBatch, rect: Rectangle, style: GradientStyle = GradientStyle.NEUTRAL) {
        drawGradient(batch, rect.x, rect.y, rect.width, rect.height, style)
    }

    fun drawGradientBorderPanel(
        batch: SpriteBatch,
        rect: Rectangle,
        accent: GradientStyle,
        inset: Float = 6f
    ) {
        drawSquareGradientBorderPanel(batch, rect, accent, inset)
    }

    /** Square-cornered bordered panel used by the Volley pause and continue dialogs. */
    fun drawSquareGradientBorderPanel(
        batch: SpriteBatch,
        rect: Rectangle,
        accent: GradientStyle,
        inset: Float = 6f
    ) {
        drawGradient(batch, rect.x - inset, rect.y - inset, rect.width + inset * 2f, rect.height + inset * 2f, accent, .22f)
        drawGradient(batch, rect.x, rect.y, rect.width, rect.height, accent)
        drawGradient(
            batch,
            rect.x + inset,
            rect.y + inset,
            rect.width - inset * 2f,
            rect.height - inset * 2f,
            GradientStyle.NEUTRAL
        )
    }

    /** Compatibility entry point; deliberately square to avoid scaled-corner artifacts. */
    fun drawRoundedGradientBorderPanel(
        batch: SpriteBatch,
        rect: Rectangle,
        accent: GradientStyle,
        inset: Float = 6f
    ) {
        drawSquareGradientBorderPanel(batch, rect, accent, inset)
    }

    /** Compact compatibility entry point using the same clean square border. */
    fun drawCompactRoundedGradientBorderPanel(
        batch: SpriteBatch,
        rect: Rectangle,
        accent: GradientStyle,
        inset: Float = 2f
    ) {
        drawSquareGradientBorderPanel(batch, rect, accent, inset)
    }

    fun drawGradientProgress(batch: SpriteBatch, rect: Rectangle, style: GradientStyle) {
        drawGradient(batch, rect.x, rect.y, rect.width, rect.height, style)
    }

    fun dispose() {
        gradients.values.forEach(Texture::dispose)
    }

    private fun drawGradient(
        batch: SpriteBatch,
        x: Float,
        y: Float,
        width: Float,
        height: Float,
        style: GradientStyle,
        alpha: Float = 1f
    ) {
        if (width <= 0f || height <= 0f) return
        batch.setColor(1f, 1f, 1f, alpha)
        batch.draw(gradients.getValue(style), x, y, width, height)
        batch.color = Color.WHITE
    }

    private fun createGradientTexture(colors: List<Color>): Texture {
        val pixmap = Pixmap(1, colors.size, Pixmap.Format.RGBA8888)
        colors.forEachIndexed { index, color ->
            // First palette color renders at the lower edge and the lightest at the upper edge.
            pixmap.drawPixel(0, colors.lastIndex - index, Color.rgba8888(color))
        }
        return Texture(pixmap).also {
            it.setFilter(Texture.TextureFilter.Linear, Texture.TextureFilter.Linear)
            pixmap.dispose()
        }
    }

    private fun colorsFor(style: GradientStyle): List<Color> = when (style) {
        GradientStyle.PRIMARY, GradientStyle.PURPLE, GradientStyle.GOLD -> listOf(
            ForgeUiPalette.primaryDark,
            ForgeUiPalette.primary,
            ForgeUiPalette.primaryLight
        )

        GradientStyle.CRIMSON, GradientStyle.MAROON -> listOf(
            ForgeUiPalette.crimsonDark,
            ForgeUiPalette.crimson,
            ForgeUiPalette.crimsonLight
        )

        GradientStyle.SUCCESS -> listOf(
            ForgeUiPalette.successDark,
            ForgeUiPalette.success,
            ForgeUiPalette.successLight
        )

        GradientStyle.EQUIPPED_BALL_GREEN -> listOf(
            ForgeUiPalette.equippedBallGreenDark,
            ForgeUiPalette.equippedBallGreen,
            ForgeUiPalette.equippedBallGreenLight
        )

        GradientStyle.NEUTRAL -> listOf(
            ForgeUiPalette.neutralBlack,
            ForgeUiPalette.neutralDark,
            ForgeUiPalette.neutralDark2
        )

        GradientStyle.DISABLED -> listOf(
            ForgeUiPalette.disabledDark,
            ForgeUiPalette.disabled,
            ForgeUiPalette.disabledLight
        )
    }
}
