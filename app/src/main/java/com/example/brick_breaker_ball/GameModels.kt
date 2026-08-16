package com.example.brick_breaker_ball

import com.badlogic.gdx.math.Rectangle
import com.badlogic.gdx.math.Vector2

enum class GamePhase { LOADING, READY, SERVING, PLAYING, RESOLVING, LEVEL_COMPLETE, GAME_OVER, PAUSED }
enum class BallElement { NORMAL, FIRE, EXPLOSIVE }
enum class BallCollisionMode { NORMAL, PIERCING, GHOST }
enum class BallSize(val radius: Float) {
    SMALL(16f), DEFAULT(22f), LARGE(30f);
    fun smaller() = entries[(ordinal - 1).coerceAtLeast(0)]
    fun larger() = entries[(ordinal + 1).coerceAtMost(entries.lastIndex)]
    companion object { fun fromRadius(radius: Float) = when { radius < 14f -> SMALL; radius > 19f -> LARGE; else -> DEFAULT } }
}
enum class PaddleMode { NORMAL, EXPANDED, SHRUNK, STICKY, LASER, MAGNETIC, SHIELDED, CURSED }
enum class BrickType(val maxHealth: Int, val breakable: Boolean = true) {
    NORMAL_ONE_HIT(1), ARMORED_TWO_HIT(2), ARMORED_THREE_HIT(3), INDESTRUCTIBLE(Int.MAX_VALUE, false),
    GLASS(1), EXPLOSIVE(1), POWERUP_CARRIER(1), MOVING_HORIZONTAL(1), MOVING_VERTICAL(1),
    REGENERATING(2), GHOST(1), SWITCH(1), LOCKED(2), KEY_BRICK(1), CHAIN_BRICK(1), BOSS_CORE(5),
    ROUGH_STONE(1), LIGHTNING_SPEED_PASS_THROUGH(1), ELECTRIC_WHITE(2), CRYSTAL_BLUE(2), CRYSTAL_RED(2),
    CRYSTAL_PURPLE(2), STONE_GRAY(2), BLACK_HOLE_TELEPORTER(Int.MAX_VALUE, false), RANDOM_INVENTORY_POWERUP(1),
    CRYSTAL_CYAN(2), ARMORED_DARK(2), CRYSTAL_PINK(2), CRYSTAL_ORANGE(2), CRYSTAL_GREEN(2),
    SPIKED_HAZARD(1, false), TRANSPARENT_SLOW_PASS_THROUGH(1)
}
enum class DamageStage { INTACT, CRACKED, CRITICAL, BROKEN }

data class Paddle(
    var x: Float = 450f,
    val y: Float = 128f,
    var width: Float = 244f,
    var targetWidth: Float = 244f,
    val height: Float = 42f,
    var velocityX: Float = 0f,
    var mode: PaddleMode = PaddleMode.NORMAL,
) {
    val bounds get() = Rectangle(x - width / 2f, y - height / 2f, width, height)
    fun animate(dt: Float) { width += (targetWidth - width) * (dt * 14f).coerceAtMost(1f) }
}

data class Ball(
    val id: Int,
    val position: Vector2 = Vector2(),
    val previousPosition: Vector2 = Vector2(),
    val velocity: Vector2 = Vector2(),
    var size: BallSize = BallSize.DEFAULT,
    var baseSize: BallSize = BallSize.DEFAULT,
    var cosmeticGroupName: String = CosmeticDefaults.BALL_GROUP,
    var cosmeticSpriteName: String = CosmeticDefaults.BALL_SPRITE,
    var element: BallElement = BallElement.NORMAL,
    var collisionMode: BallCollisionMode = BallCollisionMode.NORMAL,
    var baseSpeed: Float = 0f,
    var stuckOffset: Float? = null,
    var lastBrickId: Int = -1,
) {
    val radius get() = size.radius
    val visualDiameter get() = radius * 2f
}

data class Brick(
    val id: Int,
    val bounds: Rectangle,
    var type: BrickType,
    var health: Int = type.maxHealth,
    val hue: Float,
    var originX: Float = bounds.x,
    var originY: Float = bounds.y,
    var age: Float = 0f,
    var groupId: Int = 0,
    var locked: Boolean = type == BrickType.LOCKED,
    var ghostVisible: Boolean = true,
    var initialHealth: Int = health,
    var timedBombSeconds: Float? = null,
    var temporaryOriginalType: BrickType? = null,
    var temporaryOriginalHealth: Int = 0,
    var temporaryOriginalInitialHealth: Int = 0,
) {
    val damageStage: DamageStage get() = when {
        health <= 0 -> DamageStage.BROKEN
        !type.breakable || health >= initialHealth -> DamageStage.INTACT
        health == 1 -> DamageStage.CRITICAL
        else -> DamageStage.CRACKED
    }
}

data class FallingPowerUp(val id: Int, val type: PowerUpType, val position: Vector2, val velocity: Vector2 = Vector2(0f, -175f))
data class LaserShot(val position: Vector2, val previousPosition: Vector2 = Vector2(position), var alive: Boolean = true)

sealed interface GameplayEvent {
    data class Feedback(val text: String, val sound: String = "powerup") : GameplayEvent
    data object Explosion : GameplayEvent
    data object LaserFired : GameplayEvent
    data object LaserHit : GameplayEvent
    data object FallingWarning : GameplayEvent
}

class FixedStepLoop(private val hz: Int = 120, private val maxSteps: Int = 8) {
    val stepSeconds = 1f / hz
    private var accumulator = 0f
    fun advance(frameSeconds: Float, update: (Float) -> Unit): Float {
        accumulator += frameSeconds.coerceIn(0f, .25f)
        var steps = 0
        while (accumulator >= stepSeconds && steps < maxSteps) {
            update(stepSeconds); accumulator -= stepSeconds; steps++
        }
        if (steps == maxSteps) accumulator = accumulator.coerceAtMost(stepSeconds)
        return accumulator / stepSeconds
    }
}
