/*
 * الوظيفة: توليد تشكيلات Volley أصلية وحتمية قبل بدء اللعب.
 * الحسابات: أشكال هندسية بإحداثيات نسبية، وضبط الصعوبة، وفحص البنية دون تغيير الفيزياء.
 */
package com.example.brick_breaker_ball.gameplay

import com.example.brick_breaker_ball.campaign.VolleyBrickSeed
import com.example.brick_breaker_ball.campaign.VolleyPickupSeed
import com.example.brick_breaker_ball.campaign.VolleyStageDefinition
import com.example.brick_breaker_ball.campaign.VolleyStageRepository
import java.util.Random
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.max
import kotlin.math.roundToInt
import kotlin.math.sin
import kotlin.math.sqrt

enum class VolleyPattern {
    GRID,
    PYRAMID,
    INVERTED_PYRAMID,
    TRIANGLE,
    DIAMOND,
    CIRCLE,
    RING,
    HEXAGON,
    SPIRAL,
    ZIGZAG,
    WAVE,
    CROSS,
    SYMMETRICAL,
    DIAGONAL,
    TWIN_CHANNELS,
    MAZE,
    LAYERED,
    BOSS_ARENA,
    HYBRID
}

data class VolleyGeneratorSpec(
    val pattern: VolleyPattern = VolleyPattern.DIAMOND,
    val seed: Long = 1L,
    val difficulty: Int = 3,
    val density: Float = .78f,
    val rotation: Int = 0,
    val reflectX: Boolean = false,
    val scale: Float = 1f,
    val translateX: Float = 0f,
    val translateY: Float = 0f,
    val spacing: Int = 1,
    val combineWith: VolleyPattern? = null
)

data class VolleyGenerationStats(
    val brickCount: Int,
    val averageHp: Float,
    val maxHp: Int,
    val openBottomColumns: Int,
    val estimatedWork: Int
)

data class VolleyGeneratedLevel(
    val stage: VolleyStageDefinition,
    val spec: VolleyGeneratorSpec,
    val stats: VolleyGenerationStats
)

/** Stage inputs stay compatible with VolleySession's existing 11-column portrait grid. */
object VolleyLevelGenerator {
    const val COLUMNS = 11
    const val ROWS = 9

    fun forCampaignLevel(levelId: Int, spec: VolleyGeneratorSpec? = null): VolleyGeneratedLevel {
        val base = VolleyStageRepository.forLevel(levelId)
        val selected = spec ?: VolleyGeneratorSpec(
            pattern = if (base.bossStage) {
                VolleyPattern.BOSS_ARENA
            } else {
                VolleyPattern.entries[(base.stage + base.world * 3) % (VolleyPattern.entries.size - 2)]
            },
            seed = levelId.toLong() * 0x5DEECE66DL + base.world,
            difficulty = (1 + (levelId - 1) * 7 / VolleyStageRepository.TOTAL_LEVELS).coerceIn(1, 9)
        )
        return generate(base, selected)
    }

