/*
 * المسار: app/src/main/java/com/example/brick_breaker_ball/cosmetics/BallAbilities.kt
 * الوظيفة: قدرات الكرات المختارة ودرجاتها.
 * الحسابات: احتساب المضاعفات والضرر وفق الكرة.
 */

package com.example.brick_breaker_ball.cosmetics

/**
 * Passive gameplay identities for the individual cosmetic balls.
 *
 * Seven readable mechanics are tuned per sprite. A small mastery score percentage is unique within
 * each mechanic/trigger combination, so every ball has an individual gameplay profile.
 */
enum class BallAbilityKind {
    IMPACT_CORE,
    INFERNO_BURST,
    ARC_CHAIN,
    ARMOR_BREAKER,
    VOID_PHASE,
    FORTUNE_CORE,
    MOMENTUM_CORE
}

data class BallAbilityProfile(
    val kind: BallAbilityKind,
    val title: String,
    val description: String,
    val tier: Int,
    val triggerEveryHits: Int = 0,
    val scoreBonusPercent: Int = 0,
    val bonusDamage: Int = 0,
    val chainTargets: Int = 0,
    val fortuneEveryBreaks: Int = 0,
    val fortuneBonusScore: Int = 0,
    val speedMultiplier: Float = 1f,
    val masteryScorePercent: Int = 0,
    val startingBalls: Int = 0,
    val baseHitDamage: Int = 1,
    val shotgunAvailable: Boolean = false,
    val openingBreakerEveryVolleys: Int = 0
) {
    val tierLabel: String get() = "TIER $tier"

    /** Only Momentum converts a small part of volley size into attack. */
    val ballsPerDamage: Int get() = 60

    fun hitDamageForVolley(ballCount: Int): Int = baseHitDamage +
        if (kind == BallAbilityKind.MOMENTUM_CORE) ballCount.coerceIn(0, 160) / 60 else 0

    fun shortStat(): String = "+$startingBalls BALLS  |  ${baseHitDamage} BASE HIT"

    fun specialStat(): String =
        when (kind) {
            BallAbilityKind.IMPACT_CORE -> "+$scoreBonusPercent% SCORE"
            BallAbilityKind.INFERNO_BURST -> "FIRE / $triggerEveryHits HITS"
            BallAbilityKind.ARC_CHAIN -> "CHAIN / $triggerEveryHits HITS"
            BallAbilityKind.ARMOR_BREAKER -> "+$bonusDamage ARMOR DMG / $triggerEveryHits HITS"
            BallAbilityKind.VOID_PHASE -> "+$bonusDamage PHASE HIT / $triggerEveryHits HITS"
            BallAbilityKind.FORTUNE_CORE -> "+$fortuneBonusScore / $fortuneEveryBreaks BREAKS"
            BallAbilityKind.MOMENTUM_CORE -> "+1 HIT / 60 BALLS"
        }
}

object BallAbilityCatalog {
    private val numberPattern = Regex("^(\\d{2,3})_")

    val default: BallAbilityProfile get() = profileForSprite(VolleyBallSelection.STARTER_SPRITE)

    fun ballNumber(spriteName: String): Int = if (spriteName.startsWith("new_ball_")) {
        106 + (spriteName.removePrefix("new_ball_").toIntOrNull() ?: 0)
    } else {
        numberPattern.find(spriteName)?.groupValues?.getOrNull(1)?.toIntOrNull() ?: 1
    }

    fun profileForBall(ball: BallSpriteDefinition): BallAbilityProfile = profileForSprite(ball.spriteName)

    fun profileForSprite(spriteName: String): BallAbilityProfile {
        val number = ballNumber(spriteName).coerceIn(1, 165)
        val tier = when (BallCollectionCatalog.offer(spriteName).rarity) {
            BallRarity.NORMAL -> 1
            BallRarity.RARE -> 2
            BallRarity.LEGENDARY -> 3
            BallRarity.EPIC -> 4
        }
        val (damage, extraBalls) = BallCollectionCatalog.power(spriteName)
        return profileForArchetype((number - 1) % 7, tier, (number - 1) / 7).copy(
            baseHitDamage = damage,
            startingBalls = extraBalls
        )
    }

