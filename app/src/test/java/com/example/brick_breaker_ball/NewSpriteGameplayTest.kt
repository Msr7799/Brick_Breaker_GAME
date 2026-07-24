package com.example.brick_breaker_ball

import com.badlogic.gdx.math.Rectangle
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class NewSpriteGameplayTest {
    @Test fun officialTwentyPowerUpsHaveUniqueOfficialIconsAndSafeCategories() {
        assertEquals(20, PowerUpCatalog.classicTypes.size)
        val icons = PowerUpCatalog.classicTypes.map { PowerUpCatalog.definitions.getValue(it).icon }
        assertEquals(20, icons.toSet().size)
        assertTrue(icons.all { it.startsWith("powerup_") && SpriteId.from(it) != null })
        assertTrue(SHOP_ELIGIBLE_TYPES.all { PowerUpCatalog.definitions.getValue(it).category == PowerUpCategory.GOOD })
        assertFalse(PowerUpType.KILL_PADDLE in SHOP_ELIGIBLE_TYPES)
        assertFalse(PowerUpType.INSTANT_KILL_BALL in SHOP_ELIGIBLE_TYPES)
        assertFalse(PowerUpType.GHOST_BALL in SHOP_ELIGIBLE_TYPES)
    }

    @Test fun plusFourAndFifteenBallsUseUniqueIdsAndInheritedState() {
        val game = playing(); game.activatePowerUp(PowerUpType.FIRE_BALL); game.activatePowerUp(PowerUpType.MEGA_BALL)
        assertTrue(game.activatePowerUp(PowerUpType.MULTIBALL_PLUS_4)); assertEquals(5, game.balls.size)
        assertTrue(game.activatePowerUp(PowerUpType.MULTIBALL_15)); assertEquals(15, game.balls.size)
        assertEquals(15, game.balls.map { it.id }.toSet().size)
        assertTrue(game.balls.all { it.element == BallElement.FIRE && it.size == BallSize.LARGE })
    }

    @Test fun ghostBallPassesThroughWithoutBreakingAndOneHitStillProtectsSteel() {
        val ghost = collisionGame(BrickType.CRYSTAL_BLUE); ghost.activatePowerUp(PowerUpType.GHOST_BALL); hitUp(ghost)
        assertEquals(2, ghost.bricks.first { it.id == 1 }.health); assertTrue(ghost.ball.velocity.y > 0f)
        val oneHit = collisionGame(BrickType.CRYSTAL_BLUE); oneHit.activatePowerUp(PowerUpType.ONE_HIT_ANY_BRICK); hitUp(oneHit)
        assertNull(oneHit.bricks.firstOrNull { it.id == 1 })
        val steel = collisionGame(BrickType.INDESTRUCTIBLE); steel.activatePowerUp(PowerUpType.ONE_HIT_ANY_BRICK); hitUp(steel)
        assertEquals(BrickType.INDESTRUCTIBLE, steel.bricks.first { it.id == 1 }.type)
    }

    @Test fun timedBombModeArmsEachStruckBrickThenExplodesIt() {
        val game = playing(); game.bricks.clear()
        game.bricks += Brick(1, Rectangle(400f, 550f, 80f, 44f), BrickType.CRYSTAL_BLUE, 2, 0f)
        game.bricks += Brick(2, Rectangle(490f, 550f, 80f, 44f), BrickType.NORMAL_ONE_HIT, 1, 0f)
        game.bricks += Brick(3, Rectangle(580f, 550f, 80f, 44f), BrickType.CRYSTAL_GREEN, 2, 0f)
        game.bricks += Brick(4, Rectangle(670f, 550f, 80f, 44f), BrickType.CRYSTAL_RED, 2, 0f)
        game.bricks += Brick(5, Rectangle(760f, 650f, 80f, 44f), BrickType.NORMAL_ONE_HIT, 1, 0f)
        game.bricks += Brick(6, Rectangle(40f, 1450f, 80f, 44f), BrickType.NORMAL_ONE_HIT, 1, 0f)
        assertTrue(game.activatePowerUp(PowerUpType.TIMED_BOMB_BRICKS)); hitUp(game)
        assertEquals(1, game.bricks.count { it.timedBombSeconds != null })
        assertEquals(2, game.bricks.first { it.id == 1 }.health)
        advance(game, GameplayTuning.TIMED_BOMB_DURATION + .1f)
        assertNull(game.bricks.firstOrNull { it.id == 1 })
    }

    @Test fun redSpikeCardCreatesExactlyFourTemporaryRandomKillBricks() {
        val game = playing(); game.bricks.clear()
        repeat(6) { index -> game.bricks += Brick(index + 1, Rectangle(80f + index * 120f, 600f, 90f, 44f), BrickType.CRYSTAL_BLUE, 2, index / 6f) }
        val originals = game.bricks.associate { it.id to it.type }
        assertTrue(game.activatePowerUp(PowerUpType.INSTANT_KILL_BALL))
        assertEquals(4, game.bricks.count { it.type == BrickType.SPIKED_HAZARD })
        val hazard = game.bricks.first { it.type == BrickType.SPIKED_HAZARD }; val lives = game.lives
        game.ball.position.set(hazard.bounds.x + hazard.bounds.width / 2f, hazard.bounds.y - 100f)
        game.ball.velocity.set(0f, 800f); game.ball.baseSpeed = 800f; game.phase = GamePhase.PLAYING; game.update(.18f)
        assertEquals(lives - 1, game.lives); assertTrue(game.bricks.any { it.id == hazard.id })
        game.phase = GamePhase.SERVING; advance(game, 17f)
        assertEquals(0, game.bricks.count { it.type == BrickType.SPIKED_HAZARD })
        assertTrue(game.bricks.all { it.type == originals.getValue(it.id) })
    }

    @Test fun temporarySpikeBricksAndBombFuseSurvivePauseRestore() {
        val prefs = TestPreferences(); val store = PausedSessionStore(prefs); val game = GameSession()
        assertTrue(game.activatePowerUp(PowerUpType.INSTANT_KILL_BALL))
        game.bricks.first { it.type != BrickType.SPIKED_HAZARD && it.type.breakable }.timedBombSeconds = 1.25f
        store.save(game.level, game)
        val restored = store.restore()!!.second
        assertEquals(4, restored.bricks.count { it.type == BrickType.SPIKED_HAZARD && it.temporaryOriginalType != null })
        assertEquals(1, restored.bricks.count { it.timedBombSeconds != null })
        restored.phase = GamePhase.SERVING; advance(restored, 17f)
        assertEquals(0, restored.bricks.count { it.type == BrickType.SPIKED_HAZARD })
    }

    @Test fun ongoingOfficialEffectsExpireButPaddleSizeDoesNot() {
        val game = playing(); game.activatePowerUp(PowerUpType.MEGA_BALL); game.activatePowerUp(PowerUpType.MULTIBALL_PLUS_4)
        assertEquals(BallSize.LARGE, game.ball.size); assertEquals(5, game.balls.size)
        game.phase = GamePhase.SERVING; advance(game, 17f); assertEquals(BallSize.DEFAULT, game.ball.size); assertEquals(1, game.balls.size)
        val width = game.paddle.targetWidth; game.activatePowerUp(PowerUpType.EXPAND_PADDLE); advance(game, 20f)
        assertTrue(game.paddle.targetWidth > width); assertTrue(PowerUpType.EXPAND_PADDLE in game.powerUps)
    }

    @Test fun passThroughSpeedBricksAndRoughStoneApplyTheirRules() {
        val fast = collisionGame(BrickType.LIGHTNING_SPEED_PASS_THROUGH); val beforeFast = fast.ball.baseSpeed; hitUp(fast)
        assertNull(fast.bricks.firstOrNull { it.id == 1 }); assertTrue(fast.ball.baseSpeed > beforeFast); assertTrue(fast.ball.velocity.y > 0f)
        val slow = collisionGame(BrickType.TRANSPARENT_SLOW_PASS_THROUGH); slow.ball.baseSpeed = 800f; slow.ball.velocity.set(0f, 800f); hitUp(slow)
        assertNull(slow.bricks.firstOrNull { it.id == 1 }); assertTrue(slow.ball.baseSpeed < 800f); assertTrue(slow.ball.velocity.y > 0f)
        val rough = collisionGame(BrickType.ROUGH_STONE); val before = rough.ball.velocity.cpy(); hitUp(rough)
        assertNull(rough.bricks.firstOrNull { it.id == 1 }); assertNotEquals(before.angleDeg(), rough.ball.velocity.angleDeg())
    }

    @Test fun blackHoleRelocatesWithinPlayfieldAndRandomItemUsesCanonicalSink() {
        val hole = collisionGame(BrickType.BLACK_HOLE_TELEPORTER); val before = hole.ball.position.cpy(); hitUp(hole)
        assertNotEquals(before, hole.ball.position); assertTrue(hole.ball.position.x in hole.ball.radius..(GameSession.WIDTH-hole.ball.radius))
        val rewards = mutableListOf<PowerUpType>(); val cache = collisionGame(BrickType.RANDOM_INVENTORY_POWERUP); cache.itemRewardSink = rewards::add; hitUp(cache)
        assertEquals(1, rewards.size); assertTrue(rewards.single() in SHOP_ELIGIBLE_TYPES)
    }

    @Test fun canonicalBrickIdsSurviveEditorJsonModelAndGameplay() {
        val state = LevelEditorState(); state.brush = BrickType.BLACK_HOLE_TELEPORTER; state.beginStroke(); state.paint(0, 0); state.endStroke()
        val level = state.toLevelDefinition()
        assertEquals("BLACK_HOLE_TELEPORTER", level.brickIds["0:0"])
        assertEquals(BrickType.BLACK_HOLE_TELEPORTER, LevelEditorState.from(level).cells[0][0].type)
        assertEquals(BrickType.BLACK_HOLE_TELEPORTER, GameSession(level = level).bricks.first().type)
    }

    private fun playing() = GameSession().apply { action(); bricks.clear(); bricks += sentinel() }
    private fun sentinel() = Brick(999, Rectangle(760f, 1450f, 80f, 44f), BrickType.NORMAL_ONE_HIT, 1, 0f)
    private fun collisionGame(type: BrickType) = playing().apply { bricks.clear(); bricks += Brick(1, Rectangle(400f,550f,80f,44f),type,type.maxHealth,0f); bricks += sentinel() }
    private fun hitUp(game: GameSession) { game.phase=GamePhase.PLAYING; game.ball.position.set(440f,430f); game.ball.velocity.set(0f,800f); game.ball.baseSpeed=800f; game.update(.18f) }
    private fun advance(game: GameSession, seconds: Float) { repeat((seconds*120).toInt()) { game.update(1f/120f) } }
}