    fun generate(base: VolleyStageDefinition, spec: VolleyGeneratorSpec): VolleyGeneratedLevel {
        require(spec.difficulty in 1..9 && spec.density in .35f..1f)
        require(spec.rotation in 0..3 && spec.scale in .65f..1.25f)
        require(spec.translateX in -0.25f..0.25f && spec.translateY in -0.25f..0.25f)
        require(spec.spacing in 1..3)
        val random = Random(spec.seed)
        val boss = base.bossStage || spec.pattern == VolleyPattern.BOSS_ARENA
        val pattern = if (boss) VolleyPattern.BOSS_ARENA else spec.pattern
        val cells = MutableList(ROWS) { MutableList<VolleyBrickSeed?>(COLUMNS) { null } }
        val material = VolleyStageRepository.editorMaterials(base.world)
            .firstOrNull { it.breakable && it != BrickType.NORMAL_ONE_HIT } ?: BrickType.ARMORED_TWO_HIT
        val progress = ((base.levelId - 1f) / (VolleyStageRepository.TOTAL_LEVELS - 1).coerceAtLeast(1)).coerceIn(0f, 1f)
        val baseHp = (2 + spec.difficulty + (progress * 13f).roundToInt()).coerceIn(2, 25)
        for (row in 0 until ROWS - 1) {
            for (col in 0 until COLUMNS) {
                val u = (col + .5f) / COLUMNS * 2f - 1f
                val v = (row + .5f) / (ROWS - 1) * 2f - 1f
                val transformed = inverseTransform(u, v, spec)
                val present = shape(pattern, transformed.first, transformed.second, row, col) ||
                    (spec.combineWith != null && shape(spec.combineWith, transformed.first, transformed.second, row, col))
                if (!present || (row % spec.spacing != 0 && spec.spacing > 1)) continue
                // Deterministic sparse cuts retain the recognizable silhouette and open shot routes.
                val keep = random.nextFloat() < spec.density || pattern == VolleyPattern.BOSS_ARENA
                if (!keep) continue
                val hp = (baseHp + (row / 3) + random.nextInt(3) - 1).coerceIn(2, 29)
                val type = if (spec.difficulty <= 2 && random.nextFloat() < .22f) {
                    BrickType.NORMAL_ONE_HIT
                } else {
                    material
                }
                cells[row][col] = VolleyBrickSeed(type, hpScale = hp.toFloat() / baseHp)
            }
        }
        // Always reserve a straight launch lane and the complete final row for safe opening shots.
        val lane = if (boss) 5 else 4 + random.nextInt(3)
        for (row in 4 until ROWS) cells[row][lane] = null
        if (cells.flatten().count { it != null } < 8) {
            for (row in 1..3) {
                for (col in 3..7) {
                    if (col != lane) {
                        cells[row][col] = VolleyBrickSeed(material, hpScale = 1f)
                    }
                }
            }
        }
        if (boss) {
            val guard = when (base.world) {
                1 -> BrickType.ARMORED_TWO_HIT
                2 -> BrickType.ARMORED_THREE_HIT
                else -> BrickType.ARMORED_DARK
            }
            listOf(2 to 2, 2 to 8, 5 to 2, 5 to 8).forEach { (row, col) ->
                cells[row][col] = VolleyBrickSeed(guard, hpScale = (1.1f + spec.difficulty * .06f), shieldLock = true)
            }
            for (row in 6 until ROWS) {
                cells[row][2] = null
                cells[row][8] = null
            }
            cells[0][5] = VolleyBrickSeed(
                BrickType.BOSS_CORE,
                hpScale = (2.2f + spec.difficulty * .12f),
                countsForClear = true
            )
        } else {
            if (spec.difficulty >= 3) {
                cells[5][3] = VolleyBrickSeed(BrickType.KEY_BRICK, groupId = 1, hpScale = .7f)
                cells[2][3] = VolleyBrickSeed(BrickType.LOCKED, groupId = 1, hpScale = 1.1f)
                for (row in 6 until ROWS) cells[row][3] = null
            }
            if (spec.difficulty >= 5) cells[2][7] = VolleyBrickSeed(BrickType.EXPLOSIVE, hpScale = .7f)
            if (spec.difficulty >= 7) cells[5][7] = VolleyBrickSeed(BrickType.POWERUP_CARRIER, hpScale = .8f)
            if (spec.pattern == VolleyPattern.TWIN_CHANNELS && spec.difficulty >= 6) {
                cells[1][1] = VolleyBrickSeed(BrickType.BLACK_HOLE_TELEPORTER, groupId = 2, countsForClear = false)
                cells[5][9] = VolleyBrickSeed(BrickType.BLACK_HOLE_TELEPORTER, groupId = 2, countsForClear = false)
                for (row in 6 until ROWS) cells[row][9] = null
            }
        }
        val pickups = if (boss) {
            emptyList()
        } else {
            when {
                base.bonusStage -> listOf(
                    VolleyPickupSeed(
                        5.5f,
                        8.5f,
                        type = VolleyBonusNodeType.MYSTERY
                    )
                )

                base.stage % 3 == 2 -> listOf(VolleyPickupSeed(5.5f, 8.5f, balls = 1))

                else -> emptyList()
            }
        }
        val stage = base.copy(
            name = "${base.name} • ${pattern.name.replace('_', ' ')}",
            rows = ROWS,
            columns = COLUMNS,
            cells = cells.map { it.toList() },
            authoredBricks = emptyList(),
            rails = emptyList(),
            pickups = pickups,
            baseHp = baseHp,
            startingBalls = (12 + spec.difficulty * 2 + progress.times(8).roundToInt()).coerceIn(12, 36),
            descentDistance = (42f - spec.difficulty * 1.5f).coerceAtLeast(26f),
            bossStage = boss
        )
        val errors = VolleyGeneratedLevelValidator.validate(stage)
        require(errors.isEmpty()) { "Generated Volley level invalid: ${errors.joinToString()}" }
        val health = stage.cells.flatten().filterNotNull().map { seed ->
            when (seed.type) {
                BrickType.BOSS_CORE -> max(40, (baseHp * seed.hpScale * VolleySession.BOSS_HP_MULTIPLIER).roundToInt())
                BrickType.ARMORED_TWO_HIT -> max(2, (2f * seed.hpScale).roundToInt())
                BrickType.ARMORED_THREE_HIT -> max(3, (3f * seed.hpScale).roundToInt())
                BrickType.BLACK_HOLE_TELEPORTER -> 0
                else -> (baseHp * seed.hpScale).roundToInt().coerceIn(1, VolleySession.MAX_STANDARD_BRICK_HP)
            }
        }.filter { it > 0 }
        return VolleyGeneratedLevel(
            stage,
            spec.copy(pattern = pattern),
            VolleyGenerationStats(
                brickCount = health.size,
                averageHp = health.average().toFloat(),
                maxHp = health.maxOrNull() ?: 0,
                openBottomColumns = cells.last().count { it == null },
                estimatedWork = health.sum()
            )
        )
    }

