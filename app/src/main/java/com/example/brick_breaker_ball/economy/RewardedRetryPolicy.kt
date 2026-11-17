package com.example.brick_breaker_ball.economy

/** Monotonic backoff shared by automatic retries and repeated button taps. */
internal class RewardedRetryPolicy {
    @Volatile private var retryAt = 0L
    private var failures = 0

    fun failed(now: Long, noFill: Boolean = false): Long {
        failures = (failures + 1).coerceAtMost(4)
        val delay = maxOf(if (noFill) 60_000L else 15_000L, 15_000L shl (failures - 1))
        retryAt = now + delay
        return delay
    }

    fun remainingMs(now: Long): Long = (retryAt - now).coerceAtLeast(0L)
    fun remainingSeconds(now: Long): Int = ((remainingMs(now) + 999L) / 1_000L).toInt()
    fun reset() { failures = 0; retryAt = 0L }
}
