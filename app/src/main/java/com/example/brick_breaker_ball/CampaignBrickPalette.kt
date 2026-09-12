/*
 * Campaign material colours are matched to the 13 supplied world references.
 * Gameplay behavior/HP remains owned by BrickType; this object changes only compatible visuals.
 */

package com.example.brick_breaker_ball

import com.badlogic.gdx.graphics.Color

/**
 * World-specific campaign palette.
 *
 * Important readability rule:
 * - one-hit bricks keep the canonical one-hit sprite and are tinted per world;
 * - two-hit material bricks may swap between the supplied intact/damaged material pairs because
 *   they all preserve the same 2-hit rule;
 * - functional/special bricks keep their canonical sprite so their behavior remains readable.
 */
internal object CampaignBrickPalette {
    private data class WorldVisualTheme(
        val materials: List<SpriteId>,
        val oneHitTint: Color
    )

    private val themes = mapOf(
        // 1 — icy dragon / crystal blue
        1 to WorldVisualTheme(
            listOf(SpriteId.BRICK_CRYSTAL_BLUE_INTACT, SpriteId.BRICK_CRYSTAL_MAROON_INTACT, SpriteId.BRICK_CRYSTAL_PURPLE_INTACT),
            Color.valueOf("6AA8FF")
        ),
        // 2 — blue/teal alien grove
        2 to WorldVisualTheme(
            listOf(SpriteId.BRICK_CRYSTAL_MAROON_INTACT, SpriteId.BRICK_CRYSTAL_BLUE_INTACT, SpriteId.BRICK_CRYSTAL_GREEN_INTACT),
            Color.valueOf("68C7E5")
        ),
        // 3 — celestial whale / bright sky
        3 to WorldVisualTheme(
            listOf(
                SpriteId.BRICK_CRYSTAL_BLUE_INTACT,
                SpriteId.BRICK_CRYSTAL_MAROON_INTACT,
                SpriteId.BRICK_CRYSTAL_PURPLE_INTACT,
                SpriteId.BRICK_CRYSTAL_PINK_INTACT,
                SpriteId.BRICK_ELECTRIC_WHITE_INTACT
            ),
            Color.valueOf("8BA7FF")
        ),
        // 4 — desert gold
        4 to WorldVisualTheme(
            listOf(SpriteId.BRICK_CRYSTAL_ORANGE_INTACT, SpriteId.BRICK_STONE_GRAY_INTACT, SpriteId.BRICK_CRYSTAL_RED_INTACT),
            Color.valueOf("E0A05C")
        ),
        // 5 — pale turquoise sky kingdom
        5 to WorldVisualTheme(
            listOf(
                SpriteId.BRICK_CRYSTAL_MAROON_INTACT,
                SpriteId.BRICK_CRYSTAL_GREEN_INTACT,
                SpriteId.BRICK_CRYSTAL_BLUE_INTACT,
                SpriteId.BRICK_ELECTRIC_WHITE_INTACT
            ),
            Color.valueOf("72C5C8")
        ),
        // 6 — electric storm spires
        6 to WorldVisualTheme(
            listOf(
                SpriteId.BRICK_ELECTRIC_WHITE_INTACT,
                SpriteId.BRICK_CRYSTAL_BLUE_INTACT,
                SpriteId.BRICK_CRYSTAL_PURPLE_INTACT,
                SpriteId.BRICK_ARMORED_DARK_INTACT,
                SpriteId.BRICK_CRYSTAL_MAROON_INTACT
            ),
            Color.valueOf("6D8CFF")
        ),
        // 7 — saturated MAROON coast
        7 to WorldVisualTheme(
            listOf(
                SpriteId.BRICK_CRYSTAL_MAROON_INTACT,
                SpriteId.BRICK_CRYSTAL_BLUE_INTACT,
                SpriteId.BRICK_CRYSTAL_GREEN_INTACT,
                SpriteId.BRICK_CRYSTAL_ORANGE_INTACT
            ),
            Color.valueOf("4CB9F2")
        ),
        // 8 — white/blue frozen citadel
        8 to WorldVisualTheme(
            listOf(
                SpriteId.BRICK_ELECTRIC_WHITE_INTACT,
                SpriteId.BRICK_CRYSTAL_MAROON_INTACT,
                SpriteId.BRICK_CRYSTAL_BLUE_INTACT,
                SpriteId.BRICK_STONE_GRAY_INTACT
            ),
            Color.valueOf("A5C9EA")
        ),
        // 9 — dark matter / purple-black vortex
        9 to WorldVisualTheme(
            listOf(
                SpriteId.BRICK_CRYSTAL_PURPLE_INTACT,
                SpriteId.BRICK_ARMORED_DARK_INTACT,
                SpriteId.BRICK_CRYSTAL_PINK_INTACT,
                SpriteId.BRICK_CRYSTAL_RED_INTACT,
                SpriteId.BRICK_STONE_GRAY_INTACT
            ),
            Color.valueOf("7B5A8D")
        ),
        // 10 — cosmic conjunction, blue steel with warm highlights
        10 to WorldVisualTheme(
            listOf(
                SpriteId.BRICK_ELECTRIC_WHITE_INTACT,
                SpriteId.BRICK_CRYSTAL_ORANGE_INTACT,
                SpriteId.BRICK_CRYSTAL_BLUE_INTACT,
                SpriteId.BRICK_CRYSTAL_PURPLE_INTACT,
                SpriteId.BRICK_CRYSTAL_MAROON_INTACT
            ),
            Color.valueOf("7F9DBD")
        ),
        // 11 — chaos / magenta-red darkness
        11 to WorldVisualTheme(
            listOf(
                SpriteId.BRICK_ARMORED_DARK_INTACT,
                SpriteId.BRICK_CRYSTAL_PURPLE_INTACT,
                SpriteId.BRICK_CRYSTAL_RED_INTACT,
                SpriteId.BRICK_CRYSTAL_PINK_INTACT,
                SpriteId.BRICK_STONE_GRAY_INTACT
            ),
            Color.valueOf("A56B9C")
        ),
        // 12 — end of time / cold clockwork steel
        12 to WorldVisualTheme(
            listOf(
                SpriteId.BRICK_STONE_GRAY_INTACT,
                SpriteId.BRICK_ELECTRIC_WHITE_INTACT,
                SpriteId.BRICK_CRYSTAL_BLUE_INTACT,
                SpriteId.BRICK_CRYSTAL_MAROON_INTACT,
                SpriteId.BRICK_CRYSTAL_ORANGE_INTACT
            ),
            Color.valueOf("8096AD")
        ),
        // 13 — dark volcano / ember red-orange
        13 to WorldVisualTheme(
            listOf(
                SpriteId.BRICK_CRYSTAL_RED_INTACT,
                SpriteId.BRICK_CRYSTAL_ORANGE_INTACT,
                SpriteId.BRICK_ARMORED_DARK_INTACT,
                SpriteId.BRICK_STONE_GRAY_INTACT,
                SpriteId.BRICK_CRYSTAL_PURPLE_INTACT
            ),
            Color.valueOf("D0784D")
        )
    )

