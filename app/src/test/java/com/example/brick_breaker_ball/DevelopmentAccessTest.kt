package com.example.brick_breaker_ball

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class DevelopmentAccessTest {
    @Test
    fun debugModeBypassesSelectionLocksWithoutChangingProgress() {
        val savedUnlockedLevel = 1
        val access = DevelopmentAccess(TestPreferences(), available = true)

        assertFalse(access.enabled)
        assertFalse(access.canSelectLevel(2, savedUnlockedLevel))
        assertFalse(access.canUseCosmetic(owned = false))
        assertTrue(access.toggle())

        (1..LevelRepository.TOTAL_LEVELS).forEach { level ->
            assertTrue("Level $level should be selectable", access.canSelectLevel(level, savedUnlockedLevel))
        }
        LevelRepository.worlds.forEach { world ->
            assertTrue("World ${world.id} should be selectable", access.canSelectWorld(world.id, savedUnlockedLevel))
        }
        assertTrue(access.canUseCosmetic(owned = false))
        assertTrue(access.canMakeTestPurchase(productAvailable = false, busy = false))
        assertFalse(access.canMakeTestPurchase(productAvailable = true, busy = true))
        assertFalse(access.toggle())
        assertFalse(access.canSelectLevel(2, savedUnlockedLevel))
    }

    @Test
    fun releaseModeIgnoresPreviouslyEnabledPreference() {
        val prefs = TestPreferences().putBoolean("enabled", true)
        val access = DevelopmentAccess(prefs, available = false)

        assertFalse(access.enabled)
        assertFalse(access.toggle())
        assertFalse(access.canSelectLevel(2, 1))
        assertFalse(access.canSelectWorld(2, 1))
        assertFalse(access.canUseCosmetic(owned = false))
        assertFalse(access.canMakeTestPurchase(productAvailable = false, busy = false))
        assertTrue(access.canMakeTestPurchase(productAvailable = true, busy = false))
    }
}
