/*
 * المسار: app/src/main/java/com/example/brick_breaker_ball/economy/AndroidMonetization.kt
 * الوظيفة: تكامل إعلانات Android وعمليات الشراء.
 * الحسابات: تحويل حالات الإعلان والشراء إلى نتائج اللعبة.
 */

package com.example.brick_breaker_ball.economy

import android.app.Activity
import android.os.Handler
import android.os.Looper
import android.os.SystemClock
import android.util.Log
import com.android.billingclient.api.AcknowledgePurchaseParams
import com.android.billingclient.api.BillingClient
import com.android.billingclient.api.BillingClientStateListener
import com.android.billingclient.api.BillingFlowParams
import com.android.billingclient.api.BillingResult
import com.android.billingclient.api.ConsumeParams
import com.android.billingclient.api.PendingPurchasesParams
import com.android.billingclient.api.ProductDetails
import com.android.billingclient.api.Purchase
import com.android.billingclient.api.PurchasesUpdatedListener
import com.android.billingclient.api.QueryProductDetailsParams
import com.android.billingclient.api.QueryPurchasesParams
import com.badlogic.gdx.Gdx
import com.example.brick_breaker_ball.BuildConfig
import com.google.android.gms.ads.AdError
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.FullScreenContentCallback
import com.google.android.gms.ads.LoadAdError
import com.google.android.gms.ads.MobileAds
import com.google.android.gms.ads.OnUserEarnedRewardListener
import com.google.android.gms.ads.RequestConfiguration
import com.google.android.gms.ads.rewarded.RewardedAd
import com.google.android.gms.ads.rewarded.RewardedAdLoadCallback
import com.google.android.gms.ads.rewarded.ServerSideVerificationOptions
import com.google.android.ump.ConsentInformation
import com.google.android.ump.ConsentRequestParameters
import com.google.android.ump.UserMessagingPlatform
import java.util.UUID
import java.util.concurrent.ConcurrentHashMap

/** Initialize the process-wide Mobile Ads SDK once, then notify both rewarded placements. */
private object MobileAdsStartup {
    private val handler = Handler(Looper.getMainLooper())
    private val readyCallbacks = mutableListOf<() -> Unit>()
    private var starting = false
    private var ready = false

    fun whenReady(activity: Activity, callback: () -> Unit) {
        activity.runOnUiThread {
            if (ready) {
                callback()
                return@runOnUiThread
            }
            readyCallbacks += callback
            if (starting) return@runOnUiThread
            starting = true
            // The supplied Play declaration targets 13+. A content rating does not identify
            // the current user's age. Keep UMP active and restrict the creative content to G.
            MobileAds.setRequestConfiguration(MobileAds.getRequestConfiguration().toBuilder()
                .setMaxAdContentRating(RequestConfiguration.MAX_AD_CONTENT_RATING_G)
                .setPublisherPrivacyPersonalizationState(RequestConfiguration.PublisherPrivacyPersonalizationState.DISABLED)
                .build())
            MobileAds.initialize(activity.applicationContext) {
                handler.post {
                    ready = true
                    starting = false
                    val callbacks = readyCallbacks.toList()
                    readyCallbacks.clear()
                    callbacks.forEach { it() }
                }
            }
        }
    }
}

/** Main-thread coordination across Revive and Shop, which share the same UMP decision. */
private object AdPrivacySession {
    val placements = mutableSetOf<AdMobRewardedAdGateway>()
    var privacyFormOpen = false
    @Volatile var fullscreenOwner: AdMobRewardedAdGateway? = null

    fun invalidateAll() { placements.toList().forEach { it.invalidatePrivacyCache() } }

    fun finishPrivacyForm() {
        privacyFormOpen = false
        invalidateAll()
        placements.toList().forEach { it.preload() }
    }
}

