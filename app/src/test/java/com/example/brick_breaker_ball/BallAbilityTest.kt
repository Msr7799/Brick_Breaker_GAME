package com.example.brick_breaker_ball

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class BallAbilityTest {
    @Test
    fun allOneHundredFiveNumberedBallsHaveAValidPowerProfile() {
        (1..105).forEach { number ->
            val sprite = "${number.toString().padStart(2, '0')}_test_ball"
            val profile = BallAbilityCatalog.profileForSprite(sprite)
            assertTrue(profile.title.isNotBlank())
            assertTrue(profile.description.isNotBlank())
            assertTrue(profile.tier in 1..5)
        }
    }

    @Test
    fun fiveProgressionTiersCoverTwentyOneBallsEach() {
        val counts = (1..105)
            .map { BallAbilityCatalog.profileForSprite("${it.toString().padStart(2, '0')}_ball").tier }
            .groupingBy { it }
            .eachCount()
        assertEquals(mapOf(1 to 21, 2 to 21, 3 to 21, 4 to 21, 5 to 21), counts)
    }

    @Test
    fun sevenAbilityArchetypesRepeatInStableOrder() {
        val kinds = (1..7).map { BallAbilityCatalog.profileForSprite("${it.toString().padStart(2, '0')}_ball").kind }
        assertEquals(BallAbilityKind.entries.toList(), kinds)
        assertEquals(
            BallAbilityKind.IMPACT_CORE,
            BallAbilityCatalog.profileForSprite("08_second_cycle").kind,
        )
    }

    @Test
    fun highNumberBallSpritesUseThreeDigitCatalogNumbers() {
        assertEquals(105, BallAbilityCatalog.ballNumber("105_gray_segmented"))
        assertEquals(5, BallAbilityCatalog.profileForSprite("105_gray_segmented").tier)
    }
}
