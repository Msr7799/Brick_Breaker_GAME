package com.example.brick_breaker_ball

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class WorldVideoBackgroundsTest {
    @Test fun suppliedVideosMapToTheirWorldsAndMissingElevenFallsBack() {
        assertEquals("splash.mp4", WorldVideoBackgrounds.SPLASH_ASSET)
        assertEquals("1.mp4", WorldVideoBackgrounds.assetForWorld(1))
        assertEquals("10.mp4", WorldVideoBackgrounds.assetForWorld(10))
        assertNull(WorldVideoBackgrounds.assetForWorld(11))
        assertEquals("12.mp4", WorldVideoBackgrounds.assetForWorld(12))
        assertEquals("13.mp4", WorldVideoBackgrounds.assetForWorld(13))
    }
}
