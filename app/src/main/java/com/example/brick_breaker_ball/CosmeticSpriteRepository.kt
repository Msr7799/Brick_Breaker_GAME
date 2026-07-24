package com.example.brick_breaker_ball

import com.badlogic.gdx.Gdx
import com.badlogic.gdx.graphics.Texture
import com.badlogic.gdx.graphics.g2d.TextureRegion
import com.badlogic.gdx.utils.JsonReader
import com.badlogic.gdx.utils.JsonValue

object CosmeticSpriteCatalogParser {
    fun balls(json: String, textureWidth: Int, textureHeight: Int): List<BallSpriteGroup> {
        val root = JsonReader().parse(json)
        require(root.getInt("image_width") == textureWidth && root.getInt("image_height") == textureHeight)
        val groups = root.get("groups") ?: error("Missing ball groups")
        val result = groups.map { group ->
            val groupIndex = group.getInt("group_index")
            val groupName = group.getString("group_name")
            val sprites = requireNotNull(group.get("sprites")).map { sprite ->
                BallSpriteDefinition(
                    groupIndex, groupName, sprite.getInt("index"), sprite.getString("name"),
                    sprite.getString("sprite_name"), sprite.getInt("x"), sprite.getInt("y"),
                    sprite.getInt("width"), sprite.getInt("height"),
                ).also { requireInBounds(it.x, it.y, it.width, it.height, textureWidth, textureHeight) }
            }
            require(group.getInt("sprite_count") == sprites.size)
            BallSpriteGroup(groupIndex, groupName, sprites)
        }
        require(root.getInt("group_count") == result.size)
        require(root.getInt("sprite_count") == result.sumOf { it.sprites.size })
        require(result.flatMap { it.sprites }.map { it.id }.distinct().size == result.sumOf { it.sprites.size })
        return result
    }

    fun paddles(json: String, sourceAtlas: String, textureWidth: Int, textureHeight: Int): List<PaddleSpriteDefinition> {
        val root = JsonReader().parse(json)
        require(root.isArray)
        return root.map { item ->
            PaddleSpriteDefinition(
                sourceAtlas = sourceAtlas,
                spriteNumber = item.getInt("spriteNumber"),
                name = item.getString("name").trim(),
                displayNameEn = item.get("displayName").getString("en"),
                displayNameAr = item.get("displayName").getString("ar", ""),
                groupId = item.get("group").getString("id"),
                x = item.getInt("x"), y = item.getInt("y"),
                width = item.getInt("width"), height = item.getInt("height"),
            ).also { requireInBounds(it.x, it.y, it.width, it.height, textureWidth, textureHeight) }
        }.also { paddles ->
            require(paddles.map { it.id }.distinct().size == paddles.size)
            require(paddles.all { it.groupId in setOf("normal", "weapon", "sticky") })
        }
    }

    private fun requireInBounds(x: Int, y: Int, width: Int, height: Int, textureWidth: Int, textureHeight: Int) {
        require(x >= 0 && y >= 0 && width > 0 && height > 0)
        require(x + width <= textureWidth && y + height <= textureHeight)
    }

    private fun <T> JsonValue.map(transform: (JsonValue) -> T): List<T> {
        val result = mutableListOf<T>(); var child = child
        while (child != null) { result += transform(child); child = child.next }
        return result
    }
}

class CosmeticSpriteRepository {
    private data class Sheet<T>(val texture: Texture, val definitions: List<T>)

    private val ballSheet = loadBalls()
    private val paddleSheets = listOfNotNull(
        loadPaddles("group1", "paddles-group1.png"),
        loadPaddles("group2", "paddles-group2.png"),
        loadPaddles("group3", "paddle-group3.png"),
    )
    val ballGroups: List<BallSpriteGroup> = ballSheet?.definitions.orEmpty()
    val balls: List<BallSpriteDefinition> = ballGroups.flatMap { it.sprites }
    val paddles: List<PaddleSpriteDefinition> = paddleSheets.flatMap { it.definitions }
    private val ballsBySelection = balls.associateBy { it.groupName to it.spriteName }
    private val paddlesById = paddles.associateBy(PaddleSpriteDefinition::id)
    private val ballRegionsById = ballSheet?.let { sheet -> sheet.definitions.flatMap(BallSpriteGroup::sprites).associate { definition ->
        definition.id to TextureRegion(sheet.texture, definition.x, definition.y, definition.width, definition.height)
    } }.orEmpty()
    private val paddleRegionsById = paddleSheets.flatMap { sheet -> sheet.definitions.map { definition ->
        definition.id to TextureRegion(sheet.texture, definition.x, definition.y, definition.width, definition.height)
    } }.toMap()

    fun ballDefinition(groupName: String, spriteName: String): BallSpriteDefinition? = ballsBySelection[groupName to spriteName]
    fun paddleDefinition(id: String): PaddleSpriteDefinition? = paddlesById[id]

    fun ballRegion(definition: BallSpriteDefinition): TextureRegion? = ballRegionsById[definition.id]

    fun paddleRegion(definition: PaddleSpriteDefinition): TextureRegion? = paddleRegionsById[definition.id]

    fun selectedBall(groupName: String, spriteName: String) = ballDefinition(groupName, spriteName)
        ?: ballDefinition(CosmeticDefaults.BALL_GROUP, CosmeticDefaults.BALL_SPRITE)

    fun selectedPaddle(id: String) = paddleDefinition(id) ?: paddleDefinition(CosmeticDefaults.PADDLE_ID)

    private fun loadBalls(): Sheet<BallSpriteGroup>? = runCatching {
        val texture = linearTexture("sprites/balls-sprites/balls-sprites-7.png")
        try {
            val json = Gdx.files.internal("sprites/balls-sprites/balls-sprites-7groups-named.json").readString("UTF-8")
            Sheet(texture, CosmeticSpriteCatalogParser.balls(json, texture.width, texture.height))
        } catch (failure: Throwable) { texture.dispose(); throw failure }
    }.getOrNull()

    private fun loadPaddles(group: String, imageFile: String): Sheet<PaddleSpriteDefinition>? = runCatching {
        val texture = linearTexture("sprites/paddles-sprites/$imageFile")
        try {
            val json = Gdx.files.internal("sprites/paddles-sprites/paddles-$group.json").readString("UTF-8")
            Sheet(texture, CosmeticSpriteCatalogParser.paddles(json, group, texture.width, texture.height))
        } catch (failure: Throwable) { texture.dispose(); throw failure }
    }.getOrNull()

    private fun linearTexture(path: String) = Texture(Gdx.files.internal(path)).apply {
        setFilter(Texture.TextureFilter.Linear, Texture.TextureFilter.Linear)
        setWrap(Texture.TextureWrap.ClampToEdge, Texture.TextureWrap.ClampToEdge)
    }

    fun dispose() {
        ballSheet?.texture?.dispose()
        paddleSheets.forEach { it.texture.dispose() }
    }
}
