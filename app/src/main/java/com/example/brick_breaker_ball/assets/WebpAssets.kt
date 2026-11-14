/*
 * المسار: app/src/main/java/com/example/brick_breaker_ball/assets/WebpAssets.kt
 * الوظيفة: حل مسارات صور WebP في الأصول.
 * الحسابات: اشتقاق الامتداد والمسار الصحيحين.
 */

package com.example.brick_breaker_ball.assets

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import com.badlogic.gdx.Gdx
import com.badlogic.gdx.graphics.Pixmap
import com.badlogic.gdx.graphics.Texture
import com.badlogic.gdx.graphics.TextureData
import com.badlogic.gdx.utils.GdxRuntimeException
import java.io.ByteArrayOutputStream

/** Android decodes WebP; LibGDX receives lossless PNG bytes with the decoded alpha intact. */
internal fun pixmapFromAsset(path: String): Pixmap {
    val file = Gdx.files.internal(path)
    if (!path.endsWith(".webp", ignoreCase = true)) return Pixmap(file)
    val bitmap = file.read().use { stream ->
        val options = BitmapFactory.Options().apply {
            inScaled = false
        }
        requireNotNull(BitmapFactory.decodeStream(stream, null, options)) { "Unable to decode WebP asset: $path" }
    }
    return try {
        val png = ByteArrayOutputStream()
        check(bitmap.compress(Bitmap.CompressFormat.PNG, 100, png)) {
            "Unable to prepare WebP asset for LibGDX: $path"
        }
        val bytes = png.toByteArray()
        Pixmap(bytes, 0, bytes.size)
    } finally {
        bitmap.recycle()
    }
}

internal fun textureFromAsset(path: String): Texture {
    if (!path.endsWith(".webp", ignoreCase = true)) return Texture(Gdx.files.internal(path))
    return Texture(WebpTextureData(path))
}

/** A managed, sampled texture for small stage cards; never uploads the full portrait image. */
internal fun previewTextureFromAsset(path: String): Texture = Texture(WebpTextureData(path, previewMaxDimension = 768))

/** Decode a small preview off the render thread; upload the returned Pixmap on the render thread. */
internal fun previewPixmapFromAsset(path: String): Pixmap = sampledPixmapFromAsset(path, 768)

private fun sampledPixmapFromAsset(path: String, maxDimension: Int): Pixmap {
    val file = Gdx.files.internal(path)
    val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true; inScaled = false }
    file.read().use { BitmapFactory.decodeStream(it, null, bounds) }
    var sample = 1
    while (maxOf(bounds.outWidth, bounds.outHeight) / sample > maxDimension) sample *= 2
    val options = BitmapFactory.Options().apply { inSampleSize = sample; inScaled = false }
    val bitmap = file.read().use { stream ->
        requireNotNull(BitmapFactory.decodeStream(stream, null, options)) { "Unable to decode stage preview: $path" }
    }
    return try {
        val png = ByteArrayOutputStream()
        check(bitmap.compress(Bitmap.CompressFormat.PNG, 100, png)) { "Unable to prepare stage preview: $path" }
        val bytes = png.toByteArray()
        Pixmap(bytes, 0, bytes.size)
    } finally {
        bitmap.recycle()
    }
}

/** Managed texture data decodes the source again after Android restores the GL context. */
private class WebpTextureData(private val path: String, private val previewMaxDimension: Int = 0) : TextureData {
    private var pixmap: Pixmap? = null
    private var width = 0
    private var height = 0
    private var format = Pixmap.Format.RGBA8888

    override fun getType(): TextureData.TextureDataType = TextureData.TextureDataType.Pixmap
    override fun isPrepared(): Boolean = pixmap != null
    override fun prepare() {
        check(pixmap == null) { "Already prepared: $path" }
        val decoded = if (previewMaxDimension > 0) sampledPixmapFromAsset(path, previewMaxDimension) else pixmapFromAsset(path)
        width = decoded.width
        height = decoded.height
        format = decoded.format
        pixmap = decoded
    }

    override fun consumePixmap(): Pixmap {
        val prepared = requireNotNull(pixmap) { "Prepare WebP before consuming: $path" }
        pixmap = null
        return prepared
    }

    override fun disposePixmap(): Boolean = true
    override fun consumeCustomData(target: Int): Unit = throw GdxRuntimeException("WebP texture data must be uploaded as a Pixmap")

    override fun getWidth(): Int = width
    override fun getHeight(): Int = height
    override fun getFormat(): Pixmap.Format = format
    override fun useMipMaps(): Boolean = false
    override fun isManaged(): Boolean = true
}
