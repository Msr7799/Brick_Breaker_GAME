package com.example.brick_breaker_ball

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

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
            assertTrue(result.valid, "Level ${level.id} invalid: ${result.errors}")
        }
    }

    @Test
    fun averageHitPointWorkRisesWorldByWorld() {
        var previous = 0.0
        LevelRepository.worlds.forEach { world ->
            val levels = (LevelRepository.firstLevel(world.id)..LevelRepository.lastLevel(world.id)).map(LevelRepository::level)
            val average = levels.map(::effort).average()
            assertTrue(average >= previous, "World ${world.id} effort $average fell below $previous")
            previous = average
        }
    }

    @Test
    fun averageStartingSpeedRisesWorldByWorld() {
        var previous = 0.0
        LevelRepository.worlds.forEach { world ->
            val levels = (LevelRepository.firstLevel(world.id)..LevelRepository.lastLevel(world.id)).map(LevelRepository::level)
            val average = levels.map { it.ballSpeed.toDouble() }.average()
            assertTrue(average > previous, "World ${world.id} speed $average did not rise above $previous")
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
            assertTrue(type.breakable, "$type must disappear/break according to the annotated rules")
            assertEquals(1, type.maxHealth, "$type should resolve on first collision")
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
            assertTrue(CampaignBrickPalette.palette(world.id).isNotEmpty(), "World ${world.id} palette is empty")
        }
    }
}
