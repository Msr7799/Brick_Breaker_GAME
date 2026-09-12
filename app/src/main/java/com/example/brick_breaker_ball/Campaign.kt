/*
 * ملاحظات صيانة الملف:
 * المسار: app/src/main/java/com/example/brick_breaker_ball/Campaign.kt
 * المؤلف: mohamed alromaihi
 * الدوال الموجودة: `level`، `worldLevelCount`، `firstLevel`، `lastLevel`، `worldForLevel`، `isBonusStage`، `createLevel`، `key`، `place`، `worldMaterials`، `boardHash`، `validate`، `typeAt`، `stars`، `bestScore`، `markGameStarted`، `complete`، `saveSettings`، `loadSettings`
 */

package com.example.brick_breaker_ball

import com.badlogic.gdx.Gdx
import com.badlogic.gdx.Preferences

data class WorldDefinition(val id: Int, val name: String, val subtitle: String, val backgroundRegion: String, val tintHex: String)
data class LevelDefinition(
    val id: Int = -1,
    val world: Int = 1,
    val name: String = "UNTITLED",
    val rows: Int = 8,
    val columns: Int = 9,
    val layout: List<String> = List(8) { "........." },
    val ballSpeed: Float = 600f,
    val lives: Int = 3,
    val threeStarScore: Int = 5000,
    val modifiers: Set<String> = emptySet(),
    val customId: String? = null,
    val dropRate: Float = .12f,
    val deathRailEnabled: Boolean = true,
    val bossHealth: Int = 5,
    val brickGroups: Map<String, Int> = emptyMap(),
    /** Canonical BrickType IDs for custom/new levels; layout remains as a legacy migration fallback. */
    val brickIds: Map<String, String> = emptyMap()
)

object LevelRepository {
    const val REGULAR_WORLD_LEVELS = 7
    const val FINAL_WORLD_LEVELS = 6
    const val TOTAL_LEVELS = 90

    val worlds = listOf(
        WorldDefinition(1, "CRYSTAL LEVIATHAN", "Crystal water dragon beneath the deep", "world_2_crystal", "6AA8FF"),
        WorldDefinition(2, "CELESTIAL GROVE", "Mushroom kingdom beneath planetary skies", "world_2_crystal", "68C7E5"),
        WorldDefinition(3, "VOID WHALE", "A cosmic whale sailing the stars", "world_4_zerog", "8BA7FF"),
        WorldDefinition(4, "AZURE PYRAMID", "Ancient desert city of crystal pyramids", "world_1_foundry", "E0A05C"),
        WorldDefinition(5, "SKY KINGDOM", "Floating city and islands above the clouds", "world_2_crystal", "72C5C8"),
        WorldDefinition(6, "STORM SPIRE", "Storm towers charged with lightning", "world_4_zerog", "6D8CFF"),
        WorldDefinition(7, "CRYSTAL COAST", "Crystal shore and the water pyramid", "world_2_crystal", "4CB9F2"),
        WorldDefinition(8, "FROZEN CITADEL", "Frozen fortress beneath the aurora", "world_3_magma", "A5C9EA"),
        WorldDefinition(9, "DARK MATTER", "Enter the dark vortex", "world_4_zerog", "7B5A8D"),
        WorldDefinition(10, "CONJUNCTION", "Survive the planetary alignment", "world_4_zerog", "7F9DBD"),
        WorldDefinition(11, "CHAOS", "Face the unraveling cosmos", "world_3_magma", "A56B9C"),
        WorldDefinition(12, "THE END OF TIME", "Reach the edge of all time", "world_2_crystal", "8096AD"),
        WorldDefinition(13, "DARK VOLCANO", "The final world", "world_3_magma", "D0784D")
    )

    /**
     * Difficulty deliberately rises world-by-world.  Base speed stays below MAX_SPEED so late
     * worlds still have room for the game's gradual in-run acceleration instead of starting
     * permanently clamped at the speed ceiling.
     */
    private data class WorldDifficulty(
        val baseTargets: Int,
        val targetGrowthPerStage: Int,
        val baseBallSpeed: Float,
        val stageSpeedStep: Float,
        val dropRate: Float
    )

