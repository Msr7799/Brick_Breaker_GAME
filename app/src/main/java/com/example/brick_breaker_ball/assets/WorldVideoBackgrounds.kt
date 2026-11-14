/*
 * المسار: app/src/main/java/com/example/brick_breaker_ball/assets/WorldVideoBackgrounds.kt
 * الوظيفة: تحديد فيديو الخلفية المناسب لكل عالم.
 * الحسابات: اختيار المقطع بحسب العالم والمرحلة.
 */

package com.example.brick_breaker_ball.assets

/**
 * The Android host owns the ExoPlayer for the opening splash and portrait menu.
 */
object WorldVideoBackgrounds {
    const val SPLASH_ASSET = "splash.mp4"
    const val MENU_ASSET = "start-screen/main-screen/main-screen-volley.mp4"

    private var activeAsset: String? = null
    private var readyAsset: String? = null
    private var playAsset: ((String) -> Unit)? = null
    private var stopPlayback: (() -> Unit)? = null

    /** ملاحظة صيانة: الدالة `assetForWorld` تنفّذ مسؤولية محلية يعتمد عليها هذا الجزء من اللعبة؛ راجع استدعاءاتها واختباراتها قبل تعديلها. */

    /** ملاحظة صيانة: الدالة `isVideoVisible` تتحقق من الشرط المطلوب وتعيد نتيجة يمكن لبقية النظام الاعتماد عليها؛ راجع استدعاءاتها واختباراتها قبل تعديلها. */
    @Synchronized fun isVideoVisible(): Boolean = activeAsset != null && readyAsset == activeAsset

    /** Keeps the static LibGDX fallback visible until ExoPlayer has rendered a real frame. */
    @Synchronized fun markReady(asset: String) {
        if (activeAsset == asset) readyAsset = asset
    }

    /** ملاحظة صيانة: الدالة `bind` تنفّذ مسؤولية محلية يعتمد عليها هذا الجزء من اللعبة؛ راجع استدعاءاتها واختباراتها قبل تعديلها. */
    @Synchronized fun bind(play: (String) -> Unit, stop: () -> Unit) {
        playAsset = play
        stopPlayback = stop
        activeAsset?.let(play)
    }

    /** ملاحظة صيانة: الدالة `unbind` تنفّذ مسؤولية محلية يعتمد عليها هذا الجزء من اللعبة؛ راجع استدعاءاتها واختباراتها قبل تعديلها. */
    @Synchronized fun unbind() {
        activeAsset = null
        readyAsset = null
        playAsset = null
        stopPlayback = null
    }

    /** ملاحظة صيانة: الدالة `show` تنفّذ انتقالًا أو تعرض التدفق المطلوب للمستخدم؛ راجع استدعاءاتها واختباراتها قبل تعديلها. */

    /** ملاحظة صيانة: الدالة `showSplash` تنفّذ انتقالًا أو تعرض التدفق المطلوب للمستخدم؛ راجع استدعاءاتها واختباراتها قبل تعديلها. */
    @Synchronized fun showSplash() = showAsset(SPLASH_ASSET)

    @Synchronized fun showMenu() = showAsset(MENU_ASSET)

    /** ملاحظة صيانة: الدالة `showAsset` تنفّذ انتقالًا أو تعرض التدفق المطلوب للمستخدم؛ راجع استدعاءاتها واختباراتها قبل تعديلها. */
    private fun showAsset(asset: String) {
        if (activeAsset == asset) return
        activeAsset = asset
        readyAsset = null
        playAsset?.invoke(asset)
    }

    /** ملاحظة صيانة: الدالة `hide` تنفّذ مسؤولية محلية يعتمد عليها هذا الجزء من اللعبة؛ راجع استدعاءاتها واختباراتها قبل تعديلها. */
    @Synchronized fun hide() {
        if (activeAsset == null) return
        activeAsset = null
        readyAsset = null
        stopPlayback?.invoke()
    }
}
