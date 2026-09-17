package com.example.brick_breaker_ball

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class ProductionEnhancementsTest {
    @Test
    fun everyWorldExposesTheCompleteClassicTalismanPool() {
        LevelRepository.worlds.forEach { world ->
            val pool = PowerUpCatalog.worldDropTypes(world.id)
            assertEquals(20, pool.size)
            assertEquals(PowerUpCatalog.classicTypes, pool.toSet())
        }
    }

    @Test
    fun everyCampaignStageHasAGuaranteedTalismanCarrier() {
        LevelRepository.levels.forEach { level ->
            assertTrue(
                "Level ${level.id} in world ${level.world} must expose a guaranteed talisman carrier",
                level.brickIds.values.any { it == BrickType.POWERUP_CARRIER.name },
            )
        }
    }

    @Test
    fun forcedCarrierStillDropsWhenThreeTalismansAreAlreadyFalling() {
        val director = PowerUpDropDirector(seed = 99L, world = 1)
        assertNotNull(director.choose(lives = 3, activeFalling = 3, forced = true))
    }

    @Test
    fun rewardedContinueRestoresThreeLivesAndStopsAfterThreeUses() {
        val session = GameSession()

        repeat(GameSession.MAX_REWARDED_REVIVES) { index ->
            session.balls.clear()
            session.lives = 0
            session.phase = GamePhase.GAME_OVER

            assertTrue(session.reviveFromRewardedAd())
            assertEquals(GameSession.REWARDED_REVIVE_LIVES, session.lives)
            assertEquals(index + 1, session.rewardedRevivesUsed)
            assertEquals(GamePhase.SERVING, session.phase)
            assertTrue(session.balls.isNotEmpty())
        }

        session.balls.clear()
        session.lives = 0
        session.phase = GamePhase.GAME_OVER
        assertFalse(session.reviveFromRewardedAd())
        assertEquals(0, session.rewardedRevivesRemaining)
    }

    @Test
    fun gameOverPauseRestoreKeepsRewardedContinueUsage() {
        val prefs = TestPreferences()
        val store = PausedSessionStore(prefs)
        val session = GameSession()

        repeat(2) {
            session.balls.clear()
            session.lives = 0
            session.phase = GamePhase.GAME_OVER
            assertTrue(session.reviveFromRewardedAd())
        }

        session.balls.clear()
        session.lives = 0
        session.phase = GamePhase.GAME_OVER
        store.save(session.level, session)

        val restored = requireNotNull(store.restore()).second
        assertEquals(GamePhase.GAME_OVER, restored.phase)
        assertEquals(0, restored.lives)
        assertEquals(2, restored.rewardedRevivesUsed)
        assertEquals(1, restored.rewardedRevivesRemaining)
        assertTrue(restored.balls.isEmpty())
    }

    @Test
    fun expandTalismanNowCreatesALargerPaddleStep() {
        assertTrue(GameSession.PADDLE_EXPAND_STEP >= 70f)
        val session = GameSession()
        assertTrue(session.activatePowerUp(PowerUpType.EXPAND_PADDLE))
        assertEquals(
            GameSession.BASE_PADDLE_WIDTH + GameSession.PADDLE_EXPAND_STEP,
            session.paddle.targetWidth,
            .01f,
        )
    }

    @Test
    fun magneticTalismanUsesTheStrengthenedField() {
        assertTrue(GameplayTuning.MAGNET_RANGE >= 640f)
        assertTrue(GameplayTuning.MAGNET_HORIZONTAL_RANGE >= 700f)
        assertTrue(GameplayTuning.MAGNET_STEER_RATE >= 18f)
        assertTrue(GameplayTuning.MAGNET_MAX_BLEND >= .8f)
        assertTrue(GameplayTuning.POWERUP_MAGNET_PULL >= 8f)
        assertTrue(GameplayTuning.POWERUP_MAGNET_MAX_HORIZONTAL_SPEED >= 700f)
    }
}
