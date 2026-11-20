package com.example.brick_breaker_ball.gameplay

import com.badlogic.gdx.math.Vector2
import com.example.brick_breaker_ball.campaign.VolleyStageRepository
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class VolleyStage90BossAccessTest {
    @Test fun `world core stays protected until all other objectives are cleared`() {
        val stage = VolleyStageRepository.forLevel(90)
        val session = VolleySession(stage, stage.world, seed = 90L)
        assertEquals(3, session.bricks.count { it.volleyShieldLock && it.health > 0 })
        assertFalse(session.revealDelayedFinalCoreIfReady())
    }

    @Test fun `clear arena exposes the original core without losing save identity`() {
        val stage = VolleyStageRepository.forLevel(90)
        val session = VolleySession(stage, stage.world, seed = 90L)
        val core = session.bricks.single { it.type == BrickType.BOSS_CORE && it.health > 0 }
        val originalId = core.id
        val originalHealth = core.health
        assertTrue(core.bounds.y >= session.boardTop)
        session.bricks.filter { it.type != BrickType.BOSS_CORE && it.countsForClear }
            .forEach { it.health = 0 }

        assertTrue("Core was not exposed in a free lane", session.revealDelayedFinalCoreIfReady())
        assertTrue(core.bounds.y + core.bounds.height < session.boardTop)
        assertEquals(originalId, core.id)
        assertEquals(originalHealth, core.health)
        assertFalse("Core repositioned twice", session.revealDelayedFinalCoreIfReady())
        assertFalse("Core disappeared from clear objectives", session.levelComplete)
        assertTrue(session.bricks.none { it !== core && it.health > 0 && it.bounds.overlaps(core.bounds) })

        val saved = session.snapshot()
        val restored = VolleySession(stage, stage.world, seed = 90L)
        restored.restore(saved)
        val restoredCore = restored.bricks.single { it.id == originalId }
        assertEquals(core.bounds.x, restoredCore.bounds.x, .01f)
        assertEquals(core.bounds.y, restoredCore.bounds.y, .01f)
        assertEquals(originalHealth, restoredCore.health)
    }

    @Test fun `ball can strike the exposed core from the launcher`() {
        val stage = VolleyStageRepository.forLevel(90)
        val session = VolleySession(stage, stage.world, seed = 90L)
        session.bricks.filter { it.type != BrickType.BOSS_CORE && it.countsForClear }
            .forEach { it.health = 0 }
        assertTrue(session.revealDelayedFinalCoreIfReady())
        val core = session.bricks.single { it.type == BrickType.BOSS_CORE && it.health > 0 }
        val startingHp = core.health
        assertTrue(session.startVolley(Vector2(0f, 1f)))
        var frames = 0
        while (core.health == startingHp && !session.gameOver && frames++ < 900) {
            session.advance(.025f)
            session.drainEvents()
        }
        assertTrue("A direct ball did not damage the exposed core", core.health < startingHp)
    }

    @Test fun `core finds a centered free lane after twenty row descents`() {
        val stage = VolleyStageRepository.forLevel(90)
        val session = VolleySession(stage, stage.world, seed = 901L)
        session.bricks.filter { it.type != BrickType.BOSS_CORE && it.countsForClear }
            .forEach { it.health = 0 }
        session.bricks.filter { it.type == BrickType.INDESTRUCTIBLE && it.health > 0 }
            .forEach { it.bounds.y -= stage.descentDistance * 20f }

        assertTrue("No safe boss position after the original formation advanced",
            session.revealDelayedFinalCoreIfReady())
        val core = session.bricks.single { it.type == BrickType.BOSS_CORE && it.health > 0 }
        assertEquals(330f, core.bounds.x, .01f)
        assertTrue(core.bounds.y + core.bounds.height < session.boardTop)
        assertTrue(session.bricks.none { it !== core && it.health > 0 && it.bounds.overlaps(core.bounds) })
    }
}
