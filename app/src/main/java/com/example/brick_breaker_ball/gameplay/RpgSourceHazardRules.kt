package com.example.brick_breaker_ball.gameplay

/**
 * Exact timed source mechanics from recovered RPG Godot scripts:
 * - Enemies/Spike_Traps/enemy_spike_trap_2x_2.gd
 * - Enemies/Vortex Rock1, Ice1 and Black1 GDScript variants
 *
 * Timers advance in real foreground seconds, including while aiming, and do
 * not speed up during the optional fast-forward collision simulation.
 * No new snapshot fields, stage IDs, or asset licenses are required.
 */
internal object RpgSourceHazardRules {
    const val SPIKE_REARM_SECONDS = 10f // campaign; endless source is 2s, not imported here
    const val SPIKE_START_DELAY_SECONDS = .1f // Godot _ready awaits 0.1 seconds
    const val SPIKE_REVEAL_SECONDS = .2f
    const val SPIKE_HIDE_SECONDS = .2f
    const val VORTEX_ABSORB_SECONDS = .4f

    data class VortexTiming(val activeSeconds: Float, val inactiveSeconds: Float) {
        val period: Float get() = activeSeconds + inactiveSeconds
    }

    fun vortexTiming(scene: String?): VortexTiming = when {
        scene?.contains("/Black1_vortex/") == true -> VortexTiming(4f, 2.5f)
        scene?.contains("/Ice1_Vortex/") == true -> VortexTiming(3f, 3f)
        else -> VortexTiming(2f, 4f) // Rock1_Vortex (the source's default)
    }

    fun vortexActive(scene: String?, clockSeconds: Float): Boolean {
        val cycle = vortexTiming(scene)
        val current = ((clockSeconds % cycle.period) + cycle.period) % cycle.period
        return current < cycle.activeSeconds
    }

    fun spikeArmed(secondsRemaining: Float, elapsedSeconds: Float): Boolean =
        elapsedSeconds >= SPIKE_START_DELAY_SECONDS && secondsRemaining <= 0f

    /** Godot animates hiding and reappearing over 0.2s each, while retaining an immutable hitbox. */
    fun spikeVisualStrength(secondsRemaining: Float, elapsedSeconds: Float): Float {
        if (elapsedSeconds < SPIKE_START_DELAY_SECONDS) return 0f
        if (secondsRemaining <= 0f)
            return ((elapsedSeconds - SPIKE_START_DELAY_SECONDS) /
                SPIKE_REVEAL_SECONDS).coerceIn(0f, 1f).takeIf {
                    elapsedSeconds <= SPIKE_START_DELAY_SECONDS + SPIKE_REVEAL_SECONDS
                } ?: 1f
        if (secondsRemaining > SPIKE_REARM_SECONDS - SPIKE_HIDE_SECONDS)
            return ((secondsRemaining - (SPIKE_REARM_SECONDS - SPIKE_HIDE_SECONDS)) /
                SPIKE_HIDE_SECONDS).coerceIn(0f, 1f)
        if (secondsRemaining < SPIKE_REVEAL_SECONDS)
            return (1f - secondsRemaining / SPIKE_REVEAL_SECONDS).coerceIn(0f, 1f)
        return 0f
    }
}
