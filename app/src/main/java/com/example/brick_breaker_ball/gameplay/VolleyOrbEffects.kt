/*
 * يطبّق خصائص تمائم Volley الرقمية مع حصر التفعيل في اصطدام الكرة المباشر.
 */
package com.example.brick_breaker_ball.gameplay

import java.util.Random
import kotlin.math.ceil
import kotlin.math.max

data class VolleyOrbHit(
    val damage: Int,
    val splashDamage: Int = 0,
    val splashRadiusBonus: Int = 0,
    val lightningDamage: Int = 0,
    val staticFieldPercent: Float = 0f,
    val hammerDamage: Int = 0,
    val hammerPierceChance: Float = 0f,
    val reaperDamage: Int = 0,
    val poisonDamage: Int = 0,
    val poisonInterval: Float = 1f,
    val splitDamage: Int = 0,
    val splitPathPercent: Float = 0f,
    val empowerBonus: Int = 0,
    val reaperWrathChance: Float = 0f,
    val reaperWrathDuration: Float = 0f,
    val reaperWrathInterval: Float = .5f,
    val critical: Boolean = false
)

/** Distinct equipped copies stack; secondary hits never roll another Orb effect. */
class VolleyOrbEffects(
    equipped: List<VolleyOrbDefinition>,
    private val random: Random,
    private val stageLevel: Int = 1,
    private val extremeMode: Boolean = false,
    private val combatBoost: Float = 1f,
    private val referenceRules: Boolean = false,
    private val playerLevel: Int = 1
) {
    val orbs: List<VolleyOrbDefinition> = run {
        val uniqueFamilies = mutableSetOf<String>()
        equipped.distinctBy { it.uid ?: "legacy:${it.family}" }
            .filter { it.family !in setOf("LUCK", "SECONDW") || uniqueFamilies.add(it.family) }
            .take(8)
    }
    private fun total(name: String): Float = orbs.sumOf { it.stat(name).toDouble() }.toFloat()
    private fun referenceTotal(name: String): Double = orbs.sumOf { it.referenceStat(name) }
    constructor(orb: VolleyOrbDefinition?, random: Random, stageLevel: Int = 1) :
        this(listOfNotNull(orb), random, stageLevel)

    fun startingBalls(): Int = if (referenceRules)
        VolleyReferenceCombat.startingBalls(playerLevel, orbs) -
            VolleyReferenceCombat.startingBalls(playerLevel, emptyList())
    else orbs.sumOf { ceil(it.stat("increased_ball_count").coerceAtLeast(0f) * combatBoost).toInt() }

    fun rewardMultiplier(): Float = 1f + total("goldfind") / 100f

    fun dropChanceMultiplier(): Float = 1f + total("increased_drop_rate") / 100f

    /** A 0.6% base roll; difficulty and Luck are relative multipliers. */
    fun rollsExtraOrbPoint(difficultyDropMultiplier: Float = 1f,
                           sourceBonusRate: Float = 0f, sourceQualityMultiplier: Float = 1f): Boolean =
        random.nextFloat() * 100f < ((.6f + sourceBonusRate).coerceAtMost(100f) *
            difficultyDropMultiplier.coerceAtLeast(0f) * dropChanceMultiplier() *
            sourceQualityMultiplier).coerceAtMost(100f)

    /** RPG items.gd rolls each rarity only after the preceding rarity succeeds. */
    fun rollNaturalOrbRank(sourceQualityMultiplier: Float = 1f): Int {
        // The source's ancient Luck Orb makes Magic the minimum natural drop.
        // items.gd resets Magic to 3.5 after applying quality; Ancient Luck overrides it to 100.
        if (orbs.none { it.family == "LUCK" && it.rank >= 5 } &&
            random.nextFloat() * 100f >= 3.5f) return 0
        if ((!extremeMode && stageLevel <= 30) || random.nextFloat() * 100f >= 2.7f * sourceQualityMultiplier) return 1
        if ((!extremeMode && stageLevel <= 60) || random.nextFloat() * 100f >= 1.3f * sourceQualityMultiplier) return 2
        // The source reserves the further legendary roll for endless mode.
        return 3
    }

    /** Each ball touching a triggered explosion gets its own documented ignition roll. */
    fun rollsIgnition(): Boolean = roll(
        if (referenceRules) referenceTotal("explosive_chance_to_ignite").coerceIn(0.0, 10.0).toFloat()
        else combatChance(orbs.sumOf { it.stat("explosive_chance_to_ignite").toDouble() }.toFloat(), 10f * combatBoost), 100f
    )

    private fun combatChance(chance: Float, cap: Float): Float =
        (chance * combatBoost).coerceIn(0f, cap)

    fun hit(base: Int, @Suppress("UNUSED_PARAMETER") boss: Boolean, totalBalls: Int = 0,
            activeBalls: Int = 0, currentBonusDamage: Int = 0, fireball: Boolean = false): VolleyOrbHit {
        if (referenceRules) return referenceHit(base, totalBalls, activeBalls, currentBonusDamage, fireball)
        if (orbs.isEmpty()) return VolleyOrbHit(base)
        // Percentage on legendary ranks is a percentage of the current level in the item script.
        val rawFlat = orbs.sumOf { item ->
            ceil(stageLevel.coerceAtLeast(1) * item.stat("additional_damage_percent") / 100f).toInt() +
                item.stat("damage").toInt() + item.stat("additional_damage").toInt()
        }
        val flat = ceil(rawFlat * combatBoost).toInt()
        // RPG applies overall base damage as a percentage after flat base damage is assembled.
        val baseDamage = max(1, ceil((base + flat) * (1f + total("orb_overall_base_damage") / 100f)).toInt())
        val warriorPercent = orbs.sumOf { it.stat("ancient_ball_to_bonus_damage").toDouble() }.toFloat()
        val warriorBonus = if (warriorPercent > 0f && activeBalls > 0)
            activeBalls * ceil(baseDamage * warriorPercent / 100f).toInt() else 0
        var damage = baseDamage + warriorBonus
        val empowerCritMultiple = orbs.maxOf { it.stat("empower_multi_to_crit").toInt().coerceAtLeast(1) }
        orbs.forEach { item -> if (roll(combatChance(item.stat("increased_crit_chance"), 100f), 100f)) {
            val critPercent = item.stat("increased_crit_damage") + totalBalls * item.stat("ancient_crit_damage")
            damage = max(damage + 1, ceil(damage * (1f + critPercent / 100f)).toInt())
            damage += currentBonusDamage.coerceAtLeast(0) * (empowerCritMultiple - 1)
        } }
        val empowerBonus = orbs.sumOf { item ->
            if (roll(combatChance(item.stat("empower_chance"), 3f * combatBoost), 100f))
                max(1, ceil(baseDamage * item.stat("empower_amount") / 100f).toInt()) else 0
        }
        fun procDamage(chance: String, percent: String, multiplier: String,
                       cap: Float = 10f): Int {
            if (!roll(combatChance(total(chance), cap), 100f)) return 0
            return max(1, ceil(baseDamage * total(percent) / 100f *
                (1f + total(multiplier) / 100f)).toInt())
        }
        var staticFieldPercent = 0f
        val lightningDamage = if (!roll(combatChance(total("lightning_bolt_chance"),
                10f + total("increased_max_lightning_bolt_chance")), 100f)) 0
            else if (roll(combatChance(total("ancient_lightning_static_field_chance"), 100f), 100f)) {
                staticFieldPercent = total("ancient_lightning_static_field_damage")
                0
            } else max(1, ceil(baseDamage * total("lightning_bolt_damage") / 100f *
                (1f + total("lightning_bolt_damage_multiplier") / 100f)).toInt())
        val unlitSplash = procDamage("explosive_chance", "explosive_damage",
            "explosive_damage_multiplier", 10f + total("increased_max_explosive_chance"))
        val reaperDamage = orbs.sumOf { item ->
            val cap = 10f + item.stat("increased_max_reaper_chance")
            if (!roll(combatChance(item.stat("reaper_chance"), cap), 100f)) 0
            else max(1, ceil(baseDamage * item.stat("reaper_damage") / 100f *
                (1f + item.stat("reaper_damage_multiplier") / 100f)).toInt()) * 2
        }
        val fireballBonus = if (fireball) orbs.sumOf { it.stat("fireball_damage_bonus").toDouble() }
            .toFloat().times(combatBoost).coerceAtMost(50f * combatBoost) else 0f
        return VolleyOrbHit(
            damage = damage,
            splashDamage = if (unlitSplash > 0) ceil(unlitSplash * (1f + fireballBonus / 100f)).toInt() else 0,
            splashRadiusBonus = orbs.maxOf { it.stat("explosive_radius_increase").toInt().coerceAtLeast(0) },
            lightningDamage = lightningDamage,
            staticFieldPercent = staticFieldPercent,
            hammerDamage = procDamage("hammer_chance", "hammer_damage",
                "hammer_damage_multiplier", 10f + total("increased_max_hammer_chance")),
            hammerPierceChance = when {
                orbs.any { it.family == "HAMMER" && it.rank >= 5 } -> 100f
                orbs.any { it.family == "HAMMER" && it.rank >= 4 } -> 50f * combatBoost
                else -> 0f
            },
            reaperDamage = reaperDamage,
            poisonDamage = procDamage("poison_chance", "poison_damage_percent_of_total_damage",
                "poison_damage_multiplier", (if (orbs.any { it.family == "PUTRID" }) 8f else 5f) +
                    total("increased_max_poison_chance")),
            poisonInterval = orbs.map { it.stat("poison_tick_interval") }.filter { it > 0f }.minOrNull() ?: 1f,
            splitDamage = procDamage("split_chance", "split_damage",
                "split_damage_multiplier", 10f + total("increased_max_split_chance")),
            splitPathPercent = (orbs.sumOf { it.stat("ancient_split_damage").toDouble() }
                .toFloat() * combatBoost).coerceIn(0f, 100f),
            empowerBonus = empowerBonus,
            reaperWrathChance = if (reaperDamage > 0) combatChance(
                total("reapers_wrath_on_hit_chance") +
                    total("ancient_reapers_wrath_on_hit_chance").coerceAtMost(5f), 100f) else 0f,
            reaperWrathDuration = orbs.maxOf { it.stat("reapers_wrath_duration") }.coerceAtLeast(0f),
            reaperWrathInterval = .5f / (1f + total("reapers_wrath_spin_speed").coerceIn(0f, 25f) / 100f)
        )
    }

    /** base is the already assembled reference attack; never add equipped flat stats twice. */
    private fun referenceHit(base: Int, totalBalls: Int, activeBalls: Int,
                             currentBonusDamage: Int, fireball: Boolean): VolleyOrbHit {
        val attack = base.coerceAtLeast(1).toDouble()
        fun amount(percent: String, multiplier: String): Double =
            attack * referenceTotal(percent).coerceAtLeast(0.0) / 100.0 *
                (1.0 + referenceTotal(multiplier).coerceAtLeast(0.0) / 100.0)
        fun chance(name: String, maximum: Float = 10f, capIncrease: String? = null): Float =
            referenceTotal(name).coerceIn(0.0, (maximum.toDouble() +
                (capIncrease?.let { referenceTotal(it).coerceIn(0.0, 100.0) } ?: 0.0))
                .coerceAtMost(100.0)).toFloat()
        fun proc(chanceName: String, percent: String, multiplier: String,
                 maximum: Float = 10f, capIncrease: String? = null): Int =
            if (roll(chance(chanceName, maximum, capIncrease), 100f))
                ceil(amount(percent, multiplier)).toInt().coerceAtLeast(0) else 0

        // Empower is aggregated and rolled before this collision's normal damage.
        val empower = if (roll(chance("empower_chance", 3f), 100f))
            ceil(attack * referenceTotal("empower_amount").coerceAtLeast(0.0) / 100.0).toInt() else 0
        val warriorPercent = referenceTotal("ancient_ball_to_bonus_damage").coerceIn(0.0, 10.0)
        val warrior = activeBalls.coerceAtLeast(0).toDouble() * ceil(attack * warriorPercent / 100.0)
        val bonus = currentBonusDamage.coerceAtLeast(0).toDouble() + empower + warrior
        val critical = roll(chance("increased_crit_chance", 100f), 100f)
        val damage = if (critical) {
            val percent = referenceTotal("increased_crit_damage").toDouble() +
                totalBalls.coerceAtLeast(0).toDouble() * referenceTotal("ancient_crit_damage")
            // Source crits include round bonus only when the Empower crit multiplier exists.
            ceil(attack * (1.0 + percent / 100.0) +
                bonus * referenceTotal("empower_multi_to_crit").coerceAtLeast(0.0)).toInt()
        } else (attack + bonus).toInt()

        // Preserve source proc order: poison, explosive, lightning, hammer, split, reaper.
        val poison = proc("poison_chance", "poison_damage_percent_of_total_damage",
            "poison_damage_multiplier", 5f, "increased_max_poison_chance")
        val splash = if (roll(chance("explosive_chance", 10f, "increased_max_explosive_chance"), 100f)) {
            val raw = amount("explosive_damage", "explosive_damage_multiplier")
            if (fireball) (raw * (1.0 + referenceTotal("fireball_damage_bonus").coerceIn(0.0, 50.0) / 100.0))
                .toInt().coerceAtLeast(0) else ceil(raw).toInt().coerceAtLeast(0)
        } else 0
        var staticField = 0f
        val lightning = if (roll(chance("lightning_bolt_chance", 10f,
                "increased_max_lightning_bolt_chance"), 100f)) {
            if (roll(chance("ancient_lightning_static_field_chance", 15f), 100f)) {
                staticField = referenceTotal("ancient_lightning_static_field_damage").coerceIn(0.0, 5.0).toFloat()
                0
            } else ceil(amount("lightning_bolt_damage", "lightning_bolt_damage_multiplier")).toInt()
        } else 0
        val hammer = proc("hammer_chance", "hammer_damage", "hammer_damage_multiplier",
            10f, "increased_max_hammer_chance")
        val split = proc("split_chance", "split_damage", "split_damage_multiplier",
            10f, "increased_max_split_chance")
        val reaper = proc("reaper_chance", "reaper_damage", "reaper_damage_multiplier",
            10f, "increased_max_reaper_chance")
        return VolleyOrbHit(
            damage = damage.coerceAtLeast(0),
            splashDamage = splash,
            splashRadiusBonus = referenceTotal("explosive_radius_increase").toInt().coerceIn(0, 1),
            lightningDamage = lightning,
            staticFieldPercent = staticField,
            hammerDamage = hammer,
            hammerPierceChance = when {
                orbs.any { it.family == "HAMMER" && it.rank >= 5 } -> 100f
                orbs.any { it.family == "HAMMER" && it.rank >= 4 } -> 50f
                else -> 0f
            },
            reaperDamage = reaper,
            poisonDamage = poison,
            poisonInterval = (orbs.map { it.referenceStat("poison_tick_interval") }.filter { it > 0.0 }
                .minOrNull() ?: 1.0).toFloat(),
            splitDamage = split,
            splitPathPercent = referenceTotal("ancient_split_damage").coerceIn(0.0, 100.0).toFloat(),
            empowerBonus = empower,
            reaperWrathChance = if (reaper > 0) (referenceTotal("reapers_wrath_on_hit_chance") +
                referenceTotal("ancient_reapers_wrath_on_hit_chance").coerceIn(0.0, 5.0)).coerceIn(0.0, 100.0).toFloat() else 0f,
            reaperWrathDuration = orbs.maxOfOrNull { it.referenceStat("reapers_wrath_duration") }
                ?.coerceAtLeast(0.0)?.toFloat() ?: 0f,
            reaperWrathInterval = (.5 / (1.0 + referenceTotal("reapers_wrath_spin_speed").coerceIn(0.0, 25.0) / 100.0)).toFloat(),
            critical = critical
        )
    }

    fun rollsReaperWrath(chance: Float): Boolean = roll(chance, 100f)
    fun rollsHammerPierce(chance: Float): Boolean = roll(chance, 100f)

    private fun roll(chance: Float, denominator: Float): Boolean =
        chance > 0f && random.nextFloat() * denominator < chance
}
