package com.example.brick_breaker_ball

import com.badlogic.gdx.math.Rectangle
import com.badlogic.gdx.math.Vector2
import org.junit.Assert.*
import org.junit.Test
import kotlin.math.abs

class Phase1PowerUpTest {
    @Test fun expandPaddleStacksFourTimesAndStopsAtMaximum() {
        val game = GameSession()
        listOf(280f,316f,352f,388f).forEachIndexed { index, width ->
            assertTrue(game.activatePowerUp(PowerUpType.EXPAND_PADDLE)); assertEquals(index+1,game.expandPaddleStacks);assertEquals(width, game.paddle.targetWidth, .01f)
        }
        assertFalse(game.canActivatePowerUp(PowerUpType.EXPAND_PADDLE)); assertFalse(game.activatePowerUp(PowerUpType.EXPAND_PADDLE))
    }

    @Test fun shrinkPaddleStepsAndCancelsExpand() {
        val game = GameSession(); game.activatePowerUp(PowerUpType.EXPAND_PADDLE)
        assertTrue(game.activatePowerUp(PowerUpType.SHRINK_PADDLE)); assertEquals(244f, game.paddle.targetWidth, .01f)
        assertFalse(PowerUpType.EXPAND_PADDLE in game.powerUps); assertTrue(PowerUpType.SHRINK_PADDLE in game.powerUps)
        game.activatePowerUp(PowerUpType.SHRINK_PADDLE); assertEquals(208f, game.paddle.targetWidth, .01f)
    }

    @Test fun superShrinkUsesMinimumAndSynchronizesState() {
        val game = GameSession(); assertTrue(game.activatePowerUp(PowerUpType.SUPER_SHRINK))
        assertEquals(110f, game.paddle.targetWidth, .01f); assertEquals(PaddleMode.SHRUNK, game.paddle.mode)
        assertTrue(PowerUpType.SUPER_SHRINK in game.powerUps); assertFalse(game.activatePowerUp(PowerUpType.SUPER_SHRINK))
    }

    @Test fun piercingDamagesStrongBrickWithoutReflectingButSteelReflects() {
        val strong = collisionGame(BrickType.ARMORED_THREE_HIT); strong.activatePowerUp(PowerUpType.PIERCING_BALL); hitUp(strong)
        assertEquals(2, strong.bricks.single { it.id == 1 }.health); assertTrue(strong.ball.velocity.y > 0f)
        val steel = collisionGame(BrickType.INDESTRUCTIBLE); steel.activatePowerUp(PowerUpType.PIERCING_BALL); hitUp(steel)
        assertEquals(BrickType.INDESTRUCTIBLE, steel.bricks.single { it.id == 1 }.type); assertTrue(steel.ball.velocity.y < 0f)
    }

    @Test fun fireDestroysDirectBrickAndAppliesRadialDamage() {
        val game = playing(); game.bricks.clear()
        game.bricks += Brick(1, Rectangle(400f,550f,80f,44f),BrickType.ARMORED_THREE_HIT,3,0f)
        game.bricks += Brick(2, Rectangle(495f,550f,80f,44f),BrickType.NORMAL_ONE_HIT,1,0f)
        game.bricks += sentinel()
        game.activatePowerUp(PowerUpType.FIRE_BALL); hitUp(game)
        assertNull(game.bricks.firstOrNull { it.id == 1 }); assertNull(game.bricks.firstOrNull { it.id == 2 })
        assertTrue(game.consumeEvents().any { it == GameplayEvent.Explosion })
    }

    @Test fun fireExpirationDoesNotClearStillActivePiercing() {
        val game = GameSession(); game.activatePowerUp(PowerUpType.FIRE_BALL); advance(game,8f)
        game.activatePowerUp(PowerUpType.PIERCING_BALL); advance(game,9f)
        assertEquals(BallElement.NORMAL, game.ball.element); assertEquals(BallCollisionMode.PIERCING, game.ball.collisionMode)
        assertFalse(PowerUpType.FIRE_BALL in game.powerUps); assertTrue(PowerUpType.PIERCING_BALL in game.powerUps)
    }