    private fun inverseTransform(u: Float, v: Float, spec: VolleyGeneratorSpec): Pair<Float, Float> {
        var x = (u - spec.translateX) / spec.scale
        var y = (v - spec.translateY) / spec.scale
        repeat(spec.rotation) {
            val old = x
            x = y
            y = -old
        }
        if (spec.reflectX) x = -x
        return x to y
    }

    private fun shape(p: VolleyPattern, x: Float, y: Float, row: Int, col: Int): Boolean {
        val ax = abs(x)
        val ay = abs(y)
        val radius = sqrt(x * x + y * y)
        val transformedRow = (((y + 1f) * (ROWS - 1) / 2f)).toInt()
        val transformedColumn = (((x + 1f) * COLUMNS / 2f)).toInt()
        return when (p) {
            VolleyPattern.GRID -> transformedRow % 2 == 0 || transformedColumn % 2 == 0

            VolleyPattern.PYRAMID -> ax < (.85f - y * .52f) && y > -.9f

            VolleyPattern.INVERTED_PYRAMID -> ax < (.85f + y * .52f) && y < .9f

            VolleyPattern.TRIANGLE -> x > -.85f && y > -.9f && x + y < .65f

            VolleyPattern.DIAMOND -> ax / .88f + ay / .86f < 1f

            VolleyPattern.CIRCLE -> radius < .82f

            VolleyPattern.RING -> radius in 0.48f..0.92f

            VolleyPattern.HEXAGON -> ay < .74f && ax + ay * .58f < .91f

            VolleyPattern.SPIRAL -> {
                val theta = atan2(y, x).toDouble() + PI
                abs(radius - (.19 + theta / (2 * PI) * .67)).toFloat() < .16f
            }

            VolleyPattern.ZIGZAG -> abs(x - sin((y + 1f) * PI * 2).toFloat() * .55f) < .22f

            VolleyPattern.WAVE -> abs(y - sin((x + 1f) * PI * 1.5).toFloat() * .45f) < .19f

            VolleyPattern.CROSS -> ax < .23f || ay < .22f

            VolleyPattern.SYMMETRICAL -> abs(ax - .49f) < .23f || (ay < .23f && ax < .83f)

            VolleyPattern.DIAGONAL -> abs(y - x * .85f) < .18f || abs(y + x * .85f) < .18f

            VolleyPattern.TWIN_CHANNELS -> ax in 0.32f..0.84f && ay < .88f

            VolleyPattern.MAZE -> (
                transformedRow % 3 == 0 && transformedColumn !=
                    (transformedRow * 2 + 3) % COLUMNS
                ) ||
                (transformedColumn % 3 == 0 && transformedRow % 3 != 1)

            VolleyPattern.LAYERED -> ay < .86f && (transformedRow % 3 != 2 || ax > .48f)

            VolleyPattern.BOSS_ARENA ->
                (ax > .56f && ay < .82f) ||
                    (ay < .24f && ax < .8f) || (y < -.50f && ax < .36f)

            VolleyPattern.HYBRID -> (ax + ay < .85f) || (ay < .15f && ax < .9f)
        }
    }
}

