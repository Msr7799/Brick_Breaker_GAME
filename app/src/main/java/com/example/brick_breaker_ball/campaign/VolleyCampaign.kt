/*
 * المسار: app/src/main/java/com/example/brick_breaker_ball/campaign/VolleyCampaign.kt
 * الوظيفة: بناء مراحل وعوالم حملة Volley.
 * الحسابات: حساب توزيع الطوب والأهداف حسب رقم المرحلة.
 */

package com.example.brick_breaker_ball.campaign

import com.example.brick_breaker_ball.BuildConfig
import com.example.brick_breaker_ball.assets.VolleyAssetCatalog
import com.example.brick_breaker_ball.gameplay.BrickType
import com.example.brick_breaker_ball.gameplay.VolleyBonusNodeType
import com.example.brick_breaker_ball.gameplay.VolleyBrickVisualFamily
import com.example.brick_breaker_ball.gameplay.VolleyHazardKind
import com.example.brick_breaker_ball.gameplay.VolleyOpeningSalvo
import com.example.brick_breaker_ball.gameplay.VolleyPieceShape
import com.example.brick_breaker_ball.gameplay.VolleyRankedFormation
import com.example.brick_breaker_ball.gameplay.VolleyRankedGeometry
import com.example.brick_breaker_ball.gameplay.VolleySession
import kotlin.math.roundToInt

/**
 * Authored campaign data for VOLLEY mode.
 *
 * Volley follows the authored portrait stage backgrounds under assets/volley-world. Each numbered
 * JPG is one playable stage, so world sizes intentionally vary. Later stages use the supplied
 * and newly authored silhouettes on an 11-column board.
 */
data class VolleyBrickSeed(
    val type: BrickType,
    val shape: VolleyPieceShape = VolleyPieceShape.RECTANGLE,
    val groupId: Int = 0,
    val hpScale: Float = 1f,
    val countsForClear: Boolean? = null,
    val consumeOnHit: Boolean = true,
    val shieldLock: Boolean = false,
    val hazardKind: VolleyHazardKind? = null,
    val atlasSpriteName: String? = null
)

/** Exact world-space brick exported by the web editor for the unchanged first stage. */
data class VolleyAuthoredBrickSeed(
    val sourceRow: Int,
    val sourceColumn: Int,
    val x: Float,
    val y: Float,
    val width: Float,
    val height: Float,
    val type: BrickType,
    val health: Int,
    val shape: VolleyPieceShape,
    val atlasSpriteName: String? = null,
    val groupId: Int = 0,
    val countsForClear: Boolean = true,
    val shieldLock: Boolean = false,
    val hazardKind: VolleyHazardKind? = null,
    val sourceKind: String? = null,
    val sourceScene: String? = null,
    val sourceBonusValue: Int = 0,
    val connectedBombGroup: Int = 0,
    val bombDestroyable: Boolean = false,
    val slideHorizontal: Int = 0,
    val slideVertical: Int = 0,
    val slideStartsMoved: Boolean = true,
    /** Keeps imported brick IDs stable while clearing a world boss's arena. */
    val suppressedByWorldBoss: Boolean = false,
    /** Base health before level difficulty scaling. */
    val sourceBaseHealth: Float? = null,
    val sourceBurnCharges: Int = 1,
    val sourceProtectionPerRound: Int = 2
)

data class VolleyRailSeed(
    /** Grid-space coordinates. X is 0..columns, Y is 0..rows measured downward from the brick ceiling. */
    val x1: Float,
    val y1: Float,
    val x2: Float,
    val y2: Float,
    val thickness: Float = 22f,
    val fixed: Boolean = false
)

data class VolleyPickupSeed(
    /** Grid-space coordinates, normally centered inside an intentionally empty cell. */
    val x: Float,
    val y: Float,
    val balls: Int = 1,
    val type: VolleyBonusNodeType = VolleyBonusNodeType.BALL,
    val scoreValue: Int = 0
)

typealias VolleyBonusNodeSeed = VolleyPickupSeed

enum class VolleyCarrierReward { BALL, RANDOM_POSITIVE_ITEM }
enum class VolleyStageArchetype {
    OPEN_FUNNEL,
    SIDE_CHANNEL,
    LOCKED_POCKET,
    RICOCHET_VAULT,
    TWIN_LOOP,
    REWARD_FORK,
    ELITE_GAUNTLET,
    DIAMOND_CITADEL,
    CHEVRON_CASCADE,
    WORLD_CORE
}

data class VolleyStageDefinition(
    val levelId: Int,
    val world: Int,
    val stage: Int,
    val name: String,
    val rows: Int = 8,
    val columns: Int = 9,
    val cells: List<List<VolleyBrickSeed?>>,
    val authoredBricks: List<VolleyAuthoredBrickSeed> = emptyList(),
    val rails: List<VolleyRailSeed>,
    val pickups: List<VolleyPickupSeed>,
    val startingBalls: Int,
    val baseHp: Int,
    val descentDistance: Float = VolleySession.ROW_PITCH,
    val parTurns: Int,
    val bonusStage: Boolean,
    val bossStage: Boolean,
    val archetype: VolleyStageArchetype = VolleyStageArchetype.OPEN_FUNNEL,
    val carrierDropTable: List<VolleyCarrierReward> = listOf(VolleyCarrierReward.BALL),
    val backgroundId: Int = world,
    val backgroundAsset: String,
    val defaultVisualFamily: VolleyBrickVisualFamily = VolleyBrickVisualFamily.STANDARD_GRAY,
    val twoStarScore: Int = 1_500,
    val threeStarScore: Int = 2_500,
    val sourceScene: String? = null
)

/** Canonical Volley campaign derived from the supplied numbered portrait backgrounds. */
object VolleyStageRepository {
    const val WORLDS_COUNT = 9
    const val LEVELS_PER_WORLD = 18

