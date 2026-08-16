package com.example.brick_breaker_ball

import com.badlogic.gdx.Gdx
import com.badlogic.gdx.Preferences
import com.badlogic.gdx.math.Rectangle
import com.badlogic.gdx.math.Vector2

class PausedSessionStore(private val prefs: Preferences = Gdx.app.getPreferences("brickbreakerball-paused-session-v1")) {
    fun hasPausedGame() = prefs.getBoolean("valid", false)

    fun save(level: LevelDefinition, session: GameSession) {
        val balls = session.balls.joinToString(";") { b -> listOf(
            b.id,b.position.x,b.position.y,b.previousPosition.x,b.previousPosition.y,b.velocity.x,b.velocity.y,
            b.size.name,b.baseSize.name,b.cosmeticGroupName,b.cosmeticSpriteName,b.element.name,b.collisionMode.name,b.baseSpeed,b.stuckOffset?:"n"
        ).joinToString(",") }
        val bricks = session.bricks.joinToString(";") { b -> listOf(b.id,b.bounds.x,b.bounds.y,b.bounds.width,b.bounds.height,b.type.name,b.health,b.hue,b.originX,b.originY,b.age,b.groupId,b.locked,b.ghostVisible,b.initialHealth,b.timedBombSeconds?:"n",b.temporaryOriginalType?.name?:"n",b.temporaryOriginalHealth,b.temporaryOriginalInitialHealth).joinToString(",") }
        val powers = session.fallingPowerUps.joinToString(";") { p -> listOf(p.id,p.type.name,p.position.x,p.position.y,p.velocity.x,p.velocity.y).joinToString(",") }
        val timers = session.powerUps.timers.entries.joinToString(";") { "${it.key.name},${it.value}" }
        val persistent = session.powerUps.persistentSnapshot().joinToString(",") { it.name }
        val lasers = session.laserShots.joinToString(";") { s -> listOf(s.position.x,s.position.y,s.previousPosition.x,s.previousPosition.y).joinToString(",") }
        prefs.putInteger("version",8).putBoolean("valid",true).putInteger("level",level.id).putInteger("lives",session.lives).putInteger("score",session.score)
            .putString("baseBallSize",session.baseBallSize.name)
            .putString("ballGroup",session.selectedBallGroupName).putString("ballSprite",session.selectedBallSpriteName)
            .putString("phase",session.phase.name)
            .putFloat("paddle",session.paddle.x).putFloat("paddleWidth",session.paddle.width).putFloat("targetWidth",session.paddle.targetWidth)
            .putString("paddleMode",session.paddle.mode.name).putString("balls",balls).putString("bricks",bricks).putString("powers",powers)
            .putString("timers",timers).putString("persistent",persistent).putString("lasers",lasers)
            .putFloat("laserCooldown",session.laserCooldown).putInteger("nextBallId",session.nextBallId).putInteger("nextPowerUpId",session.nextPowerUpId)
            .putBoolean("shield",session.bottomShield).putInteger("explosionExpansion",session.explosionExpansion)
            .putBoolean("fallingBricksMode",session.fallingBricksMode).putInteger("expandPaddleStacks",session.expandPaddleStacks).flush()
    }

