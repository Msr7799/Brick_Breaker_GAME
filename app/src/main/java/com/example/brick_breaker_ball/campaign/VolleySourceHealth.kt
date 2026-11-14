package com.example.brick_breaker_ball.campaign

import kotlin.math.ceil

/** HP multipliers used by Volley enemies. */
internal object VolleySourceHealth {
    const val BOSS_FIRST_WORLD_HIT_BUDGET = 180
    const val BOSS_EXTRA_HIT_BUDGET_PER_WORLD = 30
    const val BOSS_MINIMUM_DAMAGE_HITS = 12

    fun rawHealth(base: Float, enemyLevel: Int, difficulty: VolleyDifficulty): Double {
        require(enemyLevel in 1..160)
        val multiplier = when (difficulty) {
            VolleyDifficulty.NORMAL -> 8.0 + 2.1 * (enemyLevel - 1)
            VolleyDifficulty.HARD -> (20.0 + 2.1 * (enemyLevel - 1)) * 5.0
            VolleyDifficulty.EXTREME -> (150.0 + (enemyLevel + 499) * 8.75) * 10.0
        }
        return base.toDouble() * multiplier
    }

    fun initialHealth(base: Float, enemyLevel: Int, difficulty: VolleyDifficulty): Int =
        ceil(rawHealth(base, enemyLevel, difficulty)).toInt().coerceAtLeast(1)

    // Attack follows campaign progress, never the health of the brick being struck.
    // A 1,000-HP enemy therefore stays tougher than a 100-HP enemy at the same stage.
    fun ballAttack(enemyLevel: Int, initialHealth: Int): Int {
        require(initialHealth >= 0)
        return 1 + (enemyLevel.coerceIn(1, 160) - 1) / 12
    }

    /** Boss durability grows by world instead of reusing the ordinary 30-hit normalization. */
    fun bossBallAttack(world: Int, initialHealth: Int): Int {
        val budget = BOSS_FIRST_WORLD_HIT_BUDGET +
            (world.coerceIn(1, 9) - 1) * BOSS_EXTRA_HIT_BUDGET_PER_WORLD
        return ceil(initialHealth.toDouble() / budget).toInt().coerceAtLeast(1)
    }

    /** Every hit is limited, not just the first: a second huge attack cannot bypass protection. */
    fun limitBossDamage(amount: Int, initialHealth: Int): Int =
        amount.coerceAtMost((initialHealth / BOSS_MINIMUM_DAMAGE_HITS).coerceAtLeast(1))
}