    /** Generated from the numbered world images that actually exist; gaps never invalidate a world. */
    private val worldStageImageNumbers = BuildConfig.VOLLEY_STAGE_IMAGES.split(';').map { world ->
        world.split(',').mapNotNull(String::toIntOrNull)
    }
    val WORLD_STAGE_COUNTS = listOf(18, 18, 18, 18, 18, 18, 18, 17, 17)
    val TOTAL_LEVELS: Int = WORLD_STAGE_COUNTS.sum()

    private val worldAssetFolders = listOf(
        "01_Mystery_Forest",
        "02_Venom_Wilds",
        "03_Aqua_Vortex",
        "04_Desert_Spirits",
        "05_Ice_Grave",
        "06_Tormented_Souls",
        "07_Moon_light",
        "08_Infernal_Gates",
        "09_Final_Eclipse"
    )

    internal fun worldAssetFolder(world: Int): String = worldAssetFolders[world.coerceIn(1, WORLDS_COUNT) - 1]

    val worlds = listOf(
        WorldDefinition(1, "MYSTERY FOREST", "Funnel shots through the ancient forest", "volley_1", "74D99F"),
        WorldDefinition(2, "VENOM WILDS", "Survive poisoned passages and hostile growth", "volley_2", "8CCB65"),
        WorldDefinition(3, "AQUA VORTEX", "Bank volleys through flooded ruins", "volley_3", "55D9E8"),
        WorldDefinition(4, "DESERT SPIRITS", "Discover narrow routes through buried halls", "volley_4", "E6B76C"),
        WorldDefinition(5, "ICE GRAVE", "Break open frozen multi-bounce chambers", "volley_5", "9DDCFF"),
        WorldDefinition(6, "TORMENTED SOULS", "Cross haunted strongholds and cursed rails", "volley_6", "A8B8FF"),
        WorldDefinition(7, "MOON LIGHT", "Challenge the guardian of the moonlit shrine", "volley_7", "D8C7FF"),
        WorldDefinition(8, "INFERNAL GATES", "Master dangerous rails above molten stone", "volley_8", "FF754D"),
        WorldDefinition(9, "FINAL ECLIPSE", "Conquer the final portal gauntlet", "volley_9", "9F86E8")
    )

    private val bonusWorlds = setOf(3, 6, 9)
    private val stageLabels = listOf(
        "OPENING SALVO",
        "CROSSFIRE",
        "TWIN GATES",
        "RICOCHET VAULT",
        "FALLING FORTRESS",
        "RELIC CACHE",
        "PRISM BASTION",
        "DIAMOND CITADEL",
        "CHEVRON CASCADE",
        "WORLD CORE"
    )
    private val authoredArchetypes = listOf(
        VolleyStageArchetype.OPEN_FUNNEL,
        VolleyStageArchetype.SIDE_CHANNEL,
        VolleyStageArchetype.LOCKED_POCKET,
        VolleyStageArchetype.RICOCHET_VAULT,
        VolleyStageArchetype.TWIN_LOOP,
        VolleyStageArchetype.REWARD_FORK,
        VolleyStageArchetype.ELITE_GAUNTLET,
        VolleyStageArchetype.DIAMOND_CITADEL,
        VolleyStageArchetype.CHEVRON_CASCADE
    )

    // Opening the world map needs only the selected world's stages. Building all 21,808
    // imported bricks on that tap can stall the render thread during the screen fade.
    private val loadedStages = arrayOfNulls<VolleyStageDefinition>(TOTAL_LEVELS)
    val stages: List<VolleyStageDefinition> = object : AbstractList<VolleyStageDefinition>() {
        override val size: Int = TOTAL_LEVELS

        override fun get(index: Int): VolleyStageDefinition {
            if (index !in 0 until size) throw IndexOutOfBoundsException("Stage index $index")
            return loadedStages[index] ?: createStage(index + 1).also { loadedStages[index] = it }
        }
    }

    fun forLevel(levelId: Int): VolleyStageDefinition = stages[(levelId - 1).coerceIn(stages.indices)]
    fun levelDefinition(levelId: Int): LevelDefinition = forLevel(levelId).let { stage ->
        LevelDefinition(id = stage.levelId, world = stage.world, name = stage.name)
    }
    fun worldLevelCount(world: Int): Int = WORLD_STAGE_COUNTS.getOrElse(world - 1) { 0 }

    /** World-specific paint swatches use the same material selection as authored stages. */
    fun editorMaterials(world: Int): List<BrickType> = volleyMaterials(world.coerceIn(1, WORLDS_COUNT))
    fun firstLevel(world: Int): Int = WORLD_STAGE_COUNTS.take((world - 1).coerceAtLeast(0)).sum() + 1
    fun lastLevel(world: Int): Int = firstLevel(world) + worldLevelCount(world) - 1
    fun worldForLevel(levelId: Int): Int {
        val safeLevel = levelId.coerceIn(1, TOTAL_LEVELS)
        return worlds.first { safeLevel in firstLevel(it.id)..lastLevel(it.id) }.id
    }

    fun backgroundAsset(world: Int, stage: Int): String {
        require(world in 1..WORLDS_COUNT) { "Volley world must be in 1..$WORLDS_COUNT" }
        require(stage in 1..worldLevelCount(world)) {
            "Volley stage $stage is outside world $world's authored range"
        }
        val imageNumber = when {
            stage == worldLevelCount(world) -> 10
            world >= 8 && stage >= 15 -> stage - 7
            else -> (stage + 1) / 2
        }
        require(imageNumber in worldStageImageNumbers[world - 1]) { "Missing world $world stage image $imageNumber" }
        return "volley-world/${worldAssetFolder(world)}/$imageNumber.webp"
    }

    private fun createStage(levelId: Int): VolleyStageDefinition {
        return VolleySourceStageData.forLevel(levelId, worldAssetFolder(worldForLevel(levelId)))
    }

