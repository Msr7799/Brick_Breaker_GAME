package com.example.brick_breaker_ball

import org.junit.Assert.assertTrue
import org.junit.Test

class DevelopmentAccessTest {
    @Test fun everyCampaignLevelAndWorldCanBeSelectedWithoutChangingProgress() {
        val savedUnlockedLevel = 1

        (1..LevelRepository.TOTAL_LEVELS).forEach { level ->
            assertTrue("Level $level should be selectable", DevelopmentAccess.canSelectLevel(level, savedUnlockedLevel))
        }
        LevelRepository.worlds.forEach { world ->
            assertTrue("World ${world.id} should be selectable", DevelopmentAccess.canSelectWorld(world.id, savedUnlockedLevel))
        }
    }
}
