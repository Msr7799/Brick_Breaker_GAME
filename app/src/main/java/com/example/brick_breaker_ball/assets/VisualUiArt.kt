package com.example.brick_breaker_ball.assets

import com.badlogic.gdx.Gdx
import com.badlogic.gdx.graphics.Pixmap
import com.badlogic.gdx.graphics.Texture
import com.badlogic.gdx.graphics.g2d.TextureRegion
import com.badlogic.gdx.utils.Disposable
import com.badlogic.gdx.utils.JsonReader
import kotlin.math.roundToInt

/** Small, lazy GPU copies of the authored glossy symbols; generated masters stay untouched. */
class VisualUiArt : Disposable {
    private val manifest = JsonReader().parse(Gdx.files.internal("ui/game-symbols/manifest.json"))
    private val regions = mutableMapOf<String, TextureRegion>()

    fun region(name: String): TextureRegion = regions.getOrPut(name) {
        val entry = requireNotNull(manifest.get(name)) { "Missing game symbol: $name" }
        val source = Pixmap(Gdx.files.internal("ui/game-symbols/${entry.getString("file")}"))
        try {
            val crop = entry.get("crop")
            val x = crop.getInt("x"); val y = crop.getInt("y")
            val width = crop.getInt("width"); val height = crop.getInt("height")
            require(x >= 0 && y >= 0 && width > 0 && height > 0 &&
                x + width <= source.width && y + height <= source.height) { "Invalid symbol crop: $name" }
            val scale = minOf(1f, 192f / maxOf(width, height))
            val small = Pixmap((width * scale).roundToInt().coerceAtLeast(1),
                (height * scale).roundToInt().coerceAtLeast(1), Pixmap.Format.RGBA8888)
            try {
                small.setBlending(Pixmap.Blending.None)
                small.setFilter(Pixmap.Filter.BiLinear)
                small.drawPixmap(source, x, y, width, height, 0, 0, small.width, small.height)
                TextureRegion(Texture(small).apply {
                    setFilter(Texture.TextureFilter.Linear, Texture.TextureFilter.Linear)
                })
            } finally { small.dispose() }
        } finally { source.dispose() }
    }

    override fun dispose() {
        regions.values.forEach { it.texture.dispose() }
        regions.clear()
    }
}
