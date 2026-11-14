package com.example.brick_breaker_ball.assets

import com.badlogic.gdx.Gdx
import com.badlogic.gdx.files.FileHandle
import com.badlogic.gdx.utils.JsonReader
import com.example.brick_breaker_ball.gameplay.VolleyPieceShape
import java.io.File

/** Visual-only world colors. Original source kinds still own damage, spells and rewards. */
internal object VolleyBrickPalette {
    private const val ASSET = "volley-source/world-brick-palettes.json"
    private val root by lazy {
        val file = if (Gdx.files != null) Gdx.files.internal(ASSET) else {
            FileHandle(listOf(File("app/src/main/assets/$ASSET"), File("src/main/assets/$ASSET"))
                .first { it.isFile })
        }
        JsonReader().parse(file)
    }

    fun visualScene(original: String, world: Int, kind: String?, shape: VolleyPieceShape): String {
        val role = when (kind) {
            "GRASS", "SAND", "ICE", "BLACK" -> "ordinary"
            "BLOCKER", "SLIDER" -> "blocker"
            else -> return original
        }
        val theme = root.get("worlds").getString(world.toString(), "black")
        val shapeName = when (shape) {
            VolleyPieceShape.RECTANGLE -> "SQ"
            VolleyPieceShape.TRIANGLE_NW -> "NW"
            VolleyPieceShape.TRIANGLE_NE -> "NE"
            VolleyPieceShape.TRIANGLE_SW -> "SW"
            VolleyPieceShape.TRIANGLE_SE -> "SE"
        }
        return root.get("styles").get(theme).get(role).getString(shapeName)
    }
}
