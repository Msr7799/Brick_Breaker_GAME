package com.example.brick_breaker_ball.campaign

import kotlin.math.floor
import kotlin.math.pow

/** Separate RPG combat level: neither stage unlocks nor cosmetic strength are player XP. */
object VolleyPlayerProgress {
    internal const val PREFERENCE_PREFIX = "volley_combat_"
    internal const val SCHEMA_KEY = "volley_combat_schema_v1"
    data class State(val level: Int = 1, val experience: Long = 0L)

    /** Positive Godot round(), with enough range for carried experience in long-lived saves. */
    fun requiredExperience(level: Int): Long {
        require(level >= 1)
        return floor(100.0 + level.toDouble().pow(1.8) + level.toDouble() * 4.0 + .5).toLong()
            .coerceAtLeast(1L)
    }

    /** The source checks one level-up per killed enemy, retaining excess XP for later kills. */
    fun award(state: State, amount: Int): State {
        require(state.level >= 1 && state.experience >= 0L && amount >= 0)
        val experience = if (Long.MAX_VALUE - state.experience < amount.toLong()) Long.MAX_VALUE
            else state.experience + amount.toLong()
        val threshold = requiredExperience(state.level)
        return if (state.level < Int.MAX_VALUE && experience >= threshold) {
            State(state.level + 1, experience - threshold)
        } else State(state.level, experience)
    }

    /** Enemies.gd prep_enemies(): the two Normal XP reductions are intentionally sequential. */
    fun enemyExperience(enemyLevel: Int, difficulty: VolleyDifficulty, playerLevel: Int): Int {
        require(enemyLevel in 1..160 && playerLevel >= 1)
        var experience = enemyLevel / 9 + 2
        when (difficulty) {
            VolleyDifficulty.HARD -> experience *= 2
            VolleyDifficulty.EXTREME -> experience = experience * 2 + 30
            VolleyDifficulty.NORMAL -> if (playerLevel.toLong() - enemyLevel >= 30L) {
                experience = experience / 8 + 1
            }
        }
        val gap = playerLevel.toLong() - enemyLevel
        val reduce = when (difficulty) {
            VolleyDifficulty.NORMAL -> gap > 30L
            VolleyDifficulty.HARD -> gap > 100L
            VolleyDifficulty.EXTREME -> gap > 250L
        }
        // The source XP and literal 8 are ints: ceil(xp / 8) receives an already truncated int.
        if (reduce) experience = experience / 8 + 1
        return experience
    }
}