    // Ball pace raised modestly across the whole campaign: roughly +40 base speed per world.
    // This keeps the game noticeably snappier without pushing late worlds straight into MAX_SPEED.
    private val worldDifficulty = mapOf(
        1 to WorldDifficulty(22, 2, 600f, 4.0f, .180f),
        2 to WorldDifficulty(25, 2, 620f, 4.0f, .174f),
        3 to WorldDifficulty(28, 2, 640f, 4.2f, .168f),
        4 to WorldDifficulty(31, 2, 660f, 4.2f, .162f),
        5 to WorldDifficulty(34, 2, 680f, 4.4f, .156f),
        6 to WorldDifficulty(37, 2, 700f, 4.4f, .150f),
        7 to WorldDifficulty(40, 2, 720f, 4.6f, .144f),
        8 to WorldDifficulty(43, 2, 740f, 4.6f, .138f),
        9 to WorldDifficulty(46, 2, 760f, 4.8f, .132f),
        10 to WorldDifficulty(49, 2, 780f, 4.8f, .126f),
        11 to WorldDifficulty(52, 2, 800f, 5.0f, .120f),
        12 to WorldDifficulty(54, 2, 820f, 5.0f, .115f),
        13 to WorldDifficulty(60, 2, 840f, 5.0f, .110f)
    )
    val levels: List<LevelDefinition> = (1..TOTAL_LEVELS).map(::createLevel)

    /** ملاحظة صيانة: الدالة `level` تنفّذ مسؤولية محلية يعتمد عليها هذا الجزء من اللعبة؛ راجع استدعاءاتها واختباراتها قبل تعديلها. */
    fun level(id: Int) = levels[(id - 1).coerceIn(0, levels.lastIndex)]

    /** ملاحظة صيانة: الدالة `worldLevelCount` تنفّذ مسؤولية محلية يعتمد عليها هذا الجزء من اللعبة؛ راجع استدعاءاتها واختباراتها قبل تعديلها. */
    fun worldLevelCount(world: Int): Int = if (world == worlds.last().id) FINAL_WORLD_LEVELS else REGULAR_WORLD_LEVELS

    /** ملاحظة صيانة: الدالة `firstLevel` تنفّذ مسؤولية محلية يعتمد عليها هذا الجزء من اللعبة؛ راجع استدعاءاتها واختباراتها قبل تعديلها. */
    fun firstLevel(world: Int): Int = worlds.takeWhile { it.id < world }.sumOf { worldLevelCount(it.id) } + 1

    /** ملاحظة صيانة: الدالة `lastLevel` تنفّذ مسؤولية محلية يعتمد عليها هذا الجزء من اللعبة؛ راجع استدعاءاتها واختباراتها قبل تعديلها. */
    fun lastLevel(world: Int): Int = firstLevel(world) + worldLevelCount(world) - 1

    /** ملاحظة صيانة: الدالة `worldForLevel` تنفّذ مسؤولية محلية يعتمد عليها هذا الجزء من اللعبة؛ راجع استدعاءاتها واختباراتها قبل تعديلها. */
    fun worldForLevel(levelId: Int): Int = worlds.first { levelId in firstLevel(it.id)..lastLevel(it.id) }.id

    /** ملاحظة صيانة: الدالة `isBonusStage` تتحقق من الشرط المطلوب وتعيد نتيجة يمكن لبقية النظام الاعتماد عليها؛ راجع استدعاءاتها واختباراتها قبل تعديلها. */
    private fun isBonusStage(world: Int, stage: Int): Boolean = world in setOf(3, 6, 9, 12) && stage == 6

