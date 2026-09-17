/*
 * ملاحظات صيانة الملف:
 * المسار: app/src/main/java/com/example/brick_breaker_ball/GameModels.kt
 * المؤلف: mohamed alromaihi
 * الدوال الموجودة: `smaller`، `larger`، `fromRadius`، `animate`، `advance`
 */

package com.example.brick_breaker_ball

import com.badlogic.gdx.math.Rectangle
import com.badlogic.gdx.math.Vector2

enum class GamePhase { LOADING, READY, SERVING, PLAYING, RESOLVING, LEVEL_COMPLETE, GAME_OVER, PAUSED }
enum class BallElement { NORMAL, FIRE, EXPLOSIVE }
enum class BallCollisionMode { NORMAL, PIERCING, GHOST }
enum class BallSize(val radius: Float) {
    // Customization still exposes SMALL / DEFAULT / LARGE. MEGA is a gameplay-only
    // size used by the MEGA_BALL power-up so the cosmetic LARGE option stays unchanged.
    SMALL(12f),
    DEFAULT(22f),
    MEGA(GameplayTuning.MEGA_BALL_RADIUS),
    LARGE(44f);

    /** Moved one customization step smaller; MEGA is treated as temporary gameplay state. */
    fun smaller() = when (this) {
        SMALL -> SMALL
        DEFAULT -> SMALL
        MEGA -> DEFAULT
        LARGE -> DEFAULT
    }

    /** Moved one customization step larger without exposing MEGA as a selectable size. */
    fun larger() = when (this) {
        SMALL -> DEFAULT
        DEFAULT -> LARGE
        MEGA -> LARGE
        LARGE -> LARGE
    }

    /** Size used specifically by the MEGA_BALL power-up. */
    fun megaBoosted() = if (this == LARGE) LARGE else MEGA

    /** ملاحظة صيانة: الدالة `fromRadius` تحوّل البيانات أو تبني المعرّف المتوافق مع بقية النظام؛ راجع استدعاءاتها واختباراتها قبل تعديلها. */
    companion object {
        fun fromRadius(radius: Float) = when {
            radius < 17f -> SMALL
            radius < 29f -> DEFAULT
            radius < 40f -> MEGA
            else -> LARGE
        }
    }
}
enum class PaddleMode { NORMAL, EXPANDED, SHRUNK, STICKY, LASER, MAGNETIC, SHIELDED, CURSED }
enum class BrickType(val maxHealth: Int, val breakable: Boolean = true) {
    NORMAL_ONE_HIT(1),
    ARMORED_TWO_HIT(2),
    ARMORED_THREE_HIT(3),
    INDESTRUCTIBLE(Int.MAX_VALUE, false),
    GLASS(1),
    EXPLOSIVE(1),
    POWERUP_CARRIER(1),
    MOVING_HORIZONTAL(1),
    MOVING_VERTICAL(1),
    REGENERATING(2),
    GHOST(1),
    SWITCH(1),
    LOCKED(2),
    KEY_BRICK(1),
    CHAIN_BRICK(1),
    BOSS_CORE(5),

    // The detailed sprite map says rough stone breaks on contact and kicks the ball
    // away at an irregular angle. The two transparent field bricks are consumed
    // when the ball passes through them.
    ROUGH_STONE(1),
    LIGHTNING_SPEED_PASS_THROUGH(1),
    ELECTRIC_WHITE(2),
    CRYSTAL_BLUE(2),
    CRYSTAL_RED(2),
    CRYSTAL_PURPLE(2),
    STONE_GRAY(2),
    BLACK_HOLE_TELEPORTER(Int.MAX_VALUE, false),
    RANDOM_INVENTORY_POWERUP(1),
    CRYSTAL_MAROON(2),
    ARMORED_DARK(2),
    CRYSTAL_PINK(2),
    CRYSTAL_ORANGE(2),
    CRYSTAL_GREEN(2),
    SPIKED_HAZARD(1, false),
    TRANSPARENT_SLOW_PASS_THROUGH(1)
}
enum class DamageStage { INTACT, CRACKED, CRITICAL, BROKEN }

data class Paddle(
    var x: Float = 450f,
    val y: Float = 128f,
    var width: Float = 244f,
    var targetWidth: Float = 244f,
    val height: Float = 42f,
    var velocityX: Float = 0f,
    var mode: PaddleMode = PaddleMode.NORMAL
) {
    val bounds get() = Rectangle(x - width / 2f, y - height / 2f, width, height)

    /** ملاحظة صيانة: الدالة `animate` تحدّث الحالة المتغيرة خلال دورة التشغيل أو المحاكاة؛ راجع استدعاءاتها واختباراتها قبل تعديلها. */
    fun animate(dt: Float) {
        width += (targetWidth - width) * (dt * 14f).coerceAtMost(1f)
    }
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
    var abilityFireCharge: Boolean = false
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
    var temporaryOriginalInitialHealth: Int = 0
) {
    val damageStage: DamageStage get() = when {
        health <= 0 -> DamageStage.BROKEN
        !type.breakable || health >= initialHealth -> DamageStage.INTACT
        health == 1 -> DamageStage.CRITICAL
        else -> DamageStage.CRACKED
    }
}

data class FallingPowerUp(val id: Int, val type: PowerUpType, val position: Vector2, val velocity: Vector2 = Vector2(0f, GameplayTuning.POWERUP_FALL_SPEED))
data class LaserShot(
    val position: Vector2,
    val previousPosition: Vector2 = Vector2(position),
    var alive: Boolean = true,
    val fire: Boolean = false,
    val piercing: Boolean = false,
    val hitBrickIds: MutableSet<Int> = mutableSetOf()
)

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

    /** ملاحظة صيانة: الدالة `advance` تحدّث الحالة المتغيرة خلال دورة التشغيل أو المحاكاة؛ راجع استدعاءاتها واختباراتها قبل تعديلها. */
    fun advance(frameSeconds: Float, update: (Float) -> Unit): Float {
        accumulator += frameSeconds.coerceIn(0f, .25f)
        var steps = 0
        while (accumulator >= stepSeconds && steps < maxSteps) {
            update(stepSeconds)
            accumulator -= stepSeconds
            steps++
        }
        if (steps == maxSteps) accumulator = accumulator.coerceAtMost(stepSeconds)
        return accumulator / stepSeconds
    }
}
