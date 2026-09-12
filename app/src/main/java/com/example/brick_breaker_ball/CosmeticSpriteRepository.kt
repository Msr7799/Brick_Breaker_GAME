/*
 * ملاحظات صيانة الملف:
 * المسار: app/src/main/java/com/example/brick_breaker_ball/CosmeticSpriteRepository.kt
 * المؤلف: mohamed alromaihi
 * نظام الكرات الحالي يعتمد على ملفات PNG مستقلة، وليس Sprite Sheet أو مجموعات.
 */

package com.example.brick_breaker_ball

import com.badlogic.gdx.Gdx
import com.badlogic.gdx.graphics.Texture
import com.badlogic.gdx.graphics.g2d.TextureRegion
import com.badlogic.gdx.utils.JsonReader
import com.badlogic.gdx.utils.JsonValue

data class PaddleSheetAsset(
    val group: String,
    val jsonFile: String,
    val imageFile: String,
    val groupNumber: Int
)

object CosmeticSpriteCatalogParser {
    private val paddleJsonPattern = Regex("^paddles-group(\\d+)\\.json$")

    fun paddles(json: String, sourceAtlas: String, textureWidth: Int, textureHeight: Int): List<PaddleSpriteDefinition> {
        val root = JsonReader().parse(json)
        require(root.isArray)
        return root.map { item ->
            val groupId = item.get("group")?.getString("id") ?: item.getString("type")
            val style = item.getString("style", "style-${item.getInt("row", 1)}")
            PaddleSpriteDefinition(
                sourceAtlas = sourceAtlas,
                spriteNumber = item.getInt(
                    "spriteNumber",
                    (item.getInt("row", 1) - 1) * 3 + item.getInt("column", 1)
                ),
                name = paddleName(item, groupId, style),
                displayNameEn = paddleDisplayName(item, style, "en"),
                displayNameAr = paddleDisplayName(item, style, "ar"),
                groupId = groupId,
                x = item.getInt("x"), y = item.getInt("y"),
                width = item.getInt("width"), height = item.getInt("height")
            ).also { requireInBounds(it.x, it.y, it.width, it.height, textureWidth, textureHeight) }
        }.also { paddles ->
            require(paddles.map { it.id }.distinct().size == paddles.size)
            require(paddles.all { it.groupId in setOf("normal", "weapon", "sticky") })
        }
    }

    private fun paddleName(item: JsonValue, groupId: String, style: String): String =
        item.getString("name", "paddle_${slug(groupId)}_${slug(style)}").trim()

    private fun paddleDisplayName(item: JsonValue, style: String, language: String): String {
        val displayName = item.get("displayName")
        if (displayName != null) return displayName.getString(language, "")
        return if (language == "en") style.replace('_', ' ') else ""
    }

    private fun slug(value: String): String = value.trim().lowercase()
        .replace(Regex("[^a-z0-9]+"), "_")
        .trim('_')

    fun paddleSheets(fileNames: Iterable<String>): List<PaddleSheetAsset> {
        val names = fileNames.toSet()
        return names.mapNotNull { jsonFile ->
            val match = paddleJsonPattern.matchEntire(jsonFile) ?: return@mapNotNull null
            val groupNumber = match.groupValues[1].toIntOrNull() ?: return@mapNotNull null
            val group = "group$groupNumber"
            val imageFile = listOf("paddles-$group.png", "paddle-$group.png").firstOrNull(names::contains)
                ?: return@mapNotNull null
            PaddleSheetAsset(group, jsonFile, imageFile, groupNumber)
        }.sortedBy(PaddleSheetAsset::groupNumber)
    }

    private fun requireInBounds(x: Int, y: Int, width: Int, height: Int, textureWidth: Int, textureHeight: Int) {
        require(x >= 0 && y >= 0 && width > 0 && height > 0)
        require(x + width <= textureWidth && y + height <= textureHeight)
    }

    private fun <T> JsonValue.map(transform: (JsonValue) -> T): List<T> {
        val result = mutableListOf<T>()
        var child = child
        while (child != null) {
            result += transform(child)
            child = child.next
        }
        return result
    }
}

class CosmeticSpriteRepository {
    private companion object {
        const val PADDLE_DIRECTORY = "sprites/paddles-sprites"
        const val BALL_DIRECTORY = "sprites/balls-sprites/balls_set"

        val PRIMARY_BALL_SPRITES = listOf(
            "38_chrome_mirror",
            "39_black_gloss",
            "43_amber_glass",
            "44_orange_glass",
            "42_smoky_stone",
            "41_pearl_white"
        )

        val numberedBallPattern = Regex("^(\\d{2})_(.+)\\.png$", RegexOption.IGNORE_CASE)
    }

    private data class BallFile(val spriteName: String, val displayName: String, val assetPath: String)
    private data class BallAsset(val texture: Texture, val definition: BallSpriteDefinition)
    private data class Sheet<T>(val texture: Texture, val definitions: List<T>)

    private val ballAssets = loadBalls()
    private val paddleSheets = CosmeticSpriteCatalogParser
        .paddleSheets(Gdx.files.internal(PADDLE_DIRECTORY).list().map { it.name() })
        .mapNotNull(::loadPaddles)

