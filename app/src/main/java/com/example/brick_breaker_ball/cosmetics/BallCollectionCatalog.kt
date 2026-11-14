/*
 * المسار: app/src/main/java/com/example/brick_breaker_ball/cosmetics/BallCollectionCatalog.kt
 * الوظيفة: عروض كرات الزينة وندرتها وفتحها.
 * الحسابات: ربط رقم الكرة بالسعر والمرحلة والندرة.
 */

package com.example.brick_breaker_ball.cosmetics

import com.example.brick_breaker_ball.campaign.VolleyStageRepository
import com.example.brick_breaker_ball.gameplay.Ball

/** Store rules are keyed by sprite identity, never display order or a mutable list index. */
enum class BallRarity { NORMAL, RARE, LEGENDARY, EPIC }

data class BallCollectionOffer(
    val rarity: BallRarity,
    val stage: Int? = null,
    val coins: Int? = null,
    val gems: Int? = null,
    val playProductId: String? = null
) {
    init {
        require(listOfNotNull(stage, coins, gems).isNotEmpty())
        require(stage == null || (coins == null && gems == null))
    }
}

object BallCollectionCatalog {
    /** The exact six premium frames supplied in New-Balls.png. They never unlock from play. */
    val epicFrames = setOf(4, 5, 6, 13, 14, 57)
    private val legendaryOriginals = setOf(
        17, 18, 19, 20, 21, 22, 23, 28, 29, 30, 31,
        47, 56, 61, 63, 64, 66, 67, 71, 72, 74, 75, 76, 79, 84, 86, 88, 89, 92, 93,
        94, 95, 97, 99, 100
    )
    private val rareOriginals = (41..105).toSet() - legendaryOriginals
    private val legendaryNew = setOf(
        29, 30, 31, 32, 33, 34, 35, 36, 37, 38, 39, 40,
        41, 42, 43, 44, 48, 49, 50, 51, 52, 56, 58, 59
    )

    fun offer(ball: BallSpriteDefinition): BallCollectionOffer = offer(ball.spriteName)

    fun offer(spriteName: String): BallCollectionOffer {
        if (spriteName.startsWith("new_ball_")) {
            val frame = spriteName.removePrefix("new_ball_").toIntOrNull() ?: 0
            return when {
                frame in epicFrames -> BallCollectionOffer(
                    BallRarity.EPIC,
                    gems = 1_000,
                    playProductId = "epic_ball_${frame.toString().padStart(3, '0')}_299"
                )

                frame in legendaryNew -> BallCollectionOffer(
                    BallRarity.LEGENDARY,
                    gems = 190 + (frame % 8) * 35
                )

                frame <= 21 -> BallCollectionOffer(
                    BallRarity.NORMAL,
                    stage = stageWithin(frame, 0, 21, 1, 48)
                )

                frame in 22..28 || frame in 45..47 -> BallCollectionOffer(
                    BallRarity.RARE,
                    stage = stageWithin(frame, 22, 47, 49, 86)
                )

                else -> BallCollectionOffer(BallRarity.RARE, coins = 8_000 + (frame % 7) * 1_500)
            }
        }
        val number = spriteName.substringBefore('_').toIntOrNull() ?: 38
        return when {
            number in legendaryOriginals -> BallCollectionOffer(
                BallRarity.LEGENDARY,
                gems = 120 + ((number * 7) % 10) * 30
            )

            number in rareOriginals && number <= 55 -> BallCollectionOffer(
                BallRarity.RARE,
                stage = stageWithin(number, 41, 55, 50, 90)
            )

            number in rareOriginals -> BallCollectionOffer(
                BallRarity.RARE,
                coins = 7_500 + ((number - 56) % 8) * 1_500
            )

            else -> BallCollectionOffer(
                BallRarity.NORMAL,
                stage = stageWithin(number, 1, 40, 1, 76)
            )
        }
    }

    private fun stageWithin(value: Int, first: Int, last: Int, start: Int, end: Int): Int = (start + (value - first).coerceIn(0, last - first) * (end - start) / (last - first).coerceAtLeast(1))
        .coerceIn(1, VolleyStageRepository.TOTAL_LEVELS)
}

/** Stable, visual names for the 60 supplied sprites in plist frame order. */
object NewBallNames {
    private val names = listOf(
        "Ruby Lacquer", "Slate Fracture", "Amethyst Shell", "Moss Glass", "Phoenix Eye",
        "Titan Moon", "Abyss Reactor", "Cobalt Plate", "Azure Pebble", "Midnight Drop",
        "Rust Planet", "Moss Crack", "Solar Lacquer", "Magma Crown", "Aurora Sphere",
        "Ocean Marble", "Star Etching", "Azure Orbit", "Night Constellation", "Amber Sun",
        "Shale Moon", "Glass Tide", "Emerald Bloom", "Opal Flame", "Shadow Pearl",
        "Frost Vein", "Red Planet", "Deep Orbit", "Ocean Cells", "Cyan Scar",
        "Emerald Glass", "Solar Rune", "Rose Ember", "Blue Fossil", "Crimson Jewel",
        "Moss Crystals", "Tempered Steel", "Fire Nova", "Tidal Surge", "Solar Egg",
        "Glacier Pearl", "Chrome Moon", "Snow Globe", "Prism Earth", "Violet Nebula",
        "Mist Prism", "Molten Lattice", "Sea Ice", "Silver Fossil", "Snowfall",
        "Crystal Soil", "Lilac Spiral", "Lunar Pearl", "Bronze Meteor", "Rose Petal",
        "Prismatic Core", "Solar Orbit", "Eclipse Geode", "Starlight Pearl", "Night Orbit"
    )

    fun name(frame: Int): String = names.getOrElse(frame) { "Atlas Ball ${frame + 1}" }
}
