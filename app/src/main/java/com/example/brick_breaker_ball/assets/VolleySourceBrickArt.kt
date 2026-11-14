package com.example.brick_breaker_ball.assets

import com.badlogic.gdx.Gdx
import com.badlogic.gdx.graphics.Pixmap
import com.badlogic.gdx.graphics.Texture
import com.badlogic.gdx.graphics.g2d.TextureRegion
import com.badlogic.gdx.utils.Disposable
import com.badlogic.gdx.utils.JsonReader
import com.badlogic.gdx.utils.JsonValue
import com.example.brick_breaker_ball.gameplay.VolleyPieceShape
import kotlin.math.roundToInt

/** Imported campaign brick art with original Forge addon symbols. */
class VolleySourceBrickArt : Disposable {
    data class Layer(val region: TextureRegion, val width: Float, val height: Float,
                     val offsetX: Float = 0f, val offsetY: Float = 0f)
    data class Visual(val layers: List<Layer>, val healthLabelYFraction: Float = .67f)

    private val manifest = JsonReader().parse(Gdx.files.internal("volley-source/bricks/manifest.json"))
    private val textures = mutableMapOf<String, Texture>()
    private val visuals = mutableMapOf<String, Visual>()

    fun visual(sourceScene: String, world: Int, kind: String?, shape: VolleyPieceShape): Visual? {
        val selectedScene = VolleyBrickPalette.visualScene(sourceScene, world, kind, shape)
        // A recolored reinforced brick still needs its authored cap. Include the
        // original scene in the cache key so it cannot reuse an ordinary visual.
        val visualKey = "$selectedScene|$sourceScene"
        visuals[visualKey]?.let { return it }
        val entry = requireNotNull(manifest.get(selectedScene)) { "Missing brick art: $selectedScene" }
        val addonEntry = manifest.get(sourceScene) ?: entry
        if (entry.get("mainVisible")?.asBoolean() == false) return null
        return visuals.getOrPut(visualKey) {
            val sourceScale = entry.getFloat("spriteScale")
            val viewportScale = 900f / 720f
            fun layer(path: String, scale: Float, offsetX: Float = 0f, offsetY: Float = 0f,
                      metadata: JsonValue? = null): Layer {
                val texture = textures.getOrPut(path) {
                    loadTexture(path, metadata).apply {
                        setFilter(Texture.TextureFilter.Linear, Texture.TextureFilter.Linear)
                    }
                }
                return Layer(TextureRegion(texture),
                    (metadata?.getFloat("logicalWidth", texture.width.toFloat()) ?: texture.width.toFloat()) * scale * viewportScale,
                    (metadata?.getFloat("logicalHeight", texture.height.toFloat()) ?: texture.height.toFloat()) * scale * viewportScale,
                    offsetX, offsetY)
            }
            val layers = mutableListOf(layer(entry.getString("texture"), sourceScale))
            val addonScale = addonEntry.getFloat("spriteScale")
            var overlay = addonEntry.get("overlays")?.child
            while (overlay != null) {
                layers += layer(overlay.getString("texture"), addonScale * overlay.getFloat("scale"),
                    overlay.getFloat("x") * addonScale * viewportScale,
                    -overlay.getFloat("y") * addonScale * viewportScale, overlay)
                overlay = overlay.next
            }
            Visual(layers, addonEntry.getFloat("healthLabelYFraction", .67f))
        }
    }

    /** Crop transparent generation margins and upload at most 256 px per edge. */
    private fun loadTexture(path: String, metadata: JsonValue?): Texture {
        val file = Gdx.files.internal("volley-source/bricks/$path")
        val crop = metadata?.get("crop") ?: return Texture(file)
        val source = Pixmap(file)
        try {
            val x = crop.getInt("x")
            val y = crop.getInt("y")
            val width = crop.getInt("width")
            val height = crop.getInt("height")
            require(x >= 0 && y >= 0 && width > 0 && height > 0 &&
                x + width <= source.width && y + height <= source.height) { "Invalid addon crop: $path" }
            val ratio = minOf(1f, 256f / maxOf(width, height))
            val scaled = Pixmap((width * ratio).roundToInt().coerceAtLeast(1),
                (height * ratio).roundToInt().coerceAtLeast(1), Pixmap.Format.RGBA8888)
            try {
                scaled.setBlending(Pixmap.Blending.None)
                scaled.setFilter(Pixmap.Filter.BiLinear)
                scaled.drawPixmap(source, x, y, width, height, 0, 0, scaled.width, scaled.height)
                return Texture(scaled)
            } finally {
                scaled.dispose()
            }
        } finally {
            source.dispose()
        }
    }

    override fun dispose() {
        textures.values.forEach { it.dispose() }
        textures.clear()
        visuals.clear()
    }
}