    @Test fun extraLifeAddsExactlyOneAndRejectsAtNine() {
        val game = GameSession(); game.lives=8; assertTrue(game.activatePowerUp(PowerUpType.EXTRA_LIFE)); assertEquals(9,game.lives)
        assertFalse(game.activatePowerUp(PowerUpType.EXTRA_LIFE)); assertEquals(9,game.lives)
    }

    @Test fun killPaddleUsesOneCanonicalLifeLoss() {
        val game = playing();game.activatePowerUp(PowerUpType.MULTI_BALL);assertTrue(game.balls.size>1);val before=game.lives
        assertTrue(game.activatePowerUp(PowerUpType.KILL_PADDLE));assertEquals(before-1,game.lives);assertEquals(GamePhase.SERVING,game.phase);assertEquals(1,game.balls.size)
    }

    @Test fun setOffExplodingUsesOneChainVisitedSet() {
        val game=playing();game.bricks.clear()
        game.bricks+=Brick(1,Rectangle(350f,550f,80f,44f),BrickType.EXPLOSIVE,1,0f)
        game.bricks+=Brick(2,Rectangle(440f,550f,80f,44f),BrickType.EXPLOSIVE,1,0f);game.bricks+=sentinel()
        assertTrue(game.activatePowerUp(PowerUpType.SET_OFF_EXPLODING));assertTrue(game.bricks.none{it.type==BrickType.EXPLOSIVE})
    }

    @Test fun levelWarpCompletesOnceAndRejectsBoss() {
        val game=playing();assertTrue(game.activatePowerUp(PowerUpType.LEVEL_WARP));assertEquals(GamePhase.LEVEL_COMPLETE,game.phase);assertEquals(1,game.levelCompletionCount)
        assertFalse(game.activatePowerUp(PowerUpType.LEVEL_WARP));assertEquals(1,game.levelCompletionCount)
        val boss=GameSession(level=LevelRepository.level(7));assertFalse(boss.canActivatePowerUp(PowerUpType.LEVEL_WARP))
    }

    @Test fun shrinkBallIsBoundedAndMultiballInheritsSize() {
        val game=playing();assertTrue(game.activatePowerUp(PowerUpType.SHRINK_BALL));assertEquals(BallSize.SMALL,game.ball.size)
        assertFalse(game.activatePowerUp(PowerUpType.SHRINK_BALL));game.activatePowerUp(PowerUpType.MULTI_BALL)
        assertTrue(game.balls.all{it.size==BallSize.SMALL})
    }

    @Test fun megaBallIsBoundedAndNewBallsInheritSize() {
        val game=playing();assertTrue(game.activatePowerUp(PowerUpType.MEGA_BALL));assertEquals(BallSize.LARGE,game.ball.size)
        assertFalse(game.activatePowerUp(PowerUpType.MEGA_BALL));game.activatePowerUp(PowerUpType.EIGHT_BALL)
        assertTrue(game.balls.all{it.size==BallSize.LARGE})
    }

    @Test fun zapConvertsDocumentedTargetsAndExcludesBossAndPuzzleLocks() {
        val game=playing();game.bricks.clear();var id=1
        val targets=listOf(BrickType.INDESTRUCTIBLE,BrickType.ARMORED_TWO_HIT,BrickType.ARMORED_THREE_HIT,BrickType.REGENERATING,BrickType.GHOST)
        targets.forEach{game.bricks+=Brick(id++,Rectangle(id*70f,600f,60f,40f),it,it.maxHealth,0f)}
        game.bricks+=Brick(id++,Rectangle(50f,700f,60f,40f),BrickType.BOSS_CORE,5,0f)
        game.bricks+=Brick(id,Rectangle(120f,700f,60f,40f),BrickType.LOCKED,2,0f)
        assertTrue(game.activatePowerUp(PowerUpType.ZAP_BRICKS))
        assertTrue(game.bricks.take(targets.size).all{it.type==BrickType.NORMAL_ONE_HIT&&it.health==1})
        assertTrue(game.bricks.any{it.type==BrickType.BOSS_CORE});assertTrue(game.bricks.any{it.type==BrickType.LOCKED})
    }

