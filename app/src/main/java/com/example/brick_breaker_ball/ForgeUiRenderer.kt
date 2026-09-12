package com.example.brick_breaker_ball

import com.badlogic.gdx.graphics.Color
import com.badlogic.gdx.graphics.Pixmap
import com.badlogic.gdx.graphics.Texture
import com.badlogic.gdx.graphics.g2d.SpriteBatch
import com.badlogic.gdx.math.Rectangle

/**
 * Generated UI gradients. Accent colors are never multiplied into pre-colored UI textures,
 * so burnt orange stays orange, crimson stays crimson, and olive green is used intentionally.
 *
 * The renderer stays inside the active SpriteBatch and uses tiny generated gradient textures,
 * avoiding ShapeRenderer batch switching and per-frame helper Rectangle allocations.
 */
class ForgeUiRenderer {
    enum class GradientStyle {
        PRIMARY,
        CRIMSON,
        SUCCESS,
        NEUTRAL,
        DISABLED,

        // Compatibility styles for older call sites while the project is being cleaned up.
        PURPLE,
        MAROON,
        GOLD,
    }

    private val gradients = GradientStyle.entries.associateWith { style ->
        createGradientTexture(colorsFor(style))
    }

    fun drawGradientButton(batch: SpriteBatch, rect: Rectangle, style: GradientStyle) {
        // Dark foundation gives the button a subtle forged/raised silhouette.
        drawGradient(batch, rect.x, rect.y - 3f, rect.width, rect.height + 3f, GradientStyle.NEUTRAL, .96f)
        drawGradient(batch, rect.x + 2f, rect.y + 2f, rect.width - 4f, rect.height - 5f, style)
    }

    fun drawGradientPanel(batch: SpriteBatch, rect: Rectangle, style: GradientStyle = GradientStyle.NEUTRAL) {
        drawGradient(batch, rect.x, rect.y, rect.width, rect.height, style)
    }

    fun drawGradientBorderPanel(
        batch: SpriteBatch,
        rect: Rectangle,
        accent: GradientStyle,
        inset: Float = 6f,
    ) {
        drawGradient(
            batch,
            rect.x - inset,
            rect.y - inset,
            rect.width + inset * 2f,
            rect.height + inset * 2f,
            accent,
            .20f,
        )
        drawGradient(batch, rect.x, rect.y, rect.width, rect.height, accent)
        drawGradient(
            batch,
            rect.x + inset,
            rect.y + inset,
            rect.width - inset * 2f,
            rect.height - inset * 2f,
            GradientStyle.NEUTRAL,
        )
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
        alpha: Float = 1f,
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
            ForgeUiPalette.primaryLight,
        )

        GradientStyle.CRIMSON, GradientStyle.MAROON -> listOf(
            ForgeUiPalette.crimsonDark,
            ForgeUiPalette.crimson,
            ForgeUiPalette.crimsonLight,
        )

        GradientStyle.SUCCESS -> listOf(
            ForgeUiPalette.successDark,
            ForgeUiPalette.success,
            ForgeUiPalette.successLight,
        )

        GradientStyle.NEUTRAL -> listOf(
            ForgeUiPalette.neutralBlack,
            ForgeUiPalette.neutralDark,
            ForgeUiPalette.neutralDark2,
        )

        GradientStyle.DISABLED -> listOf(
            ForgeUiPalette.disabledDark,
            ForgeUiPalette.disabled,
            ForgeUiPalette.disabledLight,
        )
    }
}
