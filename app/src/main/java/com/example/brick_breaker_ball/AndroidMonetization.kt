package com.example.brick_breaker_ball

import android.app.Activity
import android.os.Handler
import android.os.Looper
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
import com.google.android.gms.ads.AdError
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.FullScreenContentCallback
import com.google.android.gms.ads.LoadAdError
import com.google.android.gms.ads.MobileAds
import com.google.android.gms.ads.OnUserEarnedRewardListener
import com.google.android.gms.ads.rewarded.RewardedAd
import com.google.android.gms.ads.rewarded.RewardedAdLoadCallback
import java.util.UUID
import java.util.concurrent.ConcurrentHashMap

/** Android implementation of the platform monetization bridges used by the LibGDX game. */
class AdMobRewardedAdGateway(
    private val activity: Activity,
    private val adUnitId: String
) : RewardedAdGateway {
    private val handler = Handler(Looper.getMainLooper())
    private var rewardedAd: RewardedAd? = null
    private var loading = false
    private var disposed = false

    @Volatile
    override var state: AdState = if (adUnitId.isBlank()) AdState.DISABLED else AdState.LOADING
        private set

    private val retryRunnable = Runnable {
        if (!disposed && rewardedAd == null && adUnitId.isNotBlank()) preload()
    }

    init {
        if (adUnitId.isNotBlank()) {
            activity.runOnUiThread {
                MobileAds.initialize(activity) { preload() }
            }
        }
    }

    override fun preload() {
        if (disposed || adUnitId.isBlank()) {
            state = AdState.DISABLED
            return
        }
        activity.runOnUiThread {
            if (disposed || loading || rewardedAd != null || state == AdState.SHOWING) return@runOnUiThread
            handler.removeCallbacks(retryRunnable)
            loading = true
            state = AdState.LOADING
            RewardedAd.load(
                activity,
                adUnitId,
                AdRequest.Builder().build(),
                object : RewardedAdLoadCallback() {
                    override fun onAdLoaded(ad: RewardedAd) {
                        if (disposed) return
                        loading = false
                        rewardedAd = ad
                        state = AdState.READY
                    }

                    override fun onAdFailedToLoad(error: LoadAdError) {
                        if (disposed) return
                        loading = false
                        rewardedAd = null
                        state = AdState.UNAVAILABLE
                        handler.removeCallbacks(retryRunnable)
                        handler.postDelayed(retryRunnable, RETRY_DELAY_MS)
                    }
                }
            )
        }
    }

    override fun show(callback: (RewardedAdResult) -> Unit) {
        if (disposed || adUnitId.isBlank()) {
            dispatch { callback(RewardedAdResult.Failed("Reward ads are unavailable")) }
            return
        }
        activity.runOnUiThread {
            val ad = rewardedAd
            if (ad == null || state != AdState.READY) {
                preload()
                dispatch { callback(RewardedAdResult.Failed("Reward ad is still loading")) }
                return@runOnUiThread
            }

            rewardedAd = null
            state = AdState.SHOWING
            val opportunity = RewardOpportunity("ad-${UUID.randomUUID()}")

            ad.fullScreenContentCallback = object : FullScreenContentCallback() {
                override fun onAdDismissedFullScreenContent() {
                    opportunity.close()?.let { result -> dispatch { callback(result) } }
                    state = AdState.LOADING
                    preload()
                }

                override fun onAdFailedToShowFullScreenContent(error: AdError) {
                    if (opportunity.close() != null) {
                        dispatch { callback(RewardedAdResult.Failed("Ad could not be shown. Please try again.")) }
                    }
                    state = AdState.LOADING
                    preload()
                }
            }

            ad.show(
                activity,
                OnUserEarnedRewardListener {
                    opportunity.earn()?.let { result -> dispatch { callback(result) } }
                }
            )
        }
    }

    override fun dispose() {
        disposed = true
        handler.removeCallbacks(retryRunnable)
        rewardedAd = null
        loading = false
        state = AdState.DISABLED
    }

    private fun dispatch(block: () -> Unit) {
        val app = Gdx.app
        if (app != null) app.postRunnable(block) else activity.runOnUiThread(block)
    }

    private companion object {
        const val RETRY_DELAY_MS = 15_000L
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
                ShopCatalog.products.filterNot { it.storeId in returned }.forEach {
                    android.util.Log.w("BrickBreakerBilling", "Play product unavailable: ${it.storeId}")
                }
            }
        }
    }

    override fun price(product: ShopProduct): String? {
        val details = productDetails[product.storeId] ?: return null
        return selectedOffer(details)?.formattedPrice
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
                                activity.runOnUiThread { consume(purchase) }
                            }
                        } else {
                            val storeId = purchase.products.firstOrNull { id -> ShopCatalog.billableProducts.any { it.storeId == id } }
                            val recovery = recoveryCallback
                            if (storeId != null && recovery != null) {
                                dispatch {
                                    recovery(RecoveredPurchase(storeId, purchase.purchaseToken))
                                    activity.runOnUiThread { consume(purchase) }
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
                        activity.runOnUiThread { consume(purchase) }
                    }
                }
            }
        }
    }

    private fun consume(purchase: Purchase) {
        if (disposed || !billingClient.isReady) return
        val params = ConsumeParams.newBuilder().setPurchaseToken(purchase.purchaseToken).build()
        billingClient.consumeAsync(params) { _, _ -> /* Reconcile retries any item left owned. */ }
    }

    private fun selectedOffer(details: ProductDetails): ProductDetails.OneTimePurchaseOfferDetails? = details.oneTimePurchaseOfferDetailsList?.firstOrNull() ?: details.oneTimePurchaseOfferDetails

    private fun clearActivePurchase() {
        activeProduct = null
        activeCallback = null
    }

    override fun dispose() {
        disposed = true
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
