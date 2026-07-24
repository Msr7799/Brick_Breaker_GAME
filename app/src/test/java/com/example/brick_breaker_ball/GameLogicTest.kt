package com.example.brick_breaker_ball

import com.badlogic.gdx.math.Rectangle
import com.badlogic.gdx.math.Vector2
import org.junit.Assert.*
import org.junit.Test
import kotlin.math.abs
import java.io.File

class GameLogicTest {
    @Test fun fixedStepRunsTwicePerSixtieth() {
        var steps = 0; FixedStepLoop().advance(1f / 60f) { steps++ }; assertEquals(2, steps)
    }
    @Test fun ballWaitsForLaunchAndReplayRecordsCommand() {
        val game = GameSession(); assertEquals(GamePhase.SERVING, game.phase); assertEquals(0f, game.ball.velocity.len(), .001f)
        game.action(); assertTrue(game.ball.velocity.y > 0f); assertEquals("ACTION", game.replay.snapshot().inputs.last().command)
    }
    @Test fun servedBallLaunchesInSwipeDirection() {
        val left = GameSession(); left.launchServe(-1f)
        val right = GameSession(); right.launchServe(1f)
        assertTrue(left.ball.velocity.x < 0f); assertTrue(left.ball.velocity.y > 0f)
        assertTrue(right.ball.velocity.x > 0f); assertTrue(right.ball.velocity.y > 0f)
    }
    @Test fun stickyBallUsesTheSameSwipeDirectionLaunch() {
        val game = playingSession()
        game.ball.stuckOffset = 0f; game.ball.velocity.setZero()
        assertTrue(game.hasAttachedBalls())
        game.launchAttachedBalls(-1f)
        assertNull(game.ball.stuckOffset)
        assertTrue(game.ball.velocity.x < 0f); assertTrue(game.ball.velocity.y > 0f)
    }
    @Test fun enlargedBricksStayInsidePlayfieldWalls() {
        val game = GameSession()
        assertTrue(game.bricks.all { abs(it.bounds.width - 92f) < .01f && it.bounds.height == 58f })
        assertTrue(game.bricks.all { it.bounds.x >= 18f && it.bounds.x + it.bounds.width <= GameSession.WIDTH - 18f })
    }
    @Test fun sweptCollisionPreventsTunnelling() {
        val hit = SweptCollision.circleVsAabb(Vector2(0f, 50f), Vector2(1000f, 0f), 5f, Rectangle(500f, 40f, 20f, 20f))
        assertNotNull(hit); assertTrue(hit!!.time in 0.49f..0.5f); assertEquals(-1f, hit.normalX, .001f)
    }
    @Test fun wallBounceAndSpeedClamp() {
        val game = playingSession(); game.ball.position.set(34f, 500f); game.ball.velocity.set(-900f, 200f)
        game.update(.05f); assertTrue(game.ball.velocity.x > 0f)
        game.ball.velocity.set(5000f, 1f); game.clampSpeed(game.ball)
        assertTrue(game.ball.velocity.len() <= GameSession.MAX_SPEED + .01f); assertTrue(abs(game.ball.velocity.y) >= GameSession.MIN_VERTICAL)
    }
    @Test fun paddleCenterAndEdgeProduceDifferentAngles() {
        val center = playingSession(); center.ball.position.set(center.paddle.x, 180f); center.ball.velocity.set(0f, -700f); center.update(.15f)
        val edge = playingSession(); edge.ball.position.set(edge.paddle.x + edge.paddle.width * .42f, 180f); edge.ball.velocity.set(0f, -700f); edge.update(.15f)
        assertTrue(center.ball.velocity.y > 0f); assertTrue(edge.ball.velocity.y > 0f)
        assertTrue(abs(edge.ball.velocity.x) > abs(center.ball.velocity.x))
    }
    @Test fun multiBallBallsAreIndependentAndOneLossCostsNoLife() {
        val game = playingSession(); game.activatePowerUp(PowerUpType.TRIPLE_BALL); assertEquals(3, game.balls.size)
        val lives = game.lives; game.balls.first().position.y = -100f; game.update(.001f)
        assertEquals(2, game.balls.size); assertEquals(lives, game.lives); assertNotEquals(game.balls[0].id, game.balls[1].id)
    }
    @Test fun paddleExpansionAnimatesAndOppositeCancels() {
        val game = GameSession(); game.activatePowerUp(PowerUpType.EXPAND_PADDLE); val before = game.paddle.width
        game.movePaddle(game.paddle.x, .05f); assertTrue(game.paddle.width > before); assertEquals(280f, game.paddle.targetWidth, .01f)
        game.activatePowerUp(PowerUpType.SHRINK_PADDLE); assertEquals(244f, game.paddle.targetWidth, .01f)
        repeat(30 * 120) { game.update(1f / 120f) }
        assertEquals(244f, game.paddle.targetWidth, .01f)
    }
    @Test fun multiHitChangesDamageStageAndThenBreaks() {
        val game = collisionSession(BrickType.ARMORED_TWO_HIT)
        hitBrick(game); assertEquals(1, game.bricks.single { it.type.breakable }.health); assertEquals(DamageStage.CRITICAL, game.bricks.single { it.type.breakable }.damageStage)
        hitBrick(game); assertTrue(game.bricks.none { it.type.breakable })
    }
    @Test fun indestructibleBrickSurvives() {
        val game = collisionSession(BrickType.INDESTRUCTIBLE); hitBrick(game)
        assertEquals(1, game.bricks.size); assertEquals(BrickType.INDESTRUCTIBLE, game.bricks.single().type)
    }
    @Test fun explosiveBrickDestroysNearbyBrickWithoutLoop() {
        val game = playingSession(); game.bricks.clear()
        game.bricks += Brick(1, Rectangle(400f, 550f, 80f, 44f), BrickType.EXPLOSIVE, 1, 0f)
        game.bricks += Brick(2, Rectangle(490f, 550f, 80f, 44f), BrickType.EXPLOSIVE, 1, 0f)
        game.ball.position.set(440f, 400f); game.ball.velocity.set(0f, 900f); game.update(.2f)
        assertTrue(game.bricks.isEmpty())
    }
    @Test fun movingBrickUsesCurrentPosition() {
        val game = GameSession(); game.bricks.clear(); val brick = Brick(1, Rectangle(400f, 600f, 80f, 44f), BrickType.MOVING_HORIZONTAL, 1, 0f)
        game.bricks += brick; val before = brick.bounds.x; game.action(); game.update(.5f); assertNotEquals(before, brick.bounds.x)
    }
    @Test fun scoreMultiplierAndBallModesActivateAndExpire() {
        val game = GameSession(); game.activatePowerUp(PowerUpType.SCORE_X3); assertTrue(PowerUpType.SCORE_X3 in game.powerUps)
        game.activatePowerUp(PowerUpType.SCORE_X2); assertFalse(PowerUpType.SCORE_X3 in game.powerUps)
        game.activatePowerUp(PowerUpType.FIRE_BALL); assertEquals(BallElement.FIRE, game.ball.element)
        repeat(17 * 120) { game.update(1f / 120f) }; assertEquals(BallElement.NORMAL, game.ball.element)
    }
    @Test fun expandedLaserPaddleKeepsBothEffects() {
        val game = GameSession()
        game.activatePowerUp(PowerUpType.LASER_PADDLE)
        game.activatePowerUp(PowerUpType.EXPAND_PADDLE)
        assertTrue(PowerUpType.LASER_PADDLE in game.powerUps)
        assertTrue(PowerUpType.EXPAND_PADDLE in game.powerUps)
        assertEquals(PaddleMode.LASER, game.paddle.mode)
        assertEquals(280f, game.paddle.targetWidth, .01f)
    }
    @Test fun bottomShieldPreventsOneLifeLoss() {
        val game = playingSession(); game.activatePowerUp(PowerUpType.BOTTOM_SHIELD); val lives = game.lives
        game.ball.position.y = -100f; game.update(.001f); assertEquals(lives, game.lives); assertFalse(game.bottomShield)
    }
    @Test fun allPowerUpsHaveDefinitionsAndDistinctBadShape() {
        assertEquals(PowerUpType.entries.size, PowerUpCatalog.definitions.size)
        assertTrue(PowerUpCatalog.definitions.values.filter { it.category == PowerUpCategory.BAD }.all { it.capsuleStyle == CapsuleStyle.HEX_BAD })
    }
    @Test fun bilingualGuideClassicPowerUpsChangeGameplayState() {
        assertEquals(20, PowerUpCatalog.classicTypes.size)
        val game = playingSession()
        game.activatePowerUp(PowerUpType.SHRINK_BALL); assertEquals(BallSize.SMALL, game.ball.size)
        game.activatePowerUp(PowerUpType.MEGA_BALL); assertEquals(BallSize.LARGE, game.ball.size); assertFalse(PowerUpType.SHRINK_BALL in game.powerUps)
        game.activatePowerUp(PowerUpType.EIGHT_BALL); assertEquals(8, game.balls.size)
        game.activatePowerUp(PowerUpType.EXPAND_EXPLODING); assertEquals(2, game.explosionExpansion)
        game.activatePowerUp(PowerUpType.FALLING_BRICKS); assertTrue(game.fallingBricksMode)
        game.activatePowerUp(PowerUpType.LEVEL_WARP); assertEquals(GamePhase.LEVEL_COMPLETE, game.phase)
    }
    @Test fun campaignContainsNinetyValidLevelsAcrossThirteenWorlds() {
        assertEquals(90, LevelRepository.levels.size)
        assertEquals(13, LevelRepository.worlds.size)
        LevelRepository.levels.forEach { assertTrue("level ${it.id}: ${LevelValidator.validate(it).errors}", LevelValidator.validate(it).valid) }
        assertEquals((1..90).toList(), LevelRepository.levels.map { it.id })
        assertEquals(1, LevelRepository.level(7).world)
        assertEquals(2, LevelRepository.level(8).world)
        assertEquals(13, LevelRepository.level(90).world)
        assertTrue(GameSession(level = LevelRepository.level(1)).bricks.any { it.type.breakable })
    }
    @Test fun canonicalSpriteSourceAndManifestCoverAllRuntimeRegions() {
        val source = File("../spritesheet.png")
        val manifest = File("../docs/sprite-map-detailed.json")
        assertTrue(source.isFile); assertTrue(manifest.isFile)
        val text = manifest.readText()
        assertEquals(70, Regex("\\\"id\\\"").findAll(text.substringBefore("\"rejectedDetections\"")).count())
        listOf("paddle_normal", "ball_normal_medium", "powerup_extra_life", "brick_random_inventory_powerup", "spark_frame_08", "laser_bullet", "electric_floor_wire").forEach { assertTrue(text.contains(it)) }
        assertArrayEquals(source.readBytes(), File("src/main/assets/sprites/spritesheet.png").readBytes())
        assertFalse(File("src/main/assets/bricks-breaker_sprites-no-bg.png").exists())
        assertFalse(File("src/main/assets/bricks-breaker_sprites-no-bg.svg").exists())
    }

    private fun playingSession() = GameSession().apply { action(); bricks.clear(); bricks += Brick(999, Rectangle(700f, 1400f, 80f, 44f), BrickType.INDESTRUCTIBLE, BrickType.INDESTRUCTIBLE.maxHealth, 0f) }
    private fun collisionSession(type: BrickType) = playingSession().apply {
        bricks.clear(); bricks += Brick(1, Rectangle(400f, 550f, 80f, 44f), type, type.maxHealth, 0f)
    }
    private fun hitBrick(game: GameSession) {
        game.phase = GamePhase.PLAYING; game.ball.position.set(440f, 430f); game.ball.velocity.set(0f, 800f); game.update(.18f)
    }
}
