/*
 * ملاحظات صيانة الملف:
 * المسار: app/src/test/java/com/example/brick_breaker_ball/StartMenuAssetsTest.kt
 * المؤلف: mohamed alromaihi
 * الدوال الموجودة: `generatedMenuTexturesHaveExpectedMobileDimensionsAndAlpha`، `pauseAndSettingsArtworkIsRenderedAtExpectedDimensionsWithAlpha`، `assertButtonTexture`
 */

package com.example.brick_breaker_ball

import java.io.File
import javax.imageio.ImageIO
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class StartMenuAssetsTest {
    private val directory = File("src/main/assets/backgrounds/start-screen")

    /** ملاحظة صيانة: الدالة `generatedMenuTexturesHaveExpectedMobileDimensionsAndAlpha` توثّق حالة اختبار أو تهيئة آلية وتحمي السلوك المتوقع من التراجع؛ راجع استدعاءاتها واختباراتها قبل تعديلها. */
    @Test fun generatedMenuTexturesHaveExpectedMobileDimensionsAndAlpha() {
        val buttons = listOf("continue", "start", "world-map", "shop", "level-editor", "paddle&balls")
        buttons.forEach { name ->
            val image = ImageIO.read(File(directory, "$name.png"))
            assertEquals("$name width", 1500, image.width)
            assertEquals("$name height", 350, image.height)
            assertTrue("$name must preserve transparency", image.colorModel.hasAlpha())
        }
        assertButtonTexture(File(directory, "new-game.png"), "new-game", 900, 210)
        listOf("setting", "sound-on", "sound-off", "info", "exit").forEach { name ->
            val image = ImageIO.read(File(directory, "$name.png"))
            assertEquals("$name width", 192, image.width)
            assertEquals("$name height", 192, image.height)
            assertTrue("$name must preserve transparency", image.colorModel.hasAlpha())
        }
        listOf("code-on", "code-off").forEach { name ->
            val image = ImageIO.read(File(directory, "$name.png"))
            assertEquals("$name width", 500, image.width)
            assertEquals("$name height", 500, image.height)
            assertTrue("$name must preserve transparency", image.colorModel.hasAlpha())
        }
        assertButtonTexture(File(directory, "charms-bag.png"), "charms-bag", 1500, 350)
    }

    /** ملاحظة صيانة: الدالة `pauseAndSettingsArtworkIsRenderedAtExpectedDimensionsWithAlpha` توثّق حالة اختبار أو تهيئة آلية وتحمي السلوك المتوقع من التراجع؛ راجع استدعاءاتها واختباراتها قبل تعديلها. */
    @Test fun pauseAndSettingsArtworkIsRenderedAtExpectedDimensionsWithAlpha() {
        val pauseDirectory = File(directory, "pused")
        listOf("customise", "main-menu", "restart-level", "resume", "setting", "shop").forEach { name ->
            assertButtonTexture(File(pauseDirectory, "$name.png"), name, 1500, 350)
        }
        assertButtonTexture(File(pauseDirectory, "charms-bag.png"), "pause charms-bag", 1500, 350)

        val settingsDirectory = File(directory, "setting")
        listOf("game-developer", "github").forEach { name ->
            assertButtonTexture(File(settingsDirectory, "$name.png"), name, 900, 210)
        }
        assertButtonTexture(File(settingsDirectory, "save&back.png"), "save&back", 1500, 350)
        listOf("color-blind-palette", "contrest-ball", "haptics", "motion", "sound").forEach { name ->
            val image = ImageIO.read(File(settingsDirectory, "$name.png"))
            assertEquals("$name width", 1500, image.width)
            assertEquals("$name height", 350, image.height)
            assertTrue("$name must preserve transparency", image.colorModel.hasAlpha())
        }
        listOf("check-on", "check-off").forEach { name ->
            val image = ImageIO.read(File(settingsDirectory, "$name.png"))
            assertEquals("$name width", 192, image.width)
            assertEquals("$name height", 192, image.height)
            assertTrue("$name must preserve transparency", image.colorModel.hasAlpha())
        }
        assertFalse(
            "Runtime backgrounds must not retain unused SVG source files",
            File("src/main/assets/backgrounds").walkTopDown().any { it.isFile && it.extension.equals("svg", true) }
        )
    }

    /** ملاحظة صيانة: الدالة `assertButtonTexture` توثّق حالة اختبار أو تهيئة آلية وتحمي السلوك المتوقع من التراجع؛ راجع استدعاءاتها واختباراتها قبل تعديلها. */
    private fun assertButtonTexture(file: File, name: String, expectedWidth: Int, expectedHeight: Int) {
        val image = ImageIO.read(file)
        assertEquals("$name width", expectedWidth, image.width)
        assertEquals("$name height", expectedHeight, image.height)
        assertTrue("$name must preserve transparency", image.colorModel.hasAlpha())
    }
}
