/*
 * ملاحظات صيانة الملف:
 * المسار: app/src/test/java/com/example/brick_breaker_ball/DevelopmentAccessTest.kt
 * المؤلف: mohamed alromaihi
 * الدوال الموجودة: `everyCampaignLevelAndWorldCanBeSelectedWithoutChangingProgress`
 */

package com.example.brick_breaker_ball

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class DevelopmentAccessTest {
    /** ملاحظة صيانة: الدالة `everyCampaignLevelAndWorldCanBeSelectedWithoutChangingProgress` توثّق حالة اختبار أو تهيئة آلية وتحمي السلوك المتوقع من التراجع؛ راجع استدعاءاتها واختباراتها قبل تعديلها. */
    @Test fun everyCampaignLevelAndWorldCanBeSelectedWithoutChangingProgress() {
        val savedUnlockedLevel = 1
        val access = DevelopmentAccess(TestPreferences())

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
}
