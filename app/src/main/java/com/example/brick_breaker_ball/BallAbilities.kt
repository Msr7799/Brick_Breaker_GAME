package com.example.brick_breaker_ball

/**
 * Passive gameplay identities for the 105 cosmetic balls.
 *
 * The numeric prefix in each ball sprite filename is the stable catalog key. The 105 balls are
 * split into five progression tiers (21 balls per tier) and seven readable archetypes. This keeps
 * the collection meaningful without creating 105 unrelated mechanics that would be impossible to
 * balance or explain to players.
 */
enum class BallAbilityKind {
    IMPACT_CORE,
    INFERNO_BURST,
    ARC_CHAIN,
    ARMOR_BREAKER,
    VOID_PHASE,
    FORTUNE_CORE,
    MOMENTUM_CORE,
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
) {
    val tierLabel: String get() = "TIER $tier"

    fun shortStat(): String = when (kind) {
        BallAbilityKind.IMPACT_CORE -> "+$scoreBonusPercent% SCORE"
        BallAbilityKind.INFERNO_BURST -> "FIRE / $triggerEveryHits HITS"
        BallAbilityKind.ARC_CHAIN -> "CHAIN / $triggerEveryHits HITS"
        BallAbilityKind.ARMOR_BREAKER -> "+$bonusDamage ARMOR DMG / $triggerEveryHits HITS"
        BallAbilityKind.VOID_PHASE -> "PHASE / $triggerEveryHits HITS"
        BallAbilityKind.FORTUNE_CORE -> "+$fortuneBonusScore / $fortuneEveryBreaks BREAKS"
        BallAbilityKind.MOMENTUM_CORE -> "+${((speedMultiplier - 1f) * 100f).toInt()}% SPEED"
    }
}

object BallAbilityCatalog {
    private val numberPattern = Regex("^(\\d{2,3})_")

    val default: BallAbilityProfile get() = profileForSprite(CosmeticDefaults.BALL_SPRITE)

    fun ballNumber(spriteName: String): Int = numberPattern.find(spriteName)?.groupValues?.getOrNull(1)?.toIntOrNull() ?: 1

    fun profileForBall(ball: BallSpriteDefinition): BallAbilityProfile = profileForSprite(ball.spriteName)

    fun profileForSprite(spriteName: String): BallAbilityProfile {
        val number = ballNumber(spriteName).coerceIn(1, 105)
        val tier = ((number - 1) / 21 + 1).coerceIn(1, 5)
        return profileForArchetype((number - 1) % 7, tier)
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

    private fun profileForArchetype(archetype: Int, tier: Int): BallAbilityProfile = when (archetype) {
        0 -> {
            val bonus = 4 + (tier - 1) * 2
            BallAbilityProfile(
                kind = BallAbilityKind.IMPACT_CORE,
                title = "IMPACT CORE",
                description = "Every brick destroyed by this ball awards $bonus% bonus score.",
                tier = tier,
                scoreBonusPercent = bonus,
            )
        }

        1 -> {
            val every = (12 - tier).coerceAtLeast(7)
            BallAbilityProfile(
                kind = BallAbilityKind.INFERNO_BURST,
                title = "INFERNO BURST",
                description = "Every $every direct brick hits becomes a charged fire hit that can splash nearby bricks.",
                tier = tier,
                triggerEveryHits = every,
            )
        }

        2 -> {
            val every = (13 - tier).coerceAtLeast(7)
            BallAbilityProfile(
                kind = BallAbilityKind.ARC_CHAIN,
                title = "ARC CHAIN",
                description = "Every $every direct brick hits arcs one bonus hit into a nearby breakable brick.",
                tier = tier,
                triggerEveryHits = every,
                chainTargets = 1,
            )
        }

        3 -> {
            val every = (11 - tier).coerceAtLeast(6)
            BallAbilityProfile(
                kind = BallAbilityKind.ARMOR_BREAKER,
                title = "ARMOR BREAKER",
                description = "Every $every direct brick hits deals one extra damage to the struck breakable brick.",
                tier = tier,
                triggerEveryHits = every,
                bonusDamage = 1,
            )
        }

        4 -> {
            val every = (16 - tier).coerceAtLeast(10)
            BallAbilityProfile(
                kind = BallAbilityKind.VOID_PHASE,
                title = "VOID PHASE",
                description = "Every $every direct brick hits phases through durability and instantly breaks one breakable brick.",
                tier = tier,
                triggerEveryHits = every,
            )
        }

        5 -> {
            val every = (9 - tier).coerceAtLeast(4)
            val bonus = 50 + tier * 25
            BallAbilityProfile(
                kind = BallAbilityKind.FORTUNE_CORE,
                title = "FORTUNE CORE",
                description = "Every $every bricks destroyed grants an additional $bonus score.",
                tier = tier,
                fortuneEveryBreaks = every,
                fortuneBonusScore = bonus,
            )
        }

        else -> {
            val multiplier = 1.02f + (tier - 1) * .01f
            BallAbilityProfile(
                kind = BallAbilityKind.MOMENTUM_CORE,
                title = "MOMENTUM CORE",
                description = "Maintains ${((multiplier - 1f) * 100f).toInt()}% extra ball speed while preserving normal control limits.",
                tier = tier,
                speedMultiplier = multiplier,
            )
        }
    }
}
