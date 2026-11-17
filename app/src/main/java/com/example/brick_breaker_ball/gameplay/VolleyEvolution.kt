package com.example.brick_breaker_ball.gameplay

import java.io.Serializable
import java.util.Random

/** Release defaults stay off until the plan's save, free-clear and device gates pass. */
data class VolleyFeatureFlags(
    val bossAbilities: Boolean = false,
    val referenceBallRoles: Boolean = false,
    val launchStyles: Boolean = false
) : Serializable {
    val anyEnabled: Boolean get() = bossAbilities || referenceBallRoles || launchStyles
    companion object { private const val serialVersionUID = 1L }
}

object VolleyProductionFeatures {
    val defaults = VolleyFeatureFlags()
    const val RULES_VERSION = 1
}

/** Same 48-bit algorithm as java.util.Random; state can resume without changing the sequence. */
internal class VolleyRandom(seed: Long) : Random(0L) {
    var savedState: Long = (seed xor MULTIPLIER) and MASK
        private set

    override fun next(bits: Int): Int {
        savedState = (savedState * MULTIPLIER + 11L) and MASK
        return (savedState ushr (48 - bits)).toInt()
    }

    fun restoreState(value: Long) {
        require(value in 0..MASK) { "Invalid Volley RNG state" }
        savedState = value
    }

    companion object {
        private const val MULTIPLIER = 25214903917L
        private const val MASK = (1L shl 48) - 1
    }
}

enum class VolleyLaunchStyle { STRAIGHT, RICOCHET_GUIDE, FOCUSED_VOLLEY }

data class VolleyEvolutionSnapshot(
    val version: Int = VolleyProductionFeatures.RULES_VERSION,
    val features: VolleyFeatureFlags = VolleyFeatureFlags(),
    val boss: VolleyBossAbilityState = VolleyBossAbilityState(),
    val role: VolleyRoleBudget = VolleyRoleBudget(),
    val launchStyle: VolleyLaunchStyle = VolleyLaunchStyle.STRAIGHT
) : Serializable {
    fun requireValid() {
        require(version == VolleyProductionFeatures.RULES_VERSION)
        require(boss.rootRenewals in 0..2 && boss.forecastTurn >= 0 && boss.activeTurn >= 0 &&
            boss.vulnerabilityThroughTurn >= 0)
        require(boss.forecastTargets.size <= 2 && boss.activeTargets.size <= 2 &&
            boss.forecastTargets.all { it > 0 } && boss.activeTargets.all { it > 0 })
        require(role.hits >= 0 && role.directDamage in 0..Int.MAX_VALUE.toLong() &&
            role.spentDamage in 0..(role.directDamage / 20))
    }

    companion object { private const val serialVersionUID = 1L }
}

/** Integer budget: role damage can never exceed five percent of qualifying direct damage. */
data class VolleyRoleBudget(
    val hits: Int = 0,
    val directDamage: Long = 0,
    val spentDamage: Long = 0
) : Serializable {
    fun record(damage: Int): VolleyRoleBudget = copy(
        hits = (hits.toLong() + 1).coerceAtMost(Int.MAX_VALUE.toLong()).toInt(),
        directDamage = (directDamage + damage.coerceAtLeast(0).toLong()).coerceAtMost(Int.MAX_VALUE.toLong())
    )
    fun available(): Int = (directDamage / 20L - spentDamage).coerceIn(0L, Int.MAX_VALUE.toLong()).toInt()
    fun spend(amount: Int): VolleyRoleBudget = copy(spentDamage = spentDamage + amount.coerceIn(0, available()))
    companion object { private const val serialVersionUID = 1L }
}