    /**
     * Only material bricks with the canonical two-hit contract are palette-swapped.  Their
     * next-state damaged sprite remains paired by DetailedSpriteSheet.nextState().
     */
    private val twoHitMaterialTypes = setOf(
        BrickType.ARMORED_TWO_HIT,
        BrickType.ELECTRIC_WHITE,
        BrickType.CRYSTAL_BLUE,
        BrickType.CRYSTAL_RED,
        BrickType.CRYSTAL_PURPLE,
        BrickType.STONE_GRAY,
        BrickType.CRYSTAL_MAROON,
        BrickType.ARMORED_DARK,
        BrickType.CRYSTAL_PINK,
        BrickType.CRYSTAL_ORANGE,
        BrickType.CRYSTAL_GREEN
    )

    fun spriteFor(type: BrickType, world: Int, brickId: Int): SpriteId? {
        if (type !in twoHitMaterialTypes) return null
        val choices = palette(world)
        return choices[(brickId * 5 + type.ordinal * 3 + world).mod(choices.size)]
    }

    /**
     * Tint canonical one-hit/special one-hit bricks without borrowing a misleading 2-HP crystal
     * sprite.  Functional bricks keep a stable semantic accent while NORMAL_ONE_HIT follows the
     * current world's palette.
     */
    fun tintFor(type: BrickType, world: Int): Color = when (type) {
        BrickType.NORMAL_ONE_HIT -> themes.getValue(world.coerceIn(1, 13)).oneHitTint
        BrickType.EXPLOSIVE -> Color.valueOf("FF5D4D")
        BrickType.GLASS -> Color.valueOf("B9F4FF")
        BrickType.MOVING_HORIZONTAL, BrickType.MOVING_VERTICAL -> Color.valueOf("57D8FF")
        BrickType.GHOST -> Color(0.66f, 0.54f, 1f, 0.78f)
        BrickType.SWITCH -> Color.valueOf("6DEBFF")
        BrickType.KEY_BRICK -> Color.valueOf("FFC857")
        BrickType.CHAIN_BRICK -> Color.valueOf("E8F6FF")
        else -> Color.WHITE
    }

    internal fun palette(world: Int): List<SpriteId> = themes.getValue(world.coerceIn(1, 13)).materials
}
