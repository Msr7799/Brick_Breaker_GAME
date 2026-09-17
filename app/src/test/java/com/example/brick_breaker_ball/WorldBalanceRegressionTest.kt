package com.example.brick_breaker_ball

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class WorldBalanceRegressionTest {
    private fun effort(level: LevelDefinition): Int = level.brickIds.values.sumOf { id ->
        val type = BrickType.valueOf(id)
        when {
            !type.breakable -> 0
            type == BrickType.BOSS_CORE -> level.bossHealth
            else -> type.maxHealth.coerceAtMost(4)
        }
    }

    @Test
    fun everyGeneratedCampaignLevelValidates() {
        LevelRepository.levels.forEach { level ->
            val result = LevelValidator.validate(level)
            assertTrue("Level ${level.id} invalid: ${result.errors}", result.valid)
        }
    }

    @Test
    fun averageHitPointWorkRisesWorldByWorld() {
        var previous = 0.0
        LevelRepository.worlds.forEach { world ->
            val levels = (LevelRepository.firstLevel(world.id)..LevelRepository.lastLevel(world.id)).map(LevelRepository::level)
            val average = levels.map(::effort).average()
            assertTrue("World ${world.id} effort $average fell below $previous", average >= previous)
            previous = average
        }
    }

    @Test
    fun averageStartingSpeedRisesWorldByWorld() {
        var previous = 0.0
        LevelRepository.worlds.forEach { world ->
            val levels = (LevelRepository.firstLevel(world.id)..LevelRepository.lastLevel(world.id)).map(LevelRepository::level)
            val average = levels.map { it.ballSpeed.toDouble() }.average()
            assertTrue("World ${world.id} speed $average did not rise above $previous", average > previous)
            previous = average
        }
    }

    @Test
    fun annotatedBreakableSpecialBricksUseCanonicalOneHitContract() {
        listOf(
            BrickType.ROUGH_STONE,
            BrickType.LIGHTNING_SPEED_PASS_THROUGH,
            BrickType.TRANSPARENT_SLOW_PASS_THROUGH
        ).forEach { type ->
            assertTrue("$type must disappear/break according to the annotated rules", type.breakable)
            assertEquals("$type should resolve on first collision", 1, type.maxHealth)
        }
    }

    @Test
    fun plusFourAndFifteenBallTalismansAreInstantSpawns() {
        assertEquals(EffectScope.INSTANT, PowerUpCatalog.definitions.getValue(PowerUpType.MULTIBALL_PLUS_4).scope)
        assertEquals(EffectScope.INSTANT, PowerUpCatalog.definitions.getValue(PowerUpType.MULTIBALL_15).scope)
    }

    @Test
    fun everyWorldHasAVisualMaterialPalette() {
        LevelRepository.worlds.forEach { world ->
            assertTrue("World ${world.id} palette is empty", CampaignBrickPalette.palette(world.id).isNotEmpty())
        }
    }
}
