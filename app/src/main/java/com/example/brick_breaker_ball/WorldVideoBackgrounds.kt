/*
 * ملاحظات صيانة الملف:
 * المسار: app/src/main/java/com/example/brick_breaker_ball/WorldVideoBackgrounds.kt
 * المؤلف: mohamed alromaihi
 * الدوال الموجودة: `assetForWorld`، `isVideoVisible`، `bind`، `unbind`، `show`، `showSplash`، `showAsset`، `hide`
 */

package com.example.brick_breaker_ball

/**
 * The Android host owns the ExoPlayer; gameplay only requests an asset by world.
 * Every campaign world maps to its matching numbered MP4 asset.
 */
object WorldVideoBackgrounds {
    const val SPLASH_ASSET = "splash.mp4"
    private val suppliedAssets = (1..13).associateWith { "$it.mp4" }

    private var activeAsset: String? = null
    private var visible = false
    private var playAsset: ((String) -> Unit)? = null
    private var stopPlayback: (() -> Unit)? = null

    /** ملاحظة صيانة: الدالة `assetForWorld` تنفّذ مسؤولية محلية يعتمد عليها هذا الجزء من اللعبة؛ راجع استدعاءاتها واختباراتها قبل تعديلها. */
    fun assetForWorld(world: Int): String? = suppliedAssets[world]

    /** ملاحظة صيانة: الدالة `isVideoVisible` تتحقق من الشرط المطلوب وتعيد نتيجة يمكن لبقية النظام الاعتماد عليها؛ راجع استدعاءاتها واختباراتها قبل تعديلها. */
    fun isVideoVisible(): Boolean = visible

    /** ملاحظة صيانة: الدالة `bind` تنفّذ مسؤولية محلية يعتمد عليها هذا الجزء من اللعبة؛ راجع استدعاءاتها واختباراتها قبل تعديلها. */
    @Synchronized fun bind(play: (String) -> Unit, stop: () -> Unit) {
        playAsset = play
        stopPlayback = stop
        activeAsset?.let(play)
    }

    /** ملاحظة صيانة: الدالة `unbind` تنفّذ مسؤولية محلية يعتمد عليها هذا الجزء من اللعبة؛ راجع استدعاءاتها واختباراتها قبل تعديلها. */
    @Synchronized fun unbind() {
        activeAsset = null
        visible = false
        playAsset = null
        stopPlayback = null
    }

    /** ملاحظة صيانة: الدالة `show` تنفّذ انتقالًا أو تعرض التدفق المطلوب للمستخدم؛ راجع استدعاءاتها واختباراتها قبل تعديلها. */
    @Synchronized fun show(world: Int) {
        val asset = assetForWorld(world)
        if (asset == null) {
            hide()
            return
        }
        showAsset(asset)
    }

    /** ملاحظة صيانة: الدالة `showSplash` تنفّذ انتقالًا أو تعرض التدفق المطلوب للمستخدم؛ راجع استدعاءاتها واختباراتها قبل تعديلها. */
    @Synchronized fun showSplash() = showAsset(SPLASH_ASSET)

    /** ملاحظة صيانة: الدالة `showAsset` تنفّذ انتقالًا أو تعرض التدفق المطلوب للمستخدم؛ راجع استدعاءاتها واختباراتها قبل تعديلها. */
    private fun showAsset(asset: String) {
        if (visible && activeAsset == asset) return
        activeAsset = asset
        visible = true
        playAsset?.invoke(asset)
    }

    /** ملاحظة صيانة: الدالة `hide` تنفّذ مسؤولية محلية يعتمد عليها هذا الجزء من اللعبة؛ راجع استدعاءاتها واختباراتها قبل تعديلها. */
    @Synchronized fun hide() {
        if (!visible && activeAsset == null) return
        activeAsset = null
        visible = false
        stopPlayback?.invoke()
    }
}
