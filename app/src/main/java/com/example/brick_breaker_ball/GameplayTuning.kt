/*
 * ملاحظات صيانة الملف:
 * المسار: app/src/main/java/com/example/brick_breaker_ball/GameplayTuning.kt
 * المؤلف: mohamed alromaihi
 * الدوال الموجودة: لا توجد دوال مسماة في هذا الملف.
 */

package com.example.brick_breaker_ball

/** Central balance values for mechanics described by the detailed sprite map without numeric values. */
object GameplayTuning {
    const val FIRE_BLAST_RADIUS = 145f
    const val LARGE_FIRE_BLAST_RADIUS = 205f
    const val LARGE_FIRE_MAX_TARGETS = 4
    const val TIMED_BOMB_DURATION = 2.5f
    const val TIMED_BOMB_RADIUS = 155f
    const val LIGHTNING_SPEED_MULTIPLIER = 1.18f
    const val TRANSPARENT_SLOW_MULTIPLIER = .82f

    /** Falling power-up balance. */
    const val POWERUP_FALL_SPEED = -135f
    const val POWERUP_VISUAL_SIZE = 104f
    const val POWERUP_PICKUP_WIDTH = 88f
    const val POWERUP_PICKUP_HEIGHT = 70f
    const val POWERUP_DESPAWN_Y = -36f
    const val POWERUP_PADDLE_CLEARANCE = 36f
    const val TIMED_POWERUP_DURATION = 24f

    /** Mega-ball gameplay radius. Kept separate from the player's LARGE cosmetic size. */
    const val MEGA_BALL_RADIUS = 35f

    /** Attraction field measured upward from the paddle center. Stronger than the old assist so the talisman feels valuable. */
    const val MAGNET_RANGE = 650f
    const val MAGNET_HORIZONTAL_RANGE = 720f
    const val MAGNET_STEER_RATE = 19f
    const val MAGNET_MAX_BLEND = .82f

    /** Campaign talisman cadence: more frequent drops with a bounded per-level budget and a pity window. */
    const val POWERUP_DROP_BUDGET = 24
    const val POWERUP_DROP_MIN_INTERVAL = 4f
    const val POWERUP_DROP_PITY_SECONDS = 14f
    const val POWERUP_DROP_PITY_CHANCE = .78f

    /** Falling-talisman magnet: stronger pull with damping so capsules lock onto the paddle without wild oscillation. */
    const val POWERUP_MAGNET_PULL = 8.5f
    const val POWERUP_MAGNET_DAMPING = 3.6f
    const val POWERUP_MAGNET_MAX_HORIZONTAL_SPEED = 720f
    const val SLOW_BALL_MULTIPLIER = .62f
    const val LASER_COOLDOWN = .32f
    const val LASER_AUTO_CHARGE_COOLDOWN = .16f
    const val DUAL_PADDLE_OFFSET_Y = 105f
    const val TELEPORT_ATTEMPTS = 24
    const val SPIKED_HAZARD_KILLS_BALL = true
    const val TEMPORARY_SPIKE_COUNT = 3
}
