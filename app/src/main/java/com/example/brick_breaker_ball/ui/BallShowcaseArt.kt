package com.example.brick_breaker_ball.ui

import com.badlogic.gdx.graphics.Color
import com.badlogic.gdx.graphics.Pixmap
import com.badlogic.gdx.graphics.Texture
import com.badlogic.gdx.graphics.g2d.NinePatch
import com.badlogic.gdx.graphics.g2d.SpriteBatch
import com.badlogic.gdx.graphics.g2d.TextureRegion
import com.badlogic.gdx.math.Rectangle
import com.badlogic.gdx.utils.Disposable
import kotlin.math.abs
import kotlin.math.sqrt

/** Small managed UI textures. Construct once per screen; never allocate GPU resources in render. */
internal class BallShowcaseArt : Disposable {
    private val surfaceTexture = texture(96, 96) { x, y ->
        val dx = (abs(x - 47.5f) - 23.5f).coerceAtLeast(0f)
        val dy = (abs(y - 47.5f) - 23.5f).coerceAtLeast(0f)
        val alpha = (24f - sqrt(dx * dx + dy * dy)).coerceIn(0f, 1f)
        val light = .86f + (1f - y / 95f) * .14f
        Color.rgba8888(light, light, light, alpha)
    }
    private val surface = NinePatch(TextureRegion(surfaceTexture), 26, 26, 26, 26)
    private val ringTexture = texture(256, 128) { x, y ->
        val dx = (x - 127.5f) / 122f
        val dy = (y - 63.5f) / 56f
        val radius = sqrt(dx * dx + dy * dy)
        val rim = (1f - abs(radius - .85f) / .045f).coerceIn(0f, 1f)
        val inner = (1f - abs(radius - .68f) / .017f).coerceIn(0f, 1f) * .35f
        Color.rgba8888(1f, 1f, 1f, maxOf(rim, inner) * .82f)
    }
    private val ring = TextureRegion(ringTexture)
    private val scratch = Color()

    fun panel(batch: SpriteBatch, rect: Rectangle, face: Color, border: Color,
              scale: Float, pressed: Boolean = false, opacity: Float = 1f) {
        val inset = 2f * scale
        val corner = minOf(20f * scale, rect.height * .4f, rect.width * .4f)
        surface.setLeftWidth(corner); surface.setRightWidth(corner)
        surface.setTopHeight(corner); surface.setBottomHeight(corner)
        batch.setColor(border.r, border.g, border.b, opacity)
        surface.draw(batch, rect.x, rect.y, rect.width, rect.height)
        val dim = if (pressed) .78f else 1f
        batch.setColor(face.r * dim, face.g * dim, face.b * dim, opacity)
        surface.draw(batch, rect.x + inset, rect.y + inset,
            rect.width - inset * 2f, rect.height - inset * 2f)
        batch.color = Color.WHITE
    }

    fun ring(batch: SpriteBatch, rect: Rectangle, accent: Color, opacity: Float) {
        scratch.set(accent); scratch.a = opacity
        batch.color = scratch
        batch.draw(ring, rect.x, rect.y, rect.width, rect.height)
        batch.color = Color.WHITE
    }

    private fun texture(width: Int, height: Int, pixel: (Float, Float) -> Int): Texture {
        val pixmap = Pixmap(width, height, Pixmap.Format.RGBA8888)
        try {
            pixmap.setBlending(Pixmap.Blending.None)
            for (y in 0 until height) for (x in 0 until width) pixmap.drawPixel(x, y, pixel(x.toFloat(), y.toFloat()))
            return Texture(pixmap).apply { setFilter(Texture.TextureFilter.Linear, Texture.TextureFilter.Linear) }
        } finally { pixmap.dispose() }
    }

    override fun dispose() {
        surfaceTexture.dispose()
        ringTexture.dispose()
    }
}
