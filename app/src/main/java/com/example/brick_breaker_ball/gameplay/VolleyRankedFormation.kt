/*
 * المسار: app/src/main/java/com/example/brick_breaker_ball/gameplay/VolleyRankedFormation.kt
 * الوظيفة: تكوين تشكيلات المراحل المصنفة.
 * الحسابات: تحويل الصفوف والأعمدة إلى قطع طوب.
 */

package com.example.brick_breaker_ball.gameplay

import com.example.brick_breaker_ball.campaign.VolleyBrickSeed
import com.example.brick_breaker_ball.campaign.VolleyPickupSeed
import com.example.brick_breaker_ball.campaign.VolleyRailSeed
import com.example.brick_breaker_ball.campaign.VolleyStageRepository
import kotlin.math.abs
import kotlin.math.ceil
import kotlin.math.roundToInt

/** Keeps the ranked silhouettes while placing reachable puzzle controls and bounded HP. */
internal object VolleyRankedFormation {
    /** Three upper slants and three lower posts from the supplied stage-eight reference. */
    fun rails(levelId: Int): List<VolleyRailSeed> {
        if (levelId != 11) return emptyList()
        return listOf(
            VolleyRailSeed(.58f, .10f, 2.40f, 1.03f, 24f),
            VolleyRailSeed(5.05f, .08f, 5.90f, 1.32f, 24f),
            VolleyRailSeed(9.90f, .08f, 9.03f, 1.32f, 24f),
            VolleyRailSeed(1.48f, 5.55f, 1.48f, 7.47f, 24f),
            VolleyRailSeed(5.52f, 5.55f, 5.52f, 7.47f, 24f),
            VolleyRailSeed(9.48f, 5.55f, 9.48f, 7.47f, 24f)
        )
    }

    /** Keep descent visible, while the longer formations retain room to be cleared. */
    fun descentDistance(levelId: Int): Float {
        val progress = (levelId - 1).toFloat() / (VolleyStageRepository.TOTAL_LEVELS - 1).coerceAtLeast(1)
        val gradual = (48f - 26f * progress).coerceIn(22f, 48f)
        val world = VolleyStageRepository.worldForLevel(levelId)
        val boss = levelId == VolleyStageRepository.lastLevel(world)
        return when {
            // The Aqua Vortex core previously shifted less than two-thirds of one brick.
            world == 3 && boss -> 58f

            boss -> maxOf(40f, gradual)

            else -> maxOf(30f, gradual)
        }
    }

    fun ordinaryHp(levelId: Int, row: Int, column: Int): Int {
        val weak = baseOrdinaryHp(levelId)
        return when ((row * 13 + column * 7) % 20) {
            in 0..10 -> weak
            in 11..18 -> weak + 4
            else -> weak + 6
        }
    }

    private fun baseOrdinaryHp(levelId: Int): Int = 6 +
        (levelId - 2).coerceAtLeast(0) * 10 /
        (VolleyStageRepository.TOTAL_LEVELS - 2).coerceAtLeast(1)

    /** Keep the requested late-stage shape changes from undoing their earlier easing. */
    fun formationHp(levelId: Int, row: Int, column: Int): Int {
        val scale = when (levelId) {
            73 -> .85f
            74 -> .85f
            75 -> .80f
            88 -> .65f
            90 -> .65f
            else -> 1f
        }
        return (ordinaryHp(levelId, row, column) * scale).roundToInt().coerceAtLeast(2)
    }

    private fun multiHitType(type: BrickType, replacement: BrickType): BrickType = when (type) {
        BrickType.NORMAL_ONE_HIT, BrickType.ROUGH_STONE,
        BrickType.LIGHTNING_SPEED_PASS_THROUGH, BrickType.TRANSPARENT_SLOW_PASS_THROUGH -> replacement

        else -> type
    }

