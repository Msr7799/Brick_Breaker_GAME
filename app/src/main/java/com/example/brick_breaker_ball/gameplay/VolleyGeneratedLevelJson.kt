/*
 * الوظيفة: تصدير واستيراد مراحل Volley المولدة بصيغة JSON محددة الإصدار.
 * الحسابات: التحقق من الحدود والأنواع والمجموعات قبل تمرير أي مرحلة إلى محرك اللعب.
 */
package com.example.brick_breaker_ball.gameplay

import com.badlogic.gdx.utils.JsonReader
import com.badlogic.gdx.utils.JsonWriter
import com.example.brick_breaker_ball.campaign.VolleyBrickSeed
import com.example.brick_breaker_ball.campaign.VolleyPickupSeed
import com.example.brick_breaker_ball.campaign.VolleyStageDefinition
import com.example.brick_breaker_ball.campaign.VolleyStageRepository
import java.io.StringWriter

object VolleyGeneratedLevelJson {
    const val SCHEMA_VERSION = 1
    const val MAX_BYTES = 256 * 1024

    fun export(generated: VolleyGeneratedLevel): String {
        val stage = generated.stage
        val spec = generated.spec
        val output = StringWriter()
        val json = JsonWriter(output)
        json.setOutputType(JsonWriter.OutputType.json)
        json.`object`()
        json.set("schemaVersion", SCHEMA_VERSION)
        json.set("worldId", stage.world)
        json.set("levelId", stage.levelId)
        json.set("randomSeed", spec.seed)
        json.set("patternType", spec.pattern.name)
        json.name("arena").`object`()
        json.set("columns", stage.columns)
        json.set("rows", stage.rows)
        json.set("left", VolleySession.BOARD_LEFT)
        json.set("right", VolleySession.BOARD_RIGHT)
        json.set("brickStartTop", VolleySession.BRICK_START_TOP)
        json.set("brickHeight", VolleySession.BRICK_HEIGHT)
        json.set("gap", VolleySession.BRICK_GAP)
        json.pop()
        json.name("difficulty").`object`()
        json.set("rating", spec.difficulty)
        json.set("density", spec.density)
        json.set("baseHp", stage.baseHp)
        json.set("startingBalls", stage.startingBalls)
        json.set("descentDistance", stage.descentDistance)
        json.pop()
        json.name("transform").`object`()
        json.set("rotation", spec.rotation)
        json.set("reflectX", spec.reflectX)
        json.set("scale", spec.scale)
        json.set("translateX", spec.translateX)
        json.set("translateY", spec.translateY)
        json.set("spacing", spec.spacing)
        json.set("combineWith", spec.combineWith?.name)
        json.pop()
        json.name("specialMechanics").`object`()
        json.set("bossGuards", stage.cells.flatten().count { it?.shieldLock == true })
        json.set("bonusStage", stage.bonusStage)
        json.pop()
        json.name("boss").`object`()
        json.set("enabled", stage.bossStage)
        json.set("coreCount", stage.cells.flatten().count { it?.type == BrickType.BOSS_CORE })
        json.pop()
        json.name("validation").`object`()
        json.set("structural", "passed")
        json.set("ballSimulation", "not_proven")
        json.pop()
        json.name("bricks").array()
        stage.cells.forEachIndexed { row, line ->
            line.forEachIndexed { col, seed ->
                if (seed != null) {
                    json.`object`()
                    json.set("row", row)
                    json.set("column", col)
                    json.set("type", seed.type.name)
                    json.set("shape", seed.shape.name)
                    json.set("hpScale", seed.hpScale)
                    json.set("groupId", seed.groupId)
                    json.set("shieldLock", seed.shieldLock)
                    json.set("countsForClear", seed.countsForClear)
                    json.pop()
                }
            }
        }
        json.pop()
        json.name("pickups").array()
        stage.pickups.forEach { pickup ->
            json.`object`()
            json.set("x", pickup.x)
            json.set("y", pickup.y)
            json.set("balls", pickup.balls)
            json.set("type", pickup.type.name)
            json.set("scoreValue", pickup.scoreValue)
            json.pop()
        }
        json.pop()
        json.pop()
        json.flush()
        return output.toString()
    }

