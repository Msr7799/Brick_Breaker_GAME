package com.example.brick_breaker_ball.ui

import com.badlogic.gdx.graphics.Color
import com.badlogic.gdx.graphics.Pixmap
import com.badlogic.gdx.graphics.Texture
import com.badlogic.gdx.graphics.g2d.NinePatch
import com.badlogic.gdx.graphics.g2d.SpriteBatch
import com.badlogic.gdx.graphics.g2d.TextureRegion
import com.badlogic.gdx.math.Rectangle
import com.badlogic.gdx.utils.Disposable
import com.example.brick_breaker_ball.assets.GameAssets
import kotlin.math.abs
import kotlin.math.sqrt

/** Reuses SHOP/ORBS materials. Only the transparent stage light is generated and owned here. */
internal class BallShowcaseArt(assets: GameAssets) : Disposable {
    private val stone = NinePatch(assets.boards2.requireRegion("stone_button_style_01"), 38, 38, 18, 18)
    private val wood = NinePatch(assets.boards2.requireRegion("scroll_board_content_area"), 28, 28, 28, 28)
    private val ornament = NinePatch(assets.boards2.requireRegion("ornate_button_blank"), 68, 68, 20, 20)
    private val green = NinePatch(assets.ballCollectionButtons.requireRegion("wide_green"), 100, 100, 28, 28)
    private val frame = assets.boards2.requireRegion("large_stone_item_panel").let { r ->
        val w = r.regionWidth; val h = r.regionHeight; val c = 64
        arrayOf(TextureRegion(r, 0, 0, c, c), TextureRegion(r, c, 0, w - 2 * c, c), TextureRegion(r, w - c, 0, c, c),
            TextureRegion(r, 0, c, c, h - 2 * c), TextureRegion(r, w - c, c, c, h - 2 * c),
            TextureRegion(r, 0, h - c, c, c), TextureRegion(r, c, h - c, w - 2 * c, c), TextureRegion(r, w - c, h - c, c, c))
    }
    private val ringTexture = run {
        val pixmap = Pixmap(256, 128, Pixmap.Format.RGBA8888)
        try {
            pixmap.setBlending(Pixmap.Blending.None)
            for (y in 0 until 128) for (x in 0 until 256) {
                val dx = (x - 127.5f) / 122f; val dy = (y - 63.5f) / 56f
                val radius = sqrt(dx * dx + dy * dy)
                val alpha = maxOf((1f - abs(radius - .85f) / .045f).coerceIn(0f, 1f),
                    (1f - abs(radius - .68f) / .017f).coerceIn(0f, 1f) * .35f)
                pixmap.drawPixel(x, y, Color.rgba8888(1f, 1f, 1f, alpha * .82f))
            }
            Texture(pixmap).apply { setFilter(Texture.TextureFilter.Linear, Texture.TextureFilter.Linear) }
        } finally { pixmap.dispose() }
    }
    private val ring = TextureRegion(ringTexture)
    private val woodBounds = Rectangle()

    private fun patch(batch: SpriteBatch, patch: NinePatch, rect: Rectangle, scale: Float,
                      side: Float, top: Float, pressed: Boolean = false, selected: Boolean = false) {
        patch.setLeftWidth(minOf(side * scale, rect.width * .25f))
        patch.setRightWidth(minOf(side * scale, rect.width * .25f))
        patch.setTopHeight(minOf(top * scale, rect.height * .35f))
        patch.setBottomHeight(minOf(top * scale, rect.height * .35f))
        val dim = if (pressed) .78f else 1f
        batch.setColor(dim, dim * (if (selected) .95f else 1f), dim * (if (selected) .72f else 1f), 1f)
        patch.draw(batch, rect.x, rect.y, rect.width, rect.height)
        batch.color = Color.WHITE
    }

    fun stone(batch: SpriteBatch, rect: Rectangle, scale: Float, pressed: Boolean = false, selected: Boolean = false) =
        patch(batch, stone, rect, scale, 32f, 18f, pressed, selected)

    fun title(batch: SpriteBatch, rect: Rectangle, scale: Float) = patch(batch, ornament, rect, scale, 64f, 20f)

    fun action(batch: SpriteBatch, rect: Rectangle, scale: Float, active: Boolean, pressed: Boolean) =
        patch(batch, if (active) green else ornament, rect, scale, if (active) 42f else 64f, 20f, pressed)

    fun tray(batch: SpriteBatch, rect: Rectangle, scale: Float) {
        val c = minOf(52f * scale, rect.height * .2f, rect.width * .2f)
        val x = rect.x; val y = rect.y; val w = rect.width; val h = rect.height
        // Keep the wood within the carved border; preserve its transparent outer silhouette.
        val inset = c * .60f
        woodBounds.set(x + inset, y + inset, w - inset * 2f, h - inset * 2f)
        patch(batch, wood, woodBounds, scale, 24f, 24f)
        batch.color = Color.WHITE
        batch.draw(frame[0], x, y + h - c, c, c)
        batch.draw(frame[1], x + c, y + h - c, w - 2f * c, c)
        batch.draw(frame[2], x + w - c, y + h - c, c, c)
        batch.draw(frame[3], x, y + c, c, h - 2f * c)
        batch.draw(frame[4], x + w - c, y + c, c, h - 2f * c)
        batch.draw(frame[5], x, y, c, c)
        batch.draw(frame[6], x + c, y, w - 2f * c, c)
        batch.draw(frame[7], x + w - c, y, c, c)
    }

    fun ring(batch: SpriteBatch, rect: Rectangle, accent: Color) {
        batch.setColor(accent.r, accent.g, accent.b, .55f)
        batch.draw(ring, rect.x, rect.y, rect.width, rect.height)
        batch.color = Color.WHITE
    }

    override fun dispose() = ringTexture.dispose()
}