    private fun createLegacyStage(levelId: Int): VolleyStageDefinition {
        val world = worldForLevel(levelId)
        val stage = levelId - firstLevel(world) + 1
        val boss = stage == worldLevelCount(world)
        val bonus = world in bonusWorlds && stage == 6 && !boss
        val rows = 8
        val columns = 9
        val grid = MutableList(rows) { MutableList<VolleyBrickSeed?>(columns) { null } }
        val rails = mutableListOf<VolleyRailSeed>()
        val pickups = mutableListOf<VolleyPickupSeed>()
        val material = volleyMaterials(world)

        fun put(
            row: Int,
            col: Int,
            type: BrickType = material[Math.floorMod(row + col + world, material.size)],
            group: Int = 0,
            hp: Float = 1f
        ) {
            if (row in 0 until rows && col in 0 until columns) {
                grid[row][col] = VolleyBrickSeed(type = type, groupId = group, hpScale = hp)
            }
        }

        fun putSeed(row: Int, col: Int, seed: VolleyBrickSeed) {
            if (row in 0 until rows && col in 0 until columns) grid[row][col] = seed
        }

        fun putShape(row: Int, col: Int, shape: VolleyPieceShape) {
            if (row in 0 until rows && col in 0 until columns) {
                grid[row][col] = VolleyBrickSeed(
                    type = material[Math.floorMod(row + col + world, material.size)],
                    shape = shape
                )
            }
        }

        fun shapeExisting(row: Int, col: Int, shape: VolleyPieceShape) {
            if (row in 0 until rows && col in 0 until columns) {
                grid[row][col] = grid[row][col]?.copy(shape = shape)
            }
        }

        fun mask(vararg lines: String) {
            lines.forEachIndexed { row, line ->
                line.take(columns).forEachIndexed { column, cell ->
                    when (cell) {
                        '#' -> put(row, column)
                        '1' -> putShape(row, column, VolleyPieceShape.TRIANGLE_NW)
                        '2' -> putShape(row, column, VolleyPieceShape.TRIANGLE_NE)
                        '3' -> putShape(row, column, VolleyPieceShape.TRIANGLE_SW)
                        '4' -> putShape(row, column, VolleyPieceShape.TRIANGLE_SE)
                    }
                }
            }
        }

        val archetype = if (boss) {
            VolleyStageArchetype.WORLD_CORE
        } else {
            authoredArchetypes[Math.floorMod(stage - 1 + (world - 1) * 2, authoredArchetypes.size)]
        }
        if (boss) {
            buildBossFortress(world, rails, pickups, material, ::put, ::putSeed)
            // Keep every boss mechanic and HP value intact while shaping the outer fortress into
            // a crown above and two bank-shot wings below. These are real triangular colliders.
            listOf(0 to 0, 1 to 0, 5 to 0, 6 to 0, 7 to 1).forEach { (r, c) ->
                shapeExisting(r, c, VolleyPieceShape.TRIANGLE_NW)
            }
            listOf(0 to 8, 1 to 8, 5 to 8, 6 to 8, 7 to 7).forEach { (r, c) ->
                shapeExisting(r, c, VolleyPieceShape.TRIANGLE_NE)
            }
            listOf(4 to 0, 5 to 2, 6 to 3).forEach { (r, c) ->
                shapeExisting(r, c, VolleyPieceShape.TRIANGLE_SW)
            }
            listOf(4 to 8, 5 to 6, 6 to 5).forEach { (r, c) ->
                shapeExisting(r, c, VolleyPieceShape.TRIANGLE_SE)
            }
        } else {
            // Imported portrait formations are authored as silhouettes.
            // Each board starts with one readable architectural idea from the supplied references:
            // canopy gate, falling diagonal curtain, split bastions, three towers, rift chevrons,
            // portal arch, then a reinforced citadel. Triangles always continue an edge or stair.
            when (archetype) {
                VolleyStageArchetype.OPEN_FUNNEL -> {
                    // Dense canopy with a narrow central keyhole and two descending bank-shot legs.
                    mask("1#######2", "#########", "###1#2###", "##1...2##", ".1#...#2.", "1##...##2", "#.......#", ".........")
                    rails += VolleyRailSeed(.70f, 5.30f, 2.75f, 4.12f, 22f)
                    rails += VolleyRailSeed(8.30f, 5.30f, 6.25f, 4.12f, 22f)
                    put(2, 4, BrickType.POWERUP_CARRIER, hp = .72f)
                    pickups += VolleyPickupSeed(4.5f, 5.48f, 1)
                }

                VolleyStageArchetype.SIDE_CHANNEL -> {
                    // Repeating diagonal rain resolves into five separate lower pillars.
                    mask("1#1#1#1#2", "#1#1#1#2.", "1#1#1#2..", "#1#1#2...", "1#1#2....", "#.#.#.#.#", "3.3.3.3.4", ".........")
                    rails += VolleyRailSeed(.65f, 4.95f, 2.55f, 3.92f, 22f)
                    rails += VolleyRailSeed(8.35f, 4.95f, 6.45f, 3.92f, 22f)
                    pickups += VolleyPickupSeed(if (world % 2 == 0) 1.5f else 7.5f, 5.35f, 1)
                }

                VolleyStageArchetype.LOCKED_POCKET -> {
                    // Four rectangular bastions are divided by one continuous diagonal canyon.
                    mask("###...###", "###1.2###", "####.####", "###1.2###", "###1.2###", "##1...2##", "3#.....#4", ".........")
                    val enterRight = world % 2 == 1
                    if (enterRight) {
                        rails += VolleyRailSeed(8.20f, 5.00f, 6.45f, 4.08f, 23f)
                        rails += VolleyRailSeed(5.95f, 4.00f, 4.95f, 3.38f, 20f)
                    } else {
                        rails += VolleyRailSeed(.80f, 5.00f, 2.55f, 4.08f, 23f)
                        rails += VolleyRailSeed(3.05f, 4.00f, 4.05f, 3.38f, 20f)
                    }
                    put(2, 4, BrickType.POWERUP_CARRIER, hp = .82f)
                    pickups += VolleyPickupSeed(if (enterRight) 7.55f else 1.45f, 5.15f, 1 + world / 8)
                }

                VolleyStageArchetype.RICOCHET_VAULT -> {
                    // Three towers: paired side keeps and a taller central spine.
                    mask("12.1#2.12", "##.###.##", "34.3#4.34", "##.###.##", "12.1#2.12", "##.###.##", "34..#..34", ".........")
                    rails += VolleyRailSeed(.85f, 4.95f, 3.35f, 3.72f, 23f)
                    rails += VolleyRailSeed(8.15f, 4.95f, 5.65f, 3.72f, 23f)
                    put(2, 4, BrickType.POWERUP_CARRIER, hp = .86f)
                    pickups += VolleyPickupSeed(4.5f, 4.78f, 1)
                }

                VolleyStageArchetype.TWIN_LOOP -> {
                    // A broad V-shaped rift funnels shots toward the centre without closing it.
                    mask("#.......#", "##.....##", "###...###", "####.####", "3##1.2##4", ".3#1.2#4.", "..31.24..", ".........")
                    rails += VolleyRailSeed(.75f, 5.05f, 2.65f, 4.05f, 22f)
                    rails += VolleyRailSeed(2.85f, 4.00f, 4.15f, 4.72f, 20f)
                    rails += VolleyRailSeed(6.15f, 4.00f, 4.85f, 4.72f, 20f)
                    rails += VolleyRailSeed(8.25f, 5.05f, 6.35f, 4.05f, 22f)
                    pickups += VolleyPickupSeed(4.5f, 5.32f, 2)
                }

                VolleyStageArchetype.REWARD_FORK -> {
                    // A ceremonial portal arch frames a large playable chamber and two entrances.
                    mask("..1###2..", ".1#####2.", "1##...##2", "###...###", "##1...2##", "#1.....2#", "3#.....#4", ".........")
                    rails += VolleyRailSeed(.85f, 5.00f, 3.10f, 3.95f, 22f)
                    rails += VolleyRailSeed(4.50f, 5.05f, 4.50f, 4.15f, 18f)
                    rails += VolleyRailSeed(8.15f, 5.00f, 5.90f, 3.95f, 22f)
                    put(2, 4, BrickType.POWERUP_CARRIER, hp = .75f)
                    pickups += VolleyPickupSeed(4.5f, 5.55f, 2)
                }

                VolleyStageArchetype.ELITE_GAUNTLET -> {
                    // Reinforced citadel: solid crown above a tapered armoured keel.
                    mask("1#######2", "####.####", "###.#.###", "#########", "##3###4##", ".3#####4.", "..3###4..", ".........")
                    rails += VolleyRailSeed(.65f, 5.02f, 2.20f, 4.15f, 24f, fixed = true)
                    rails += VolleyRailSeed(8.35f, 5.02f, 6.80f, 4.15f, 24f, fixed = true)
                    rails += VolleyRailSeed(3.25f, 4.10f, 4.15f, 4.72f, 19f)
                    rails += VolleyRailSeed(5.75f, 4.10f, 4.85f, 4.72f, 19f)
                    pickups += VolleyPickupSeed(if (world % 2 == 0) 1.35f else 7.65f, 5.35f, 2)
                }

                VolleyStageArchetype.DIAMOND_CITADEL -> {
                    // Concentric diamond keep adapted from the reference game's framed fortresses.
                    mask("#...#...#", "##.###.##", ".#######.", "1#######2", ".3#####4.", "..3###4..", "...3#4...", ".........")
                    rails += VolleyRailSeed(.72f, 5.25f, 3.18f, 3.92f, 23f)
                    rails += VolleyRailSeed(8.28f, 5.25f, 5.82f, 3.92f, 23f)
                    rails += VolleyRailSeed(3.18f, 3.92f, 4.18f, 4.52f, 19f)
                    rails += VolleyRailSeed(5.82f, 3.92f, 4.82f, 4.52f, 19f)
                    put(3, 4, BrickType.POWERUP_CARRIER, hp = .88f)
                    pickups += VolleyPickupSeed(4.5f, 5.38f, 2)
                }

                VolleyStageArchetype.CHEVRON_CASCADE -> {
                    // Three descending chevrons create alternating bank lanes like the supplied
                    // Dark Forest and Frozen Pass references without violating the 9x8 grid.
                    mask("1#2...1#2", "#1#2.1#2#", "##1###2##", ".##1#2##.", "..##.##..", ".1#...#2.", "3.......4", ".........")
                    rails += VolleyRailSeed(.68f, 5.22f, 2.92f, 4.02f, 22f)
                    rails += VolleyRailSeed(3.22f, 4.02f, 4.18f, 4.62f, 19f)
                    rails += VolleyRailSeed(8.32f, 5.22f, 6.08f, 4.02f, 22f)
                    rails += VolleyRailSeed(5.78f, 4.02f, 4.82f, 4.62f, 19f)
                    pickups += VolleyPickupSeed(4.5f, 5.52f, 2)
                }

                VolleyStageArchetype.WORLD_CORE -> Unit
            }

            addWorldMechanics(world, stage, ::put, ::putSeed)
            if (stage >= 8) {
                // Late-world images become reinforced geometric chambers rather than another
                // copy of an early board. Keep six open cells on the final row for fair aiming.
                shapeExisting(6, 0, VolleyPieceShape.TRIANGLE_SW)
                shapeExisting(6, 8, VolleyPieceShape.TRIANGLE_SE)
                rails += VolleyRailSeed(.75f, 6.30f, 2.35f, 5.45f, 23f, fixed = stage >= 9)
                rails += VolleyRailSeed(8.25f, 6.30f, 6.65f, 5.45f, 23f, fixed = stage >= 9)
            }
            if (bonus) {
                // Reward rooms are still puzzles: the valuable carriers sit behind the fork instead
                // of replacing the board with a flat giveaway row.
                listOf(0 to 2, 0 to 6, 2 to 2, 2 to 6).forEach { (r, c) ->
                    put(r, c, BrickType.RANDOM_INVENTORY_POWERUP, hp = .64f)
                }
                pickups += VolleyPickupSeed(4.5f, 5.65f, 3)
            }

            addStageRewardNode(world, stage, pickups)
            applyHealthRhythm(world, stage, grid, material)
        }

        // Begin with a manageable volley; grant more balls as ranked boards become sturdier.
        // Boss durability is keyed to world order, not that world's variable image count. This
        // keeps a one-image future world stronger than the preceding world's tenth stage.
        // The old curve ended around 9 HP, which made late volleys collapse too quickly. Every
        // image now advances one global durability curve from 3 to 30; runtime still caps ordinary
        // targets at 30 while bosses retain their dedicated multiplier.
        val baseHp = 3 + ((levelId - 1) * 27 / (TOTAL_LEVELS - 1).coerceAtLeast(1))
        val extraRows = if (levelId == 1) 0 else (VolleyRankedGeometry.mask(levelId).size - VolleyRankedGeometry.ROWS)
        val startingBalls = (
            12 + ((levelId - 1) * 22 / (TOTAL_LEVELS - 1).coerceAtLeast(1)) +
                extraRows * 2
            ).coerceAtMost(34)
        val parTurns = (5 + world / 3 + stage / 3 + (if (boss) 2 else 0) + extraRows / 2).coerceIn(5, 12)
        val label = if (boss) {
            "${worlds[world - 1].name} • CORE"
        } else {
            "${worlds[world - 1].name} • ${stageLabels[(stage - 1) % (stageLabels.size - 1)]}"
        }

        val ranked = levelId in 2..TOTAL_LEVELS
        val rankedCells = if (!ranked) {
            grid.map { it.toList() }
        } else {
            VolleyRankedFormation.cells(levelId, world, baseHp, grid, material)
        }
        // Introduce one early Item, then offer a second, visible Item route in longer worlds.
        // World stage four already carries its authored Mystery node.
        if (ranked && (levelId == 2 || stage == 8)) {
            pickups += VolleyPickupSeed(4.5f, 8.5f, type = VolleyBonusNodeType.MYSTERY)
        }
        val rankedPickups = if (!ranked) {
            pickups.toList()
        } else {
            VolleyRankedFormation.pickups(pickups, rankedCells)
        }
        val requiredWork = rankedCells.flatten().filterNotNull().sumOf { seed ->
            val rawHp = maxOf(1, (baseHp * seed.hpScale).roundToInt())
            val scaledHp = rawHp.coerceAtMost(VolleySession.MAX_STANDARD_BRICK_HP)
            when (seed.type) {
                BrickType.INDESTRUCTIBLE, BrickType.BLACK_HOLE_TELEPORTER, BrickType.SPIKED_HAZARD -> 0

                BrickType.NORMAL_ONE_HIT, BrickType.ROUGH_STONE,
                BrickType.LIGHTNING_SPEED_PASS_THROUGH, BrickType.TRANSPARENT_SLOW_PASS_THROUGH -> 1

                BrickType.BOSS_CORE -> maxOf(40, (rawHp * VolleySession.BOSS_HP_MULTIPLIER).roundToInt())

                BrickType.ARMORED_TWO_HIT -> maxOf(2, (2f * seed.hpScale).roundToInt())

                BrickType.ARMORED_THREE_HIT -> maxOf(3, (3f * seed.hpScale).roundToInt())

                BrickType.REGENERATING -> maxOf(2, scaledHp)

                else -> scaledHp
            }
        }
        val stageDescent = VolleyRankedFormation.descentDistance(levelId)
        val twoStar = maxOf(1_200, requiredWork * 17 + parTurns * 90)
        val threeStar = maxOf(twoStar + 500, requiredWork * 25 + parTurns * 130)

        return VolleyStageDefinition(
            levelId = levelId,
            world = world,
            stage = stage,
            name = label,
            rows = if (ranked) rankedCells.size else rows,
            columns = if (ranked) VolleyRankedGeometry.COLUMNS else columns,
            cells = rankedCells,
            authoredBricks = if (levelId == 1) VolleyOpeningSalvo.bricks else emptyList(),
            rails = if (ranked) VolleyRankedFormation.rails(levelId) else emptyList(),
            pickups = rankedPickups,
            startingBalls = startingBalls,
            baseHp = baseHp,
            descentDistance = stageDescent,
            parTurns = parTurns,
            bonusStage = bonus,
            bossStage = boss,
            archetype = archetype,
            carrierDropTable = if (bonus) {
                listOf(VolleyCarrierReward.BALL, VolleyCarrierReward.RANDOM_POSITIVE_ITEM)
            } else {
                listOf(VolleyCarrierReward.BALL)
            },
            backgroundId = world,
            backgroundAsset = backgroundAsset(world, stage),
            defaultVisualFamily = VolleyAssetCatalog.familyFor(material.first(), world),
            twoStarScore = twoStar,
            threeStarScore = threeStar
        )
    }

