package com.example.brick_breaker_ball

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class PaddleAbilityTest {
    @Test
    fun everyPaddleStyleHasAStableAbility() {
        val ids = PaddleAbilityCatalog.knownNormalPaddleIds()
        assertEquals(36, ids.size)
        assertEquals(36, ids.distinct().size)
        ids.forEach { id ->
            val profile = PaddleAbilityCatalog.profileForNormalPaddle(id)
            assertTrue(profile.title.isNotBlank())
            assertTrue(profile.description.isNotBlank())
            assertTrue(profile.tier in 1..3)
        }
    }

    @Test
    fun firstFourStylesMatchDesignedProgression() {
        val ids = PaddleAbilityCatalog.knownNormalPaddleIds()
        assertEquals(PaddleAbilityKind.PRECISION_CORE, PaddleAbilityCatalog.profileForNormalPaddle(ids[0]).kind)
        assertEquals(PaddleAbilityKind.HYPER_GLIDE, PaddleAbilityCatalog.profileForNormalPaddle(ids[1]).kind)
        assertEquals(PaddleAbilityKind.IMPACT_BOOST, PaddleAbilityCatalog.profileForNormalPaddle(ids[2]).kind)
        assertEquals(PaddleAbilityKind.INFERNO_RHYTHM, PaddleAbilityCatalog.profileForNormalPaddle(ids[3]).kind)
        assertEquals(4, PaddleAbilityCatalog.profileForNormalPaddle(ids[3]).fireEveryHits)
    }

    @Test
    fun hyperGlideMovesPaddleFasterThanStarter() {
        val ids = PaddleAbilityCatalog.knownNormalPaddleIds()
        val precision = GameSession(paddleAbility = PaddleAbilityCatalog.profileForNormalPaddle(ids[0]))
        val glide = GameSession(paddleAbility = PaddleAbilityCatalog.profileForNormalPaddle(ids[1]))
        precision.movePaddle(800f, .016f)
        glide.movePaddle(800f, .016f)
        assertTrue(glide.paddle.x > precision.paddle.x)
    }

    @Test
    fun impactBoostTapArmsShortPerfectWindow() {
        val ids = PaddleAbilityCatalog.knownNormalPaddleIds()
        val session = GameSession(paddleAbility = PaddleAbilityCatalog.profileForNormalPaddle(ids[2]))
        session.phase = GamePhase.PLAYING
        session.action()
        assertTrue(session.abilityTapWindowRemaining > 0f)
        session.update(1f)
        assertEquals(0f, session.abilityTapWindowRemaining, .0001f)
    }

    @Test
    fun pausedSessionPreservesAbilityStateAndFireCharge() {
        val prefs = TestPreferences()
        val store = PausedSessionStore(prefs)
        val level = LevelRepository.level(1)
        val profile = PaddleAbilityCatalog.profile(PaddleAbilityKind.INFERNO_RHYTHM, 2)
        val session = GameSession(level = level, paddleAbility = profile)
        session.paddleAbilityHitCount = 3
        session.ball.abilityFireCharge = true
        store.save(level, session)

        val restored = requireNotNull(store.restore()).second
        assertEquals(PaddleAbilityKind.INFERNO_RHYTHM, restored.paddleAbility.kind)
        assertEquals(2, restored.paddleAbility.tier)
        assertEquals(3, restored.paddleAbilityHitCount)
        assertTrue(restored.ball.abilityFireCharge)
    }
}
