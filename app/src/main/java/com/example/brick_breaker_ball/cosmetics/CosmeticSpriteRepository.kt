/*
 * المسار: app/src/main/java/com/example/brick_breaker_ball/cosmetics/CosmeticSpriteRepository.kt
 * الوظيفة: فهرسة صور الكرات من الأطلس وتحميلها.
 * الحسابات: قراءة أبعاد PNG واقتصاص إطارات الأطلس.
 */

package com.example.brick_breaker_ball.cosmetics

import com.badlogic.gdx.Gdx
import com.badlogic.gdx.graphics.Texture
import com.badlogic.gdx.graphics.g2d.TextureAtlas
import com.badlogic.gdx.graphics.g2d.TextureRegion
import com.badlogic.gdx.utils.JsonReader
import java.io.DataInputStream
import java.io.InputStream

internal data class PngDimensions(val width: Int, val height: Int)

/** PNG stores its dimensions in the IHDR, so the cosmetic catalog needs no GPU texture. */
internal fun readPngDimensions(input: InputStream): PngDimensions {
    val data = DataInputStream(input)
    val signature = ByteArray(8)
    data.readFully(signature)
    require(signature.contentEquals(byteArrayOf(-119, 80, 78, 71, 13, 10, 26, 10))) {
        "Invalid PNG signature"
    }
    require(data.readInt() == 13 && data.readInt() == 0x49484452) { "Missing PNG IHDR" }
    val width = data.readInt()
    val height = data.readInt()
    require(width > 0 && height > 0) { "Invalid PNG dimensions" }
    return PngDimensions(width, height)
}

class CosmeticSpriteRepository {
    private companion object {
        const val BALL_DIRECTORY = "sprites/balls-sprites"
        const val NUMBERED_BALL_ATLAS = "atlases/balls.atlas"

        val PRIMARY_BALL_SPRITES = listOf(
            "38_chrome_mirror",
            "39_black_gloss",
            "43_amber_glass",
            "44_orange_glass",
            "42_smoky_stone",
            "41_pearl_white"
        )

        val numberedBallPattern = Regex("^(\\d{2,3})_(.+)$", RegexOption.IGNORE_CASE)
    }

    private data class BallAsset(
        val assetPath: String,
        val definition: BallSpriteDefinition,
        val atlasFrame: Boolean
    )
    private val ballAssets = loadBalls()
    private var numberedBallAtlas: TextureAtlas? = null
    private val ballTextures = mutableMapOf<String, Texture>()
    private val loadedBallRegions = mutableMapOf<String, TextureRegion>()

    /** Individual balls in progression order. The six user-selected starters come first, then the remaining numbered balls. */
    val balls: List<BallSpriteDefinition> = ballAssets.map(BallAsset::definition)

    private val ballsBySelection = balls.associateBy { it.groupName to it.spriteName }
    private val ballAssetsById = ballAssets.associateBy { it.definition.id }

    fun ballDefinition(groupName: String, spriteName: String): BallSpriteDefinition? = ballsBySelection[groupName to spriteName]

    fun ballRegion(definition: BallSpriteDefinition): TextureRegion? {
        val asset = ballAssetsById[definition.id] ?: return null
        return loadedBallRegions.getOrPut(definition.id) {
            if (asset.assetPath == NUMBERED_BALL_ATLAS) {
                val atlas = numberedBallAtlas ?: TextureAtlas(Gdx.files.internal(NUMBERED_BALL_ATLAS)).also {
                    numberedBallAtlas = it
                }
                return@getOrPut requireNotNull(atlas.findRegion(asset.definition.spriteName)) {
                    "Missing ball atlas region: ${asset.definition.spriteName}"
                }
            }
            val texture = ballTextures.getOrPut(asset.assetPath) { linearTexture(asset.assetPath) }
            if (asset.atlasFrame) {
                TextureRegion(
                    texture,
                    asset.definition.x,
                    asset.definition.y,
                    asset.definition.width,
                    asset.definition.height
                )
            } else {
                TextureRegion(texture)
            }
        }
    }

    fun selectedBall(groupName: String, spriteName: String) = ballDefinition(groupName, spriteName)
        ?: ballDefinition(CosmeticDefaults.BALL_GROUP, CosmeticDefaults.BALL_SPRITE)
        ?: balls.firstOrNull()

    // تنظيف Volley: الاختيار القديم أو غير المعروف يرجع لكرة موجودة في أطلس الكور بدل شيت Classic.
    fun requireBallRegion(groupName: String, spriteName: String): TextureRegion {
        val definition = requireNotNull(selectedBall(groupName, spriteName)) {
            "No ball is available in the Volley cosmetic atlas"
        }
        return requireNotNull(ballRegion(definition)) { "Missing Volley ball region: ${definition.id}" }
    }

