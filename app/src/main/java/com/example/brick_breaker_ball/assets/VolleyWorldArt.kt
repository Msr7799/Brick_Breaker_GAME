/*
 * المسار: app/src/main/java/com/example/brick_breaker_ball/assets/VolleyWorldArt.kt
 * الوظيفة: تحميل رسوم عوالم Volley وأطالسها.
 * الحسابات: تحويل مواقع الإطارات وأبعادها.
 */

package com.example.brick_breaker_ball.assets

import com.badlogic.gdx.Gdx
import com.badlogic.gdx.graphics.Texture
import com.badlogic.gdx.graphics.g2d.TextureRegion
import com.badlogic.gdx.utils.Disposable
import com.badlogic.gdx.utils.JsonReader
import com.example.brick_breaker_ball.campaign.VolleyStageRepository
import com.example.brick_breaker_ball.gameplay.Brick
import com.example.brick_breaker_ball.gameplay.VolleyBrickVisualFamily
import com.example.brick_breaker_ball.gameplay.VolleyPieceShape

/** Runtime slices for one supplied Volley world spritesheet and its dedicated cannon. */
data class VolleyPieceVisual(val region: TextureRegion, val rotationDegrees: Float = 0f)
data class VolleyEditorTile(val name: String, val region: TextureRegion, val shape: VolleyPieceShape)

class VolleyWorldArt(val world: Int) : Disposable {
    private data class Slice(val name: String, val x: Int, val y: Int, val width: Int, val height: Int)
    private data class TriangleRegion(val region: TextureRegion, val shape: VolleyPieceShape)

    private val folder = VolleyStageRepository.worldAssetFolder(world)
    private val packedAtlas = world == 1 &&
        Gdx.files.internal("volley-world/$folder/atlas-pic.webp").exists() &&
        Gdx.files.internal("volley-world/$folder/atlas-pic.json").exists()
    private val sheetPath = "volley-world/$folder/${if (packedAtlas) "atlas-pic.webp" else "spritesheet.webp"}"
    private val sheetPixmap = pixmapFromAsset(sheetPath)
    private val texture = textureFromAsset(sheetPath).apply {
        setFilter(Texture.TextureFilter.Linear, Texture.TextureFilter.Linear)
        setWrap(Texture.TextureWrap.ClampToEdge, Texture.TextureWrap.ClampToEdge)
    }
    private val cannonTexture = textureFromAsset(volleyCannonAssetPath(world)).apply {
        setFilter(Texture.TextureFilter.Linear, Texture.TextureFilter.Linear)
        setWrap(Texture.TextureWrap.ClampToEdge, Texture.TextureWrap.ClampToEdge)
    }
    private val regions: List<TextureRegion>
    private val namedRegions: Map<String, TextureRegion>
    private val editorTiles: List<VolleyEditorTile>
    private val triangleRegions = mutableMapOf<VolleyPieceShape, MutableList<TextureRegion>>()

    init {
        val expectedSource = listOf("1.png", "6.png", "2.png", "7.png", "4.png", "5.png", "9.png", "3.png", "8.png")[world - 1]
        val manifestName = if (packedAtlas) "atlas-pic.json" else "atlas.json"
        val frames = JsonReader().parse(Gdx.files.internal("volley-world/$folder/$manifestName")).get("frames")
        val slices = mutableListOf<Slice>()
        var entry = frames.child
        while (entry != null) {
            if (packedAtlas) {
                val frame = entry.get("frame")
                slices += Slice(entry.name, frame.getInt("x"), frame.getInt("y"), frame.getInt("w"), frame.getInt("h"))
            } else {
                val origin = entry.get("origin")
                if (origin.getString("sheet") == expectedSource) {
                    slices += Slice(entry.name, origin.getInt("x"), origin.getInt("y"), origin.getInt("w"), origin.getInt("h"))
                }
            }
            entry = entry.next
        }
        val orderedSlices = slices.sortedBy { it.name.substringAfterLast('_').substringBefore('.').toIntOrNull() ?: Int.MAX_VALUE }
        regions = orderedSlices.map { slice ->
            require(slice.x >= 0 && slice.y >= 0 && slice.x + slice.width <= texture.width && slice.y + slice.height <= texture.height) {
                "Volley world $world slice ${slice.name} is outside spritesheet bounds"
            }
            TextureRegion(texture, slice.x, slice.y, slice.width, slice.height)
        }
        namedRegions = orderedSlices.zip(regions).associate { (slice, region) -> slice.name to region }
        // The editor needs the complete authored sheet, including its square,
        // triangular, and wide crops. Classify once while the source pixmap exists.
        editorTiles = orderedSlices.zip(regions).map { (slice, region) ->
            val shape = classifyTriangle(slice)
            if (shape != null) triangleRegions.getOrPut(shape) { mutableListOf() } += region
            VolleyEditorTile(slice.name, region, shape ?: VolleyPieceShape.RECTANGLE)
        }
        require(regions.size >= 40) { "Volley world $world spritesheet exposes only ${regions.size} usable slices" }
        require(triangleRegions.values.sumOf { it.size } >= 1) { "Volley world $world has no geometric triangle slices" }
        sheetPixmap.dispose()
    }