    /** Invalid or oversized imports fail before touching campaign data or persistent saves. */
    fun decode(json: String): Result<VolleyGeneratedLevel> = runCatching {
        require(json.toByteArray(Charsets.UTF_8).size <= MAX_BYTES) { "JSON is too large" }
        val root = JsonReader().parse(json)
        require(root.getInt("schemaVersion", -1) == SCHEMA_VERSION) { "Unsupported schema version: ${json.take(180)}" }
        val levelId = root.getInt("levelId")
        require(levelId in 1..VolleyStageRepository.TOTAL_LEVELS) { "Invalid level ID" }
        val base = VolleyStageRepository.forLevel(levelId)
        require(root.getInt("worldId") == base.world) { "World/level mismatch" }
        val arena = requireNotNull(root.get("arena"))
        require(
            arena.getInt("columns") == VolleyLevelGenerator.COLUMNS &&
                arena.getInt("rows") == VolleyLevelGenerator.ROWS
        ) { "Unsupported arena dimensions" }
        require(
            arena.getFloat("left") == VolleySession.BOARD_LEFT &&
                arena.getFloat("right") == VolleySession.BOARD_RIGHT &&
                arena.getFloat("brickStartTop") == VolleySession.BRICK_START_TOP &&
                arena.getFloat("brickHeight") == VolleySession.BRICK_HEIGHT &&
                arena.getFloat("gap") == VolleySession.BRICK_GAP
        ) { "Arena physics contract changed" }
        val difficulty = requireNotNull(root.get("difficulty"))
        val transform = requireNotNull(root.get("transform"))
        val spec = VolleyGeneratorSpec(
            pattern = VolleyPattern.valueOf(root.getString("patternType")),
            seed = root.getLong("randomSeed"),
            difficulty = difficulty.getInt("rating"), density = difficulty.getFloat("density"),
            rotation = transform.getInt("rotation"), reflectX = transform.getBoolean("reflectX"),
            scale = transform.getFloat("scale"), translateX = transform.getFloat("translateX"),
            translateY = transform.getFloat("translateY"), spacing = transform.getInt("spacing"),
            combineWith = transform.getString("combineWith", "").takeIf { it.isNotBlank() }
                ?.let(VolleyPattern::valueOf)
        )
        require(
            spec.difficulty in 1..9 && spec.density in .35f..1f && spec.rotation in 0..3 &&
                spec.scale in .65f..1.25f && spec.translateX in -0.25f..0.25f &&
                spec.translateY in -0.25f..0.25f && spec.spacing in 1..3
        ) { "Invalid generator parameters" }
        val cells = MutableList(VolleyLevelGenerator.ROWS) {
            MutableList<VolleyBrickSeed?>(VolleyLevelGenerator.COLUMNS) { null }
        }
        val bricks = requireNotNull(root.get("bricks"))
        require(bricks.size in 1..VolleyLevelGenerator.COLUMNS * VolleyLevelGenerator.ROWS) { "Invalid brick count" }
        for (index in 0 until bricks.size) {
            val entry = bricks.get(index)
            val row = entry.getInt("row")
            val column = entry.getInt("column")
            require(row in cells.indices && column in cells[row].indices && cells[row][column] == null) {
                "Duplicate or out-of-bounds brick"
            }
            val hpScale = entry.getFloat("hpScale")
            require(hpScale.isFinite() && hpScale in .01f..5f) { "Invalid brick HP scale" }
            cells[row][column] = VolleyBrickSeed(
                type = BrickType.valueOf(entry.getString("type")),
                shape = VolleyPieceShape.valueOf(entry.getString("shape")),
                hpScale = hpScale,
                groupId = entry.getInt("groupId", 0),
                shieldLock = entry.getBoolean("shieldLock", false),
                countsForClear = entry.get("countsForClear")?.takeUnless { it.isNull }?.asBoolean()
            )
        }
        val boss = root.get("boss")?.getBoolean("enabled", false) ?: false
        val special = requireNotNull(root.get("specialMechanics"))
        val pickupNodes = requireNotNull(root.get("pickups"))
        require(pickupNodes.size <= 30) { "Too many bonus nodes" }
        val pickups = (0 until pickupNodes.size).map { index ->
            val node = pickupNodes.get(index)
            VolleyPickupSeed(
                x = node.getFloat("x"),
                y = node.getFloat("y"),
                balls = node.getInt("balls"),
                type = VolleyBonusNodeType.valueOf(node.getString("type")),
                scoreValue = node.getInt("scoreValue")
            )
        }
        val stage = base.copy(
            rows = VolleyLevelGenerator.ROWS, columns = VolleyLevelGenerator.COLUMNS,
            cells = cells.map { it.toList() }, authoredBricks = emptyList(),
            rails = emptyList(), pickups = pickups,
            baseHp = difficulty.getInt("baseHp"),
            startingBalls = difficulty.getInt("startingBalls"),
            descentDistance = difficulty.getFloat("descentDistance"),
            bonusStage = special.getBoolean("bonusStage", false),
            bossStage = boss
        )
        val errors = VolleyGeneratedLevelValidator.validate(stage)
        require(errors.isEmpty()) { errors.joinToString() }
        val hp = cells.flatten().filterNotNull().map {
            (stage.baseHp * it.hpScale).toInt().coerceAtLeast(1)
        }
        VolleyGeneratedLevel(
            stage,
            spec,
            VolleyGenerationStats(
                brickCount = hp.size,
                averageHp = hp.average().toFloat(),
                maxHp = hp.maxOrNull() ?: 0,
                openBottomColumns = cells.last().count { it == null },
                estimatedWork = hp.sum()
            )
        )
    }
}
