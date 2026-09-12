/*
 * ملاحظات صيانة الملف:
 * المسار: app/src/test/java/com/example/brick_breaker_ball/CampaignProgressionTest.kt
 * المؤلف: mohamed alromaihi
 * الدوال الموجودة: `ninetyLevelsAreNineByEightUniqueAndDistributedAcrossThirteenWorlds`، `worldsIntroduceAllPermanentBrickTypesAndDifficultyRisesAcrossWorlds`، `carrierAndDropRatesGiveEveryWorldRandomPowerUpOpportunities`، `everyCampaignLevelUsesTheIncreasedBallSpeedCurve`
 */

package com.example.brick_breaker_ball

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class CampaignProgressionTest {
    /** ملاحظة صيانة: الدالة `ninetyLevelsAreNineByEightUniqueAndDistributedAcrossThirteenWorlds` توثّق حالة اختبار أو تهيئة آلية وتحمي السلوك المتوقع من التراجع؛ راجع استدعاءاتها واختباراتها قبل تعديلها. */
    @Test fun ninetyLevelsAreNineByEightUniqueAndDistributedAcrossThirteenWorlds() {
        assertEquals(90, LevelRepository.TOTAL_LEVELS)
        assertEquals((1..13).toList(), LevelRepository.levels.map { it.world }.distinct())
        (1..12).forEach { world -> assertEquals(7, LevelRepository.levels.count { it.world == world }) }
        assertEquals(6, LevelRepository.levels.count { it.world == 13 })
        assertTrue(
            LevelRepository.levels.all { level ->
                level.rows == 8 && level.columns == 9 && level.layout.size == 8 &&
                    level.layout.all { row -> row.length == 9 }
            }
        )
        (1..13).forEach { world ->
            val layouts = LevelRepository.levels.filter {
                it.world == world
            }.map { it.brickIds.entries.sortedBy { entry -> entry.key }.joinToString() }
            assertEquals("world $world has duplicate boards", LevelRepository.worldLevelCount(world), layouts.toSet().size)
        }
    }

    /** ملاحظة صيانة: الدالة `worldsIntroduceAllPermanentBrickTypesAndDifficultyRisesAcrossWorlds` توثّق حالة اختبار أو تهيئة آلية وتحمي السلوك المتوقع من التراجع؛ راجع استدعاءاتها واختباراتها قبل تعديلها. */
    @Test fun worldsIntroduceAllPermanentBrickTypesAndDifficultyRisesAcrossWorlds() {
        val used = LevelRepository.levels.flatMap { it.brickIds.values }.map(BrickType::valueOf).toSet()
        assertEquals(BrickType.entries.toSet() - BrickType.SPIKED_HAZARD, used)
        (1 until 13).forEach { world ->
            val previous = LevelRepository.levels.filter { it.world == world }.maxOf { it.ballSpeed }
            val next = LevelRepository.levels.filter { it.world == world + 1 }.minOf { it.ballSpeed }
            assertTrue("world ${world + 1} must start faster", next > previous)
        }
        assertTrue(LevelRepository.levels.filter { it.world == 1 }.all { BrickType.ARMORED_THREE_HIT.name !in it.brickIds.values })
        assertTrue(
            LevelRepository.levels.filter { it.world == 6 }.all {
                BrickType.KEY_BRICK.name in it.brickIds.values &&
                    BrickType.LOCKED.name in it.brickIds.values
            }
        )
        assertEquals(13, LevelRepository.levels.count { "BOSS" in it.modifiers && BrickType.BOSS_CORE.name in it.brickIds.values })
    }

    /** ملاحظة صيانة: الدالة `carrierAndDropRatesGiveEveryWorldRandomPowerUpOpportunities` توثّق حالة اختبار أو تهيئة آلية وتحمي السلوك المتوقع من التراجع؛ راجع استدعاءاتها واختباراتها قبل تعديلها. */
    @Test fun carrierAndDropRatesGiveEveryWorldRandomPowerUpOpportunities() {
        (1..13).forEach { world ->
            val levels = LevelRepository.levels.filter { it.world == world }
            assertTrue(
                levels.any {
                    BrickType.POWERUP_CARRIER.name in it.brickIds.values ||
                        BrickType.RANDOM_INVENTORY_POWERUP.name in it.brickIds.values
                }
            )
            assertTrue(levels.all { it.dropRate in 0f..1f })
        }
        assertTrue(LevelRepository.levels.last().dropRate > LevelRepository.levels.first().dropRate)
    }

    /** ملاحظة صيانة: الدالة `everyCampaignLevelUsesTheIncreasedBallSpeedCurve` توثّق حالة اختبار أو تهيئة آلية وتحمي السلوك المتوقع من التراجع؛ راجع استدعاءاتها واختباراتها قبل تعديلها. */
    @Test fun everyCampaignLevelUsesTheIncreasedBallSpeedCurve() {
        LevelRepository.levels.forEach { level ->
            val stage = level.id - LevelRepository.firstLevel(level.world) + 1
            val previousSpeed = 540f + (level.world - 1) * 30f + (stage - 1) * 4f
            assertTrue("level ${level.id} should be faster", level.ballSpeed > previousSpeed)
        }
    }
}
