/*
 * المسار: app/src/main/java/com/example/brick_breaker_ball/gameplay/VolleyEditorData.kt
 * الوظيفة: نماذج مراحل المحرر وقراءتها وكتابتها.
 * الحسابات: تحويل شبكة العناصر إلى إحداثيات المرحلة.
 */

package com.example.brick_breaker_ball.gameplay

import com.badlogic.gdx.Gdx
import com.badlogic.gdx.utils.JsonReader
import com.badlogic.gdx.utils.JsonWriter
import com.example.brick_breaker_ball.assets.VolleyAssetCatalog
import com.example.brick_breaker_ball.campaign.VolleyBrickSeed
import com.example.brick_breaker_ball.campaign.VolleyCarrierReward
import com.example.brick_breaker_ball.campaign.VolleyPickupSeed
import com.example.brick_breaker_ball.campaign.VolleyStageDefinition
import com.example.brick_breaker_ball.campaign.VolleyStageRepository
import java.io.StringWriter
import java.util.UUID

/** Player-authored Volley data. Campaign definitions are never edited in place. */
data class VolleyEditorDraft(
    var id: String = UUID.randomUUID().toString(),
    var name: String = "MY VOLLEY STAGE",
    var world: Int = 1,
    var backgroundStage: Int = 1,
    var startingBalls: Int = 15,
    var baseHp: Int = 2,
    var generatorSeed: Long? = null,
    var generatorPattern: VolleyPattern? = null,
    var generatorDifficulty: Int = 3,
    var bonusStage: Boolean = false,
    var bossStage: Boolean = false,
    val cells: MutableList<MutableList<VolleyBrickSeed?>> = MutableList(9) { MutableList(11) { null } },
    val pickups: MutableList<VolleyPickupSeed> = mutableListOf()
) {
    fun duplicate(newId: Boolean = false): VolleyEditorDraft = copy(
        id = if (newId) UUID.randomUUID().toString() else id,
        cells = cells.map { it.toMutableList() }.toMutableList(),
        pickups = pickups.toMutableList()
    )

    fun validationError(): String? {
        if (world !in 1..VolleyStageRepository.WORLDS_COUNT ||
            backgroundStage !in 1..VolleyStageRepository.worldLevelCount(world)
        ) {
            return "SELECT A VALID WORLD AND BACKGROUND"
        }
        if (cells.size != 9 || cells.any { it.size != 11 }) return "THE BOARD MUST BE 11 X 9"
        if (pickups.any { pickup ->
                val col = pickup.x.toInt()
                val row = pickup.y.toInt()
                col !in 0..10 || row !in 0..8 || pickup.x != col + .5f ||
                    pickup.y != row + .5f || cells[row][col] != null
            }
        ) {
            return "BALL PICKUPS NEED EMPTY GRID CELLS"
        }
        if (cells.flatten().none { seed -> seed?.let { it.type.breakable && it.type != BrickType.SWITCH } == true }) {
            return "ADD AT LEAST ONE BREAKABLE BRICK"
        }
        val keys = cells.flatten().filterNotNull().filter { it.type == BrickType.KEY_BRICK || it.type == BrickType.SWITCH }
            .map { it.groupId }.toSet()
        if (cells.flatten().filterNotNull().any { it.type == BrickType.LOCKED && it.groupId !in keys }) {
            return "LOCKED BRICKS NEED A MATCHING KEY OR SWITCH"
        }
        return null
    }

    fun toStage(): VolleyStageDefinition {
        require(validationError() == null) { validationError() ?: "Invalid Volley stage" }
        val template = VolleyStageRepository.forLevel(VolleyStageRepository.firstLevel(world))
        return template.copy(
            stage = backgroundStage,
            name = name.trim().ifBlank { "MY VOLLEY STAGE" },
            rows = 9,
            columns = 11,
            cells = cells.map { it.toList() },
            authoredBricks = emptyList(),
            rails = emptyList(),
            pickups = pickups.toList(),
            startingBalls = startingBalls.coerceIn(1, 99),
            baseHp = baseHp.coerceIn(1, 30),
            bonusStage = bonusStage,
            bossStage = bossStage,
            carrierDropTable = listOf(VolleyCarrierReward.BALL),
            backgroundId = world,
            backgroundAsset = VolleyStageRepository.backgroundAsset(world, backgroundStage),
            defaultVisualFamily = VolleyAssetCatalog.familyFor(BrickType.NORMAL_ONE_HIT, world)
        )
    }
}

/** One bounded, versioned local JSON library. Invalid entries are skipped without losing valid work. */
class VolleyEditorRepository {
    private val file get() = Gdx.files.local("volley-editor/levels-v1.json")
    private val backup get() = file.sibling("levels-v1.backup.json")

    fun list(): List<VolleyEditorDraft> = read(file) ?: read(backup) ?: emptyList()

    private fun read(source: com.badlogic.gdx.files.FileHandle): List<VolleyEditorDraft>? {
        if (!source.exists()) return null
        return try {
            val root = JsonReader().parse(source)
            if (root.getInt("version", -1) != 1) return null
            val entries = root.get("levels") ?: return null
            (0 until minOf(entries.size, 100)).mapNotNull { readDraft(entries.get(it)) }
        } catch (_: Exception) {
            null
        }
    }