    /** ملاحظة صيانة: الدالة `createLevel` تنشئ الكائنات أو البيانات اللازمة لهذه المسؤولية؛ راجع استدعاءاتها واختباراتها قبل تعديلها. */
    private fun createLevel(id: Int): LevelDefinition {
        val world = worldForLevel(id)
        val stage = id - firstLevel(world) + 1
        val stageCount = worldLevelCount(world)
        val bonusStage = isBonusStage(world, stage)
        val difficulty = worldDifficulty.getValue(world)
        val rows = 8
        val columns = 9
        val cells = MutableList(rows) { MutableList<BrickType?>(columns) { null } }
        val groups = mutableMapOf<String, Int>()

        /** ملاحظة صيانة: الدالة `key` تحوّل البيانات أو تبني المعرّف المتوافق مع بقية النظام؛ راجع استدعاءاتها واختباراتها قبل تعديلها. */
        fun key(row: Int, column: Int) = BrickCodec.key(row, column)

        /** ملاحظة صيانة: الدالة `place` تنفّذ مسؤولية محلية يعتمد عليها هذا الجزء من اللعبة؛ راجع استدعاءاتها واختباراتها قبل تعديلها. */
        fun place(row: Int, column: Int, type: BrickType, group: Int = 0) {
            if (row !in 0 until rows || column !in 0 until columns) return
            cells[row][column] = type
            if (group > 0) groups[key(row, column)] = group else groups.remove(key(row, column))
        }

        val targetCount = (difficulty.baseTargets + (stage - 1) * difficulty.targetGrowthPerStage).coerceAtMost(66)
        // From world 2 onward, reserve the bottom campaign row for a guaranteed full extra brick line.
        // The board stays 8x9 (editor/validator compatibility); density increases without changing geometry.
        val hasExtraBrickRow = world >= 2
        val proceduralRowRange = if (hasExtraBrickRow) 0 until (rows - 1) else 0 until rows
        val positions = proceduralRowRange.flatMap { row -> (0 until columns).map { column -> row to column } }
            .sortedBy { (row, column) -> boardHash(id, row, column) }.take(targetCount)
        val materials = worldMaterials(world)
        positions.forEachIndexed { index, (row, column) -> place(row, column, materials[(index + stage + row * 2) % materials.size]) }

        if (hasExtraBrickRow) {
            val extraRow = rows - 1
            (0 until columns).forEach { column ->
                place(extraRow, column, materials[(column + stage + world) % materials.size])
            }
        }

        when (stage) {
            1 -> (0..8).forEach { c -> place(0, c, materials[c % materials.size]) }

            2 -> (0..5).forEach { r ->
                place(r, (r + world) % columns, materials[(r + stage) % materials.size])
                place(r, columns - 1 - (r + world) % columns, BrickType.NORMAL_ONE_HIT)
            }

            3 -> (1..7).forEach { c ->
                place(2, c, materials[(c + 1) % materials.size])
                place(4, c, materials[(c + 3) % materials.size])
            }

            4 -> (0..6).forEach { r ->
                place(r, 0, materials[r % materials.size])
                place(r, 8, materials[(r + 2) % materials.size])
            }

            5 -> (0..6).forEach { r -> (r..(8 - r.coerceAtMost(4))).forEach { c -> place(r, c, materials[(r + c) % materials.size]) } }

            6 -> (0..8).forEach { c -> place(3, c, materials[(c + stage) % materials.size]) }

            7 -> (0..6).forEach { r -> place(r, (r * 2 + world) % columns, materials[r % materials.size]) }
        }

        if (!bonusStage) {
            // Each world teaches a small mechanic set, then combines more mechanics in later stages.
            // Fixed coordinates are deliberately separated so one mechanic does not silently overwrite another.
            when (world) {
                1 -> {
                    if (stage >= 4) place(2, 4, BrickType.ROUGH_STONE)
                    if (stage >= 6) place(1, 6, BrickType.POWERUP_CARRIER)
                }

                2 -> {
                    if (stage >= 2) place(1, 2, BrickType.POWERUP_CARRIER)
                    if (stage >= 4) place(3, 6, BrickType.EXPLOSIVE)
                    if (stage >= 6) place(2, 4, BrickType.ROUGH_STONE)
                }

                3 -> {
                    if (stage >= 2) place(1, 1, BrickType.MOVING_HORIZONTAL)
                    if (stage >= 3) place(2, 7, BrickType.MOVING_VERTICAL)
                    if (stage >= 4) place(4, 2, BrickType.LIGHTNING_SPEED_PASS_THROUGH)
                    if (stage >= 5) place(4, 6, BrickType.TRANSPARENT_SLOW_PASS_THROUGH)
                    if (stage >= 7) place(1, 4, BrickType.POWERUP_CARRIER)
                }

                4 -> {
                    if (stage >= 2) place(1, 4, BrickType.REGENERATING)
                    if (stage >= 3) place(3, 2, BrickType.EXPLOSIVE)
                    if (stage >= 4) place(3, 6, BrickType.EXPLOSIVE)
                    if (stage >= 5) place(2, 4, BrickType.ROUGH_STONE)
                    if (stage >= 7) place(1, 7, BrickType.POWERUP_CARRIER)
                }

                5 -> {
                    if (stage >= 2) {
                        place(1, 3, BrickType.SWITCH, 2)
                        place(2, 5, BrickType.GHOST, 2)
                    }
                    if (stage >= 3) place(4, 4, BrickType.BLACK_HOLE_TELEPORTER)
                    if (stage >= 5) place(3, 7, BrickType.REGENERATING)
                    if (stage >= 7) place(1, 1, BrickType.MOVING_HORIZONTAL)
                }

                6 -> {
                    if (stage >= 2) {
                        place(1, 1, BrickType.KEY_BRICK, 1)
                        place(1, 7, BrickType.LOCKED, 1)
                    }
                    if (stage >= 3) place(2, 7, BrickType.LOCKED, 1)
                    if (stage >= 4) place(4, 2, BrickType.LIGHTNING_SPEED_PASS_THROUGH)
                    if (stage >= 5) place(4, 6, BrickType.TRANSPARENT_SLOW_PASS_THROUGH)
                    if (stage >= 7) place(3, 4, BrickType.MOVING_VERTICAL)
                }

                7 -> {
                    if (stage >= 2) {
                        place(3, 2, BrickType.CHAIN_BRICK, 3)
                        place(3, 4, BrickType.CHAIN_BRICK, 3)
                        place(3, 6, BrickType.CHAIN_BRICK, 3)
                    }
                    if (stage >= 3) {
                        place(1, 1, BrickType.KEY_BRICK, 1)
                        place(1, 7, BrickType.LOCKED, 1)
                    }
                    if (stage >= 5) place(4, 4, BrickType.BLACK_HOLE_TELEPORTER)
                    if (stage >= 7) place(2, 7, BrickType.REGENERATING)
                }

                8 -> {
                    if (stage >= 2) {
                        place(2, 2, BrickType.INDESTRUCTIBLE)
                        place(2, 6, BrickType.INDESTRUCTIBLE)
                    }
                    if (stage >= 3) place(1, 4, BrickType.MOVING_HORIZONTAL)
                    if (stage >= 4) place(4, 2, BrickType.LIGHTNING_SPEED_PASS_THROUGH)
                    if (stage >= 5) place(4, 6, BrickType.TRANSPARENT_SLOW_PASS_THROUGH)
                    if (stage >= 7) place(3, 4, BrickType.REGENERATING)
                }

                9 -> {
                    if (stage >= 2) place(4, 2, BrickType.BLACK_HOLE_TELEPORTER)
                    if (stage >= 3) place(4, 6, BrickType.BLACK_HOLE_TELEPORTER)
                    if (stage >= 4) {
                        place(1, 3, BrickType.SWITCH, 4)
                        place(2, 5, BrickType.GHOST, 4)
                    }
                    if (stage >= 5) place(3, 4, BrickType.REGENERATING)
                    if (stage >= 7) {
                        place(2, 1, BrickType.INDESTRUCTIBLE)
                        place(2, 7, BrickType.INDESTRUCTIBLE)
                    }
                }

                10 -> {
                    if (stage >= 2) {
                        place(1, 1, BrickType.KEY_BRICK, 1)
                        place(1, 7, BrickType.LOCKED, 1)
                    }
                    if (stage >= 3) {
                        place(2, 1, BrickType.SWITCH, 2)
                        place(2, 7, BrickType.GHOST, 2)
                    }
                    if (stage >= 4) {
                        place(3, 2, BrickType.CHAIN_BRICK, 3)
                        place(3, 4, BrickType.CHAIN_BRICK, 3)
                        place(3, 6, BrickType.CHAIN_BRICK, 3)
                    }
                    if (stage >= 5) place(4, 2, BrickType.MOVING_HORIZONTAL)
                    if (stage >= 7) place(4, 6, BrickType.MOVING_VERTICAL)
                }

                11 -> {
                    if (stage >= 2) {
                        place(2, 1, BrickType.INDESTRUCTIBLE)
                        place(2, 7, BrickType.INDESTRUCTIBLE)
                    }
                    if (stage >= 3) {
                        place(3, 2, BrickType.EXPLOSIVE)
                        place(3, 6, BrickType.EXPLOSIVE)
                    }
                    if (stage >= 4) place(1, 4, BrickType.REGENERATING)
                    if (stage >= 5) place(4, 4, BrickType.BLACK_HOLE_TELEPORTER)
                    if (stage >= 7) {
                        place(1, 1, BrickType.MOVING_HORIZONTAL)
                        place(1, 7, BrickType.MOVING_VERTICAL)
                    }
                }

                12 -> {
                    if (stage >= 2) {
                        place(2, 1, BrickType.INDESTRUCTIBLE)
                        place(2, 7, BrickType.INDESTRUCTIBLE)
                    }
                    if (stage >= 3) {
                        place(1, 1, BrickType.KEY_BRICK, 1)
                        place(1, 7, BrickType.LOCKED, 1)
                        place(2, 7, BrickType.LOCKED, 1)
                    }
                    if (stage >= 4) {
                        place(3, 2, BrickType.CHAIN_BRICK, 3)
                        place(3, 4, BrickType.CHAIN_BRICK, 3)
                        place(3, 6, BrickType.CHAIN_BRICK, 3)
                    }
                    if (stage >= 5) {
                        place(4, 2, BrickType.LIGHTNING_SPEED_PASS_THROUGH)
                        place(4, 6, BrickType.TRANSPARENT_SLOW_PASS_THROUGH)
                    }
                    if (stage >= 7) place(4, 4, BrickType.BLACK_HOLE_TELEPORTER)
                }

                13 -> {
                    if (stage >= 2) {
                        place(1, 1, BrickType.INDESTRUCTIBLE)
                        place(1, 7, BrickType.INDESTRUCTIBLE)
                        place(4, 1, BrickType.INDESTRUCTIBLE)
                        place(4, 7, BrickType.INDESTRUCTIBLE)
                    }
                    if (stage >= 3) {
                        place(2, 1, BrickType.KEY_BRICK, 1)
                        place(2, 7, BrickType.LOCKED, 1)
                        place(3, 7, BrickType.LOCKED, 1)
                    }
                    if (stage >= 4) {
                        place(3, 2, BrickType.CHAIN_BRICK, 3)
                        place(3, 4, BrickType.CHAIN_BRICK, 3)
                        place(3, 6, BrickType.CHAIN_BRICK, 3)
                        place(4, 4, BrickType.BLACK_HOLE_TELEPORTER)
                    }
                    if (stage >= 5) {
                        place(1, 3, BrickType.REGENERATING)
                        place(1, 5, BrickType.REGENERATING)
                        place(4, 2, BrickType.EXPLOSIVE)
                        place(4, 6, BrickType.EXPLOSIVE)
                    }
                    if (stage >= 6) {
                        place(2, 2, BrickType.MOVING_HORIZONTAL)
                        place(2, 6, BrickType.MOVING_VERTICAL)
                    }
                }
            }
        }
        if (bonusStage) {
            // Item-rewarding bricks are deliberately limited to four bonus boards.
            listOf(0 to 1, 0 to 3, 0 to 5, 0 to 7, 2 to 2, 2 to 4, 2 to 6).forEach { (row, column) ->
                place(row, column, BrickType.RANDOM_INVENTORY_POWERUP)
            }
            (1..7).filter { it % 2 == 1 }.forEach { column -> place(1, column, BrickType.CRYSTAL_MAROON) }
            place(3, 4, BrickType.POWERUP_CARRIER)
        }
        val bossHp = 5 + world * 2
        if (stage == stageCount) {
            place(3, 4, BrickType.BOSS_CORE)
            place(2, 3, BrickType.ARMORED_DARK)
            place(2, 5, BrickType.ARMORED_DARK)
            place(4, 3, BrickType.ARMORED_DARK)
            place(4, 5, BrickType.ARMORED_DARK)
        }

        val brickIds = buildMap { cells.forEachIndexed { row, line -> line.forEachIndexed { column, type -> type?.let { put(key(row, column), it.name) } } } }
        val layout = cells.map { row -> row.joinToString("") { type -> type?.let(BrickCodec::symbol)?.toString() ?: "." } }
        val breakableCount = cells.flatten().count { it?.breakable == true }
        val hitPointWork = cells.flatten().sumOf { type ->
            when {
                type == null || !type.breakable -> 0
                type == BrickType.BOSS_CORE -> bossHp
                else -> type.maxHealth.coerceAtMost(4)
            }
        }
        return LevelDefinition(
            id = id, world = world, name = if (bonusStage) "WORLD $world • BONUS CACHE" else "WORLD $world • STAGE ${stage.toString().padStart(2, '0')}", rows = rows, columns = columns, layout = layout,
            ballSpeed = (difficulty.baseBallSpeed + (stage - 1) * difficulty.stageSpeedStep).coerceIn(GameSession.MIN_SPEED, GameSession.MAX_SPEED),
            lives = if (world == 1 || bonusStage) 4 else 3,
            threeStarScore = maxOf(1500, hitPointWork * 95 + breakableCount * 30 + world * 180 + stage * 70),
            modifiers = buildSet {
                if (stage == stageCount) {
                    add("BOSS")
                    add("CHALLENGE")
                }
                if (bonusStage) add("BONUS")
                if (world >= 4) add("VOLATILE")
            },
            dropRate = if (bonusStage) (difficulty.dropRate + .08f).coerceAtMost(.28f) else difficulty.dropRate,
            deathRailEnabled = true,
            bossHealth = bossHp, brickGroups = groups, brickIds = brickIds
        )
    }

