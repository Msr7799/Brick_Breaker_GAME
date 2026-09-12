package com.example.brick_breaker_ball

import com.badlogic.gdx.math.Vector2
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class GameplayRulesRegressionTest {
    private fun levelWith(vararg placements: Triple<Int, Int, BrickType>): LevelDefinition {
        val ids = placements.associate { (row, col, type) -> BrickCodec.key(row, col) to type.name }
        return LevelDefinition(
            id = 9001,
            world = 1,
            name = "RULE TEST",
            layout = List(8) { "........." },
            brickIds = ids,
            deathRailEnabled = false
        )
    }

    @Test
    fun stageOneHasNoExplosiveTeachingTrap() {
        val stageOne = LevelRepository.level(1)
        assertFalse(stageOne.brickIds.values.any { it == BrickType.EXPLOSIVE.name })
    }

    @Test
    fun megaBallIsVisiblyMega() {
        assertTrue(BallSize.LARGE.radius / BallSize.DEFAULT.radius >= 1.8f)
        assertTrue(BallSize.SMALL.radius < BallSize.DEFAULT.radius)
    }

    @Test
    fun piercingBallCanDestroySteelAndContinue() {
        val session = GameSession(
            level = levelWith(
                Triple(7, 4, BrickType.INDESTRUCTIBLE),
                Triple(6, 4, BrickType.NORMAL_ONE_HIT)
            )
        )
        assertTrue(session.activatePowerUp(PowerUpType.PIERCING_BALL))
        val steel = session.bricks.first { it.type == BrickType.INDESTRUCTIBLE }
        val ball = session.ball
        ball.position.set(steel.bounds.x + steel.bounds.width / 2f, steel.bounds.y - ball.radius - 8f)
        ball.previousPosition.set(ball.position)
        ball.velocity.set(0f, 700f)
        ball.baseSpeed = 700f
        session.phase = GamePhase.PLAYING

        session.update(.12f)

        assertFalse(session.bricks.any { it.type == BrickType.INDESTRUCTIBLE })
        assertTrue(ball.velocity.y > 0f, "Piercing should not reflect from destroyed steel")
    }

    @Test
    fun laserShotsInheritFireAndPiercingEffects() {
        val session = GameSession(level = levelWith(Triple(7, 0, BrickType.NORMAL_ONE_HIT)))
        assertTrue(session.activatePowerUp(PowerUpType.LASER_PADDLE))
        assertTrue(session.activatePowerUp(PowerUpType.FIRE_BALL))
        assertTrue(session.activatePowerUp(PowerUpType.PIERCING_BALL))
        session.phase = GamePhase.PLAYING
        session.ball.position.set(100f, 300f)
        session.ball.velocity.set(0f, 600f)
        session.ball.baseSpeed = 600f

        session.action()

        assertTrue(session.laserShots.isNotEmpty())
        session.laserShots.forEach { shot ->
            assertTrue(shot.fire)
            assertTrue(shot.piercing)
        }
    }

    @Test
    fun losingALifeClearsTemporaryEffectsAndFallingTalismans() {
        val session = GameSession(level = levelWith(Triple(7, 0, BrickType.NORMAL_ONE_HIT)))
        assertTrue(session.activatePowerUp(PowerUpType.EXPAND_PADDLE))
        assertTrue(session.activatePowerUp(PowerUpType.FIRE_BALL))
        assertTrue(session.activatePowerUp(PowerUpType.MAGNETIC_PADDLE))
        session.fallingPowerUps += FallingPowerUp(999, PowerUpType.EXTRA_LIFE, Vector2(100f, 500f))
        val livesBefore = session.lives

        assertTrue(session.activatePowerUp(PowerUpType.KILL_PADDLE))

        assertEquals(livesBefore - 1, session.lives)
        assertTrue(session.powerUps.activeEffects().isEmpty())
        assertTrue(session.fallingPowerUps.isEmpty())
        assertEquals(PaddleMode.NORMAL, session.paddle.mode)
        assertEquals(GameSession.BASE_PADDLE_WIDTH, session.paddle.width)
        assertEquals(GameSession.BASE_PADDLE_WIDTH, session.paddle.targetWidth)
        assertEquals(1, session.balls.size)
        assertEquals(BallElement.NORMAL, session.ball.element)
        assertEquals(BallCollisionMode.NORMAL, session.ball.collisionMode)
        assertEquals(BallSize.DEFAULT, session.ball.size)
    }
}
