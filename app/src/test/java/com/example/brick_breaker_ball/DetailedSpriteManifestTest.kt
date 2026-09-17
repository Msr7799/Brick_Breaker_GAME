/*
 * ملاحظات صيانة الملف:
 * المسار: app/src/test/java/com/example/brick_breaker_ball/DetailedSpriteManifestTest.kt
 * المؤلف: mohamed alromaihi
 * الدوال الموجودة: `canonicalManifestAndPixelsAreValid`، `requiredFamiliesAndRuntimeCopiesAreComplete`
 */

package com.example.brick_breaker_ball

import com.badlogic.gdx.utils.JsonReader
import java.io.File
import javax.imageio.ImageIO
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class DetailedSpriteManifestTest {
    private val root = generateSequence(File(requireNotNull(System.getProperty("user.dir")))) { it.parentFile }
        .first { File(it, "app/src/main/assets/sprites").isDirectory }
    private val imageFile = File(root, "app/src/main/assets/sprites/spritesheet.png")
    private val manifestFile = File(root, "app/src/main/assets/sprites/sprite-map-detailed.json")

    /** ملاحظة صيانة: الدالة `canonicalManifestAndPixelsAreValid` توثّق حالة اختبار أو تهيئة آلية وتحمي السلوك المتوقع من التراجع؛ راجع استدعاءاتها واختباراتها قبل تعديلها. */
    @Test fun canonicalManifestAndPixelsAreValid() {
        val image = ImageIO.read(imageFile)
        assertEquals(1254, image.width)
        assertEquals(1254, image.height)
        assertTrue(image.colorModel.hasAlpha())
        val document = JsonReader().parse(manifestFile.readText())
        val sprites = document.get("sprites")
        assertEquals(70, sprites.size)
        val ids = sprites.map { it.getString("id") }
        assertEquals(70, ids.toSet().size)
        assertEquals(SpriteId.entries.map(SpriteId::value).toSet(), ids.toSet())
        sprites.forEach { sprite ->
            val rect = sprite.get("rect")
            val x = rect.getInt("x")
            val y = rect.getInt("y")
            val width = rect.getInt("width")
            val height = rect.getInt("height")
            assertTrue(width > 0 && height > 0 && x >= 0 && y >= 0 && x + width <= image.width && y + height <= image.height)
            assertTrue(
                "${sprite.getString("id")} has no visible pixels",
                (y until y + height).any { py ->
                    (x until x + width).any { px -> (image.getRGB(px, py) ushr 24) != 0 }
                }
            )
            listOf("nextState", "previousState").forEach { field ->
                sprite.getString(field, null)?.let { assertTrue("$field points to missing $it", it in ids) }
            }
        }
        val rejected = document.get("rejectedDetections").map {
            it.getString("id", it.getString("sourceDetection", ""))
        }.filter(String::isNotBlank)
        assertTrue(rejected.none { it in ids })
    }

    /** ملاحظة صيانة: الدالة `requiredFamiliesAndRuntimeCopiesAreComplete` توثّق حالة اختبار أو تهيئة آلية وتحمي السلوك المتوقع من التراجع؛ راجع استدعاءاتها واختباراتها قبل تعديلها. */
    @Test fun requiredFamiliesAndRuntimeCopiesAreComplete() {
        val ids = JsonReader().parse(manifestFile.readText()).get("sprites").map { it.getString("id") }.toSet()
        val abilities = listOf("normal", "fire", "piercing")
        val sizes = listOf("small", "medium", "large")
        assertTrue(abilities.flatMap { ability -> sizes.map { size -> "ball_${ability}_$size" } }.all { it in ids })
        assertEquals((1..8).map { "spark_frame_${it.toString().padStart(2, '0')}" }, ids.filter { it.startsWith("spark_frame_") }.sorted())
        assertTrue(imageFile.exists())
        assertTrue(manifestFile.exists())
        assertFalse(File(root, "app/src/main/assets/sprites/spritesheet-with-notes.png").exists())
        assertFalse(File(root, "app/src/main/assets/atlases/gameplay.atlas").exists())
        assertFalse(File(root, "app/src/main/assets/atlases/gameplay.png").exists())
        assertFalse(File(root, "tools/assets_staging/selected/atlas_inputs/gameplay").exists())
        assertFalse(File(root, "tools/sprite_extraction").exists())
        assertFalse(
            File(root, "app/src/main/java").walkTopDown().filter(File::isFile).any {
                "sprite1" in it.readText() ||
                    "sprite2" in it.readText()
            }
        )
    }

    @Test fun annotatedTwoHitBrickPairsUseIntactThenDamagedState() {
        val document = JsonReader().parse(manifestFile.readText())
        val byId = document.get("sprites").associateBy { it.getString("id") }
        val bases = listOf(
            "brick_electric_white", "brick_crystal_blue", "brick_crystal_red", "brick_crystal_purple",
            "brick_stone_gray", "brick_crystal_MAROON", "brick_armored_dark", "brick_crystal_pink",
            "brick_crystal_orange", "brick_crystal_green"
        )
        bases.forEach { base ->
            val intact = requireNotNull(byId["${base}_intact"])
            val damaged = requireNotNull(byId["${base}_damaged"])
            assertEquals("${base}_damaged", intact.getString("nextState"))
            assertEquals("${base}_intact", damaged.getString("previousState"))
        }
        listOf(
            BrickType.ELECTRIC_WHITE, BrickType.CRYSTAL_BLUE, BrickType.CRYSTAL_RED, BrickType.CRYSTAL_PURPLE,
            BrickType.STONE_GRAY, BrickType.CRYSTAL_MAROON, BrickType.ARMORED_DARK, BrickType.CRYSTAL_PINK,
            BrickType.CRYSTAL_ORANGE, BrickType.CRYSTAL_GREEN
        ).forEach { type ->
            assertEquals(2, type.maxHealth)
            assertTrue(type.breakable)
        }
    }
}