    fun cells(
        levelId: Int,
        world: Int,
        baseHp: Int,
        source: List<List<VolleyBrickSeed?>>,
        materials: List<BrickType>
    ): List<List<VolleyBrickSeed?>> {
        val lines = VolleyRankedGeometry.mask(levelId)
        require(lines.isNotEmpty())
        require(lines.all { it.length == VolleyRankedGeometry.COLUMNS })
        val bottomRow = lines.indexOfLast { row -> row.any { it != '.' } }
        require(bottomRow >= 0)
        val startingClearance = VolleySession.BRICK_START_TOP -
            (bottomRow + 1) * VolleySession.ROW_PITCH - VolleySession.DANGER_Y
        require(startingClearance >= descentDistance(levelId) * 3f) {
            "Volley rank $levelId starts too close to the danger line"
        }
        val weakHp = baseOrdinaryHp(levelId)
        val multiHitMaterial = materials.first { it !in setOf(BrickType.NORMAL_ONE_HIT, BrickType.ROUGH_STONE) }
        val grid = MutableList(lines.size) { row ->
            MutableList<VolleyBrickSeed?>(VolleyRankedGeometry.COLUMNS) { column ->
                if (lines[row][column] == '.') {
                    null
                } else {
                    val paletteType = if (world == 1) {
                        when (Math.floorMod(row * 7 + column * 3 + levelId, 13)) {
                            in 0..9 -> BrickType.CRYSTAL_GREEN
                            10, 11 -> BrickType.CRYSTAL_MAROON
                            else -> BrickType.ARMORED_DARK
                        }
                    } else {
                        materials[Math.floorMod(row + column + world, materials.size)]
                    }
                    val type = multiHitType(paletteType, multiHitMaterial)
                    val hp = formationHp(levelId, row, column)
                    val shape = shapeOf(lines[row][column])
                    val sprite = if (world == 1 && type == BrickType.CRYSTAL_GREEN &&
                        shape == VolleyPieceShape.RECTANGLE
                    ) {
                        val variant = when (Math.floorMod(row * 7 + column * 3 + levelId, 11)) {
                            in 0..5 -> "004"
                            in 6..8 -> "005"
                            9 -> "012"
                            else -> "050"
                        }
                        "spritesheet_c1842d55_$variant.png"
                    } else {
                        null
                    }
                    VolleyBrickSeed(type, shape, hpScale = hp.toFloat() / baseHp, atlasSpriteName = sprite)
                }
            }
        }

        fun launchFacing(row: Int, column: Int): Boolean = ((row + 1) until lines.size).none { lines[it][column] != '.' }

        // Preserve puzzle groups while giving their controls an exposed face toward the launcher.
        val special = source.flatMapIndexed { row, cells ->
            cells.mapIndexedNotNull { column, seed ->
                if (seed != null && (seed.type !in materials || seed.groupId != 0 || seed.shieldLock)) {
                    Triple(row, column, seed)
                } else {
                    null
                }
            }
        }.sortedWith(
            compareBy<Triple<Int, Int, VolleyBrickSeed>> { (_, _, seed) ->
                when {
                    seed.type == BrickType.BOSS_CORE -> 0
                    seed.type == BrickType.KEY_BRICK || seed.type == BrickType.SWITCH -> 1
                    seed.shieldLock -> 2
                    seed.type == BrickType.RANDOM_INVENTORY_POWERUP -> 3
                    seed.type == BrickType.BLACK_HOLE_TELEPORTER -> 4
                    seed.type == BrickType.LOCKED || seed.type == BrickType.GHOST -> 5
                    else -> 6
                }
            }.thenBy { it.first }.thenBy { it.second }
        )
        val used = mutableSetOf<Pair<Int, Int>>()
        val controllerRows = mutableMapOf<Int, Int>()
        special.forEach { (sourceRow, sourceColumn, seed) ->
            val targetRow = sourceRow * (lines.size - 1f) / (source.size - 1).coerceAtLeast(1)
            val targetColumn = sourceColumn * (VolleyRankedGeometry.COLUMNS - 1f) / (source[0].size - 1).coerceAtLeast(1)
            // Moon's reordered boss has a crown core and two reachable lower guards.
            val pinnedBossPiece = when {
                // The photographed lower ice arm exposes a square switch tile.
                levelId == 84 && seed.type == BrickType.SWITCH -> 11 to 7

                levelId == 70 && seed.type == BrickType.BOSS_CORE -> 0 to 5

                levelId == 70 && seed.shieldLock -> when (sourceRow to sourceColumn) {
                    1 to 2 -> 1 to 3
                    1 to 6 -> 1 to 7
                    3 to 2 -> 8 to 3
                    3 to 6 -> 8 to 7
                    else -> null
                }

                levelId == 70 && seed.type == BrickType.BLACK_HOLE_TELEPORTER -> when (sourceRow to sourceColumn) {
                    1 to 4 -> 4 to 1
                    4 to 4 -> 4 to 9
                    else -> null
                }

                levelId == 90 && seed.shieldLock -> when (sourceRow to sourceColumn) {
                    1 to 2 -> 1 to 2
                    1 to 6 -> 1 to 8
                    3 to 2 -> 8 to 2
                    3 to 6 -> 8 to 7
                    else -> null
                }

                else -> null
            }
            if (pinnedBossPiece != null) {
                require(grid[pinnedBossPiece.first][pinnedBossPiece.second] != null && pinnedBossPiece !in used) {
                    "Volley rank $levelId boss cell $pinnedBossPiece is unavailable"
                }
            }
            val position = pinnedBossPiece ?: grid.indices.flatMap { row ->
                grid[row].indices.mapNotNull { column ->
                    if (grid[row][column] != null && (row to column) !in used) row to column else null
                }
            }.minByOrNull { (row, column) ->
                val dy = row - targetRow
                val dx = column - targetColumn
                val exposed = launchFacing(row, column)
                val exposurePenalty = when {
                    seed.type == BrickType.KEY_BRICK || seed.type == BrickType.SWITCH -> if (exposed) 0f else 400f
                    seed.shieldLock || seed.type == BrickType.RANDOM_INVENTORY_POWERUP -> if (exposed) 0f else 35f
                    seed.type == BrickType.BOSS_CORE -> row * 50f
                    seed.type == BrickType.INDESTRUCTIBLE || seed.type == BrickType.SPIKED_HAZARD -> if (exposed) 25f else 0f
                    else -> 0f
                }
                val behindController = if (seed.type == BrickType.LOCKED || seed.type == BrickType.GHOST) {
                    val controlRow = controllerRows[seed.groupId]
                    if (controlRow != null && row >= controlRow) 250f else 0f
                } else {
                    0f
                }
                dy * dy + dx * dx + exposurePenalty + behindController +
                    (if (seed.type == BrickType.BOSS_CORE && column !in 3..7) 220f else 0f) +
                    (if (lines[row][column] == '#') 0f else 64f) +
                    (if (seed.type == BrickType.BOSS_CORE) abs(column - 5) * 3f else 0f)
            }
            val (row, column) = requireNotNull(position) {
                "Volley rank $levelId has too few occupied cells for its puzzle pieces"
            }
            val hp = when {
                // VolleySession rounds base HP before applying the boss multiplier.
                // Pick the smallest integer that still rounds to the target boss tier.
                seed.type == BrickType.BOSS_CORE ->
                    ceil((72 + world * 18 - .5f) / VolleySession.BOSS_HP_MULTIPLIER) / baseHp

                seed.shieldLock -> (weakHp + 6 + world).toFloat() / baseHp

                seed.type == BrickType.LOCKED || seed.type == BrickType.GHOST || seed.type == BrickType.REGENERATING ->
                    (weakHp + 3).toFloat() / baseHp

                seed.type == BrickType.SWITCH -> 2f / baseHp

                else -> formationHp(levelId, row, column).toFloat() / baseHp
            }
            val type = when {
                seed.shieldLock && seed.type in setOf(BrickType.ARMORED_TWO_HIT, BrickType.ARMORED_THREE_HIT) ->
                    BrickType.ARMORED_DARK

                else -> multiHitType(seed.type, multiHitMaterial)
            }
            grid[row][column] = seed.copy(type = type, shape = VolleyPieceShape.RECTANGLE, hpScale = hp)
            used += position
            if (seed.type == BrickType.KEY_BRICK || seed.type == BrickType.SWITCH) {
                controllerRows[seed.groupId] = row
            }
        }
        return grid.map { it.toList() }
    }