    fun save(draft: VolleyEditorDraft) {
        require(draft.validationError() == null) { draft.validationError() ?: "Invalid stage" }
        val levels = list().toMutableList()
        val index = levels.indexOfFirst { it.id == draft.id }
        if (index >= 0) {
            levels[index] = draft.duplicate()
        } else {
            require(levels.size < 100) { "Stage library is full" }
            levels.add(0, draft.duplicate())
        }
        write(levels)
    }

    fun delete(id: String) = write(list().filterNot { it.id == id })

    private fun write(levels: List<VolleyEditorDraft>) {
        val payload = mapOf("version" to 1, "levels" to levels.map(::encode))
        file.parent().mkdirs()
        if (read(file) != null) file.copyTo(backup)
        val output = StringWriter()
        val writer = JsonWriter(output)
        writer.setOutputType(JsonWriter.OutputType.json)
        writeJsonValue(writer, payload)
        writer.flush()
        file.writeString(output.toString(), false, "UTF-8")
    }

    private fun writeJsonValue(writer: JsonWriter, value: Any?) {
        when (value) {
            is Map<*, *> -> {
                writer.`object`()
                value.forEach { (key, child) ->
                    writer.name(requireNotNull(key).toString())
                    writeJsonValue(writer, child)
                }
                writer.pop()
            }

            is Iterable<*> -> {
                writer.array()
                value.forEach { writeJsonValue(writer, it) }
                writer.pop()
            }

            else -> writer.value(value)
        }
    }

    private fun encode(draft: VolleyEditorDraft): Map<String, Any?> = mapOf(
        "id" to draft.id,
        "name" to draft.name,
        "world" to draft.world,
        "backgroundStage" to draft.backgroundStage,
        "startingBalls" to draft.startingBalls,
        "baseHp" to draft.baseHp,
        "generatorSeed" to draft.generatorSeed,
        "generatorPattern" to draft.generatorPattern?.name,
        "generatorDifficulty" to draft.generatorDifficulty,
        "bonusStage" to draft.bonusStage,
        "bossStage" to draft.bossStage,
        "cells" to draft.cells.map { row ->
            row.map { seed ->
                if (seed == null) {
                    null
                } else {
                    mapOf(
                        "type" to seed.type.name,
                        "shape" to seed.shape.name,
                        "group" to seed.groupId,
                        "hpScale" to seed.hpScale,
                        "shieldLock" to seed.shieldLock,
                        "countsForClear" to seed.countsForClear,
                        "sprite" to (seed.atlasSpriteName ?: "")
                    )
                }
            }
        },
        "pickups" to draft.pickups.map {
            mapOf(
                "x" to it.x,
                "y" to it.y,
                "balls" to it.balls,
                "type" to it.type.name,
                "scoreValue" to it.scoreValue
            )
        }
    )

    private fun readDraft(value: com.badlogic.gdx.utils.JsonValue): VolleyEditorDraft? {
        return try {
            val world = value.getInt("world")
            val rows = value.get("cells")
            if (world !in 1..VolleyStageRepository.WORLDS_COUNT || rows == null || rows.size != 9) return null
            val draft = VolleyEditorDraft(
                id = value.getString("id"),
                name = value.getString("name", "MY VOLLEY STAGE").take(40),
                world = world,
                backgroundStage = value.getInt("backgroundStage", 1),
                startingBalls = value.getInt("startingBalls", 15).coerceIn(1, 99),
                baseHp = value.getInt("baseHp", 2).coerceIn(1, 30),
                generatorSeed = value.get("generatorSeed")?.takeUnless { it.isNull }?.asLong(),
                generatorPattern = value.get("generatorPattern")?.takeUnless { it.isNull }?.asString()
                    ?.takeIf { it.isNotBlank() }
                    ?.let(VolleyPattern::valueOf),
                generatorDifficulty = value.getInt("generatorDifficulty", 3).coerceIn(1, 9),
                bonusStage = value.getBoolean("bonusStage", false),
                bossStage = value.getBoolean("bossStage", false)
            )
            for (row in 0 until 9) {
                val line = rows.get(row)
                if (line.size != 11) return null
                for (col in 0 until 11) {
                    val cell = line.get(col)
                    if (!cell.isNull) {
                        draft.cells[row][col] = VolleyBrickSeed(
                            type = BrickType.valueOf(cell.getString("type")),
                            shape = VolleyPieceShape.valueOf(cell.getString("shape", "RECTANGLE")),
                            groupId = cell.getInt("group", 0).coerceIn(0, 20),
                            hpScale = cell.getFloat("hpScale", 1f).coerceIn(.25f, 5f),
                            shieldLock = cell.getBoolean("shieldLock", false),
                            countsForClear = cell.get("countsForClear")?.takeUnless { it.isNull }?.asBoolean(),
                            atlasSpriteName = cell.getString("sprite", "").takeIf { it.isNotBlank() }
                        )
                    }
                }
            }
            value.get("pickups")?.let { pickups ->
                for (index in 0 until minOf(pickups.size, 99)) {
                    val node = pickups.get(index)
                    val x = node.getFloat("x")
                    val y = node.getFloat("y")
                    if (x in 0f..11f && y in 0f..9f) {
                        draft.pickups += VolleyPickupSeed(
                            x,
                            y,
                            node.getInt("balls", 1).coerceIn(1, 10),
                            type = VolleyBonusNodeType.valueOf(node.getString("type", "BALL")),
                            scoreValue = node.getInt("scoreValue", 0).coerceAtLeast(0)
                        )
                    }
                }
            }
            if (draft.validationError() == null) draft else null
        } catch (_: Exception) {
            null
        }
    }
}