    /** ملاحظة صيانة: الدالة `worldMaterials` تنفّذ مسؤولية محلية يعتمد عليها هذا الجزء من اللعبة؛ راجع استدعاءاتها واختباراتها قبل تعديلها. */
    private fun worldMaterials(world: Int): List<BrickType> = when (world) {
        // Repeated NORMAL_ONE_HIT entries in the first worlds keep the opening forgiving.
        1 -> listOf(
            BrickType.NORMAL_ONE_HIT, BrickType.NORMAL_ONE_HIT, BrickType.CRYSTAL_BLUE,
            BrickType.NORMAL_ONE_HIT, BrickType.CRYSTAL_MAROON
        )
        2 -> listOf(
            BrickType.NORMAL_ONE_HIT, BrickType.CRYSTAL_MAROON, BrickType.NORMAL_ONE_HIT,
            BrickType.CRYSTAL_GREEN, BrickType.CRYSTAL_BLUE
        )
        3 -> listOf(
            BrickType.NORMAL_ONE_HIT, BrickType.CRYSTAL_BLUE, BrickType.CRYSTAL_MAROON,
            BrickType.ELECTRIC_WHITE, BrickType.CRYSTAL_PURPLE
        )
        4 -> listOf(
            BrickType.NORMAL_ONE_HIT, BrickType.CRYSTAL_ORANGE, BrickType.STONE_GRAY,
            BrickType.CRYSTAL_RED, BrickType.CRYSTAL_ORANGE
        )
        5 -> listOf(
            BrickType.CRYSTAL_MAROON, BrickType.CRYSTAL_GREEN, BrickType.CRYSTAL_BLUE,
            BrickType.ELECTRIC_WHITE, BrickType.NORMAL_ONE_HIT
        )
        6 -> listOf(
            BrickType.ELECTRIC_WHITE, BrickType.CRYSTAL_BLUE, BrickType.CRYSTAL_PURPLE,
            BrickType.ARMORED_DARK, BrickType.CRYSTAL_MAROON
        )
        7 -> listOf(
            BrickType.CRYSTAL_MAROON, BrickType.CRYSTAL_BLUE, BrickType.CRYSTAL_GREEN,
            BrickType.CRYSTAL_ORANGE, BrickType.CRYSTAL_PINK
        )
        8 -> listOf(
            BrickType.ELECTRIC_WHITE, BrickType.CRYSTAL_MAROON, BrickType.CRYSTAL_BLUE,
            BrickType.STONE_GRAY, BrickType.ARMORED_DARK
        )
        9 -> listOf(
            BrickType.CRYSTAL_PURPLE, BrickType.ARMORED_DARK, BrickType.CRYSTAL_PINK,
            BrickType.CRYSTAL_RED, BrickType.STONE_GRAY
        )
        10 -> listOf(
            BrickType.ELECTRIC_WHITE, BrickType.CRYSTAL_ORANGE, BrickType.CRYSTAL_BLUE,
            BrickType.CRYSTAL_PURPLE, BrickType.CRYSTAL_MAROON
        )
        11 -> listOf(
            BrickType.ARMORED_DARK, BrickType.CRYSTAL_PURPLE, BrickType.CRYSTAL_RED,
            BrickType.CRYSTAL_PINK, BrickType.STONE_GRAY
        )
        12 -> listOf(
            BrickType.STONE_GRAY, BrickType.ELECTRIC_WHITE, BrickType.CRYSTAL_BLUE,
            BrickType.CRYSTAL_MAROON, BrickType.CRYSTAL_ORANGE
        )
        else -> listOf(
            BrickType.CRYSTAL_RED, BrickType.CRYSTAL_ORANGE, BrickType.ARMORED_DARK,
            BrickType.STONE_GRAY, BrickType.CRYSTAL_PURPLE
        )
    }