    /** A bottom-open cell, or a clear approach just below a filled last row. */
    fun pickups(
        seeds: List<VolleyPickupSeed>,
        cells: List<List<VolleyBrickSeed?>>
    ): List<VolleyPickupSeed> {
        val used = mutableSetOf<Int>()
        return seeds.map { seed ->
            val preferred = (seed.x * VolleyRankedGeometry.COLUMNS / 9f - .5f)
                .toInt().coerceIn(0, VolleyRankedGeometry.COLUMNS - 1)
            val open = cells.last().indices.filter { cells.last()[it] == null && it !in used }
            val column = (open.ifEmpty { cells.last().indices.filter { it !in used } })
                .minByOrNull { abs(it - preferred) } ?: preferred
            used += column
            val y = if (cells.last()[column] == null) {
                cells.size - .5f
            } else {
                cells.size + .5f
            }
            seed.copy(x = column + .5f, y = y)
        }
    }

    private fun shapeOf(symbol: Char): VolleyPieceShape = when (symbol) {
        '#' -> VolleyPieceShape.RECTANGLE
        '1' -> VolleyPieceShape.TRIANGLE_NW
        '2' -> VolleyPieceShape.TRIANGLE_NE
        '3' -> VolleyPieceShape.TRIANGLE_SW
        '4' -> VolleyPieceShape.TRIANGLE_SE
        else -> error("Unsupported ranked brick symbol: $symbol")
    }
}