    @Test fun fastBallIsTimedBoundedAndRestoresCanonicalBaseSpeed() {
        val game=GameSession();game.ball.velocity.set(300f,520f);game.ball.baseSpeed=600f
        assertTrue(game.activatePowerUp(PowerUpType.FAST_BALL));assertTrue(game.ball.velocity.len()>600f)
        advance(game,17f);assertFalse(PowerUpType.FAST_BALL in game.powerUps);assertEquals(game.ball.baseSpeed,game.ball.velocity.len(),1f)
        game.ball.baseSpeed=GameSession.MAX_SPEED;game.ball.velocity.set(0f,GameSession.MAX_SPEED);assertFalse(game.canActivatePowerUp(PowerUpType.FAST_BALL))
    }

    @Test fun slowBallIsTimedBoundedAndConflictsWithFast() {
        val game=GameSession();game.ball.velocity.set(300f,700f);game.ball.baseSpeed=760f
        game.activatePowerUp(PowerUpType.FAST_BALL);assertTrue(game.activatePowerUp(PowerUpType.SLOW_BALL));assertFalse(PowerUpType.FAST_BALL in game.powerUps)
        assertTrue(game.ball.velocity.len()>=GameSession.MIN_SPEED);advance(game,17f);assertEquals(game.ball.baseSpeed,game.ball.velocity.len(),1f)
    }

    @Test fun multiballDoublesWithUniqueIdsAndOneLossCostsNoLife() {
        val game=playing();assertTrue(game.activatePowerUp(PowerUpType.MULTI_BALL));assertEquals(2,game.balls.size)
        assertEquals(2,game.balls.map{it.id}.distinct().size);val lives=game.lives;game.balls.first().position.y=-100f;game.update(.001f);assertEquals(lives,game.lives)
    }

    @Test fun eightBallCreatesEightDistinctTrajectoriesWithoutOverlap() {
        val game=playing();assertTrue(game.activatePowerUp(PowerUpType.EIGHT_BALL));assertEquals(8,game.balls.size)
        assertEquals(8,game.balls.map{it.id}.distinct().size);assertTrue(game.balls.map{"${it.velocity.x.toInt()},${it.velocity.y.toInt()}"}.distinct().size>4)
        assertTrue(game.balls.map{"${it.position.x},${it.position.y}"}.distinct().size>1)
    }

    @Test fun multiballActivatedDuringServeLaunchesEveryCreatedBall() {
        val game=GameSession();game.activatePowerUp(PowerUpType.MULTI_BALL);assertEquals(2,game.balls.size);game.launchAttachedBalls(-1f)
        assertEquals(GamePhase.PLAYING,game.phase);assertTrue(game.balls.all{!it.velocity.isZero});assertTrue(game.balls.all{it.velocity.x<0f})
    }

    @Test fun laserUsesTwoCannonsCooldownCapAndSweptCollision() {
        val game=playing();game.ball.velocity.setZero();game.bricks.clear()
        game.bricks+=Brick(1,Rectangle(350f,400f,24f,6f),BrickType.NORMAL_ONE_HIT,1,0f);game.bricks+=sentinel()
        game.activatePowerUp(PowerUpType.LASER_PADDLE);game.action();assertEquals(2,game.laserShots.size);game.action();assertEquals(2,game.laserShots.size)
        game.update(.5f);assertNull(game.bricks.firstOrNull{it.id==1});assertTrue(game.consumeEvents().any{it==GameplayEvent.LaserHit})
    }

    @Test fun stickyCatchesAndRelaunchesBallIndependently() {
        val game=playing();game.activatePowerUp(PowerUpType.STICKY_PADDLE);game.ball.position.set(game.paddle.x,210f);game.ball.velocity.set(0f,-800f);game.ball.baseSpeed=800f
        game.update(.1f);assertNotNull(game.ball.stuckOffset);assertTrue(game.hasAttachedBalls());game.launchAttachedBalls(1f)
        assertNull(game.ball.stuckOffset);assertTrue(game.ball.velocity.x>0f)
    }

    @Test fun expandedExplosionReachesBricksOutsideNormalRadius() {
        fun configured(expanded:Boolean):GameSession{val g=playing();g.bricks.clear();g.bricks+=Brick(1,Rectangle(300f,550f,80f,44f),BrickType.EXPLOSIVE,1,0f);g.bricks+=Brick(2,Rectangle(495f,550f,80f,44f),BrickType.NORMAL_ONE_HIT,1,0f);g.bricks+=sentinel();if(expanded)g.activatePowerUp(PowerUpType.EXPAND_EXPLODING);return g}
        val normal=configured(false);normal.activatePowerUp(PowerUpType.SET_OFF_EXPLODING);assertNotNull(normal.bricks.firstOrNull{it.id==2})
        val wide=configured(true);wide.activatePowerUp(PowerUpType.SET_OFF_EXPLODING);assertNull(wide.bricks.firstOrNull{it.id==2})
    }