    /** ملاحظة صيانة: الدالة `boardHash` تنفّذ مسؤولية محلية يعتمد عليها هذا الجزء من اللعبة؛ راجع استدعاءاتها واختباراتها قبل تعديلها. */
    private fun boardHash(levelId: Int, row: Int, column: Int): Int = ((levelId * 1103515245L + row * 92821L + column * 68917L) and Int.MAX_VALUE.toLong()).toInt()
}

data class ValidationResult(val valid: Boolean, val errors: List<String>, val warnings: List<String> = emptyList())
object LevelValidator {
    /** ملاحظة صيانة: الدالة `validate` تتحقق من الشرط المطلوب وتعيد نتيجة يمكن لبقية النظام الاعتماد عليها؛ راجع استدعاءاتها واختباراتها قبل تعديلها. */
    fun validate(level: LevelDefinition): ValidationResult {
        val errors = mutableListOf<String>()
        val warnings = mutableListOf<String>()
        if (level.rows != 8 || level.columns != 9 || level.layout.size != 8 || level.layout.any { it.length != 9 }) {
            errors += "Grid must be exactly 9 columns by 8 rows"
        }
        val text = level.layout.joinToString("")

        /** ملاحظة صيانة: الدالة `typeAt` تنفّذ مسؤولية محلية يعتمد عليها هذا الجزء من اللعبة؛ راجع استدعاءاتها واختباراتها قبل تعديلها. */
        fun typeAt(row: Int, column: Int, symbol: Char): BrickType? = level.brickIds[BrickCodec.key(row, column)]?.let { runCatching { BrickType.valueOf(it) }.getOrNull() }
            ?: BrickCodec.type(symbol)
        val placed = level.layout.flatMapIndexed { row, line -> line.mapIndexedNotNull { column, symbol -> typeAt(row, column, symbol) } }
        val targets = placed.filter { it.breakable && it != BrickType.SWITCH && it != BrickType.KEY_BRICK }
        if (targets.isEmpty()) errors += "Add at least one completable target"
        if (placed.isNotEmpty() && placed.all { it == BrickType.INDESTRUCTIBLE }) errors += "A level cannot contain only indestructible bricks"
        val groupsByType = mutableMapOf<BrickType, MutableSet<Int>>()
        level.layout.forEachIndexed { row, line ->
            line.forEachIndexed { column, symbol ->
                typeAt(row, column, symbol)?.let { type -> groupsByType.getOrPut(type) { mutableSetOf() } += level.brickGroups[BrickCodec.key(row, column)] ?: 0 }
            }
        }
        val unlockers = (groupsByType[BrickType.KEY_BRICK].orEmpty() + groupsByType[BrickType.SWITCH].orEmpty())
        groupsByType[BrickType.LOCKED].orEmpty().forEach { group ->
            if (group !in unlockers && !(group == 0 && unlockers.isNotEmpty())) errors += "Locked group $group requires a Key or Switch"
        }
        groupsByType[BrickType.KEY_BRICK].orEmpty().forEach { group ->
            if (group !in groupsByType[BrickType.LOCKED].orEmpty() && !(group == 0 && groupsByType[BrickType.LOCKED].orEmpty().isNotEmpty())) {
                warnings += "Key group $group has no matching Locked brick"
            }
        }
        level.layout.forEachIndexed { row, line ->
            line.forEachIndexed { column, symbol ->
                if (symbol == 'B') {
                    val reachableNeighbor = listOf(row - 1 to column, row + 1 to column, row to column - 1, row to column + 1).any { (r, c) ->
                        r !in level.layout.indices || c !in 0 until 9 || level.layout[r][c] !in "IL"
                    }
                    if (!reachableNeighbor) errors += "Boss Core at row ${row + 1}, column ${column + 1} is unreachable"
                }
            }
        }
        if (text.count { it == 'P' } > 16) errors += "Too many carriers"
        if (level.ballSpeed !in GameSession.MIN_SPEED..GameSession.MAX_SPEED) errors += "Ball speed must be ${GameSession.MIN_SPEED.toInt()}-${GameSession.MAX_SPEED.toInt()}"
        if (level.lives !in 1..9) errors += "Lives must be 1-9"
        if (level.dropRate !in 0f..1f) errors += "Drop rate must be 0-100%"
        if (level.bossHealth !in 1..99) errors += "Boss HP must be 1-99"
        if (level.threeStarScore <= 0) errors += "Invalid score threshold"
        return ValidationResult(errors.isEmpty(), errors.distinct(), warnings.distinct())
    }
}

