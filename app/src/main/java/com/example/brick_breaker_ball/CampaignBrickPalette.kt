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
        // 2 — shadowed winged guardians
        2 to WorldVisualTheme(
            listOf(SpriteId.BRICK_ARMORED_DARK_INTACT, SpriteId.BRICK_STONE_GRAY_INTACT, SpriteId.BRICK_CRYSTAL_PURPLE_INTACT, SpriteId.BRICK_CRYSTAL_BLUE_INTACT),
            Color.valueOf("8EA5C8")
        ),
        // 3 — moonlit jungle: cool blue, violet and pale moonlight
        3 to WorldVisualTheme(
            listOf(
                SpriteId.BRICK_CRYSTAL_BLUE_INTACT,
                SpriteId.BRICK_CRYSTAL_MAROON_INTACT,
                SpriteId.BRICK_CRYSTAL_PURPLE_INTACT,
                SpriteId.BRICK_ELECTRIC_WHITE_INTACT
            ),
            Color.valueOf("8BA7FF")
        ),
        // 4 — desert gold
        4 to WorldVisualTheme(
            listOf(SpriteId.BRICK_CRYSTAL_ORANGE_INTACT, SpriteId.BRICK_STONE_GRAY_INTACT, SpriteId.BRICK_CRYSTAL_RED_INTACT),
            Color.valueOf("E0A05C")
        ),
        // 5 — warm golden horizon
        5 to WorldVisualTheme(
            listOf(
                SpriteId.BRICK_CRYSTAL_ORANGE_INTACT,
                SpriteId.BRICK_CRYSTAL_RED_INTACT,
                SpriteId.BRICK_STONE_GRAY_INTACT,
                SpriteId.BRICK_ELECTRIC_WHITE_INTACT
            ),
            Color.valueOf("E8B971")
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
        // 7 — azure coast with tropical green accents
        7 to WorldVisualTheme(
            listOf(
                SpriteId.BRICK_CRYSTAL_MAROON_INTACT,
                SpriteId.BRICK_CRYSTAL_BLUE_INTACT,
                SpriteId.BRICK_CRYSTAL_GREEN_INTACT,
                SpriteId.BRICK_ELECTRIC_WHITE_INTACT
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
        // 10 — luminous cosmic whale
        10 to WorldVisualTheme(
            listOf(
                SpriteId.BRICK_ELECTRIC_WHITE_INTACT,
                SpriteId.BRICK_CRYSTAL_BLUE_INTACT,
                SpriteId.BRICK_CRYSTAL_PURPLE_INTACT,
                SpriteId.BRICK_CRYSTAL_PINK_INTACT,
                SpriteId.BRICK_CRYSTAL_MAROON_INTACT
            ),
            Color.valueOf("A8BDF8")
        ),
        // 11 — ember portal and dark stone
        11 to WorldVisualTheme(
            listOf(
                SpriteId.BRICK_ARMORED_DARK_INTACT,
                SpriteId.BRICK_CRYSTAL_RED_INTACT,
                SpriteId.BRICK_CRYSTAL_ORANGE_INTACT,
                SpriteId.BRICK_STONE_GRAY_INTACT
            ),
            Color.valueOf("D98355")
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
                SpriteId.BRICK_STONE_GRAY_INTACT
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

    private val colorBlindMaterials = listOf(
        SpriteId.BRICK_ELECTRIC_WHITE_INTACT,
        SpriteId.BRICK_STONE_GRAY_INTACT,
        SpriteId.BRICK_ARMORED_DARK_INTACT,
        SpriteId.BRICK_CRYSTAL_BLUE_INTACT,
    )

    fun spriteFor(type: BrickType, world: Int, brickId: Int, colorBlind: Boolean = false, levelId: Int = 0): SpriteId? {
        if (type !in twoHitMaterialTypes) return null
        val choices = if (colorBlind) colorBlindMaterials else palette(world)
        return choices[(brickId + type.ordinal * 3 + world + levelId).mod(choices.size)]
    }

    /**
     * Tint canonical one-hit/special one-hit bricks without borrowing a misleading 2-HP crystal
     * sprite.  Functional bricks keep a stable semantic accent while NORMAL_ONE_HIT follows the
     * current world's palette.
     */
    fun tintFor(type: BrickType, world: Int, colorBlind: Boolean = false): Color {
        if (colorBlind) return colorBlindTintFor(type)
        return when (type) {
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
    }

    private fun colorBlindTintFor(type: BrickType): Color = when (type) {
        BrickType.NORMAL_ONE_HIT -> Color.valueOf("56B4E9")
        BrickType.EXPLOSIVE -> Color.valueOf("D55E00")
        BrickType.GLASS -> Color.valueOf("F0E442")
        BrickType.MOVING_HORIZONTAL, BrickType.MOVING_VERTICAL -> Color.valueOf("009E73")
        BrickType.GHOST -> Color.valueOf("CC79A7")
        BrickType.SWITCH -> Color.valueOf("0072B2")
        BrickType.KEY_BRICK -> Color.valueOf("E69F00")
        BrickType.CHAIN_BRICK -> Color.WHITE
        else -> Color.WHITE
    }

    internal fun palette(world: Int): List<SpriteId> = themes.getValue(world.coerceIn(1, 13)).materials
}
