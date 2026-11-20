package com.example.brick_breaker_ball.gameplay

import com.example.brick_breaker_ball.campaign.VolleyStageRepository
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class VolleyStageAccessTest {
    @Test fun `all 160 stages start with valid live clear targets`() {
        for (id in 1..160) {
            val stage = VolleyStageRepository.forLevel(id)
            val session = VolleySession(stage, stage.world, seed = id.toLong())
            assertEquals(id, stage.levelId)
            assertFalse("Stage $id started already cleared", session.levelComplete)
            assertTrue("Stage $id has no objectives", session.bricks.any { it.health > 0 && it.countsForClear })
            assertEquals("Stage $id has inconsistent IDs", session.bricks.size,
                session.bricks.map { it.id }.toSet().size)
        }
    }

    @Test fun `each world finale retains its boss and exact guard count`() {
        val finals = listOf(18, 36, 54, 72, 90, 108, 126, 143, 160)
        finals.forEachIndexed { index, id ->
            val stage = VolleyStageRepository.forLevel(id)
            val session = VolleySession(stage, stage.world, seed = id.toLong())
            val core = session.bricks.filter { it.type == BrickType.BOSS_CORE && it.health > 0 }
            val guards = session.bricks.filter { it.volleyShieldLock && it.health > 0 }
            assertTrue("Stage $id is not marked as a finale", stage.bossStage)
            assertEquals("Stage $id has incorrect boss count", 1, core.size)
            assertEquals("Stage $id has incorrect guard count",
                if (index < 3) 2 else if (index < 6) 3 else 4, guards.size)
            assertTrue("Stage $id boss is not a clear objective", core.single().countsForClear)
            assertTrue("Stage $id boss horizontal bounds escaped board",
                core.single().bounds.x >= VolleySession.BOARD_LEFT &&
                    core.single().bounds.x + core.single().bounds.width <= VolleySession.BOARD_RIGHT)
        }
    }

    @Test fun `single shot search replays deterministically from the same seed`() {
        val stage = VolleyStageRepository.forLevel(1)
        val first = VolleyStageAccessProbe.search(
            stage, seed = 579L, playerLevel = 1, maxTurns = 1, angles = floatArrayOf(0f)
        )
        val second = VolleyStageAccessProbe.search(
            stage, seed = 579L, playerLevel = 1, maxTurns = 1, angles = floatArrayOf(0f)
        )
        assertEquals(first, second)
        assertEquals(1, first.stageId)
        assertTrue("No shot was completed", first.shots.isNotEmpty())
    }
}