    /** Individual balls in progression order. The six user-selected starters come first, then the remaining numbered balls. */
    val balls: List<BallSpriteDefinition> = ballAssets.map(BallAsset::definition)
    val paddles: List<PaddleSpriteDefinition> = paddleSheets.flatMap { it.definitions }

    private val ballsBySelection = balls.associateBy { it.groupName to it.spriteName }
    private val paddlesById = paddles.associateBy(PaddleSpriteDefinition::id)
    private val ballRegionsById = ballAssets.associate { it.definition.id to TextureRegion(it.texture) }
    private val paddleRegionsById = paddleSheets.flatMap { sheet ->
        sheet.definitions.map { definition ->
            definition.id to TextureRegion(sheet.texture, definition.x, definition.y, definition.width, definition.height)
        }
    }.toMap()

    fun ballDefinition(groupName: String, spriteName: String): BallSpriteDefinition? =
        ballsBySelection[groupName to spriteName]

    fun paddleDefinition(id: String): PaddleSpriteDefinition? = paddlesById[id]

    fun ballRegion(definition: BallSpriteDefinition): TextureRegion? = ballRegionsById[definition.id]

    fun paddleRegion(definition: PaddleSpriteDefinition): TextureRegion? = paddleRegionsById[definition.id]

    fun selectedBall(groupName: String, spriteName: String) = ballDefinition(groupName, spriteName)
        ?: ballDefinition(CosmeticDefaults.BALL_GROUP, CosmeticDefaults.BALL_SPRITE)
        ?: balls.firstOrNull()

    fun selectedPaddle(id: String) = paddleDefinition(id) ?: paddleDefinition(CosmeticDefaults.PADDLE_ID)

    private fun loadBalls(): List<BallAsset> {
        val directory = Gdx.files.internal(BALL_DIRECTORY)
        val files = directory.list()
            .filter { handle ->
                handle.extension().equals("png", ignoreCase = true) &&
                    !handle.name().startsWith("balls-sheet", ignoreCase = true)
            }
            .map { it.name() }

        val byNumber = files.mapNotNull { fileName ->
            numberedBallPattern.matchEntire(fileName)?.let { match ->
                match.groupValues[1].toIntOrNull()?.let { number -> number to fileName }
            }
        }.groupBy({ it.first }, { it.second })

        val selectedFiles = mutableListOf<String>()
        val usedNumbers = mutableSetOf<Int>()

        PRIMARY_BALL_SPRITES.forEach { spriteName ->
            val fileName = "$spriteName.png"
            if (fileName in files && fileName !in selectedFiles) {
                selectedFiles += fileName
                numberedBallPattern.matchEntire(fileName)?.groupValues?.getOrNull(1)?.toIntOrNull()?.let(usedNumbers::add)
            }
        }

        byNumber.toSortedMap().forEach { (number, candidates) ->
            if (number in usedNumbers) return@forEach
            candidates.sorted().firstOrNull()?.let { selectedFiles += it }
        }

        return selectedFiles.mapIndexedNotNull { index, fileName ->
            val match = numberedBallPattern.matchEntire(fileName)
            val spriteName = fileName.removeSuffix(".png")
            val displayName = match?.groupValues?.getOrNull(2)
                ?.split('_')
                ?.joinToString(" ") { part ->
                    part.replaceFirstChar { if (it.isLowerCase()) it.titlecase() else it.toString() }
                }
                ?: spriteName.replace('_', ' ')
            val file = BallFile(
                spriteName = spriteName,
                displayName = displayName,
                assetPath = "$BALL_DIRECTORY/$fileName"
            )
            runCatching {
                val texture = linearTexture(file.assetPath)
                val definition = BallSpriteDefinition(
                    groupIndex = 0,
                    groupName = CosmeticDefaults.BALL_GROUP,
                    index = index + 1,
                    name = file.displayName,
                    spriteName = file.spriteName,
                    x = 0,
                    y = 0,
                    width = texture.width,
                    height = texture.height
                )
                BallAsset(texture, definition)
            }.onFailure { Gdx.app.error("CosmeticSpriteRepository", "Unable to load ball ${file.assetPath}", it) }
                .getOrNull()
        }
    }

    private fun loadPaddles(asset: PaddleSheetAsset): Sheet<PaddleSpriteDefinition>? = runCatching {
        val texture = linearTexture("$PADDLE_DIRECTORY/${asset.imageFile}")
        try {
            val json = Gdx.files.internal("$PADDLE_DIRECTORY/${asset.jsonFile}").readString("UTF-8")
            Sheet(texture, CosmeticSpriteCatalogParser.paddles(json, asset.group, texture.width, texture.height))
        } catch (failure: Throwable) {
            texture.dispose()
            throw failure
        }
    }.getOrNull()

    private fun linearTexture(path: String) = Texture(Gdx.files.internal(path)).apply {
        setFilter(Texture.TextureFilter.Linear, Texture.TextureFilter.Linear)
        setWrap(Texture.TextureWrap.ClampToEdge, Texture.TextureWrap.ClampToEdge)
    }

    fun dispose() {
        ballAssets.forEach { it.texture.dispose() }
        paddleSheets.forEach { it.texture.dispose() }
    }
}
