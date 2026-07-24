package com.example.brick_breaker_ball

import com.badlogic.gdx.utils.JsonReader
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File
import javax.imageio.ImageIO

class DetailedSpriteManifestTest {
    private val root = File(requireNotNull(System.getProperty("user.dir"))).let { if (File(it, "spritesheet.png").exists()) it else it.parentFile }
    private val imageFile = File(root, "spritesheet.png")
    private val manifestFile = File(root, "docs/sprite-map-detailed.json")

    @Test fun canonicalManifestAndPixelsAreValid() {
        val image = ImageIO.read(imageFile)
        assertEquals(1254, image.width); assertEquals(1254, image.height); assertTrue(image.colorModel.hasAlpha())
        val document = JsonReader().parse(manifestFile.readText())
        val sprites = document.get("sprites")
        assertEquals(70, sprites.size)
        val ids = sprites.map { it.getString("id") }
        assertEquals(70, ids.toSet().size)
        assertEquals(SpriteId.entries.map(SpriteId::value).toSet(), ids.toSet())
        sprites.forEach { sprite ->
            val rect = sprite.get("rect")
            val x = rect.getInt("x"); val y = rect.getInt("y")
            val width = rect.getInt("width"); val height = rect.getInt("height")
            assertTrue(width > 0 && height > 0 && x >= 0 && y >= 0 && x + width <= image.width && y + height <= image.height)
            assertTrue("${sprite.getString("id")} has no visible pixels", (y until y + height).any { py ->
                (x until x + width).any { px -> (image.getRGB(px, py) ushr 24) != 0 }
            })
            listOf("nextState", "previousState").forEach { field ->
                sprite.getString(field, null)?.let { assertTrue("$field points to missing $it", it in ids) }
            }
        }
        val rejected = document.get("rejectedDetections").map { it.getString("id", it.getString("sourceDetection", "")) }.filter(String::isNotBlank)
        assertTrue(rejected.none { it in ids })
    }

    @Test fun requiredFamiliesAndRuntimeCopiesAreComplete() {
        val ids = JsonReader().parse(manifestFile.readText()).get("sprites").map { it.getString("id") }.toSet()
        val abilities = listOf("normal", "fire", "piercing")
        val sizes = listOf("small", "medium", "large")
        assertTrue(abilities.flatMap { ability -> sizes.map { size -> "ball_${ability}_$size" } }.all { it in ids })
        assertEquals((1..8).map { "spark_frame_${it.toString().padStart(2, '0')}" }, ids.filter { it.startsWith("spark_frame_") }.sorted())
        val runtimeImage = File(root, "app/src/main/assets/sprites/spritesheet.png")
        val runtimeManifest = File(root, "app/src/main/assets/sprites/sprite-map-detailed.json")
        assertTrue(runtimeImage.exists()); assertTrue(runtimeManifest.exists())
        assertTrue(imageFile.readBytes().contentEquals(runtimeImage.readBytes()))
        assertTrue(manifestFile.readBytes().contentEquals(runtimeManifest.readBytes()))
        assertFalse(File(root, "app/src/main/assets/sprites/spritesheet-with-notes.png").exists())
        assertFalse(File(root, "app/src/main/assets/atlases/gameplay.atlas").exists())
        assertFalse(File(root, "app/src/main/assets/atlases/gameplay.png").exists())
        assertFalse(File(root, "tools/assets_staging/selected/atlas_inputs/gameplay").exists())
        assertFalse(File(root, "tools/sprite_extraction").exists())
        assertFalse(File(root, "app/src/main/java").walkTopDown().filter(File::isFile).any { "sprite1" in it.readText() || "sprite2" in it.readText() })
    }
}