enum class GraphicsQuality { LOW, MEDIUM, HIGH }
data class GameSettings(
    var masterVolume: Float = .8f,
    var musicVolume: Float = .55f,
    var sfxVolume: Float = .8f,
    var haptics: Boolean = true,
    var reduceMotion: Boolean = false,
    var highContrastBall: Boolean = false,
    var colorBlind: Boolean = false,
    var leftHanded: Boolean = false,
    var relativeControl: Boolean = false,
    var sensitivity: Float = 1f,
    var textScale: Float = 1f,
    var quality: GraphicsQuality = GraphicsQuality.HIGH,
    var selectedBallSpriteName: String = CosmeticDefaults.BALL_SPRITE,
    var selectedBallGroupName: String = CosmeticDefaults.BALL_GROUP,
    var selectedBallBaseSize: BallSize = BallSize.DEFAULT,
    var selectedPaddleId: String = CosmeticDefaults.PADDLE_ID,
    var selectedWeaponPaddleId: String = CosmeticDefaults.WEAPON_PADDLE_ID,
    var selectedStickyPaddleId: String = CosmeticDefaults.STICKY_PADDLE_ID
)

class ProgressStore(private val prefs: Preferences = Gdx.app.getPreferences("brickbreakerball-progress-v1")) {
    val settings = GameSettings()
    var unlockedLevel: Int = prefs.getInteger("unlocked", 1).coerceIn(1, LevelRepository.TOTAL_LEVELS)
        private set
    var hasStartedGame: Boolean = prefs.getBoolean("has_started_game", false)
        private set
    init {
        loadSettings()
    }