    fun brick(brick: Brick): TextureRegion {
        val cracked = brick.health > 0 && brick.initialHealth > 1 && brick.health.toLong() * 2 <= brick.initialHealth.toLong()
        val family = brick.volleyVisualFamily ?: VolleyAssetCatalog.familyFor(brick.type, world)
        val pair = when (family) {
            VolleyBrickVisualFamily.STANDARD_GRAY,
            VolleyBrickVisualFamily.STANDARD_GREEN,
            VolleyBrickVisualFamily.STANDARD_CYAN
            -> 0 to 2

            VolleyBrickVisualFamily.STANDARD_BROWN,
            VolleyBrickVisualFamily.STANDARD_BLUE
            -> 1 to 3

            VolleyBrickVisualFamily.STANDARD_ORANGE,
            VolleyBrickVisualFamily.STANDARD_GOLD
            -> 4 to 5

            VolleyBrickVisualFamily.STANDARD_RED,
            VolleyBrickVisualFamily.STANDARD_PURPLE,
            VolleyBrickVisualFamily.STANDARD_PINK
            -> 6 to 7

            VolleyBrickVisualFamily.ROUGH -> 2 to 3

            VolleyBrickVisualFamily.REGENERATING -> fromEnd(4) to fromEnd(3)

            VolleyBrickVisualFamily.CARRIER -> fromEnd(12) to fromEnd(11)

            VolleyBrickVisualFamily.ARMORED -> fromEnd(10) to fromEnd(9)

            VolleyBrickVisualFamily.ICE -> fromEnd(8) to fromEnd(7)

            VolleyBrickVisualFamily.EXPLOSIVE -> fromEnd(7) to fromEnd(6)

            VolleyBrickVisualFamily.SWITCH -> fromEnd(6) to fromEnd(5)

            VolleyBrickVisualFamily.GHOST -> fromEnd(5) to fromEnd(4)

            VolleyBrickVisualFamily.LOCK,
            VolleyBrickVisualFamily.SHIELD_LOCK
            -> fromEnd(4) to fromEnd(3)

            VolleyBrickVisualFamily.CHAIN -> fromEnd(3) to fromEnd(2)

            VolleyBrickVisualFamily.GATE,
            VolleyBrickVisualFamily.SPIKE
            -> fromEnd(2) to fromEnd(1)

            VolleyBrickVisualFamily.PORTAL,
            VolleyBrickVisualFamily.BOSS
            -> fromEnd(6) to fromEnd(5)
        }
        return region(if (cracked) pair.second else pair.first)
    }

    fun editorBrickTiles(): List<VolleyEditorTile> = editorTiles
    fun editorBrickSprite(name: String): TextureRegion? = namedRegions[name]

    fun brickVisual(brick: Brick): VolleyPieceVisual {
        brick.volleyAtlasSpriteName?.let(namedRegions::get)?.let { return VolleyPieceVisual(it) }
        val target = brick.volleyPieceShape
        if (target == VolleyPieceShape.RECTANGLE) return VolleyPieceVisual(brick(brick))
        val cracked = brick.health > 0 && brick.initialHealth > 1 && brick.health.toLong() * 2 <= brick.initialHealth.toLong()
        val exact = triangleRegions[target].orEmpty()
        if (exact.isNotEmpty()) {
            // Each world sheet contains several designs for the same diagonal.
            // Brick ID keeps the chosen crop stable after movement and save restore.
            val variant = Math.floorMod(brick.id * 17 + world, exact.size)
            val index = if (cracked && exact.size > 1) (variant + 1) % exact.size else variant
            return VolleyPieceVisual(exact[index])
        }

        val fallback = triangleRegions.entries.firstNotNullOf { (shape, regions) ->
            regions.firstOrNull()?.let { TriangleRegion(it, shape) }
        }
        return VolleyPieceVisual(fallback.region, triangleRotation(fallback.shape, target))
    }

    fun topBoundary(): TextureRegion = regions.take(24).maxByOrNull { it.regionWidth.toFloat() / it.regionHeight.coerceAtLeast(1) } ?: region(0)

    fun rail(fixed: Boolean): TextureRegion {
        val candidates = regions.take(30).filter { it.regionWidth >= it.regionHeight * 2 }
        return candidates.getOrNull(if (fixed) 1 else 0) ?: topBoundary()
    }

    fun railCap(): TextureRegion = region(fromEnd(4))

    private var barrierSourceTexture: Texture? = null