/** Android implementation of the platform monetization bridges used by the LibGDX game. */
class AdMobRewardedAdGateway(
    private val activity: Activity,
    private val adUnitId: String,
    private val placement: String,
    private val requestConsentOnInit: Boolean = true
) : RewardedAdGateway {
    private val handler = Handler(Looper.getMainLooper())
    private val consentInformation = UserMessagingPlatform.getConsentInformation(activity)
    private var rewardedAd: RewardedAd? = null
    private var loading = false
    private val ssv = RewardedSsvClient(activity, if (placement == "talisman") "orb" else placement)

    @Volatile private var disposed = false
    private var mobileAdsInitialized = false
    private var mobileAdsInitRequested = false
    private var consentRequestInFlight = false
    private var consentUpdateFailed = false
    private var loadedAtElapsedMs = 0L
    private var loadGeneration = 0
    private val retryPolicy = RewardedRetryPolicy()
    @Volatile private var loadMessage = "No video available. Please try again shortly."
    @Volatile private var preparing = false
    private var hostResumed = true
    private var presentationStartedAt = 0L
    private var activeFinish: ((String?) -> Unit)? = null
    private var dismissRunnable: Runnable? = null

    override val presentationInProgress: Boolean get() = AdPrivacySession.fullscreenOwner != null
    override val preparingPresentation: Boolean get() = preparing
    override val retryInSeconds: Int get() = retryPolicy.remainingSeconds(SystemClock.elapsedRealtime())
    override val availabilityMessage: String get() = loadMessage

    @Volatile
    override var state: AdState = if (adUnitId.isBlank()) AdState.DISABLED else AdState.LOADING
        private set

    override val privacyOptionsRequired: Boolean
        get() = consentInformation.privacyOptionsRequirementStatus ==
            ConsentInformation.PrivacyOptionsRequirementStatus.REQUIRED

    private val retryRunnable = Runnable {
        if (!disposed && rewardedAd == null && adUnitId.isNotBlank()) preload()
    }
    private val consentRetryRunnable = Runnable {
        if (!disposed && !consentInformation.canRequestAds()) refreshConsent()
    }
    private val expireAdRunnable = Runnable {
        if (!disposed && discardExpiredAd()) preload()
    }
    private val loadTimeoutRunnable = Runnable {
        if (!disposed && loading) {
            loadGeneration++ // Late SDK callbacks cannot replace a newer request.
            loading = false
            scheduleLoadRetry("Video loading took too long. Please try again.")
        }
    }
    private val presentationTimeoutRunnable: Runnable = object : Runnable {
        override fun run() {
            if (disposed || !hostResumed || AdPrivacySession.fullscreenOwner !== this@AdMobRewardedAdGateway) return
            // Wait for the host window too: a mediation overlay may keep the Activity resumed.
            if (!activity.hasWindowFocus()) {
                handler.postDelayed(this, 1_000L)
                return
            }
            activeFinish?.invoke("Video interrupted. Any earned reward is saved.")
        }
    }

    init {
        AdPrivacySession.placements += this
        if (adUnitId.isNotBlank() && requestConsentOnInit) refreshConsent()
    }

    override fun refreshConsent() {
        activity.runOnUiThread {
            if (disposed || adUnitId.isBlank()) {
                if (adUnitId.isBlank()) state = AdState.DISABLED
                return@runOnUiThread
            }
            if (consentRequestInFlight || AdPrivacySession.privacyFormOpen ||
                AdPrivacySession.fullscreenOwner != null) return@runOnUiThread
            consentRequestInFlight = true
            AdPrivacySession.privacyFormOpen = true
            consentUpdateFailed = false
            // A cached ad may no longer match the user's updated privacy choice.
            AdPrivacySession.invalidateAll()
            state = AdState.LOADING
            handler.removeCallbacks(consentRetryRunnable)
            val params = ConsentRequestParameters.Builder().build()
            consentInformation.requestConsentInfoUpdate(
                activity,
                params,
                {
                    if (!disposed) {
                        consentRequestInFlight = false
                        consentUpdateFailed = false
                        UserMessagingPlatform.loadAndShowConsentFormIfRequired(activity) { formError ->
                            AdPrivacySession.finishPrivacyForm()
                            if (!disposed) {
                                if (formError != null && !consentInformation.canRequestAds()) {
                                    scheduleConsentRetry("form/${formError.errorCode}")
                                } else {
                                    startAdsIfAllowed()
                                }
                            }
                        }
                    } else AdPrivacySession.finishPrivacyForm()
                },
                { error ->
                    AdPrivacySession.finishPrivacyForm()
                    if (!disposed) {
                        consentRequestInFlight = false
                        // UMP may still have a valid decision from a previous session.
                        if (consentInformation.canRequestAds()) {
                            startAdsIfAllowed()
                        } else {
                            scheduleConsentRetry("update/${error.errorCode}")
                        }
                    }
                }
            )
        }
    }

    private fun scheduleConsentRetry(reason: String) {
        consentUpdateFailed = true
        loadMessage = "Unable to check ad privacy choices. Please check your connection."
        state = AdState.UNAVAILABLE
        handler.removeCallbacks(retryRunnable)
        handler.removeCallbacks(consentRetryRunnable)
        handler.postDelayed(consentRetryRunnable, CONSENT_REQUEST_RETRY_MS)
        Log.w(TAG, "Consent unavailable for $placement ($reason); retry scheduled")
    }

    override fun preload() {
        activity.runOnUiThread {
            if (disposed || adUnitId.isBlank()) {
                state = AdState.DISABLED
                return@runOnUiThread
            }
            if (AdPrivacySession.privacyFormOpen) {
                state = AdState.LOADING
                return@runOnUiThread
            }
            if (!consentInformation.canRequestAds()) {
                clearCachedAd()
                state = if (consentUpdateFailed) AdState.UNAVAILABLE else AdState.LOADING
                handler.removeCallbacks(retryRunnable)
                if (!consentUpdateFailed) handler.postDelayed(retryRunnable, CONSENT_RETRY_DELAY_MS)
                return@runOnUiThread
            }
            if (!mobileAdsInitialized) {
                startAdsIfAllowed()
                return@runOnUiThread
            }
            discardExpiredAd()
            if (disposed || loading || rewardedAd != null || state == AdState.SHOWING) return@runOnUiThread
            val remaining = retryPolicy.remainingMs(SystemClock.elapsedRealtime())
            if (remaining > 0L) {
                handler.removeCallbacks(retryRunnable)
                handler.postDelayed(retryRunnable, remaining)
                return@runOnUiThread
            }
            handler.removeCallbacks(retryRunnable)
            loading = true
            state = AdState.LOADING
            val generation = ++loadGeneration
            handler.postDelayed(loadTimeoutRunnable, LOAD_TIMEOUT_MS)
            RewardedAd.load(
                activity,
                adUnitId,
                AdRequest.Builder().build(),
                object : RewardedAdLoadCallback() {
                    override fun onAdLoaded(ad: RewardedAd) {
                        if (disposed || generation != loadGeneration || !loading) return
                        handler.removeCallbacks(loadTimeoutRunnable)
                        loading = false
                        if (!consentInformation.canRequestAds()) {
                            clearCachedAd()
                            state = AdState.UNAVAILABLE
                            return
                        }
                        rewardedAd = ad
                        retryPolicy.reset()
                        loadedAtElapsedMs = SystemClock.elapsedRealtime()
                        state = AdState.READY
                        if (BuildConfig.QA_DIAGNOSTICS) Log.i(TAG, "Rewarded ad ready for $placement")
                        handler.removeCallbacks(expireAdRunnable)
                        handler.postDelayed(expireAdRunnable, MAX_AD_AGE_MS)
                    }

                    override fun onAdFailedToLoad(error: LoadAdError) {
                        if (disposed || generation != loadGeneration || !loading) return
                        handler.removeCallbacks(loadTimeoutRunnable)
                        loading = false
                        Log.w(TAG, "Rewarded ad load failed for $placement: ${error.domain}/${error.code}")
                        val noFill = error.domain == "com.google.android.gms.ads" && error.code == AdRequest.ERROR_CODE_NO_FILL
                        val message = when {
                            noFill -> "No video available right now. Please try again shortly."
                            error.domain == "com.google.android.gms.ads" && error.code == AdRequest.ERROR_CODE_NETWORK_ERROR ->
                                "Cannot load a video. Please check your connection."
                            else -> "Video unavailable right now. Please try again shortly."
                        }
                        scheduleLoadRetry(message, noFill)
                    }
                }
            )
        }
    }

    override fun show(callback: (RewardedAdResult) -> Unit) = showForContext("orb", callback)

    override fun pendingRewards(context: String): List<String> =
        if (presentationInProgress) emptyList() else ssv.pending(context)
    override fun acknowledgeReward(token: String, outcome: String) = ssv.acknowledge(token, outcome)

    override fun showForContext(context: String, callback: (RewardedAdResult) -> Unit) {
        activity.runOnUiThread {
            if (activity.isFinishing || activity.isDestroyed || AdPrivacySession.privacyFormOpen ||
                AdPrivacySession.fullscreenOwner != null) {
                dispatch { callback(RewardedAdResult.Failed("Ad unavailable right now. Please try again.")) }
                return@runOnUiThread
            }
            if (disposed || adUnitId.isBlank() || !consentInformation.canRequestAds()) {
                if (!disposed) {
                    clearCachedAd()
                    state = if (adUnitId.isBlank()) AdState.DISABLED else AdState.UNAVAILABLE
                }
                dispatch { callback(RewardedAdResult.Failed("Reward ads are unavailable until privacy choices are complete")) }
                return@runOnUiThread
            }
            val expired = discardExpiredAd()
            val ad = rewardedAd
            if (ad == null || state != AdState.READY) {
                preload()
                val message = if (expired) "Rewarded video expired; loading a fresh ad." else "Reward ad is still loading"
                dispatch { callback(RewardedAdResult.Failed(message)) }
                return@runOnUiThread
            }

            clearCachedAd()
            state = AdState.SHOWING
            AdPrivacySession.fullscreenOwner = this
            preparing = true
            presentationStartedAt = SystemClock.elapsedRealtime()
            val rewardToken = "ad-${UUID.randomUUID()}"
            val rewardOwnerUid = ssv.playerId()
            val opportunity = RewardOpportunity(rewardToken)
            var finished = false
            fun finish(failure: String? = null) {
                if (finished) return
                finished = true
                activeFinish = null
                preparing = false
                dismissRunnable?.let(handler::removeCallbacks)
                dismissRunnable = null
                handler.removeCallbacks(presentationTimeoutRunnable)
                releaseFullscreen()
                val result = opportunity.close(failure)
                state = if (disposed) AdState.DISABLED else AdState.LOADING
                if (result != null && !disposed) dispatch {
                    if (result is RewardedAdResult.Earned && ssv.playerId() != rewardOwnerUid)
                        callback(RewardedAdResult.Failed("Reward saved for the account that watched the video."))
                    else callback(result)
                }
                if (!disposed) preload()
            }
            activeFinish = { failure -> finish(failure) }
            handler.postDelayed(presentationTimeoutRunnable, PRESENTATION_TIMEOUT_MS)

            ad.fullScreenContentCallback = object : FullScreenContentCallback() {
                override fun onAdShowedFullScreenContent() {
                    if (finished) return
                    preparing = false
                    if (BuildConfig.QA_DIAGNOSTICS) Log.i(TAG, "Rewarded ad opened for $placement")
                }

                override fun onAdImpression() {
                    if (BuildConfig.QA_DIAGNOSTICS) Log.i(TAG, "Rewarded ad impression for $placement")
                }

                override fun onAdDismissedFullScreenContent() {
                    if (finished || dismissRunnable != null) return
                    preparing = false
                    // Mediation sources may report earned after dismissed, as in the RPG flow.
                    if (opportunity.hasEarned) finish() else {
                        dismissRunnable = Runnable { finish() }.also {
                            handler.postDelayed(it, LATE_REWARD_GRACE_MS)
                        }
                    }
                }

                override fun onAdFailedToShowFullScreenContent(error: AdError) {
                    if (finished) return
                    Log.w(TAG, "Rewarded ad show failed for $placement: ${error.domain}/${error.code}")
                    finish("Ad could not be shown. Please try again.")
                }
            }

            ssv.prepare(rewardToken, context) { options ->
                if (finished) return@prepare
                if (disposed || activity.isFinishing || activity.isDestroyed || ssv.playerId() != rewardOwnerUid) {
                    finish("Ad could not be shown. Please try again.")
                    return@prepare
                }
                if (!hostResumed) {
                    finish("Video cancelled while the game was in the background. Please try again.")
                    return@prepare
                }
                try {
                    if (options != null) ad.setServerSideVerificationOptions(
                        ServerSideVerificationOptions.Builder().setUserId(options.userId)
                            .setCustomData(options.customData).build()
                    )
                    ad.show(activity, OnUserEarnedRewardListener {
                        opportunity.earn()?.let {
                            ssv.earned(rewardToken) // Save before dispatch, so process death can recover it.
                            // Deliver after close; very late earnings remain recoverable in the journal.
                            if (!finished && dismissRunnable != null) finish()
                        }
                    })
                } catch (error: RuntimeException) {
                    Log.w(TAG, "Rewarded presentation failed for $placement", error)
                    finish("Ad could not be shown. Please try again.")
                }
            }
        }
    }

    override fun showPrivacyOptions(callback: (String?) -> Unit) {
        if (disposed || !privacyOptionsRequired) {
            dispatch { callback(null) }
            return
        }
        activity.runOnUiThread {
            if (disposed) return@runOnUiThread
            if (AdPrivacySession.privacyFormOpen || AdPrivacySession.fullscreenOwner != null) {
                dispatch { callback("An ad or privacy form is already open.") }
                return@runOnUiThread
            }
            AdPrivacySession.privacyFormOpen = true
            AdPrivacySession.invalidateAll()
            UserMessagingPlatform.showPrivacyOptionsForm(activity) { formError ->
                AdPrivacySession.finishPrivacyForm()
                if (disposed) return@showPrivacyOptionsForm
                // An ad loaded before a privacy change must not be reused afterward.
                clearCachedAd()
                if (formError != null && !consentInformation.canRequestAds()) {
                    scheduleConsentRetry("privacy/${formError.errorCode}")
                } else {
                    startAdsIfAllowed()
                }
                dispatch { callback(formError?.message) }
            }
        }
    }

    private fun startAdsIfAllowed() {
        if (disposed || adUnitId.isBlank()) return
        if (!consentInformation.canRequestAds()) {
            clearCachedAd()
            state = AdState.LOADING
            return
        }
        if (mobileAdsInitialized) {
            preload()
            return
        }
        activity.runOnUiThread {
            if (disposed || mobileAdsInitRequested) return@runOnUiThread
            mobileAdsInitRequested = true
            MobileAdsStartup.whenReady(activity) {
                if (!disposed) {
                    mobileAdsInitialized = true
                    preload()
                }
            }
        }
    }

    private fun discardExpiredAd(): Boolean {
        if (rewardedAd == null || loadedAtElapsedMs == 0L ||
            SystemClock.elapsedRealtime() - loadedAtElapsedMs < MAX_AD_AGE_MS
        ) {
            return false
        }
        clearCachedAd()
        state = AdState.LOADING
        return true
    }

    private fun clearCachedAd() {
        rewardedAd = null
        loadedAtElapsedMs = 0L
        handler.removeCallbacks(expireAdRunnable)
    }

    internal fun invalidatePrivacyCache() {
        loadGeneration++
        loading = false
        handler.removeCallbacks(loadTimeoutRunnable)
        retryPolicy.reset()
        clearCachedAd()
        handler.removeCallbacks(retryRunnable)
        if (!disposed && state != AdState.SHOWING) state =
            if (adUnitId.isBlank()) AdState.DISABLED else AdState.LOADING
    }

    private fun releaseFullscreen() {
        if (AdPrivacySession.fullscreenOwner === this) AdPrivacySession.fullscreenOwner = null
    }

    private fun scheduleLoadRetry(message: String, noFill: Boolean = false) {
        clearCachedAd()
        loadMessage = message
        state = AdState.UNAVAILABLE
        val delay = retryPolicy.failed(SystemClock.elapsedRealtime(), noFill)
        handler.removeCallbacks(retryRunnable)
        handler.postDelayed(retryRunnable, delay)
    }

    override fun onHostPaused() {
        hostResumed = false
        handler.removeCallbacks(presentationTimeoutRunnable)
    }

    override fun onHostResumed() {
        hostResumed = true
        if (AdPrivacySession.fullscreenOwner === this) {
            handler.removeCallbacks(presentationTimeoutRunnable)
            val remaining = (PRESENTATION_TIMEOUT_MS -
                (SystemClock.elapsedRealtime() - presentationStartedAt)).coerceAtLeast(LATE_REWARD_GRACE_MS)
            handler.postDelayed(presentationTimeoutRunnable, remaining)
        }
    }

    override fun dispose() {
        disposed = true
        state = AdState.DISABLED
        activity.runOnUiThread {
            AdPrivacySession.placements -= this
            activeFinish?.invoke("Ad interrupted.")
            releaseFullscreen()
            loadGeneration++
            handler.removeCallbacks(retryRunnable)
            handler.removeCallbacks(consentRetryRunnable)
            handler.removeCallbacks(loadTimeoutRunnable)
            handler.removeCallbacks(presentationTimeoutRunnable)
            dismissRunnable?.let(handler::removeCallbacks)
            clearCachedAd()
            loading = false
        }
    }

    private fun dispatch(block: () -> Unit) {
        val app = Gdx.app
        if (app != null) app.postRunnable(block) else activity.runOnUiThread(block)
    }

    private companion object {
        const val TAG = "RewardedAds"
        const val LOAD_TIMEOUT_MS = 45_000L
        const val PRESENTATION_TIMEOUT_MS = 300_000L
        const val LATE_REWARD_GRACE_MS = 1_500L
        const val CONSENT_RETRY_DELAY_MS = 5_000L
        const val CONSENT_REQUEST_RETRY_MS = 30_000L
        const val MAX_AD_AGE_MS = 55 * 60 * 1000L
    }
}