    /** ملاحظة صيانة: الدالة `stars` تنفّذ مسؤولية محلية يعتمد عليها هذا الجزء من اللعبة؛ راجع استدعاءاتها واختباراتها قبل تعديلها. */
    fun stars(level: Int) = prefs.getInteger("stars_$level", 0)

    /** ملاحظة صيانة: الدالة `bestScore` تنفّذ مسؤولية محلية يعتمد عليها هذا الجزء من اللعبة؛ راجع استدعاءاتها واختباراتها قبل تعديلها. */
    fun bestScore(level: Int) = prefs.getInteger("score_$level", 0)
    fun totalStars(): Int = (1..LevelRepository.TOTAL_LEVELS).sumOf(::stars)

    /** ملاحظة صيانة: الدالة `markGameStarted` تحفظ البيانات أو تضيفها إلى الحالة المعتمدة في النظام؛ راجع استدعاءاتها واختباراتها قبل تعديلها. */
    fun markGameStarted() {
        if (!hasStartedGame) {
            hasStartedGame = true
            prefs.putBoolean("has_started_game", true).flush()
        }
    }

    /** ملاحظة صيانة: الدالة `complete` تنفّذ مسؤولية محلية يعتمد عليها هذا الجزء من اللعبة؛ راجع استدعاءاتها واختباراتها قبل تعديلها. */
    fun complete(level: Int, score: Int, stars: Int) {
        unlockedLevel = maxOf(unlockedLevel, (level + 1).coerceAtMost(LevelRepository.TOTAL_LEVELS))
        prefs.putInteger("unlocked", unlockedLevel).putInteger("stars_$level", maxOf(stars(level), stars))
            .putInteger("score_$level", maxOf(bestScore(level), score)).flush()
    }