    /** Stage 11 is in Forest; its selected tile comes from Venom regardless of this world's atlas. */
    private val stageEightBarrierRegion: TextureRegion by lazy {
        val sourceFolder = "volley-world/02_Venom_Wilds"
        val manifest = Gdx.files.internal("$sourceFolder/atlas-pic.json")
        val sourceSheet = "$sourceFolder/spritesheet.webp"
        if (!manifest.exists() || !Gdx.files.internal(sourceSheet).exists()) return@lazy rail(true)
        val origin = JsonReader().parse(manifest)
            .get("frames")?.get("spritesheet_005798d1_063.png")?.get("origin")
            ?: return@lazy rail(true)
        if (origin.getString("sheet", "") != "spritesheet.png") return@lazy rail(true)
        val x = origin.getInt("x")
        val y = origin.getInt("y")
        val width = origin.getInt("w")
        val height = origin.getInt("h")
        // Keep this texture managed so Android can restore it after a GL context loss.
        // World 2 already owns the same sheet; other worlds own one extra lazy texture.
        val sourceTexture = if (world == 2) texture else textureFromAsset(sourceSheet).apply {
            setFilter(Texture.TextureFilter.Linear, Texture.TextureFilter.Linear)
            setWrap(Texture.TextureWrap.ClampToEdge, Texture.TextureWrap.ClampToEdge)
            barrierSourceTexture = this
        }
        if (x < 0 || y < 0 || width <= 0 || height <= 0 ||
            x > sourceTexture.width - width || y > sourceTexture.height - height) return@lazy rail(true)
        TextureRegion(sourceTexture, x, y, width, height)
    }
    fun stageEightBarrier(): TextureRegion = stageEightBarrierRegion
    fun debris(top: Boolean): TextureRegion = region(if (top) 2 else 3)
    fun cannon(): TextureRegion = TextureRegion(cannonTexture)

    private fun fromEnd(offset: Int): Int = (regions.lastIndex - offset).coerceAtLeast(0)
    private fun region(index: Int): TextureRegion = regions[index.coerceIn(regions.indices)]

    private fun classifyTriangle(slice: Slice): VolleyPieceShape? {
        val aspect = slice.width.toFloat() / slice.height.coerceAtLeast(1)
        if (aspect !in .65f..1.70f || slice.width !in 70..260 || slice.height !in 70..260 || slice.y > sheetPixmap.height * .78f) return null

        fun density(xStart: Float, xEnd: Float, yStart: Float, yEnd: Float): Float {
            var opaque = 0
            var samples = 0
            val fromX = slice.x + (slice.width * xStart).toInt()
            val toX = slice.x + (slice.width * xEnd).toInt()
            val fromY = slice.y + (slice.height * yStart).toInt()
            val toY = slice.y + (slice.height * yEnd).toInt()
            var y = fromY
            while (y < toY) {
                var x = fromX
                while (x < toX) {
                    if ((sheetPixmap.getPixel(x, y) and 0xff) > 30) opaque++
                    samples++
                    x += 3
                }
                y += 3
            }
            return opaque.toFloat() / samples.coerceAtLeast(1)
        }

        val corners = floatArrayOf(
            density(.08f, .42f, .08f, .42f),
            density(.58f, .92f, .08f, .42f),
            density(.08f, .42f, .58f, .92f),
            density(.58f, .92f, .58f, .92f)
        )
        val min = corners.minOrNull() ?: return null
        val max = corners.maxOrNull() ?: return null
        if (max < .55f || min >= max * .45f) return null
        return when (corners.indexOfFirst { it == min }) {
            0 -> VolleyPieceShape.TRIANGLE_SE
            1 -> VolleyPieceShape.TRIANGLE_SW
            2 -> VolleyPieceShape.TRIANGLE_NE
            else -> VolleyPieceShape.TRIANGLE_NW
        }
    }

    private fun triangleRotation(source: VolleyPieceShape, target: VolleyPieceShape): Float {
        fun order(shape: VolleyPieceShape): Int = when (shape) {
            VolleyPieceShape.TRIANGLE_NW -> 0
            VolleyPieceShape.TRIANGLE_SW -> 1
            VolleyPieceShape.TRIANGLE_SE -> 2
            VolleyPieceShape.TRIANGLE_NE -> 3
            VolleyPieceShape.RECTANGLE -> 0
        }
        return Math.floorMod(order(target) - order(source), 4) * 90f
    }

    override fun dispose() {
        barrierSourceTexture?.dispose()
        barrierSourceTexture = null
        texture.dispose()
        cannonTexture.dispose()
    }
}

internal fun volleyCannonAssetPath(world: Int): String = listOf(
    "volley-world/01_Mystery_Forest/Mystery_Forest_Cannon.webp",
    "volley-world/02_Venom_Wilds/Venom_Wilds_Cannon.webp",
    "volley-world/03_Aqua_Vortex/Aqua_Vortex_Cannon.webp",
    "volley-world/04_Desert_Spirits/Desert_Spirits_Cannon.webp",
    "volley-world/05_Ice_Grave/Ice_Grave_Cannon.webp",
    "volley-world/06_Tormented_Souls/Tormented_Souls_Cannon.webp",
    "volley-world/07_Moon_light/Moon_Light_Cannon.webp",
    "volley-world/08_Infernal_Gates/Infernal_Gates_Cannon.webp",
    "volley-world/09_Final_Eclipse/Final_Eclipse_Cannon.webp"
)[world.coerceIn(1, VolleyStageRepository.WORLDS_COUNT) - 1]
