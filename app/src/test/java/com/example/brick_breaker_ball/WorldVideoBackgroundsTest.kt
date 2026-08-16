package com.example.brick_breaker_ball

import org.junit.Assert.assertEquals
import org.junit.Test

class WorldVideoBackgroundsTest {
    @Test fun suppliedVideosMapToEveryCampaignWorld() {
        assertEquals("splash.mp4", WorldVideoBackgrounds.SPLASH_ASSET)
        (1..13).forEach { world ->
            assertEquals("$world.mp4", WorldVideoBackgrounds.assetForWorld(world))
        }
    }
}