/** Structural checks are necessary but do not prove that every launch sequence wins. */
object VolleyGeneratedLevelValidator {
    fun validate(stage: VolleyStageDefinition): List<String> = buildList {
        if (stage.rows != VolleyLevelGenerator.ROWS || stage.columns != VolleyLevelGenerator.COLUMNS ||
            stage.cells.size != stage.rows || stage.cells.any { it.size != stage.columns }
        ) {
            add("grid must be 11 by 9")
        }
        if (isNotEmpty()) return@buildList
        val cells = stage.cells
        if (cells.flatten().none { it?.type?.breakable == true && it.type != BrickType.SWITCH }) add("no clear objective")
        if (cells.last().any { it != null }) add("last row must be clear")
        if ((0 until stage.columns).none { c -> (4 until stage.rows).all { r -> cells[r][c] == null } }) add("no open launch lane")
        val bottomY = VolleySession.BRICK_START_TOP - 8 * VolleySession.ROW_PITCH
        if (bottomY - VolleySession.DANGER_Y < stage.descentDistance * 3f) add("insufficient descent clearance")
        if (stage.startingBalls !in 1..99 || stage.baseHp !in 1..30) add("invalid difficulty values")
        val all = cells.flatten().filterNotNull()
        if (all.any { !it.hpScale.isFinite() || it.hpScale <= 0f || it.hpScale > 5f }) add("invalid HP scale")
        val controls = all.filter { it.type == BrickType.KEY_BRICK || it.type == BrickType.SWITCH }.map { it.groupId }.toSet()
        if (all.any { it.type == BrickType.LOCKED && (it.groupId <= 0 || it.groupId !in controls) }) add("lock has no control")
        val portals = all.filter { it.type == BrickType.BLACK_HOLE_TELEPORTER }.groupBy { it.groupId }
        if (portals.any { it.key <= 0 || it.value.size != 2 }) add("portal needs a pair")
        if (stage.pickups.any { pickup ->
                val col = pickup.x.toInt()
                val row = pickup.y.toInt()
                col !in 0 until stage.columns || row !in 0 until stage.rows ||
                    pickup.x != col + .5f || pickup.y != row + .5f ||
                    cells.getOrNull(row)?.getOrNull(col) != null || pickup.balls !in 1..10
            }
        ) {
            add("bonus node overlaps or leaves board")
        }
        val cores = all.count { it.type == BrickType.BOSS_CORE }
        val guards = all.count { it.shieldLock }
        if (stage.bossStage && (cores != 1 || guards != 4)) add("boss needs one core and four guards")
        if (!stage.bossStage && (cores != 0 || guards != 0)) add("boss parts in ordinary stage")
    }
}
