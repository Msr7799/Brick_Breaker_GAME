package com.example.brick_breaker_ball.gameplay

import kotlin.math.ceil
import kotlin.math.floor

/** Only bonuses actually recorded by a permanent progression system belong here. */
data class VolleyReferencePermanentBonuses(
    val baseDamage: Double = 0.0,
    val overallDamagePercent: Double = 0.0,
    val balls: Int = 0
)

/** Campaign formulas from Level_Select_Scene.gd, separate from stage HP and ball cosmetics. */
object VolleyReferenceCombat {
    private fun equipped(orbs: List<VolleyOrbDefinition>): List<VolleyOrbDefinition> {
        val uniqueFamilies = mutableSetOf<String>()
        return orbs.distinctBy { it.uid ?: "legacy:${it.family}" }
            .filter { it.family !in setOf("LUCK", "SECONDW") || uniqueFamilies.add(it.family) }
            .take(8)
    }

    fun baseDamage(playerLevel: Int, orbs: List<VolleyOrbDefinition>,
                   permanent: VolleyReferencePermanentBonuses = VolleyReferencePermanentBonuses()): Int {
        val level = playerLevel.coerceAtLeast(1)
        val items = equipped(orbs)
        val flat = items.sumOf { item ->
            item.referenceStat("damage").toDouble() + item.referenceStat("additional_damage").toDouble() +
                if (item.referenceStat("additional_damage_percent") > 0.0)
                    ceil(level.toDouble() * item.referenceStat("additional_damage_percent") / 100.0) else 0.0
        }
        val base = floor(1.0 + flat + permanent.baseDamage).coerceAtLeast(1.0)
        // The recovered rules floor twice; merging the percentages changes small hits.
        val levelDamagePercent = (level / 10).toDouble() * 10.0
        val withLevel = floor(base * (1.0 + levelDamagePercent / 100.0))
        val overall = items.sumOf { it.referenceStat("orb_overall_base_damage").toDouble() } +
            permanent.overallDamagePercent
        return floor(withLevel * (1.0 + overall / 100.0)).toInt().coerceAtLeast(1)
    }

    fun startingBalls(playerLevel: Int, orbs: List<VolleyOrbDefinition>,
                      permanent: VolleyReferencePermanentBonuses = VolleyReferencePermanentBonuses()): Int {
        val base = (10.0 + equipped(orbs).sumOf { it.referenceStat("increased_ball_count").toDouble() })
            .coerceAtLeast(1.0)
        val levelBonus = (playerLevel.coerceAtLeast(1) / 10).coerceAtMost(10)
        return floor(base + levelBonus + permanent.balls.coerceIn(0, 15)).toInt().coerceAtLeast(1)
    }
}