    fun restore(): Pair<LevelDefinition,GameSession>? {
        if(!hasPausedGame()) return null
        return runCatching {
            val level=LevelRepository.level(prefs.getInteger("level",1)); val version = prefs.getInteger("version",1)
            val restoredBaseSize = if (version >= 6) runCatching { BallSize.valueOf(prefs.getString("baseBallSize",BallSize.DEFAULT.name)) }.getOrDefault(BallSize.DEFAULT) else BallSize.DEFAULT
            val session=GameSession(level=level,baseBallSize=restoredBaseSize,
                selectedBallGroupName=if(version>=7)prefs.getString("ballGroup",CosmeticDefaults.BALL_GROUP) else CosmeticDefaults.BALL_GROUP,
                selectedBallSpriteName=if(version>=7)prefs.getString("ballSprite",CosmeticDefaults.BALL_SPRITE) else CosmeticDefaults.BALL_SPRITE)
            session.lives=prefs.getInteger("lives",level.lives);session.score=prefs.getInteger("score",0);session.paddle.x=prefs.getFloat("paddle",450f)
            session.paddle.width=prefs.getFloat("paddleWidth",244f);session.paddle.targetWidth=prefs.getFloat("targetWidth",session.paddle.width)
            session.paddle.mode=PaddleMode.valueOf(prefs.getString("paddleMode",PaddleMode.NORMAL.name));session.balls.clear()
            prefs.getString("balls","").split(';').filter(String::isNotBlank).forEach { raw ->
                val v=raw.split(',')
                session.balls += if (version >= 7 && v.size >= 15) {
                    Ball(id=v[0].toInt(),position=Vector2(v[1].toFloat(),v[2].toFloat()),previousPosition=Vector2(v[3].toFloat(),v[4].toFloat()),velocity=Vector2(v[5].toFloat(),v[6].toFloat()),
                        size=BallSize.valueOf(v[7]),baseSize=BallSize.valueOf(v[8]),cosmeticGroupName=v[9],cosmeticSpriteName=v[10],element=BallElement.valueOf(v[11]),collisionMode=BallCollisionMode.valueOf(v[12]),baseSpeed=v[13].toFloat(),stuckOffset=v[14].takeUnless{it=="n"}?.toFloat())
                } else if (version >= 6 && v.size >= 13) {
                    Ball(id=v[0].toInt(),position=Vector2(v[1].toFloat(),v[2].toFloat()),previousPosition=Vector2(v[3].toFloat(),v[4].toFloat()),velocity=Vector2(v[5].toFloat(),v[6].toFloat()),
                        size=BallSize.valueOf(v[7]),baseSize=BallSize.valueOf(v[8]),cosmeticGroupName=CosmeticDefaults.BALL_GROUP,cosmeticSpriteName=CosmeticDefaults.BALL_SPRITE,element=BallElement.valueOf(v[9]),collisionMode=BallCollisionMode.valueOf(v[10]),baseSpeed=v[11].toFloat(),stuckOffset=v[12].takeUnless{it=="n"}?.toFloat())
                } else if (version >= 2 && v.size >= 12) {
                    Ball(id=v[0].toInt(),position=Vector2(v[1].toFloat(),v[2].toFloat()),previousPosition=Vector2(v[3].toFloat(),v[4].toFloat()),velocity=Vector2(v[5].toFloat(),v[6].toFloat()),
                        size=BallSize.valueOf(v[7]),baseSize=BallSize.DEFAULT,element=BallElement.valueOf(v[8]),collisionMode=BallCollisionMode.valueOf(v[9]),baseSpeed=v[10].toFloat(),stuckOffset=v[11].takeUnless{it=="n"}?.toFloat())
                } else {
                    val legacyMode = v[8]; val size = BallSize.fromRadius(v[7].toFloat())
                    Ball(id=v[0].toInt(),position=Vector2(v[1].toFloat(),v[2].toFloat()),previousPosition=Vector2(v[3].toFloat(),v[4].toFloat()),velocity=Vector2(v[5].toFloat(),v[6].toFloat()),size=size,baseSize=BallSize.DEFAULT,
                        element=if(legacyMode=="FIRE")BallElement.FIRE else if(legacyMode=="EXPLOSIVE")BallElement.EXPLOSIVE else BallElement.NORMAL,
                        collisionMode=if(legacyMode=="PIERCING")BallCollisionMode.PIERCING else BallCollisionMode.NORMAL,baseSpeed=Vector2(v[5].toFloat(),v[6].toFloat()).len().coerceAtLeast(session.level.ballSpeed),stuckOffset=v[9].takeUnless{it=="n"}?.toFloat())
                }
            }
            require(session.balls.isNotEmpty());session.bricks.clear()
            prefs.getString("bricks","").split(';').filter(String::isNotBlank).forEach { raw ->
                val v=raw.split(',')
                val savedType=BrickType.valueOf(v[5])
                val temporaryOriginalType=v.getOrNull(16)?.takeUnless{it=="n"}?.let{BrickType.valueOf(it)}
                val permanentLegacySpike=savedType==BrickType.SPIKED_HAZARD && temporaryOriginalType==null
                val type=if(permanentLegacySpike)BrickType.NORMAL_ONE_HIT else savedType
                val health=if(permanentLegacySpike)1 else v[6].toInt()
                session.bricks+=Brick(v[0].toInt(),Rectangle(v[1].toFloat(),v[2].toFloat(),v[3].toFloat(),v[4].toFloat()),type,health,v[7].toFloat(),v[8].toFloat(),v[9].toFloat(),v[10].toFloat(),
                    groupId=v.getOrNull(11)?.toIntOrNull()?:0,locked=v.getOrNull(12)?.toBooleanStrictOrNull()?: (type==BrickType.LOCKED),ghostVisible=v.getOrNull(13)?.toBooleanStrictOrNull()?:true,
                    initialHealth=if(permanentLegacySpike)1 else v.getOrNull(14)?.toIntOrNull()?:health,timedBombSeconds=v.getOrNull(15)?.takeUnless{it=="n"}?.toFloatOrNull(),
                    temporaryOriginalType=temporaryOriginalType,
                    temporaryOriginalHealth=v.getOrNull(17)?.toIntOrNull()?:0,temporaryOriginalInitialHealth=v.getOrNull(18)?.toIntOrNull()?:0)
            }
            session.fallingPowerUps.clear();prefs.getString("powers","").split(';').filter(String::isNotBlank).forEach { raw -> val v=raw.split(',');runCatching{PowerUpType.valueOf(v[1])}.getOrNull()?.let{type->session.fallingPowerUps+=FallingPowerUp(v[0].toInt(),type,Vector2(v[2].toFloat(),v[3].toFloat()),Vector2(v[4].toFloat(),v[5].toFloat()))} }
            session.powerUps.clear();prefs.getString("timers","").split(';').filter(String::isNotBlank).forEach { raw ->
                val v=raw.split(',');runCatching{PowerUpType.valueOf(v[0])}.getOrNull()?.let { type ->
                    if (PowerUpCatalog.definitions.getValue(type).scope == EffectScope.TIMED) session.powerUps.timers[type]=v[1].toFloat()
                    else session.powerUps.restorePersistent(listOf(type))
                }
            }
            session.powerUps.restorePersistent(prefs.getString("persistent","").split(',').filter(String::isNotBlank).mapNotNull{runCatching{PowerUpType.valueOf(it)}.getOrNull()})
            session.baseBallSize=BallSize.DEFAULT
            session.balls.forEach { ball ->
                ball.baseSize=BallSize.DEFAULT
                ball.size=when {
                    PowerUpType.SHRINK_BALL in session.powerUps -> BallSize.SMALL
                    PowerUpType.MEGA_BALL in session.powerUps -> BallSize.LARGE
                    else -> BallSize.DEFAULT
                }
            }
            session.laserShots.clear();prefs.getString("lasers","").split(';').filter(String::isNotBlank).forEach { raw -> val v=raw.split(',');session.laserShots+=LaserShot(Vector2(v[0].toFloat(),v[1].toFloat()),Vector2(v[2].toFloat(),v[3].toFloat())) }
            session.laserCooldown=prefs.getFloat("laserCooldown",0f);session.nextBallId=prefs.getInteger("nextBallId",(session.balls.maxOfOrNull{it.id}?:0)+1);session.nextPowerUpId=prefs.getInteger("nextPowerUpId",1)
            session.bottomShield=prefs.getBoolean("shield",false);session.explosionExpansion=prefs.getInteger("explosionExpansion",1);session.fallingBricksMode=prefs.getBoolean("fallingBricksMode",false)
            session.expandPaddleStacks=if(version>=8)prefs.getInteger("expandPaddleStacks",0).coerceIn(0,GameSession.MAX_EXPAND_STACKS) else 0
            session.phase=GamePhase.valueOf(prefs.getString("phase",GamePhase.SERVING.name));level to session
        }.getOrElse { clear();null }
    }
    fun clear(){prefs.clear();prefs.flush()}
}
