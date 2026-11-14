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

    // The source game raises the player's attack through upgrades. This Volley port has no
    // equivalent persistent RPG attack stat, so keep a bounded hit budget per imported brick.
    fun ballAttack(enemyLevel: Int, initialHealth: Int): Int =
        maxOf(1, enemyLevel / 4, ceil(initialHealth / 30.0).toInt())

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