    @Test fun fallingBricksUpdatesMovingOriginsAndWarns() {
        val game=playing();game.bricks.clear();val moving=Brick(1,Rectangle(400f,300f,80f,44f),BrickType.MOVING_VERTICAL,1,0f);game.bricks+=moving;game.bricks+=sentinel()
        game.activatePowerUp(PowerUpType.FALLING_BRICKS);game.ball.position.set(game.paddle.x,210f);game.ball.velocity.set(0f,-800f);game.ball.baseSpeed=800f
        game.update(.1f);assertEquals(258f,moving.originY,.01f);assertTrue(game.consumeEvents().any{it==GameplayEvent.FallingWarning})
    }

    @Test fun fallingBrickAtPaddleBoundaryCostsExactlyOneLife() {
        val game=playing();game.bricks.clear();game.bricks+=Brick(1,Rectangle(400f,180f,80f,44f),BrickType.NORMAL_ONE_HIT,1,0f)
        game.activatePowerUp(PowerUpType.FALLING_BRICKS);val lives=game.lives;game.ball.position.set(game.paddle.x,210f);game.ball.velocity.set(0f,-800f);game.ball.baseSpeed=800f
        game.update(.1f);assertEquals(lives-1,game.lives);assertEquals(GamePhase.SERVING,game.phase)
    }

    @Test fun fallingCapsuleCollectionActivatesThroughCanonicalDispatcher() {
        val game=playing();game.fallingPowerUps+=FallingPowerUp(1,PowerUpType.EXTRA_LIFE,Vector2(game.paddle.x,game.paddle.y+5f),Vector2())
        val lives=game.lives;game.update(.001f);assertEquals(lives+1,game.lives);assertTrue(game.fallingPowerUps.isEmpty())
    }

    @Test fun randomEffectsUseSessionRngAndOnlyUsefulEligibleResults() {
        val game=playing();val names=mutableSetOf<String>()
        repeat(12){if(game.activatePowerUp(PowerUpType.RANDOM_GOOD))game.consumeEvents().filterIsInstance<GameplayEvent.Feedback>().forEach{names+=it.text}}
        assertTrue(names.size>1);assertTrue(game.powerUps.activeEffects().none{it.first==PowerUpType.RANDOM_GOOD||it.first==PowerUpType.RANDOM_BAD})
    }

    @Test fun noEffectItemIsNotConsumedAndSuccessfulItemIsConsumed() {
        val prefs=TestPreferences();val items=BoosterInventoryStore(prefs);items.add(PowerUpType.MEGA_BALL,1);val game=playing();game.ball.size=BallSize.LARGE
        assertTrue(game.useItem(PowerUpType.MEGA_BALL,items) is BoosterUseResult.Rejected);assertEquals(1,items.count(PowerUpType.MEGA_BALL))
        game.ball.size=BallSize.DEFAULT;assertTrue(game.useItem(PowerUpType.MEGA_BALL,items) is BoosterUseResult.Applied);assertEquals(0,items.count(PowerUpType.MEGA_BALL))
    }

    @Test fun paidAndRewardPoolsContainOnlyPositiveCanonicalItems() {
        assertEquals(14,SHOP_ELIGIBLE_TYPES.size);assertTrue(SHOP_ELIGIBLE_TYPES.all{PowerUpCatalog.definitions.getValue(it).category==PowerUpCategory.GOOD})
        repeat(50){assertTrue(BoosterGrantFactory.reward("reward-$it") in SHOP_ELIGIBLE_TYPES)}
    }

