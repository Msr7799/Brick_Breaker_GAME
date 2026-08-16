package com.example.brick_breaker_ball

/**
 * Campaign-only material palettes sampled from the thirteen world videos.
 *
 * BrickType remains the source of gameplay behavior and health. This object only chooses a
 * compatible intact material sprite for ordinary material bricks; functional bricks keep their
 * canonical visual language so players can still recognize hazards, movement, locks, and rewards.
 */
internal object CampaignBrickPalette {
    private val palettes = mapOf(
        1 to listOf(SpriteId.BRICK_CRYSTAL_BLUE_INTACT, SpriteId.BRICK_CRYSTAL_CYAN_INTACT, SpriteId.BRICK_CRYSTAL_PURPLE_INTACT),
        2 to listOf(SpriteId.BRICK_CRYSTAL_CYAN_INTACT, SpriteId.BRICK_CRYSTAL_GREEN_INTACT, SpriteId.BRICK_CRYSTAL_PURPLE_INTACT),
        3 to listOf(SpriteId.BRICK_CRYSTAL_BLUE_INTACT, SpriteId.BRICK_CRYSTAL_CYAN_INTACT, SpriteId.BRICK_CRYSTAL_PURPLE_INTACT, SpriteId.BRICK_CRYSTAL_PINK_INTACT, SpriteId.BRICK_ELECTRIC_WHITE_INTACT),
        4 to listOf(SpriteId.BRICK_CRYSTAL_ORANGE_INTACT, SpriteId.BRICK_STONE_GRAY_INTACT, SpriteId.BRICK_CRYSTAL_CYAN_INTACT),
        5 to listOf(SpriteId.BRICK_CRYSTAL_CYAN_INTACT, SpriteId.BRICK_CRYSTAL_GREEN_INTACT, SpriteId.BRICK_CRYSTAL_BLUE_INTACT, SpriteId.BRICK_CRYSTAL_ORANGE_INTACT),
        6 to listOf(SpriteId.BRICK_ELECTRIC_WHITE_INTACT, SpriteId.BRICK_CRYSTAL_BLUE_INTACT, SpriteId.BRICK_CRYSTAL_CYAN_INTACT, SpriteId.BRICK_CRYSTAL_PURPLE_INTACT, SpriteId.BRICK_ARMORED_DARK_INTACT),
        7 to listOf(SpriteId.BRICK_CRYSTAL_CYAN_INTACT, SpriteId.BRICK_CRYSTAL_BLUE_INTACT, SpriteId.BRICK_CRYSTAL_ORANGE_INTACT, SpriteId.BRICK_CRYSTAL_GREEN_INTACT),
        8 to listOf(SpriteId.BRICK_ELECTRIC_WHITE_INTACT, SpriteId.BRICK_CRYSTAL_CYAN_INTACT, SpriteId.BRICK_CRYSTAL_BLUE_INTACT, SpriteId.BRICK_STONE_GRAY_INTACT),
        9 to listOf(SpriteId.BRICK_CRYSTAL_PURPLE_INTACT, SpriteId.BRICK_ARMORED_DARK_INTACT, SpriteId.BRICK_CRYSTAL_PINK_INTACT, SpriteId.BRICK_STONE_GRAY_INTACT),
        10 to listOf(SpriteId.BRICK_ELECTRIC_WHITE_INTACT, SpriteId.BRICK_CRYSTAL_ORANGE_INTACT, SpriteId.BRICK_CRYSTAL_CYAN_INTACT, SpriteId.BRICK_CRYSTAL_PURPLE_INTACT, SpriteId.BRICK_CRYSTAL_BLUE_INTACT),
        11 to listOf(SpriteId.BRICK_ARMORED_DARK_INTACT, SpriteId.BRICK_CRYSTAL_PURPLE_INTACT, SpriteId.BRICK_CRYSTAL_RED_INTACT, SpriteId.BRICK_CRYSTAL_PINK_INTACT, SpriteId.BRICK_STONE_GRAY_INTACT),
        12 to listOf(SpriteId.BRICK_CRYSTAL_CYAN_INTACT, SpriteId.BRICK_CRYSTAL_BLUE_INTACT, SpriteId.BRICK_ELECTRIC_WHITE_INTACT, SpriteId.BRICK_CRYSTAL_ORANGE_INTACT, SpriteId.BRICK_ARMORED_DARK_INTACT),
        13 to listOf(SpriteId.BRICK_CRYSTAL_RED_INTACT, SpriteId.BRICK_CRYSTAL_ORANGE_INTACT, SpriteId.BRICK_ARMORED_DARK_INTACT, SpriteId.BRICK_STONE_GRAY_INTACT),
    )

    private val materialTypes = setOf(
        BrickType.NORMAL_ONE_HIT,
        BrickType.ARMORED_TWO_HIT,
        BrickType.ARMORED_THREE_HIT,
        BrickType.GLASS,
        BrickType.ROUGH_STONE,
        BrickType.ELECTRIC_WHITE,
        BrickType.CRYSTAL_BLUE,
        BrickType.CRYSTAL_RED,
        BrickType.CRYSTAL_PURPLE,
        BrickType.STONE_GRAY,
        BrickType.CRYSTAL_CYAN,
        BrickType.ARMORED_DARK,
        BrickType.CRYSTAL_PINK,
        BrickType.CRYSTAL_ORANGE,
        BrickType.CRYSTAL_GREEN,
    )

    fun spriteFor(type: BrickType, world: Int, brickId: Int): SpriteId? {
        if (type !in materialTypes) return null
        val choices = palette(world)
        return choices[(brickId * 5 + type.ordinal * 3 + world).mod(choices.size)]
    }

    internal fun palette(world: Int): List<SpriteId> = palettes.getValue(world.coerceIn(1, 13))
}
