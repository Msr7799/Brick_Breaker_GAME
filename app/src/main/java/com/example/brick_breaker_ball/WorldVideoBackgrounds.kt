package com.example.brick_breaker_ball

/**
 * The Android host owns the ExoPlayer; gameplay only requests an asset by world.
 * World 11 intentionally returns null because no 11.mp4 was supplied, allowing the
 * existing static artwork to remain the safe fallback instead of duplicating a video.
 */
object WorldVideoBackgrounds {
    const val SPLASH_ASSET = "splash.mp4"
    private val suppliedAssets = buildMap {
        (1..10).forEach { put(it, "$it.mp4") }
        put(12, "12.mp4")
        put(13, "13.mp4")
    }

    private var activeAsset: String? = null
    private var visible = false
    private var playAsset: ((String) -> Unit)? = null
    private var stopPlayback: (() -> Unit)? = null

    fun assetForWorld(world: Int): String? = suppliedAssets[world]
    fun isVideoVisible(): Boolean = visible

    @Synchronized fun bind(play: (String) -> Unit, stop: () -> Unit) {
        playAsset = play
        stopPlayback = stop
        activeAsset?.let(play)
    }

    @Synchronized fun unbind() {
        activeAsset = null
        visible = false
        playAsset = null
        stopPlayback = null
    }

    @Synchronized fun show(world: Int) {
        val asset = assetForWorld(world)
        if (asset == null) {
            hide()
            return
        }
        showAsset(asset)
    }

    @Synchronized fun showSplash() = showAsset(SPLASH_ASSET)

    private fun showAsset(asset: String) {
        if (visible && activeAsset == asset) return
        activeAsset = asset
        visible = true
        playAsset?.invoke(asset)
    }

    @Synchronized fun hide() {
        if (!visible && activeAsset == null) return
        activeAsset = null
        visible = false
        stopPlayback?.invoke()
    }
}
