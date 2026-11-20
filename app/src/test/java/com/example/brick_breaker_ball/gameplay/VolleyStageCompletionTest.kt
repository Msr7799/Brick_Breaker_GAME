package com.example.brick_breaker_ball.gameplay

import com.badlogic.gdx.math.Vector2
import com.example.brick_breaker_ball.campaign.VolleyAuthoredBrickSeed
import com.example.brick_breaker_ball.campaign.VolleyStageRepository
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class VolleyStageCompletionTest {
    @Test fun `a real ball hit on the only remaining target completes the stage`() {
        val first = VolleyStageRepository.forLevel(1)
        val simple = first.copy(
            name = "SINGLE TARGET TEST",
            sourceScene = null,
            startingBalls = 1,
            bossStage = false,
            authoredBricks = listOf(
                VolleyAuthoredBrickSeed(
                    sourceRow = 0,
                    sourceColumn = 5,
                    x = 410f,
                    y = 940f,
                    width = 80f,
                    height = 80f,
                    type = BrickType.NORMAL_ONE_HIT,
                    health = 1,
                    shape = VolleyPieceShape.RECTANGLE,
                    countsForClear = true
                )
            ),
            rails = emptyList(),
            pickups = emptyList()
        )
        val session = VolleySession(simple, simple.world, seed = 5L)
        assertTrue(session.startVolley(Vector2(0f, 1f)))
        var frames = 0
        while (!session.levelComplete && !session.gameOver && frames++ < 600) {
            session.advance(.016f)
            session.drainEvents()
        }
        assertEquals("Shot did not complete the only clear objective",
            VolleyPhase.LEVEL_COMPLETE, session.phase)
        assertTrue(session.bricks.none { it.countsForClear && it.health > 0 })
    }
}