    @Test fun pauseRestorePreservesIndependentBallAndActiveEffectState() {
        val prefs=TestPreferences();val store=PausedSessionStore(prefs);val game=playing()
        game.activatePowerUp(PowerUpType.FIRE_BALL);game.activatePowerUp(PowerUpType.PIERCING_BALL);game.activatePowerUp(PowerUpType.STICKY_PADDLE)
        game.activatePowerUp(PowerUpType.EXPAND_EXPLODING);game.activatePowerUp(PowerUpType.FALLING_BRICKS);game.ball.stuckOffset=12f
        game.laserShots+=LaserShot(Vector2(300f,400f),Vector2(300f,390f));store.save(game.level,game)
        val restored=store.restore()!!.second
        assertEquals(BallElement.FIRE,restored.ball.element);assertEquals(BallCollisionMode.PIERCING,restored.ball.collisionMode);assertEquals(12f,restored.ball.stuckOffset!!,.01f)
        assertTrue(PowerUpType.STICKY_PADDLE in restored.powerUps);assertTrue(PowerUpType.EXPAND_EXPLODING in restored.powerUps);assertEquals(2,restored.explosionExpansion);assertTrue(restored.fallingBricksMode);assertEquals(1,restored.laserShots.size)
    }

    @Test fun itemAndRewardCountsPersistInCanonicalStores() {
        val itemPrefs=TestPreferences();BoosterInventoryStore(itemPrefs).add(PowerUpType.FIRE_BALL,3);assertEquals(3,BoosterInventoryStore(itemPrefs).count(PowerUpType.FIRE_BALL))
        val rewardPrefs=TestPreferences();val reward=PendingReward("token",PowerUpType.EXTRA_LIFE,4);PendingRewardRevealStore(rewardPrefs).save(reward);assertEquals(reward,PendingRewardRevealStore(rewardPrefs).load())
    }

    @Test fun restartClearsLevelEffectsAndCreatesCleanBallState() {
        val game=playing();game.activatePowerUp(PowerUpType.FIRE_BALL);game.activatePowerUp(PowerUpType.EXPAND_EXPLODING);game.phase=GamePhase.GAME_OVER;game.action()
        assertFalse(PowerUpType.FIRE_BALL in game.powerUps);assertFalse(PowerUpType.EXPAND_EXPLODING in game.powerUps);assertEquals(BallElement.NORMAL,game.ball.element);assertEquals(1,game.explosionExpansion)
    }

    @Test fun paddleAnimationRemainsInsideScreenAtExpandedWidth() {
        val game=GameSession();game.paddle.x=GameSession.WIDTH-game.paddle.width/2f;game.activatePowerUp(PowerUpType.EXPAND_PADDLE)
        repeat(30){game.movePaddle(GameSession.WIDTH,.02f)};assertTrue(game.paddle.bounds.x>=0f);assertTrue(game.paddle.bounds.x+game.paddle.bounds.width<=GameSession.WIDTH+.01f)
    }

    @Test fun removedPlaceholderTypesAreNotInCanonicalEnum() {
        val names=PowerUpType.entries.map{it.name}.toSet();assertTrue(setOf("TIME_SLOW","COMBO_FREEZE","BRICK_REVEAL","DARKNESS","BOTTOM_HAZARD").none{it in names})
    }

    @Test fun gameplayPreservesLogicalNineByEightGridWithoutReflow() {
        val level=LevelDefinition(1,1,"GRID",8,9,List(8){"NNNNNNNNN"},600f,3,1000)
        val game=GameSession(level=level);assertEquals(72,game.bricks.size)
        assertEquals(9,game.bricks.map{it.bounds.x}.distinct().size);assertEquals(8,game.bricks.map{it.bounds.y}.distinct().size)
    }

    private fun playing()=GameSession().apply{action();bricks.clear();bricks+=sentinel()}
    private fun sentinel()=Brick(999,Rectangle(760f,1450f,80f,44f),BrickType.NORMAL_ONE_HIT,1,0f)
    private fun collisionGame(type:BrickType)=playing().apply{bricks.clear();bricks+=Brick(1,Rectangle(400f,550f,80f,44f),type,type.maxHealth,0f);bricks+=sentinel()}
    private fun hitUp(game:GameSession){game.phase=GamePhase.PLAYING;game.ball.position.set(440f,430f);game.ball.velocity.set(0f,800f);game.ball.baseSpeed=800f;game.update(.18f)}
    private fun advance(game:GameSession,seconds:Float){repeat((seconds*120).toInt()){game.update(1f/120f)}}
}
