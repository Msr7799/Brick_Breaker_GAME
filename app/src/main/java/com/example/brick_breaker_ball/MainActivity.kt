/*
 * المسار: app/src/main/java/com/example/brick_breaker_ball/MainActivity.kt
 * الوظيفة: مدخل Android وإدارة فيديو العوالم ودورة حياة التطبيق.
 * الحسابات: تحويل اتجاه الشاشة وأبعاد الفيديو إلى مساحة العرض.
 */

package com.example.brick_breaker_ball

import android.content.ActivityNotFoundException
import android.content.Intent
import android.content.pm.ActivityInfo
import android.graphics.Color
import android.graphics.PixelFormat
import android.net.Uri
import android.opengl.GLSurfaceView
import android.os.Bundle
import android.util.Log
import android.view.ViewGroup
import android.widget.FrameLayout
import android.widget.Toast
import androidx.annotation.OptIn
import androidx.media3.common.MediaItem
import androidx.media3.common.PlaybackException
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.ui.AspectRatioFrameLayout
import androidx.media3.ui.PlayerView
import com.badlogic.gdx.backends.android.AndroidApplication
import com.badlogic.gdx.backends.android.AndroidApplicationConfiguration
import com.example.brick_breaker_ball.app.PlatformActions
import com.example.brick_breaker_ball.app.CloudProgressApiClient
import com.example.brick_breaker_ball.app.CloudProgressResult
import com.example.brick_breaker_ball.app.AccountServerHttpException
import com.example.brick_breaker_ball.app.DailyRewardApiClient
import com.example.brick_breaker_ball.app.PlayGamesBridgeClient
import com.example.brick_breaker_ball.app.QaDiagnosticsOverlay
import com.example.brick_breaker_ball.app.ServerDailyStatus
import com.example.brick_breaker_ball.assets.WorldVideoBackgrounds
import com.example.brick_breaker_ball.economy.AdMobRewardedAdGateway
import com.example.brick_breaker_ball.economy.MonetizationServices
import com.example.brick_breaker_ball.economy.PlayBillingPurchaseGateway
import com.example.brick_breaker_ball.gameplay.GameplayMode
import com.google.android.gms.games.PlayGames
import com.google.android.gms.games.PlayGamesSdk
import com.google.android.gms.common.api.ApiException
import com.google.firebase.FirebaseApp
import com.google.firebase.auth.FirebaseAuth
import com.google.android.play.core.appupdate.AppUpdateManagerFactory
import com.google.android.play.core.appupdate.AppUpdateOptions
import com.google.android.play.core.install.model.AppUpdateType
import com.google.android.play.core.install.model.UpdateAvailability

@OptIn(markerClass = [UnstableApi::class])
class MainActivity : AndroidApplication() {
    private lateinit var worldVideoPlayer: ExoPlayer
    private lateinit var worldVideoView: PlayerView
    private lateinit var brickBreakerGame: BrickBreakerGame
    private var loadedAsset: String? = null
    private var playGamesAuthInProgress = false
    @Volatile private var playerAccountVerified = false
    private val appUpdateManager by lazy { AppUpdateManagerFactory.create(this) }
    @Volatile private var appUpdateCheckComplete = false
    @Volatile private var availableAppUpdateVersionCode: Int? = null
    private var appUpdateLaunchInProgress = false
    private val dailyRewardsApi by lazy { DailyRewardApiClient() }
    private val cloudProgressDelegate = lazy { CloudProgressApiClient() }
    private val cloudProgressApi by cloudProgressDelegate
    private val playGamesBridgeDelegate = lazy { PlayGamesBridgeClient() }
    private val playGamesBridge by playGamesBridgeDelegate

