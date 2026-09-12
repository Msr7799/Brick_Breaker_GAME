/*
 * ملاحظات صيانة الملف:
 * المسار: app/src/main/java/com/example/brick_breaker_ball/MainActivity.kt
 * المؤلف: mohamed alromaihi
 * الدوال الموجودة: `onCreate`، `playWorldVideo`، `stopWorldVideo`، `onPause`، `onResume`، `onDestroy`
 */

package com.example.brick_breaker_ball

import android.graphics.Color
import android.graphics.PixelFormat
import android.net.Uri
import android.opengl.GLSurfaceView
import android.os.Bundle
import android.view.ViewGroup
import android.widget.FrameLayout
import androidx.annotation.OptIn
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.ui.AspectRatioFrameLayout
import androidx.media3.ui.PlayerView
import com.badlogic.gdx.backends.android.AndroidApplication
import com.badlogic.gdx.backends.android.AndroidApplicationConfiguration

@OptIn(markerClass = [UnstableApi::class])
class MainActivity : AndroidApplication() {
    private lateinit var worldVideoPlayer: ExoPlayer
    private lateinit var worldVideoView: PlayerView
    private lateinit var brickBreakerGame: BrickBreakerGame
    private var loadedAsset: String? = null

    /** ملاحظة صيانة: الدالة `onCreate` تنفّذ العقد الموروث وتربط دورة حياة المكوّن بسلوك هذا الملف؛ راجع استدعاءاتها واختباراتها قبل تعديلها. */
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        worldVideoPlayer = ExoPlayer.Builder(this).build().apply {
            repeatMode = Player.REPEAT_MODE_ONE
            volume = 0f // The LibGDX music mixer remains the only audio source.
        }
        worldVideoView = PlayerView(this).apply {
            player = worldVideoPlayer
            useController = false
            resizeMode = AspectRatioFrameLayout.RESIZE_MODE_ZOOM
            setShutterBackgroundColor(Color.BLACK)
        }
        val monetization = MonetizationServices(
            purchaseGateway = PlayBillingPurchaseGateway(this),
            rewardedAdGateway = AdMobRewardedAdGateway(this, BuildConfig.REWARDED_AD_UNIT)
        )
        brickBreakerGame = BrickBreakerGame(monetization)
        val gameView = initializeForView(
            brickBreakerGame,
            AndroidApplicationConfiguration().apply {
                useImmersiveMode = true
                useAccelerometer = false
                useCompass = false
                a = 8
            }
        )
        (gameView as? GLSurfaceView)?.apply {
            setZOrderOnTop(true)
            holder.setFormat(PixelFormat.TRANSLUCENT)
        }
        setContentView(
            FrameLayout(this).apply {
                addView(worldVideoView, FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT))
                addView(gameView, FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT))
            }
        )
        WorldVideoBackgrounds.bind(
            play = { asset -> runOnUiThread { playWorldVideo(asset) } },
            stop = { runOnUiThread(::stopWorldVideo) }
        )
    }

    /** ملاحظة صيانة: الدالة `playWorldVideo` تنفّذ انتقالًا أو تعرض التدفق المطلوب للمستخدم؛ راجع استدعاءاتها واختباراتها قبل تعديلها. */
    private fun playWorldVideo(asset: String) {
        if (loadedAsset != asset) {
            loadedAsset = asset
            worldVideoPlayer.setMediaItem(MediaItem.fromUri(Uri.parse("asset:///backgrounds/$asset")))
            worldVideoPlayer.prepare()
        }
        worldVideoPlayer.playWhenReady = true
    }

    /** ملاحظة صيانة: الدالة `stopWorldVideo` تنظّف الحالة أو الموارد المرتبطة بهذه المسؤولية بأمان؛ راجع استدعاءاتها واختباراتها قبل تعديلها. */
    private fun stopWorldVideo() {
        worldVideoPlayer.pause()
        worldVideoPlayer.clearMediaItems()
        loadedAsset = null
    }

    /** ملاحظة صيانة: الدالة `onPause` تنفّذ العقد الموروث وتربط دورة حياة المكوّن بسلوك هذا الملف؛ راجع استدعاءاتها واختباراتها قبل تعديلها. */
    override fun onPause() {
        worldVideoPlayer.pause()
        super.onPause()
    }

    /** ملاحظة صيانة: الدالة `onResume` تنفّذ العقد الموروث وتربط دورة حياة المكوّن بسلوك هذا الملف؛ راجع استدعاءاتها واختباراتها قبل تعديلها. */
    override fun onResume() {
        super.onResume()
        if (WorldVideoBackgrounds.isVideoVisible()) worldVideoPlayer.playWhenReady = true
    }

    /** ملاحظة صيانة: الدالة `onDestroy` تنفّذ العقد الموروث وتربط دورة حياة المكوّن بسلوك هذا الملف؛ راجع استدعاءاتها واختباراتها قبل تعديلها. */
    override fun onDestroy() {
        WorldVideoBackgrounds.unbind()
        worldVideoPlayer.release()
        super.onDestroy()
    }
}