    /**
     * World mechanics follow a teaching curve: one recognizable rule is introduced,
     * then later stages combine it with the already-learned ricochet geometry.
     */
    private fun addWorldMechanics(
        world: Int,
        stage: Int,
        put: (Int, Int, BrickType, Int, Float) -> Unit,
        putSeed: (Int, Int, VolleyBrickSeed) -> Unit
    ) {
        when (world) {
            // Verdant Ruins: aiming, simple durability and basic rewards.
            1 -> {
                if (stage >= 2) put(1, 2, BrickType.GLASS, 0, .65f)
                if (stage >= 3) put(2, 4, BrickType.ROUGH_STONE, 0, .75f)
                if (stage >= 4) put(1, 6, BrickType.POWERUP_CARRIER, 0, .75f)
                if (stage >= 6) put(3, 4, BrickType.NORMAL_ONE_HIT, 0, .55f)
            }

            // Glacial Gate: armored targets plus slow/fast pass-through ice lanes.
            2 -> {
                put(1, 2, BrickType.ARMORED_TWO_HIT, 0, 1f)
                if (stage >= 3) put(2, 6, BrickType.TRANSPARENT_SLOW_PASS_THROUGH, 0, .7f)
                if (stage >= 4) put(3, 2, BrickType.LIGHTNING_SPEED_PASS_THROUGH, 0, .7f)
                if (stage >= 5) put(1, 6, BrickType.ARMORED_THREE_HIT, 0, 1.05f)
                if (stage >= 6) put(2, 4, BrickType.POWERUP_CARRIER, 0, .85f)
            }

            // Sunken Dunes: moving bricks and regeneration force target priority.
            3 -> {
                put(1, 1, BrickType.MOVING_HORIZONTAL, 0, .85f)
                if (stage >= 3) put(3, 7, BrickType.MOVING_VERTICAL, 0, .85f)
                if (stage >= 4) put(2, 4, BrickType.REGENERATING, 0, 1.1f)
                if (stage >= 6) put(1, 6, BrickType.ROUGH_STONE, 0, .85f)
            }

            // Ember Foundry: explosions create chain opportunities among tougher stone.
            4 -> {
                put(1, 4, BrickType.STONE_GRAY, 0, 1.05f)
                if (stage >= 3) put(3, 2, BrickType.EXPLOSIVE, 0, .82f)
                if (stage >= 4) put(3, 6, BrickType.EXPLOSIVE, 0, .82f)
                if (stage >= 6) put(2, 4, BrickType.REGENERATING, 0, 1.15f)
            }

            // Moonlit Isles: switch/ghost puzzles arrive as a paired, always-solvable mechanic.
            5 -> {
                val group = 500 + stage
                if (stage >= 2) {
                    putSeed(1, 3, VolleyBrickSeed(BrickType.SWITCH, groupId = group, hpScale = .7f, countsForClear = false))
                    put(2, 5, BrickType.GHOST, group, 1f)
                }
                if (stage >= 5) {
                    putSeed(3, 3, VolleyBrickSeed(BrickType.SWITCH, groupId = group + 20, hpScale = .7f, countsForClear = false))
                    put(3, 6, BrickType.GHOST, group + 20, 1.05f)
                }
            }

            // Tidal Relics: locks and keys turn route order into the puzzle.
            6 -> {
                val group = 600 + stage
                if (stage >= 2) {
                    put(1, 1, BrickType.KEY_BRICK, group, .72f)
                    put(2, 7, BrickType.LOCKED, group, 1.20f)
                }
                if (stage >= 5) {
                    put(3, 1, BrickType.KEY_BRICK, group + 20, .78f)
                    put(3, 6, BrickType.LOCKED, group + 20, 1.30f)
                }
            }

            // Crystal Grotto: portals, chains and hazards reward controlled bank shots.
            7 -> {
                val portalGroup = 700 + stage
                if (stage >= 2) {
                    putSeed(1, 1, VolleyBrickSeed(BrickType.BLACK_HOLE_TELEPORTER, groupId = portalGroup, countsForClear = false))
                    putSeed(3, 7, VolleyBrickSeed(BrickType.BLACK_HOLE_TELEPORTER, groupId = portalGroup, countsForClear = false))
                }
                if (stage >= 4) {
                    put(2, 2, BrickType.CHAIN_BRICK, 7, .9f)
                    put(2, 4, BrickType.CHAIN_BRICK, 7, .9f)
                    put(2, 6, BrickType.CHAIN_BRICK, 7, .9f)
                }
                if (stage >= 6) {
                    putSeed(4, 4, VolleyBrickSeed(BrickType.SPIKED_HAZARD, countsForClear = false, hazardKind = VolleyHazardKind.SPIKED))
                }
            }

            // Iron Furnace: indestructible blockers plus moving/regenerating targets.
            8 -> {
                if (stage >= 2) {
                    putSeed(2, 1, VolleyBrickSeed(BrickType.INDESTRUCTIBLE, countsForClear = false))
                    putSeed(2, 7, VolleyBrickSeed(BrickType.INDESTRUCTIBLE, countsForClear = false))
                }
                if (stage >= 4) put(3, 4, BrickType.REGENERATING, 0, 1.35f)
                if (stage >= 5) put(1, 4, BrickType.MOVING_HORIZONTAL, 0, 1f)
                if (stage >= 7) put(4, 4, BrickType.ARMORED_DARK, 0, 1.35f)
            }

            // Celestial Spires: precision portals + switch timing + hazards.
            9 -> {
                val portalGroup = 900 + stage
                if (stage >= 2) {
                    putSeed(1, 1, VolleyBrickSeed(BrickType.BLACK_HOLE_TELEPORTER, groupId = portalGroup, countsForClear = false))
                    putSeed(3, 7, VolleyBrickSeed(BrickType.BLACK_HOLE_TELEPORTER, groupId = portalGroup, countsForClear = false))
                }
                if (stage >= 4) {
                    val switchGroup = 920 + stage
                    putSeed(1, 3, VolleyBrickSeed(BrickType.SWITCH, groupId = switchGroup, hpScale = .75f, countsForClear = false))
                    put(2, 5, BrickType.GHOST, switchGroup, 1.15f)
                }
                if (stage >= 6) putSeed(4, 4, VolleyBrickSeed(BrickType.SPIKED_HAZARD, countsForClear = false, hazardKind = VolleyHazardKind.LASER))
            }

            // Shadow Sanctum: final-world boards deliberately combine previously learned systems.
            10 -> {
                val lockGroup = 1000 + stage
                put(1, 1, BrickType.KEY_BRICK, lockGroup, .8f)
                put(2, 7, BrickType.LOCKED, lockGroup, 1.40f)
                if (stage >= 3) {
                    put(3, 2, BrickType.CHAIN_BRICK, 10, 1f)
                    put(3, 6, BrickType.CHAIN_BRICK, 10, 1f)
                }
                if (stage >= 4) put(2, 4, BrickType.EXPLOSIVE, 0, 1f)
                if (stage >= 5) {
                    val portalGroup = 1050 + stage
                    putSeed(1, 3, VolleyBrickSeed(BrickType.BLACK_HOLE_TELEPORTER, groupId = portalGroup, countsForClear = false))
                    putSeed(4, 5, VolleyBrickSeed(BrickType.BLACK_HOLE_TELEPORTER, groupId = portalGroup, countsForClear = false))
                }
                if (stage >= 6) putSeed(4, 4, VolleyBrickSeed(BrickType.SPIKED_HAZARD, countsForClear = false, hazardKind = VolleyHazardKind.DEATH))
            }
        }
    }