    /** ملاحظة صيانة: الدالة `onCreate` تنفّذ العقد الموروث وتربط دورة حياة المكوّن بسلوك هذا الملف؛ راجع استدعاءاتها واختباراتها قبل تعديلها. */
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // Install the window content before SDK setup or libGDX accesses its decor view.
        val contentView = FrameLayout(this).apply { setBackgroundColor(Color.BLACK) }
        setContentView(contentView)
        PlayGamesSdk.initialize(this)
        worldVideoPlayer = ExoPlayer.Builder(this).build().apply {
            repeatMode = Player.REPEAT_MODE_ONE
            volume = 0f // The LibGDX music mixer remains the only audio source.
            addListener(object : Player.Listener {
                override fun onRenderedFirstFrame() {
                    loadedAsset?.let { asset ->
                        WorldVideoBackgrounds.markReady(asset)
                        if (BuildConfig.QA_DIAGNOSTICS) {
                            Log.i("MenuVideo", "First video frame rendered: $asset")
                        }
                    }
                }

                override fun onPlayerError(error: PlaybackException) {
                    if (BuildConfig.QA_DIAGNOSTICS) {
                        Log.e("MenuVideo", "Video playback failed: $loadedAsset", error)
                    }
                }
            })
        }
        worldVideoView = PlayerView(this).apply {
            player = worldVideoPlayer
            useController = false
            resizeMode = AspectRatioFrameLayout.RESIZE_MODE_ZOOM
            setShutterBackgroundColor(Color.BLACK)
        }
        contentView.addView(worldVideoView, FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT))
        val monetization = MonetizationServices(
            purchaseGateway = PlayBillingPurchaseGateway(this),
            rewardedReviveAdGateway = AdMobRewardedAdGateway(
                this,
                BuildConfig.REWARDED_REVIVE_AD_UNIT,
                placement = "revive",
                requestConsentOnInit = true
            ),
            rewardedTalismanAdGateway = AdMobRewardedAdGateway(
                this,
                BuildConfig.REWARDED_TALISMAN_AD_UNIT,
                placement = "talisman",
                requestConsentOnInit = false
            )
        )
        val platformActions = object : PlatformActions {
            override val appUpdateCheckComplete: Boolean
                get() = this@MainActivity.appUpdateCheckComplete
            override val availableAppUpdateVersionCode: Int?
                get() = this@MainActivity.availableAppUpdateVersionCode
            override fun startAppUpdate() = this@MainActivity.launchAvailableAppUpdate()
            override val cloudProgressEnabled: Boolean get() = cloudProgressApi.enabled
            override fun connectPlayerAccount() {
                runOnUiThread {
                    if (!playGamesAuthInProgress) {
                        playerAccountVerified = false
                        signInWithPlayGames()
                    }
                }
            }
            override fun readCloudProgress(accountId: String, callback: (CloudProgressResult) -> Unit) =
                cloudProgressApi.read(accountId, callback)
            override fun writeCloudProgress(accountId: String, revision: Long, saveId: String, snapshot: String, callback: (CloudProgressResult) -> Unit) =
                cloudProgressApi.write(accountId, revision, saveId, snapshot, callback)
            override val serverDailyRewardsEnabled: Boolean get() = dailyRewardsApi.enabled
            override val dailyRewardAccountId: String?
                get() = if (playerAccountVerified) FirebaseAuth.getInstance().currentUser?.uid else null
            override fun fetchDailyRewardStatus(callback: (ServerDailyStatus?, String?) -> Unit) =
                dailyRewardsApi.status(callback)
            override fun claimDailyReward(callback: (ServerDailyStatus?, String?) -> Unit) =
                dailyRewardsApi.claim(callback)
            override fun shareGame() {
                runOnUiThread {
                    val storeUrl = "https://play.google.com/store/apps/details?id=$packageName"
                    val shareIntent = Intent(Intent.ACTION_SEND).apply {
                        type = "text/plain"
                        putExtra(Intent.EXTRA_SUBJECT, getString(R.string.app_name))
                        putExtra(
                            Intent.EXTRA_TEXT,
                            "${getString(R.string.app_name)}\n$storeUrl\nhttps://youtu.be/xFAEdzZf7L4"
                        )
                    }
                    startActivity(Intent.createChooser(shareIntent, getString(R.string.share_game)))
                }
            }

            override fun openGameplayVideo() {
                runOnUiThread {
                    val videoUri = Uri.parse("https://youtu.be/xFAEdzZf7L4")
                    val youtubeIntent = Intent(Intent.ACTION_VIEW, videoUri).apply {
                        setPackage("com.google.android.youtube")
                    }
                    try {
                        startActivity(youtubeIntent)
                    } catch (_: ActivityNotFoundException) {
                        startActivity(Intent(Intent.ACTION_VIEW, videoUri))
                    }
                }
            }

            override fun setGameplayOrientation(mode: GameplayMode) {
                runOnUiThread {
                    // Volley uses portrait UI and gameplay.
                    requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_PORTRAIT
                }
            }
        }
        brickBreakerGame = BrickBreakerGame(monetization, platformActions)
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
        contentView.addView(gameView, FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT))
        WorldVideoBackgrounds.bind(
            play = { asset -> runOnUiThread { playWorldVideo(asset) } },
            stop = { runOnUiThread(::stopWorldVideo) }
        )
        signInWithPlayGames()
    }

    private fun checkForAppUpdate() {
        appUpdateCheckComplete = false
        appUpdateManager.appUpdateInfo
            .addOnSuccessListener { info ->
                availableAppUpdateVersionCode = when (info.updateAvailability()) {
                    UpdateAvailability.UPDATE_AVAILABLE,
                    UpdateAvailability.DEVELOPER_TRIGGERED_UPDATE_IN_PROGRESS -> info.availableVersionCode()
                    else -> null
                }
                appUpdateCheckComplete = true
            }
            .addOnFailureListener { error ->
                availableAppUpdateVersionCode = null
                appUpdateCheckComplete = true
                Log.w(TAG, "Google Play update check unavailable", error)
            }
    }

    private fun launchAvailableAppUpdate() {
        runOnUiThread {
            if (appUpdateLaunchInProgress) return@runOnUiThread
            appUpdateLaunchInProgress = true
            // AppUpdateInfo is single-use: request a fresh instance for every button press.
            appUpdateManager.appUpdateInfo
                .addOnSuccessListener { info ->
                    if (info.updateAvailability() != UpdateAvailability.UPDATE_AVAILABLE &&
                        info.updateAvailability() != UpdateAvailability.DEVELOPER_TRIGGERED_UPDATE_IN_PROGRESS
                    ) {
                        availableAppUpdateVersionCode = null
                        appUpdateLaunchInProgress = false
                        Toast.makeText(this, "Your game is already up to date", Toast.LENGTH_SHORT).show()
                    } else if (info.isUpdateTypeAllowed(AppUpdateType.IMMEDIATE)) {
                        appUpdateManager.startUpdateFlow(
                            info,
                            this,
                            AppUpdateOptions.newBuilder(AppUpdateType.IMMEDIATE).build()
                        ).addOnCompleteListener { result ->
                            appUpdateLaunchInProgress = false
                            if (!result.isSuccessful) {
                                Log.w(TAG, "Google Play update flow failed", result.exception)
                                openGameInPlayStore()
                            }
                        }
                    } else {
                        appUpdateLaunchInProgress = false
                        openGameInPlayStore()
                    }
                }
                .addOnFailureListener { error ->
                    appUpdateLaunchInProgress = false
                    Log.w(TAG, "Could not start Google Play update", error)
                    openGameInPlayStore()
                }
        }
    }

    private fun openGameInPlayStore() {
        val listing = "details?id=$packageName"
        try {
            startActivity(Intent(Intent.ACTION_VIEW, Uri.parse("market://$listing")).apply {
                setPackage("com.android.vending")
            })
        } catch (_: ActivityNotFoundException) {
            try {
                startActivity(Intent(Intent.ACTION_VIEW, Uri.parse("https://play.google.com/store/apps/$listing")))
            } catch (error: ActivityNotFoundException) {
                Log.w(TAG, "No app can open the Google Play listing", error)
                Toast.makeText(this, "Could not open Google Play", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun signInWithPlayGames() {
        if (playGamesAuthInProgress || FirebaseApp.getApps(this).isEmpty()) {
            Log.w(TAG, "Firebase is not configured for this app variant; skipping account sign-in")
            QaDiagnosticsOverlay.reportAccount("firebase_not_configured")
            return
        }
        // Verify the currently selected Play Games player on every process start.
        // A cached Firebase token alone may still belong to a previously selected player.
        val webClientId = BuildConfig.PLAY_GAMES_WEB_CLIENT_ID.trim()
        if (webClientId.isEmpty()) {
            Log.w(TAG, "Play Games Web OAuth client is missing; skipping account sign-in")
            QaDiagnosticsOverlay.reportAccount("game_server_client_missing")
            return
        }
        playGamesAuthInProgress = true
        QaDiagnosticsOverlay.reportAccount("checking_play_games")
        val gamesSignInClient = PlayGames.getGamesSignInClient(this)
        gamesSignInClient.isAuthenticated.addOnCompleteListener { authenticationTask ->
            if (!authenticationTask.isSuccessful) {
                finishPlayGamesSignIn(authenticationTask.exception, "check_play_games")
                return@addOnCompleteListener
            }
            if (authenticationTask.result.isAuthenticated) {
                authenticateFirebaseWithPlayGames(gamesSignInClient, webClientId)
            } else {
                QaDiagnosticsOverlay.reportAccount("signing_in_play_games")
                gamesSignInClient.signIn().addOnCompleteListener { signInTask ->
                    if (!signInTask.isSuccessful) {
                        finishPlayGamesSignIn(signInTask.exception, "sign_in_play_games")
                    } else if (signInTask.result.isAuthenticated) {
                        authenticateFirebaseWithPlayGames(gamesSignInClient, webClientId)
                    } else {
                        finishPlayGamesSignIn(IllegalStateException("Play Games sign-in was not completed"), "sign_in_play_games")
                    }
                }
            }
        }
    }

    private fun authenticateFirebaseWithPlayGames(
        gamesSignInClient: com.google.android.gms.games.GamesSignInClient,
        webClientId: String
    ) {
        QaDiagnosticsOverlay.reportAccount("requesting_server_code")
        gamesSignInClient.requestServerSideAccess(webClientId, false)
            .addOnSuccessListener { authCode ->
                QaDiagnosticsOverlay.reportAccount("exchanging_server_code")
                playGamesBridge.exchange(authCode) { customToken, error ->
                    runOnUiThread {
                        if (error != null || customToken == null) {
                            finishPlayGamesSignIn(error ?: IllegalStateException("No Firebase token from account server"), "exchange_server_code")
                        } else {
                            QaDiagnosticsOverlay.reportAccount("signing_in_firebase")
                            FirebaseAuth.getInstance().signInWithCustomToken(customToken)
                                .addOnSuccessListener {
                                    playGamesAuthInProgress = false
                                    playerAccountVerified = true
                                    QaDiagnosticsOverlay.reportAccount("connected")
                                    Log.i(TAG, "Firebase account linked through Play Games server")
                                    notifyGameAccountConnected()
                                    Toast.makeText(this, "Play Games account connected", Toast.LENGTH_SHORT).show()
                                }
                                .addOnFailureListener { finishPlayGamesSignIn(it, "sign_in_firebase") }
                        }
                    }
                }
            }
            .addOnFailureListener { finishPlayGamesSignIn(it, "request_server_code") }
    }

    private fun notifyGameAccountConnected() {
        com.badlogic.gdx.Gdx.app?.postRunnable { brickBreakerGame.onPlayGamesAccountConnected() }
    }

    private fun finishPlayGamesSignIn(error: Exception?, stage: String) {
        playGamesAuthInProgress = false
        playerAccountVerified = false
        val code = when (error) {
            is ApiException -> error.statusCode.toString()
            is AccountServerHttpException -> error.statusCode.toString()
            else -> null
        }
        QaDiagnosticsOverlay.reportAccount("${stage}_failed", code)
        if (error == null) {
            Log.w(TAG, "Play Games account connection stopped at $stage")
        } else {
            Log.e(TAG, "Play Games account connection failed at $stage; status=${code ?: "n/a"}; type=${error.javaClass.simpleName}")
            runOnUiThread {
                Toast.makeText(this, "Account connection failed. Check Play Games and Firebase setup.", Toast.LENGTH_LONG).show()
            }
        }
    }

    private companion object {
        const val TAG = "BrickBreakerAuth"
    }

    /** ملاحظة صيانة: الدالة `playWorldVideo` تنفّذ انتقالًا أو تعرض التدفق المطلوب للمستخدم؛ راجع استدعاءاتها واختباراتها قبل تعديلها. */
    private fun playWorldVideo(asset: String) {
        if (worldVideoView.player !== worldVideoPlayer) worldVideoView.player = worldVideoPlayer
        worldVideoView.resizeMode = AspectRatioFrameLayout.RESIZE_MODE_ZOOM
        if (loadedAsset != asset) {
            loadedAsset = asset
            if (BuildConfig.QA_DIAGNOSTICS) {
                Log.i("MenuVideo", "Requested video asset: $asset")
            }
            worldVideoPlayer.setMediaItem(MediaItem.fromUri(Uri.parse("asset:///backgrounds/$asset")))
            worldVideoPlayer.prepare()
        }
        worldVideoPlayer.playWhenReady = true
    }

    /** ملاحظة صيانة: الدالة `stopWorldVideo` تنظّف الحالة أو الموارد المرتبطة بهذه المسؤولية بأمان؛ راجع استدعاءاتها واختباراتها قبل تعديلها. */
    private fun stopWorldVideo() {
        worldVideoPlayer.playWhenReady = false
        worldVideoView.player = null
        worldVideoPlayer.stop()
        worldVideoPlayer.clearMediaItems()
        loadedAsset = null
    }

    /** ملاحظة صيانة: الدالة `onPause` تنفّذ العقد الموروث وتربط دورة حياة المكوّن بسلوك هذا الملف؛ راجع استدعاءاتها واختباراتها قبل تعديلها. */
    override fun onPause() {
        if (::brickBreakerGame.isInitialized) brickBreakerGame.monetization.onHostPaused()
        worldVideoPlayer.pause()
        super.onPause()
    }

    /** ملاحظة صيانة: الدالة `onResume` تنفّذ العقد الموروث وتربط دورة حياة المكوّن بسلوك هذا الملف؛ راجع استدعاءاتها واختباراتها قبل تعديلها. */
    override fun onResume() {
        super.onResume()
        if (::brickBreakerGame.isInitialized) brickBreakerGame.monetization.onHostResumed()
        if (::brickBreakerGame.isInitialized && !brickBreakerGame.monetization.rewardedAdInProgress) checkForAppUpdate()
        if (WorldVideoBackgrounds.isVideoVisible()) worldVideoPlayer.playWhenReady = true
    }

    /** ملاحظة صيانة: الدالة `onDestroy` تنفّذ العقد الموروث وتربط دورة حياة المكوّن بسلوك هذا الملف؛ راجع استدعاءاتها واختباراتها قبل تعديلها. */
    override fun onDestroy() {
        WorldVideoBackgrounds.unbind()
        if (::brickBreakerGame.isInitialized) dailyRewardsApi.close()
        if (playGamesBridgeDelegate.isInitialized()) playGamesBridge.close()
        if (cloudProgressDelegate.isInitialized()) cloudProgressApi.close()
        worldVideoView.player = null
        worldVideoPlayer.release()
        super.onDestroy()
    }
}