/** Google Play Billing implementation for the consumable booster products in [ShopCatalog]. */
class PlayBillingPurchaseGateway(
    private val activity: Activity
) : PurchaseGateway,
    PurchasesUpdatedListener {
    private val productDetails = ConcurrentHashMap<String, ProductDetails>()

    @Volatile private var connected = false

    @Volatile private var connecting = false
    private var disposed = false
    private var activeProduct: ShopProduct? = null
    private var activeCallback: ((PurchaseResult) -> Unit)? = null
    private var recoveryCallback: ((RecoveredPurchase) -> Unit)? = null
    private val settlementHandler = Handler(Looper.getMainLooper())
    private var settlementRetryCount = 0
    private val settlementRetry = Runnable {
        if (!disposed && recoveryCallback != null) {
            if (billingClient.isReady) queryOwnedPurchases() else connect()
        }
    }

    private val billingClient = BillingClient.newBuilder(activity)
        .setListener(this)
        .enablePendingPurchases(
            PendingPurchasesParams.newBuilder()
                .enableOneTimeProducts()
                .build()
        )
        .enableAutoServiceReconnection()
        .build()

    init {
        connect()
    }

    private fun connect() {
        if (disposed || billingClient.isReady || connecting) return
        connecting = true
        billingClient.startConnection(object : BillingClientStateListener {
            override fun onBillingSetupFinished(result: BillingResult) {
                connecting = false
                connected = result.responseCode == BillingClient.BillingResponseCode.OK
                if (connected) {
                    queryProducts()
                    if (recoveryCallback != null) queryOwnedPurchases()
                }
            }

            override fun onBillingServiceDisconnected() {
                connecting = false
                connected = false
            }
        })
    }

    private fun queryProducts() {
        if (disposed || !billingClient.isReady) return
        val products = ShopCatalog.billableProducts.map { product ->
            QueryProductDetailsParams.Product.newBuilder()
                .setProductId(product.storeId)
                .setProductType(BillingClient.ProductType.INAPP)
                .build()
        }
        val params = QueryProductDetailsParams.newBuilder().setProductList(products).build()
        billingClient.queryProductDetailsAsync(params) { result, detailsResult ->
            if (result.responseCode != BillingClient.BillingResponseCode.OK) return@queryProductDetailsAsync
            detailsResult.productDetailsList.forEach { details -> productDetails[details.productId] = details }
            if (BuildConfig.DEBUG) {
                val returned = detailsResult.productDetailsList.mapTo(hashSetOf()) { it.productId }
                ShopCatalog.purchasableProducts.filterNot { it.storeId in returned }.forEach {
                    android.util.Log.w("BrickBreakerBilling", "Play product unavailable: ${it.storeId}")
                }
            }
        }
    }

    override fun price(product: ShopProduct): String? {
        val details = productDetails[product.storeId] ?: return null
        val offer = selectedOffer(details) ?: return null
        return formatEnglishPlayPrice(offer.priceCurrencyCode, offer.priceAmountMicros)
    }

    override fun refreshProducts() {
        activity.runOnUiThread {
            if (disposed) return@runOnUiThread
            if (billingClient.isReady) queryProducts() else connect()
        }
    }

    override fun purchase(product: ShopProduct, callback: (PurchaseResult) -> Unit) {
        activity.runOnUiThread {
            if (disposed) {
                dispatch { callback(PurchaseResult.Failed("Store unavailable")) }
                return@runOnUiThread
            }
            if (!billingClient.isReady) {
                connect()
                dispatch { callback(PurchaseResult.Failed("Google Play is connecting. Please try again.")) }
                return@runOnUiThread
            }
            if (activeCallback != null) {
                dispatch { callback(PurchaseResult.Failed("Another purchase is already in progress")) }
                return@runOnUiThread
            }

            val details = productDetails[product.storeId]
            if (details == null) {
                queryProducts()
                dispatch { callback(PurchaseResult.Failed("This item is not available from Google Play yet")) }
                return@runOnUiThread
            }
            val offer = selectedOffer(details)
            if (offer == null) {
                dispatch { callback(PurchaseResult.Failed("No eligible Google Play offer is available")) }
                return@runOnUiThread
            }

            val offerToken = offer.offerToken
            if (offerToken.isNullOrBlank()) {
                dispatch { callback(PurchaseResult.Failed("Google Play returned an invalid offer")) }
                return@runOnUiThread
            }
            val productParams = BillingFlowParams.ProductDetailsParams.newBuilder()
                .setProductDetails(details)
                .setOfferToken(offerToken)
                .build()
            val flowParams = BillingFlowParams.newBuilder()
                .setProductDetailsParamsList(listOf(productParams))
                .build()

            activeProduct = product
            activeCallback = callback
            val result = billingClient.launchBillingFlow(activity, flowParams)
            if (result.responseCode != BillingClient.BillingResponseCode.OK) {
                clearActivePurchase()
                dispatch { callback(PurchaseResult.Failed(result.debugMessage.ifBlank { "Could not open Google Play purchase" })) }
            }
        }
    }

    override fun onPurchasesUpdated(result: BillingResult, purchases: List<Purchase>?) {
        val callback = activeCallback
        val product = activeProduct
        when (result.responseCode) {
            BillingClient.BillingResponseCode.USER_CANCELED -> {
                clearActivePurchase()
                callback?.let { dispatch { it(PurchaseResult.Cancelled) } }
            }

            BillingClient.BillingResponseCode.ITEM_ALREADY_OWNED -> {
                clearActivePurchase()
                queryOwnedPurchases()
                callback?.let { dispatch { it(PurchaseResult.Failed("Restoring your previous purchase…")) } }
            }

            BillingClient.BillingResponseCode.OK -> {
                val purchase = purchases.orEmpty().firstOrNull { bought ->
                    product == null || bought.products.contains(product.storeId)
                }
                if (purchase == null) {
                    clearActivePurchase()
                    callback?.let { dispatch { it(PurchaseResult.Failed("Google Play returned no purchase")) } }
                    return
                }
                when (purchase.purchaseState) {
                    Purchase.PurchaseState.PURCHASED -> {
                        clearActivePurchase()
                        if (callback != null && product != null) {
                            dispatch {
                                callback(PurchaseResult.Success(purchase.purchaseToken))
                                activity.runOnUiThread { finalizePurchase(purchase, product) }
                            }
                        } else {
                            val storeId = purchase.products.firstOrNull { id -> ShopCatalog.billableProducts.any { it.storeId == id } }
                            val recovery = recoveryCallback
                            if (storeId != null && recovery != null) {
                                dispatch {
                                    recovery(RecoveredPurchase(storeId, purchase.purchaseToken))
                                    activity.runOnUiThread { finalizePurchase(purchase, ShopCatalog.findByStoreId(storeId)) }
                                }
                            }
                        }
                    }

                    Purchase.PurchaseState.PENDING -> {
                        clearActivePurchase()
                        callback?.let { dispatch { it(PurchaseResult.Pending) } }
                    }

                    else -> {
                        clearActivePurchase()
                        callback?.let { dispatch { it(PurchaseResult.Failed("Purchase was not completed")) } }
                    }
                }
            }

            else -> {
                clearActivePurchase()
                callback?.let {
                    dispatch { it(PurchaseResult.Failed(result.debugMessage.ifBlank { "Google Play purchase failed" })) }
                }
            }
        }
    }

    override fun reconcile(callback: (RecoveredPurchase) -> Unit) {
        recoveryCallback = callback
        if (!billingClient.isReady) connect() else queryOwnedPurchases()
    }

    private fun queryOwnedPurchases() {
        if (disposed || !billingClient.isReady) return
        val params = QueryPurchasesParams.newBuilder()
            .setProductType(BillingClient.ProductType.INAPP)
            .build()
        billingClient.queryPurchasesAsync(params) { result, purchases ->
            if (result.responseCode != BillingClient.BillingResponseCode.OK) return@queryPurchasesAsync
            purchases.filter { it.purchaseState == Purchase.PurchaseState.PURCHASED }.forEach { purchase ->
                val storeId = purchase.products.firstOrNull { id -> ShopCatalog.billableProducts.any { it.storeId == id } } ?: return@forEach
                recoveryCallback?.let { callback ->
                    dispatch {
                        callback(RecoveredPurchase(storeId, purchase.purchaseToken))
                        activity.runOnUiThread { finalizePurchase(purchase, ShopCatalog.findByStoreId(storeId)) }
                    }
                }
            }
        }
    }

    /**
     * Consume/acknowledge only after the game-side credit callback has returned.
     * Report failures without logging purchase tokens or retrying indefinitely.
     * A later owned-purchase reconciliation may safely re-run the idempotent credit.
     */
    private fun handleSettlementResult(operation: String, result: BillingResult) {
        if (disposed) return
        if (result.responseCode == BillingClient.BillingResponseCode.OK) {
            settlementRetryCount = 0
            settlementHandler.removeCallbacks(settlementRetry)
            return
        }
        Log.w("BrickBreakerBilling", "$operation failed: code=${result.responseCode}; ${result.debugMessage}")
        val retryable = result.responseCode == BillingClient.BillingResponseCode.SERVICE_DISCONNECTED ||
            result.responseCode == BillingClient.BillingResponseCode.SERVICE_UNAVAILABLE ||
            result.responseCode == BillingClient.BillingResponseCode.NETWORK_ERROR ||
            result.responseCode == BillingClient.BillingResponseCode.ERROR
        if (retryable && recoveryCallback != null && settlementRetryCount < 3) {
            val delayMs = (5_000L shl settlementRetryCount).coerceAtMost(20_000L)
            settlementRetryCount++
            settlementHandler.removeCallbacks(settlementRetry)
            settlementHandler.postDelayed(settlementRetry, delayMs)
        }
    }

    private fun consume(purchase: Purchase) {
        if (disposed || !billingClient.isReady) return
        val params = ConsumeParams.newBuilder().setPurchaseToken(purchase.purchaseToken).build()
        billingClient.consumeAsync(params) { result, _ ->
            activity.runOnUiThread { handleSettlementResult("Consume", result) }
        }
    }

    private fun finalizePurchase(purchase: Purchase, product: ShopProduct?) {
        if (product?.ballSpriteName == null) {
            consume(purchase)
        } else if (!purchase.isAcknowledged && !disposed && billingClient.isReady) {
            val params = AcknowledgePurchaseParams.newBuilder().setPurchaseToken(purchase.purchaseToken).build()
            billingClient.acknowledgePurchase(params) { result ->
                activity.runOnUiThread { handleSettlementResult("Acknowledge", result) }
            }
        }
    }

    private fun selectedOffer(details: ProductDetails): ProductDetails.OneTimePurchaseOfferDetails? = details.oneTimePurchaseOfferDetailsList?.firstOrNull() ?: details.oneTimePurchaseOfferDetails

    private fun clearActivePurchase() {
        activeProduct = null
        activeCallback = null
    }

    override fun dispose() {
        disposed = true
        settlementHandler.removeCallbacks(settlementRetry)
        clearActivePurchase()
        recoveryCallback = null
        productDetails.clear()
        if (billingClient.isReady || connected) billingClient.endConnection()
        connected = false
        connecting = false
    }

    private fun dispatch(block: () -> Unit) {
        val app = Gdx.app
        if (app != null) app.postRunnable(block) else activity.runOnUiThread(block)
    }
}