    /** Bosses are two-phase encounters: destroy four marked guards, then expose the core. */
    private fun buildBossFortress(
        world: Int,
        rails: MutableList<VolleyRailSeed>,
        pickups: MutableList<VolleyPickupSeed>,
        material: List<BrickType>,
        put: (Int, Int, BrickType, Int, Float) -> Unit,
        putSeed: (Int, Int, VolleyBrickSeed) -> Unit
    ) {
        listOf(
            0 to 0, 0 to 1, 0 to 7, 0 to 8,
            1 to 0, 1 to 8,
            2 to 0, 2 to 8,
            3 to 0, 3 to 8,
            4 to 0, 4 to 1, 4 to 2, 4 to 6, 4 to 7, 4 to 8
        )
            .forEachIndexed { index, (r, c) ->
                put(r, c, material[Math.floorMod(world + index, material.size)], 0, 1.22f + world * .025f)
            }
        listOf(0 to 3, 0 to 5, 2 to 3, 2 to 5, 4 to 3, 4 to 5).forEachIndexed { index, (r, c) ->
            put(r, c, material[Math.floorMod(world * 2 + index, material.size)], 0, 1.30f + world * .03f)
        }
        val fortressReinforcements = buildList {
            addAll(listOf(0 to 2, 0 to 6, 1 to 1, 1 to 7, 2 to 1, 2 to 2, 2 to 6, 2 to 7, 3 to 1, 3 to 7, 4 to 4))
            if (world >= 6) addAll(listOf(1 to 3, 1 to 5))
        }
        fortressReinforcements.forEachIndexed { index, (r, c) ->
            put(r, c, material[Math.floorMod(world * 5 + index, material.size)], 0, 1.26f + world * .028f)
        }
        // Lower ricochet wings create three deliberate entry routes instead of two empty rows.
        listOf(
            5 to 0, 5 to 1, 5 to 2, 5 to 6, 5 to 7, 5 to 8,
            6 to 0, 6 to 1, 6 to 3, 6 to 5, 6 to 7, 6 to 8,
            7 to 1, 7 to 2, 7 to 6, 7 to 7
        ).forEachIndexed { index, (r, c) ->
            put(r, c, material[Math.floorMod(world * 7 + index, material.size)], 0, 1.20f + world * .026f)
        }

        val guardType = when (world) {
            1 -> BrickType.ARMORED_TWO_HIT
            2 -> BrickType.ARMORED_THREE_HIT
            3 -> BrickType.REGENERATING
            4 -> BrickType.EXPLOSIVE
            5 -> BrickType.ARMORED_DARK
            6 -> BrickType.ARMORED_DARK
            7 -> BrickType.CHAIN_BRICK
            8 -> BrickType.REGENERATING
            9 -> BrickType.ARMORED_DARK
            else -> BrickType.ARMORED_THREE_HIT
        }
        listOf(1 to 2, 1 to 6, 3 to 2, 3 to 6).forEach { (r, c) ->
            putSeed(
                r,
                c,
                VolleyBrickSeed(
                    type = guardType,
                    // Boss guards are independent weak points. Keep groupId=0 so the generic
                    // group-shield mechanic cannot consume a different guard when this one is hit.
                    groupId = 0,
                    hpScale = 1.35f + world * .035f,
                    countsForClear = true,
                    shieldLock = true
                )
            )
        }
        // The physical core sits at the top center, aligned with the framed boss portrait.
        putSeed(0, 4, VolleyBrickSeed(BrickType.BOSS_CORE, groupId = 2_100 + world, hpScale = 3.35f + world * .13f, countsForClear = true))

        // Boss geometry always has two readable bank-shot wings; later bosses lock those wings in
        // place so the challenge comes from route choice instead of visually noisy extra barriers.
        rails += VolleyRailSeed(.75f, 5.05f, 2.55f, 4.10f, 25f, fixed = world >= 6)
        rails += VolleyRailSeed(8.25f, 5.05f, 6.45f, 4.10f, 25f, fixed = world >= 6)
        if (world >= 5) {
            rails += VolleyRailSeed(3.35f, 4.30f, 4.15f, 4.78f, 19f, fixed = world >= 8)
            rails += VolleyRailSeed(5.65f, 4.30f, 4.85f, 4.78f, 19f, fixed = world >= 8)
        }
        pickups += VolleyPickupSeed(4.5f, 5.60f, if (world >= 7) 3 else 2)

        // A single world-specific mechanic is layered into each boss without compromising the four
        // guard weak points that communicate phase one.
        when (world) {
            3 -> put(1, 4, BrickType.MOVING_HORIZONTAL, 0, 1.1f)

            4 -> put(3, 4, BrickType.EXPLOSIVE, 0, 1f)

            5 -> {
                val g = 2_500 + world
                putSeed(1, 4, VolleyBrickSeed(BrickType.SWITCH, groupId = g, hpScale = .75f, countsForClear = false))
                put(3, 4, BrickType.GHOST, g, 1.1f)
            }

            6 -> {
                val g = 2_600 + world
                put(1, 4, BrickType.KEY_BRICK, g, .8f)
                put(3, 4, BrickType.LOCKED, g, 1.35f)
            }

            7 -> {
                val g = 2_700 + world
                putSeed(1, 4, VolleyBrickSeed(BrickType.BLACK_HOLE_TELEPORTER, groupId = g, countsForClear = false))
                putSeed(4, 4, VolleyBrickSeed(BrickType.BLACK_HOLE_TELEPORTER, groupId = g, countsForClear = false))
            }

            8 -> {
                putSeed(1, 4, VolleyBrickSeed(BrickType.INDESTRUCTIBLE, countsForClear = false))
                putSeed(4, 4, VolleyBrickSeed(BrickType.INDESTRUCTIBLE, countsForClear = false))
            }

            9 -> putSeed(4, 4, VolleyBrickSeed(BrickType.SPIKED_HAZARD, countsForClear = false, hazardKind = VolleyHazardKind.LASER))

            10 -> {
                val g = 3_000 + world
                put(1, 4, BrickType.KEY_BRICK, g, .82f)
                put(3, 4, BrickType.LOCKED, g, 1.45f)
                putSeed(4, 4, VolleyBrickSeed(BrickType.SPIKED_HAZARD, countsForClear = false, hazardKind = VolleyHazardKind.DEATH))
            }
        }
    }

