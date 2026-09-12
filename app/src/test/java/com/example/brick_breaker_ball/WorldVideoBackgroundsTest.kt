/*
 * ملاحظات صيانة الملف:
 * المسار: app/src/test/java/com/example/brick_breaker_ball/WorldVideoBackgroundsTest.kt
 * المؤلف: mohamed alromaihi
 * الدوال الموجودة: `suppliedVideosMapToEveryCampaignWorld`
 */

package com.example.brick_breaker_ball

import org.junit.Assert.assertEquals
import org.junit.Test

class WorldVideoBackgroundsTest {
    /** ملاحظة صيانة: الدالة `suppliedVideosMapToEveryCampaignWorld` توثّق حالة اختبار أو تهيئة آلية وتحمي السلوك المتوقع من التراجع؛ راجع استدعاءاتها واختباراتها قبل تعديلها. */
    @Test fun suppliedVideosMapToEveryCampaignWorld() {
        assertEquals("splash.mp4", WorldVideoBackgrounds.SPLASH_ASSET)
        (1..13).forEach { world ->
            assertEquals("$world.mp4", WorldVideoBackgrounds.assetForWorld(world))
        }
    }
}
