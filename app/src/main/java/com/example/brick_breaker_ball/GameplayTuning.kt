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
    const val POWERUP_DESPAWN_Y = -36f
    const val POWERUP_PADDLE_CLEARANCE = 36f
    const val TIMED_POWERUP_DURATION = 24f

    /** Mega-ball gameplay radius. Kept separate from the player's LARGE cosmetic size. */
    const val MEGA_BALL_RADIUS = 35f

    /** Attraction field measured upward from the paddle center. */
    const val MAGNET_RANGE = 540f
    const val MAGNET_HORIZONTAL_RANGE = 600f
    const val MAGNET_STEER_RATE = 13.5f
    const val MAGNET_MAX_BLEND = .64f

    /** Falling-talisman magnet: stronger pull with damping so capsules lock onto the paddle without wild oscillation. */
    const val POWERUP_MAGNET_PULL = 6.5f
    const val POWERUP_MAGNET_DAMPING = 3.2f
    const val POWERUP_MAGNET_MAX_HORIZONTAL_SPEED = 560f
    const val SLOW_BALL_MULTIPLIER = .62f
    const val LASER_COOLDOWN = .32f
    const val LASER_AUTO_CHARGE_COOLDOWN = .16f
    const val DUAL_PADDLE_OFFSET_Y = 105f
    const val TELEPORT_ATTEMPTS = 24
    const val SPIKED_HAZARD_KILLS_BALL = true
    const val TEMPORARY_SPIKE_COUNT = 3
}
