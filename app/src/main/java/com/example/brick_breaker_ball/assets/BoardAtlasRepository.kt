/*
 * المسار: app/src/main/java/com/example/brick_breaker_ball/assets/BoardAtlasRepository.kt
 * الوظيفة: قراءة أطالس لوحات الواجهة ومناطقها.
 * الحسابات: تحويل إحداثيات الأطلس إلى مستطيلات العرض.
 */

package com.example.brick_breaker_ball.assets

import com.badlogic.gdx.Gdx
import com.badlogic.gdx.graphics.Texture
import com.badlogic.gdx.graphics.g2d.TextureRegion
import com.badlogic.gdx.math.Rectangle
import com.badlogic.gdx.utils.Disposable
import com.badlogic.gdx.utils.JsonReader

/** Atlas coordinates are top-left image pixels, never screen coordinates. */
data class BoardFrame(val x: Int, val y: Int, val width: Int, val height: Int)

class BoardAtlasManifest private constructor(
    val imageWidth: Int,
    val imageHeight: Int,
    val frames: Map<String, BoardFrame>
) {
    fun requireFrame(name: String): BoardFrame = requireNotNull(frames[name]) { "Missing board sprite: $name" }

    /** Preserve an authored board's aspect ratio inside the available screen area. */
    fun contain(name: String, area: Rectangle): Rectangle {
        val frame = requireFrame(name)
        val scale = minOf(area.width / frame.width, area.height / frame.height)
        val width = frame.width * scale
        val height = frame.height * scale
        return Rectangle(area.x + (area.width - width) * .5f, area.y + (area.height - height) * .5f, width, height)
    }

    /** Map an embedded atlas crop to the matching area of the board that is actually drawn. */
    fun placement(boardName: String, childName: String, drawnBoard: Rectangle): Rectangle {
        val board = requireFrame(boardName)
        val child = requireFrame(childName)
        require(
            child.x >= board.x && child.y >= board.y &&
                child.x + child.width <= board.x + board.width &&
                child.y + child.height <= board.y + board.height
        ) {
            "$childName is outside $boardName; use an independent screen placement"
        }
        return placementNearby(boardName, childName, drawnBoard)
    }

    /** Projects an authored target that intentionally extends beyond the board crop. */
    fun placementNearby(boardName: String, childName: String, drawnBoard: Rectangle): Rectangle {
        val board = requireFrame(boardName)
        val child = requireFrame(childName)
        val scaleX = drawnBoard.width / board.width
        val scaleY = drawnBoard.height / board.height
        return Rectangle(
            drawnBoard.x + (child.x - board.x) * scaleX,
            drawnBoard.y + (board.y + board.height - child.y - child.height) * scaleY,
            child.width * scaleX,
            child.height * scaleY
        )
    }

    companion object {
        fun parse(json: String, imageWidth: Int, imageHeight: Int): BoardAtlasManifest {
            require(imageWidth > 0 && imageHeight > 0)
            val root = JsonReader().parse(json)
            val declaredSize = root.get("meta")?.get("size")
            require(
                declaredSize != null && declaredSize.getInt("w") == imageWidth &&
                    declaredSize.getInt("h") == imageHeight
            ) {
                "Board atlas PNG dimensions do not match JSON meta.size"
            }
            val parsed = linkedMapOf<String, BoardFrame>()
            val nodes = requireNotNull(root.get("frames")) { "Board atlas is missing frames" }
            for (entry in nodes) {
                val name = requireNotNull(entry.name) { "Unnamed board frame" }
                require(name !in parsed) { "Duplicate board frame: $name" }
                // All current authored boards are unrotated and untrimmed. Reject unsupported
                // encodings explicitly rather than rendering a plausible but incorrect crop.
                require(!entry.getBoolean("rotated", false) && !entry.getBoolean("trimmed", false)) {
                    "Rotated or trimmed board frame needs an explicit placement: $name"
                }
                val rect = requireNotNull(entry.get("frame")) { "Missing frame rectangle: $name" }
                val frame = BoardFrame(rect.getInt("x"), rect.getInt("y"), rect.getInt("w"), rect.getInt("h"))
                require(
                    frame.x >= 0 && frame.y >= 0 && frame.width > 0 && frame.height > 0 &&
                        frame.x + frame.width <= imageWidth && frame.y + frame.height <= imageHeight
                ) {
                    "Board frame outside PNG: $name"
                }
                parsed[name] = frame
            }
            require(parsed.isNotEmpty()) { "Board atlas has no frames" }
            return BoardAtlasManifest(imageWidth, imageHeight, parsed)
        }
    }
}

/** One texture and one cached TextureRegion per frame for the lifetime of GameAssets. */
class BoardAtlasRepository(jsonPath: String, imagePath: String) : Disposable {
    private val texture = Texture(Gdx.files.internal(imagePath)).apply {
        setFilter(Texture.TextureFilter.Linear, Texture.TextureFilter.Linear)
    }
    val manifest = BoardAtlasManifest.parse(
        Gdx.files.internal(jsonPath).readString("UTF-8"),
        texture.width,
        texture.height
    )
    private val regions = manifest.frames.mapValues { (_, frame) ->
        TextureRegion(texture, frame.x, frame.y, frame.width, frame.height)
    }

    fun findRegion(name: String): TextureRegion? = regions[name]

    fun requireRegion(name: String): TextureRegion = requireNotNull(findRegion(name)) { "Missing board sprite: $name" }

    fun contain(name: String, area: Rectangle): Rectangle = manifest.contain(name, area)

    fun placement(boardName: String, childName: String, drawnBoard: Rectangle): Rectangle = manifest.placement(boardName, childName, drawnBoard)

    fun placementNearby(boardName: String, childName: String, drawnBoard: Rectangle): Rectangle = manifest.placementNearby(boardName, childName, drawnBoard)

    override fun dispose() = texture.dispose()
}
