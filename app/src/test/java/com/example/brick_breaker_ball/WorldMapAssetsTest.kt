/*
 * ملاحظات صيانة الملف:
 * المسار: app/src/test/java/com/example/brick_breaker_ball/WorldMapAssetsTest.kt
 * المؤلف: mohamed alromaihi
 * الدوال الموجودة: `allThirteenWorldMapImagesExistWithUniformDimensions`، `worldNamesMatchTheThirteenRequestedThemes`
 */

package com.example.brick_breaker_ball

import java.io.File
import javax.imageio.ImageIO
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class WorldMapAssetsTest {
    private val assetDirectory = File("src/main/assets/backgrounds/worlds-map-images")

    /** ملاحظة صيانة: الدالة `allThirteenWorldMapImagesExistWithUniformDimensions` توثّق حالة اختبار أو تهيئة آلية وتحمي السلوك المتوقع من التراجع؛ راجع استدعاءاتها واختباراتها قبل تعديلها. */
    @Test fun allThirteenWorldMapImagesExistWithUniformDimensions() {
        val images = (1..13).map { world -> File(assetDirectory, "$world.png") }
        assertTrue(images.all(File::isFile))
        images.forEach { file ->
            val image = ImageIO.read(file)
            assertEquals("${file.name} width", 400, image.width)
            assertEquals("${file.name} height", 250, image.height)
        }
    }

    /** ملاحظة صيانة: الدالة `worldNamesMatchTheThirteenRequestedThemes` توثّق حالة اختبار أو تهيئة آلية وتحمي السلوك المتوقع من التراجع؛ راجع استدعاءاتها واختباراتها قبل تعديلها. */
    @Test fun worldNamesMatchTheThirteenRequestedThemes() {
        assertEquals(
            listOf(
                "CRYSTAL LEVIATHAN", "CELESTIAL GROVE", "VOID WHALE", "AZURE PYRAMID",
                "SKY KINGDOM", "STORM SPIRE", "CRYSTAL COAST", "FROZEN CITADEL",
                "DARK MATTER", "CONJUNCTION", "CHAOS", "THE END OF TIME", "DARK VOLCANO"
            ),
            LevelRepository.worlds.map { it.name }
        )
    }
}