    /** ملاحظة صيانة: الدالة `saveSettings` تحفظ البيانات أو تضيفها إلى الحالة المعتمدة في النظام؛ راجع استدعاءاتها واختباراتها قبل تعديلها. */
    fun saveSettings() {
        with(settings) {
            prefs.putFloat("master", masterVolume).putFloat("music", musicVolume).putFloat("sfx", sfxVolume).putBoolean("haptics", haptics).putBoolean("motion", reduceMotion).putBoolean("contrast", highContrastBall).putBoolean("colorblind", colorBlind).putBoolean("left", leftHanded).putBoolean("relative", relativeControl).putFloat("sensitivity", sensitivity).putFloat("text", textScale).putString("quality", quality.name)
                .putString("selected_ball_sprite", selectedBallSpriteName).putString("selected_ball_group", selectedBallGroupName)
                .putString("selected_ball_size", BallSize.DEFAULT.name).putString("selected_paddle_id", selectedPaddleId)
                .putString("selected_weapon_paddle_id", selectedWeaponPaddleId)
                .putString("selected_sticky_paddle_id", selectedStickyPaddleId).flush()
        }
    }

    /** ملاحظة صيانة: الدالة `loadSettings` تقرأ البيانات المطلوبة أو تسترجعها بصيغة مناسبة للاستخدام؛ راجع استدعاءاتها واختباراتها قبل تعديلها. */
    private fun loadSettings() {
        with(settings) {
            masterVolume = prefs.getFloat("master", .8f)
            musicVolume = prefs.getFloat("music", .55f)
            sfxVolume = prefs.getFloat("sfx", .8f)
            haptics = prefs.getBoolean("haptics", true)
            reduceMotion = prefs.getBoolean("motion", false)
            highContrastBall = prefs.getBoolean("contrast", false)
            colorBlind = prefs.getBoolean("colorblind", false)
            leftHanded = prefs.getBoolean("left", false)
            relativeControl = prefs.getBoolean("relative", false)
            sensitivity = prefs.getFloat("sensitivity", 1f)
            textScale = prefs.getFloat("text", 1f)
            quality = runCatching { GraphicsQuality.valueOf(prefs.getString("quality", "HIGH")) }.getOrDefault(GraphicsQuality.HIGH)
            selectedBallSpriteName = prefs.getString("selected_ball_sprite", CosmeticDefaults.BALL_SPRITE)
            selectedBallGroupName = prefs.getString("selected_ball_group", CosmeticDefaults.BALL_GROUP)
            // Ball size is gameplay-owned: every run starts Normal and size talismans change it temporarily.
            selectedBallBaseSize = BallSize.DEFAULT
            val legacyPaddleId = prefs.getString("selected_paddle_id", CosmeticDefaults.PADDLE_ID)
            selectedPaddleId = legacyPaddleId.takeIf { "_normal_" in it } ?: CosmeticDefaults.PADDLE_ID
            selectedWeaponPaddleId = prefs.getString(
                "selected_weapon_paddle_id",
                legacyPaddleId.takeIf { "_weapon_" in it } ?: CosmeticDefaults.WEAPON_PADDLE_ID
            )
            selectedStickyPaddleId = prefs.getString(
                "selected_sticky_paddle_id",
                legacyPaddleId.takeIf { "_sticky_" in it } ?: CosmeticDefaults.STICKY_PADDLE_ID
            )
        }
    }
}