    fun profile(kind: BallAbilityKind, tier: Int): BallAbilityProfile {
        val archetype = when (kind) {
            BallAbilityKind.IMPACT_CORE -> 0
            BallAbilityKind.INFERNO_BURST -> 1
            BallAbilityKind.ARC_CHAIN -> 2
            BallAbilityKind.ARMOR_BREAKER -> 3
            BallAbilityKind.VOID_PHASE -> 4
            BallAbilityKind.FORTUNE_CORE -> 5
            BallAbilityKind.MOMENTUM_CORE -> 6
        }
        return profileForArchetype(archetype, tier.coerceIn(1, 5))
    }

    private fun profileForArchetype(archetype: Int, tier: Int, variant: Int = 0): BallAbilityProfile {
        val cadence = variant / 6
        val mastery = 1 + variant
        val profile = when (archetype) {
            0 -> {
                val bonus = 8 + (tier - 1) * 4 + cadence
                BallAbilityProfile(
                    kind = BallAbilityKind.IMPACT_CORE,
                    title = "IMPACT CORE",
                    description = "Every brick destroyed by this ball awards $bonus% bonus score.",
                    tier = tier,
                    scoreBonusPercent = bonus,
                    masteryScorePercent = mastery
                )
            }

            1 -> {
                val every = (9 - tier + cadence).coerceAtLeast(4)
                BallAbilityProfile(
                    kind = BallAbilityKind.INFERNO_BURST,
                    title = "INFERNO BURST",
                    description = "Every $every direct brick hits becomes a charged fire hit that can splash nearby bricks.",
                    tier = tier,
                    triggerEveryHits = every,
                    masteryScorePercent = mastery
                )
            }

            2 -> {
                val every = (10 - tier + cadence).coerceAtLeast(5)
                BallAbilityProfile(
                    kind = BallAbilityKind.ARC_CHAIN,
                    title = "ARC CHAIN",
                    description = "Every $every direct brick hits arcs one bonus hit into a nearby breakable brick.",
                    tier = tier,
                    triggerEveryHits = every,
                    chainTargets = 1,
                    masteryScorePercent = mastery
                )
            }

            3 -> {
                val every = (8 - tier + cadence).coerceAtLeast(4)
                BallAbilityProfile(
                    kind = BallAbilityKind.ARMOR_BREAKER,
                    title = "ARMOR BREAKER",
                    description = "Every $every direct brick hits deals $tier extra damage to the struck breakable brick.",
                    tier = tier,
                    triggerEveryHits = every,
                    bonusDamage = tier,
                    masteryScorePercent = mastery
                )
            }

            4 -> {
                val every = (13 - tier + cadence).coerceAtLeast(7)
                BallAbilityProfile(
                    kind = BallAbilityKind.VOID_PHASE,
                    title = "VOID PHASE",
                    description = "Every $every direct hits adds ${tier * 2} base damage to the struck brick.",
                    tier = tier,
                    triggerEveryHits = every,
                    bonusDamage = tier * 2,
                    masteryScorePercent = mastery
                )
            }

            5 -> {
                val every = (9 - tier + cadence).coerceAtLeast(4)
                val bonus = 100 + tier * 75 + cadence * 10
                BallAbilityProfile(
                    kind = BallAbilityKind.FORTUNE_CORE,
                    title = "FORTUNE CORE",
                    description = "Every $every bricks destroyed grants an additional $bonus score.",
                    tier = tier,
                    fortuneEveryBreaks = every,
                    fortuneBonusScore = bonus,
                    masteryScorePercent = mastery
                )
            }

            else -> {
                BallAbilityProfile(
                    kind = BallAbilityKind.MOMENTUM_CORE,
                    title = "MOMENTUM CORE",
                    description = "Adds one hit damage for every 60 balls in the volley without changing ball speed.",
                    tier = tier,
                    masteryScorePercent = mastery
                )
            }
        }
        val rarityTier = tier.coerceIn(1, 4)
        return profile.copy(
            startingBalls = (rarityTier - 1) * 4,
            baseHitDamage = rarityTier,
            shotgunAvailable = rarityTier >= 3 && archetype in setOf(1, 2, 3, 4),
            openingBreakerEveryVolleys = if (rarityTier >= 3 && archetype in setOf(1, 4)) 3 else 0
        )
    }
}