    private fun addStageRewardNode(world: Int, stage: Int, pickups: MutableList<VolleyPickupSeed>) {
        val nodeX = 1.4f + Math.floorMod(world * 2 + stage * 3, 7)
        when (stage) {
            2 -> pickups += VolleyBonusNodeSeed(nodeX, 5.28f, balls = 1, type = VolleyBonusNodeType.SPEED)
            3 -> pickups += VolleyBonusNodeSeed(nodeX, 5.35f, balls = 1, type = VolleyBonusNodeType.CROWN, scoreValue = 450 + world * 40)
            4 -> pickups += VolleyBonusNodeSeed(nodeX, 5.20f, balls = 1, type = VolleyBonusNodeType.MYSTERY)
            5 -> pickups += VolleyBonusNodeSeed(nodeX, 5.05f, balls = 1 + world / 3, type = VolleyBonusNodeType.COINS)
            6 -> pickups += VolleyBonusNodeSeed(nodeX, 5.55f, balls = 1, type = VolleyBonusNodeType.STAR, scoreValue = 600 + world * 50)
            7 -> pickups += VolleyBonusNodeSeed(nodeX, 5.45f, balls = 1, type = VolleyBonusNodeType.SHIELD)
        }
    }

    private fun applyHealthRhythm(
        world: Int,
        stage: Int,
        grid: MutableList<MutableList<VolleyBrickSeed?>>,
        material: List<BrickType>
    ) {
        val ordinary = material.toSet()
        val scales = when {
            world <= 2 -> floatArrayOf(.82f, 1f, 1.12f)
            world <= 5 -> floatArrayOf(.86f, 1.02f, 1.16f)
            world <= 8 -> floatArrayOf(.9f, 1.06f, 1.2f, 1.3f)
            else -> floatArrayOf(.94f, 1.08f, 1.22f, 1.34f)
        }
        grid.forEachIndexed { row, line ->
            line.forEachIndexed { column, seed ->
                if (seed != null && seed.type in ordinary && seed.hpScale == 1f) {
                    val rhythm = scales[Math.floorMod(row * 3 + column * 5 + world + stage, scales.size)]
                    line[column] = seed.copy(hpScale = rhythm)
                }
            }
        }
    }

