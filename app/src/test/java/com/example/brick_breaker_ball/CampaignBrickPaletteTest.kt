/*
 * ملاحظات صيانة الملف:
 * المسار: app/src/test/java/com/example/brick_breaker_ball/CampaignBrickPaletteTest.kt
 * المؤلف: mohamed alromaihi
 * الدوال الموجودة: `everyWorldHasAStableVariedMaterialPalette`، `frozenAndVolcanoWorldsExcludeClashingMaterialColors`، `functionalBricksKeepTheirCanonicalVisualIdentity`
 */

package com.example.brick_breaker_ball

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class CampaignBrickPaletteTest {
    /** ملاحظة صيانة: الدالة `everyWorldHasAStableVariedMaterialPalette` توثّق حالة اختبار أو تهيئة آلية وتحمي السلوك المتوقع من التراجع؛ راجع استدعاءاتها واختباراتها قبل تعديلها. */
    @Test fun everyWorldHasAStableVariedMaterialPalette() {
        (1..13).forEach { world ->
            val palette = CampaignBrickPalette.palette(world)
            assertTrue("World $world needs multiple coordinated materials", palette.distinct().size >= 3)
            assertEquals(
                CampaignBrickPalette.spriteFor(BrickType.NORMAL_ONE_HIT, world, 17),
                CampaignBrickPalette.spriteFor(BrickType.NORMAL_ONE_HIT, world, 17)
            )
        }
    }

    /** ملاحظة صيانة: الدالة `frozenAndVolcanoWorldsExcludeClashingMaterialColors` توثّق حالة اختبار أو تهيئة آلية وتحمي السلوك المتوقع من التراجع؛ راجع استدعاءاتها واختباراتها قبل تعديلها. */
    @Test fun frozenAndVolcanoWorldsExcludeClashingMaterialColors() {
        val frozen = CampaignBrickPalette.palette(8).toSet()
        assertTrue(SpriteId.BRICK_CRYSTAL_RED_INTACT !in frozen)
        assertTrue(SpriteId.BRICK_CRYSTAL_ORANGE_INTACT !in frozen)
        assertTrue(SpriteId.BRICK_CRYSTAL_GREEN_INTACT !in frozen)

        val volcano = CampaignBrickPalette.palette(13).toSet()
        assertEquals(
            setOf(
                SpriteId.BRICK_CRYSTAL_RED_INTACT,
                SpriteId.BRICK_CRYSTAL_ORANGE_INTACT,
                SpriteId.BRICK_ARMORED_DARK_INTACT,
                SpriteId.BRICK_STONE_GRAY_INTACT
            ),
            volcano
        )
    }

    /** ملاحظة صيانة: الدالة `functionalBricksKeepTheirCanonicalVisualIdentity` توثّق حالة اختبار أو تهيئة آلية وتحمي السلوك المتوقع من التراجع؛ راجع استدعاءاتها واختباراتها قبل تعديلها. */
    @Test fun functionalBricksKeepTheirCanonicalVisualIdentity() {
        listOf(
            BrickType.EXPLOSIVE,
            BrickType.POWERUP_CARRIER,
            BrickType.MOVING_HORIZONTAL,
            BrickType.MOVING_VERTICAL,
            BrickType.REGENERATING,
            BrickType.GHOST,
            BrickType.SWITCH,
            BrickType.LOCKED,
            BrickType.KEY_BRICK,
            BrickType.CHAIN_BRICK,
            BrickType.BOSS_CORE,
            BrickType.ROUGH_STONE,
            BrickType.LIGHTNING_SPEED_PASS_THROUGH,
            BrickType.TRANSPARENT_SLOW_PASS_THROUGH
        ).forEach { type -> assertNull(type.name, CampaignBrickPalette.spriteFor(type, 13, 7)) }
    }
}
