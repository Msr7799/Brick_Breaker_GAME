package com.example.brick_breaker_ball

import java.io.File
import javax.imageio.ImageIO
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class StartMenuAssetsTest {
    private val directory = File("src/main/assets/backgrounds/start-screen")

    @Test fun generatedMenuTexturesHaveExpectedMobileDimensionsAndAlpha() {
        val buttons = listOf("continue", "start", "world-map", "shop", "level-editor", "paddle&balls", "new-game")
        buttons.forEach { name ->
            val image = ImageIO.read(File(directory, "$name.png"))
            assertEquals("$name width", 900, image.width)
            assertEquals("$name height", 210, image.height)
            assertTrue("$name must preserve transparency", image.colorModel.hasAlpha())
        }
        listOf("setting", "sound-on", "sound-off", "info", "exit").forEach { name ->
            val image = ImageIO.read(File(directory, "$name.png"))
            assertEquals("$name width", 192, image.width)
            assertEquals("$name height", 192, image.height)
            assertTrue("$name must preserve transparency", image.colorModel.hasAlpha())
        }
    }

    @Test fun pauseAndSettingsArtworkIsRenderedAtExpectedDimensionsWithAlpha() {
        val pauseDirectory = File(directory, "pused")
        listOf("customise", "main-menu", "restart-level", "resume", "setting").forEach { name ->
            assertMobileButtonTexture(File(pauseDirectory, "$name.png"), name)
        }

        val settingsDirectory = File(directory, "setting")
        listOf(
            "color-blind-palette", "contrest-ball", "game-developer", "github", "haptics",
            "motion", "save&back", "sound",
        ).forEach { name -> assertMobileButtonTexture(File(settingsDirectory, "$name.png"), name) }
        listOf("check-on", "check-off").forEach { name ->
            val image = ImageIO.read(File(settingsDirectory, "$name.png"))
            assertEquals("$name width", 192, image.width)
            assertEquals("$name height", 192, image.height)
            assertTrue("$name must preserve transparency", image.colorModel.hasAlpha())
        }
    }

    private fun assertMobileButtonTexture(file: File, name: String) {
        val image = ImageIO.read(file)
        assertEquals("$name width", 900, image.width)
        assertEquals("$name height", 210, image.height)
        assertTrue("$name must preserve transparency", image.colorModel.hasAlpha())
    }
}