    private fun volleyMaterials(world: Int): List<BrickType> = when (world) {
        1 -> listOf(BrickType.CRYSTAL_BLUE, BrickType.CRYSTAL_MAROON, BrickType.NORMAL_ONE_HIT)
        2 -> listOf(BrickType.CRYSTAL_MAROON, BrickType.ARMORED_DARK, BrickType.CRYSTAL_BLUE)
        3 -> listOf(BrickType.CRYSTAL_GREEN, BrickType.CRYSTAL_BLUE, BrickType.ROUGH_STONE)
        4 -> listOf(BrickType.STONE_GRAY, BrickType.CRYSTAL_ORANGE, BrickType.CRYSTAL_RED)
        5 -> listOf(BrickType.CRYSTAL_MAROON, BrickType.CRYSTAL_GREEN, BrickType.ELECTRIC_WHITE)
        6 -> listOf(BrickType.ELECTRIC_WHITE, BrickType.CRYSTAL_PURPLE, BrickType.ARMORED_DARK)
        7 -> listOf(BrickType.CRYSTAL_BLUE, BrickType.CRYSTAL_GREEN, BrickType.CRYSTAL_PINK)
        8 -> listOf(BrickType.ELECTRIC_WHITE, BrickType.CRYSTAL_BLUE, BrickType.STONE_GRAY, BrickType.ARMORED_DARK)
        9 -> listOf(BrickType.CRYSTAL_PURPLE, BrickType.ARMORED_DARK, BrickType.CRYSTAL_PINK)
        10 -> listOf(BrickType.CRYSTAL_RED, BrickType.CRYSTAL_ORANGE, BrickType.ARMORED_DARK, BrickType.CRYSTAL_PURPLE)
        else -> error("Unsupported Volley world: $world")
    }
}
