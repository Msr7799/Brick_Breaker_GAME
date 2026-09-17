package com.example.brick_breaker_ball

enum class PaddleAbilityKind {
    PRECISION_CORE,
    HYPER_GLIDE,
    IMPACT_BOOST,
    INFERNO_RHYTHM,
}

data class PaddleAbilityProfile(
    val kind: PaddleAbilityKind,
    val title: String,
    val description: String,
    val tier: Int,
    val moveResponseMultiplier: Float = 1f,
    val perfectWindowSeconds: Float = 0f,
    val perfectSpeedMultiplier: Float = 1f,
    val fireEveryHits: Int = 0,
)

/**
 * Gameplay identity for every paddle style set.
 *
 * The first four sets intentionally match the requested progression:
 * Titanium Edge -> balanced handling, Neon Wing -> faster paddle,
 * Quantum Slate -> timing-based speed boost, Prism Guard -> every fourth return charges fire.
 * Later unlocked styles rotate through the same four readable archetypes with a modest tier bump,
 * so every set has gameplay value without introducing 36 unrelated mechanics to balance.
 */
object PaddleAbilityCatalog {
    private val orderedNormalPaddleIds = listOf(
        "group1:paddle_normal_titanium_edge",
        "group1:paddle_normal_neon_wing",
        "group1:paddle_normal_quantum_slate",
        "group1:paddle_normal_prism_guard",
        "group1:paddle_normal_obsidian_rail",
        "group1:paddle_normal_carbon_specter",
        "group2:paddle_normal_ivory",
        "group2:paddle_normal_frost",
        "group2:paddle_normal_navy",
        "group2:paddle_normal_copper",
        "group2:paddle_normal_gold",
        "group2:paddle_normal_violet",
        "group3:paddle_normal_shadow",
        "group3:paddle_normal_ember",
        "group3:paddle_normal_sapphire",
        "group3:paddle_normal_circuit",
        "group3:paddle_normal_bronze",
        "group3:paddle_normal_crimson",
        "group4:paddle_normal_cyan",
        "group4:paddle_normal_amber",
        "group4:paddle_normal_green",
        "group4:paddle_normal_blue",
        "group4:paddle_normal_crimson",
        "group4:paddle_normal_lunar",
        "group5:paddle_normal_gold_white_alloy",
        "group5:paddle_normal_blue_alloy",
        "group5:paddle_normal_aurora_alloy",
        "group5:paddle_normal_magenta_alloy",
        "group5:paddle_normal_cyan_alloy",
        "group5:paddle_normal_solar_alloy",
        "group6:paddle_normal_blue_alloy",
        "group6:paddle_normal_orange_alloy",
        "group6:paddle_normal_green_alloy",
        "group6:paddle_normal_violet_alloy",
        "group6:paddle_normal_red_alloy",
        "group6:paddle_normal_silver_alloy",
    )

    val default: PaddleAbilityProfile get() = profileForNormalPaddle(CosmeticDefaults.PADDLE_ID)

    fun profileForNormalPaddle(normalPaddleId: String): PaddleAbilityProfile {
        val rawIndex = orderedNormalPaddleIds.indexOf(normalPaddleId)
        val index = rawIndex.coerceAtLeast(0)
        val tier = (1 + index / 12).coerceIn(1, 3)
        return profileForArchetype(index % 4, tier)
    }

    fun profile(kind: PaddleAbilityKind, tier: Int): PaddleAbilityProfile {
        val archetype = when (kind) {
            PaddleAbilityKind.PRECISION_CORE -> 0
            PaddleAbilityKind.HYPER_GLIDE -> 1
            PaddleAbilityKind.IMPACT_BOOST -> 2
            PaddleAbilityKind.INFERNO_RHYTHM -> 3
        }
        return profileForArchetype(archetype, tier.coerceIn(1, 3))
    }

    fun knownNormalPaddleIds(): List<String> = orderedNormalPaddleIds

    private fun profileForArchetype(archetype: Int, tier: Int): PaddleAbilityProfile = when (archetype) {
        0 -> {
            val bonus = 1.05f + (tier - 1) * .02f
            PaddleAbilityProfile(
                kind = PaddleAbilityKind.PRECISION_CORE,
                title = "PRECISION CORE",
                description = "+${((bonus - 1f) * 100f).toInt()}% paddle response for steady control.",
                tier = tier,
                moveResponseMultiplier = bonus,
            )
        }

        1 -> {
            val bonus = 1.18f + (tier - 1) * .04f
            PaddleAbilityProfile(
                kind = PaddleAbilityKind.HYPER_GLIDE,
                title = "HYPER GLIDE",
                description = "+${((bonus - 1f) * 100f).toInt()}% paddle response for faster movement.",
                tier = tier,
                moveResponseMultiplier = bonus,
            )
        }

        2 -> {
            val speedBonus = 1.12f + (tier - 1) * .03f
            val window = .18f + (tier - 1) * .02f
            PaddleAbilityProfile(
                kind = PaddleAbilityKind.IMPACT_BOOST,
                title = "IMPACT BOOST",
                description = "Tap just before paddle contact to boost ball speed by ${((speedBonus - 1f) * 100f).toInt()}%.",
                tier = tier,
                perfectWindowSeconds = window,
                perfectSpeedMultiplier = speedBonus,
            )
        }

        else -> PaddleAbilityProfile(
            kind = PaddleAbilityKind.INFERNO_RHYTHM,
            title = "INFERNO RHYTHM",
            description = "Every 4th normal paddle return charges one fire hit.",
            tier = tier,
            fireEveryHits = 4,
        )
    }
}