    private fun loadBalls(): List<BallAsset> {
        val atlasFile = Gdx.files.internal(NUMBERED_BALL_ATLAS)
        val regions = TextureAtlas.TextureAtlasData(atlasFile, atlasFile.parent(), false).regions
            .filter { numberedBallPattern.matches(it.name) }
        val byNumber = regions.mapNotNull { region ->
            numberedBallPattern.matchEntire(region.name)?.let { match ->
                match.groupValues[1].toIntOrNull()?.let { number -> number to region }
            }
        }.groupBy({ it.first }, { it.second })

        val selectedRegions = mutableListOf<TextureAtlas.TextureAtlasData.Region>()
        val usedNumbers = mutableSetOf<Int>()

        PRIMARY_BALL_SPRITES.forEach { spriteName ->
            val region = regions.firstOrNull { it.name == spriteName }
            if (region != null && region !in selectedRegions) {
                selectedRegions += region
                numberedBallPattern.matchEntire(region.name)?.groupValues?.getOrNull(1)?.toIntOrNull()?.let(usedNumbers::add)
            }
        }

        byNumber.toSortedMap().forEach { (number, candidates) ->
            if (number in usedNumbers) return@forEach
            candidates.sortedBy { it.name }.firstOrNull()?.let { selectedRegions += it }
        }

        val numbered = selectedRegions.mapIndexed { index, region ->
            val spriteName = region.name
            val match = numberedBallPattern.matchEntire(spriteName)
            val displayName = match?.groupValues?.getOrNull(2)
                ?.split('_')
                ?.joinToString(" ") { part ->
                    part.replaceFirstChar { if (it.isLowerCase()) it.titlecase() else it.toString() }
                }
                ?: spriteName.replace('_', ' ')
            require(!region.rotate && region.width == region.originalWidth && region.height == region.originalHeight) {
                "Numbered ball atlas must preserve full unrotated frames: $spriteName"
            }
            val definition = BallSpriteDefinition(
                groupIndex = 0,
                groupName = CosmeticDefaults.BALL_GROUP,
                index = index + 1,
                name = displayName,
                spriteName = spriteName,
                x = 0,
                y = 0,
                width = region.width,
                height = region.height
            )
            BallAsset(NUMBERED_BALL_ATLAS, definition, atlasFrame = false)
        }
        require(numbered.size == 105) { "Expected 105 numbered ball regions, found ${numbered.size}" }
        return numbered + loadNewBallAtlas(numbered.size)
    }

    /** Cocos plist coordinates are exported once to JSON; all 60 frames share one lazy GPU texture. */
    private fun loadNewBallAtlas(firstIndex: Int): List<BallAsset> = runCatching {
        val manifest = JsonReader().parse(Gdx.files.internal("$BALL_DIRECTORY/New-Balls.json"))
        val assetPath = "$BALL_DIRECTORY/New-Balls.png"
        val dimensions = Gdx.files.internal(assetPath).read().use(::readPngDimensions)
        val assets = mutableListOf<BallAsset>()
        var frame = manifest.get("frames").child
        while (frame != null) {
            val bounds = frame.get("frame")
            val x = bounds.getInt("x")
            val y = bounds.getInt("y")
            val w = bounds.getInt("w")
            val h = bounds.getInt("h")
            require(
                x >= 0 && y >= 0 && w > 0 && h > 0 &&
                    x + w <= dimensions.width && y + h <= dimensions.height
            )
            val sprite = frame.getString("name")
            val number = sprite.removePrefix("new_ball_").toInt()
            val name = NewBallNames.name(number)
            val definition = BallSpriteDefinition(
                0, CosmeticDefaults.BALL_GROUP,
                firstIndex + assets.size + 1, name, sprite, x, y, w, h
            )
            assets += BallAsset(assetPath, definition, atlasFrame = true)
            frame = frame.next
        }
        require(assets.size == 60 && assets.map { it.definition.id }.distinct().size == 60)
        assets
    }.onFailure { Gdx.app.error("CosmeticSpriteRepository", "Unable to load New-Balls atlas", it) }.getOrDefault(emptyList())

    private fun linearTexture(path: String) = Texture(Gdx.files.internal(path)).apply {
        setFilter(Texture.TextureFilter.Linear, Texture.TextureFilter.Linear)
        setWrap(Texture.TextureWrap.ClampToEdge, Texture.TextureWrap.ClampToEdge)
    }

    fun dispose() {
        loadedBallRegions.clear()
        numberedBallAtlas?.dispose()
        numberedBallAtlas = null
        ballTextures.values.forEach(Texture::dispose)
        ballTextures.clear()
    }
}
