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
    /** Attraction field measured upward from the paddle center. */
    const val MAGNET_RANGE = 390f
    const val MAGNET_HORIZONTAL_RANGE = 430f
    const val MAGNET_STRENGTH = 5.2f
    const val SLOW_BALL_MULTIPLIER = .62f
    const val LASER_COOLDOWN = .32f
    const val LASER_AUTO_CHARGE_COOLDOWN = .16f
    const val DUAL_PADDLE_OFFSET_Y = 105f
    const val TELEPORT_ATTEMPTS = 24
    const val SPIKED_HAZARD_KILLS_BALL = true
    const val TEMPORARY_SPIKE_COUNT = 3
}
