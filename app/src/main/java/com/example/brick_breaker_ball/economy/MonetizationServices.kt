/*
 * المسار: app/src/main/java/com/example/brick_breaker_ball/economy/MonetizationServices.kt
 * الوظيفة: واجهات خدمات الإعلان والشراء للعبة.
 * الحسابات: لا توجد معادلات لعب.
 */

package com.example.brick_breaker_ball.economy

sealed interface PurchaseResult {
    data class Success(val transactionId: String) : PurchaseResult
    data object Pending : PurchaseResult
    data object Cancelled : PurchaseResult
    data class Failed(val message: String) : PurchaseResult
}

data class RecoveredPurchase(val storeId: String, val transactionId: String)

interface PurchaseGateway {
    /** يبدأ شراء منتج مهيأ فقط؛ نجاح الطلب لا يعني قبول عملية معلقة. */
    fun purchase(product: ShopProduct, callback: (PurchaseResult) -> Unit)

    /** يعيد السعر المحلي الموثوق من Play؛ null تعني أن الشراء غير متاح. */
    fun price(product: ShopProduct): String? = null

    /** يعيد طلب ProductDetails من المتجر، مثلاً عند فتح شاشة المتجر من جديد. */
    fun refreshProducts() {}

    /** يستعلم عن مشتريات Play المكتملة التي لم تستهلك بعد لإعادة منحها بشكل آمن. */
    fun reconcile(callback: (RecoveredPurchase) -> Unit = {}) {}

    /** يغلق اتصال المنصة عند انتهاء النشاط. */
    fun dispose() {}
}

enum class AdState { LOADING, READY, SHOWING, UNAVAILABLE, DISABLED }
sealed interface RewardedAdResult {
    data class Earned(val rewardToken: String) : RewardedAdResult
    data object ClosedWithoutReward : RewardedAdResult
    data class Failed(val message: String) : RewardedAdResult
}

interface RewardedAdGateway {
    val state: AdState get() = AdState.DISABLED
    val presentationInProgress: Boolean get() = false
    val preparingPresentation: Boolean get() = false
    val retryInSeconds: Int get() = 0
    val availabilityMessage: String get() = "No video available. Please try again shortly."

    fun onHostPaused() {}
    fun onHostResumed() {}

    /** يعرض الإعلان الجاهز، ولا يمنح شيئاً عند الإغلاق أو الفشل. */
    fun show(callback: (RewardedAdResult) -> Unit)

    /** Context binds a revive to its run/continue index; ordinary ads remain backwards compatible. */
    fun showForContext(context: String, callback: (RewardedAdResult) -> Unit) = show(callback)
    fun pendingRewards(context: String): List<String> = emptyList()
    fun acknowledgeReward(token: String, outcome: String) {}

    /** يحمّل الإعلان التالي أو يعيد المحاولة بعد فشل الشبكة. */
    fun preload() {}

    /** يظهر فقط عندما تطلب منصة الخصوصية من الناشر توفير نقطة دخول للمستخدم. */
    val privacyOptionsRequired: Boolean get() = false

    /** يفتح خيارات الخصوصية الخاصة بالإعلانات عندما تكون مطلوبة. */
    fun showPrivacyOptions(callback: (String?) -> Unit = {}) {
        callback(null)
    }

    /** يحدّث حالة الموافقة عند بداية جلسة التطبيق. */
    fun refreshConsent() {}

    /** يلغي المراجع والطلبات المرتبطة بالنشاط. */
    fun dispose() {}
}

/** Saves each SDK earning once and emits one UI result after the presentation ends. */
class RewardOpportunity(private val token: String) {
    var hasEarned = false
        private set
    private var finished = false

    /** A very late SDK earning still needs journaling, even after the UI has timed out. */
    fun earn(): RewardedAdResult.Earned? {
        if (hasEarned) return null
        hasEarned = true
        return RewardedAdResult.Earned(token)
    }

    /** The caller waits for dismissal/grace first; an earned reward wins over a failure. */
    fun close(failure: String? = null): RewardedAdResult? {
        if (finished) return null
        finished = true
        return when {
            hasEarned -> RewardedAdResult.Earned(token)
            failure != null -> RewardedAdResult.Failed(failure)
            else -> RewardedAdResult.ClosedWithoutReward
        }
    }
}

data class MonetizationServices(
    val purchaseGateway: PurchaseGateway,
    val rewardedReviveAdGateway: RewardedAdGateway,
    val rewardedTalismanAdGateway: RewardedAdGateway
) {
    /** Privacy settings use the revive gateway as the primary AdMob/UMP owner. */
    val rewardedAdGateway: RewardedAdGateway get() = rewardedReviveAdGateway
    val rewardedAdInProgress: Boolean get() = rewardedReviveAdGateway.presentationInProgress ||
        rewardedTalismanAdGateway.presentationInProgress

    fun onHostPaused() {
        rewardedReviveAdGateway.onHostPaused()
        rewardedTalismanAdGateway.onHostPaused()
    }

    fun onHostResumed() {
        rewardedReviveAdGateway.onHostResumed()
        rewardedTalismanAdGateway.onHostResumed()
    }

    fun dispose() {
        purchaseGateway.dispose()
        rewardedReviveAdGateway.dispose()
        rewardedTalismanAdGateway.dispose()
    }

    companion object {
        /** الفشل الآمن هو الافتراضي، ولا توجد بوابة تمنح نجاحاً وهمياً في التطبيق. */
        fun unavailable(): MonetizationServices {
            val unavailableAds = object : RewardedAdGateway {
                override fun show(callback: (RewardedAdResult) -> Unit) = callback(RewardedAdResult.Failed("Ads unavailable"))
            }
            return MonetizationServices(
                purchaseGateway = object : PurchaseGateway {
                    override fun purchase(product: ShopProduct, callback: (PurchaseResult) -> Unit) = callback(PurchaseResult.Failed("Store unavailable"))
                },
                rewardedReviveAdGateway = unavailableAds,
                rewardedTalismanAdGateway = unavailableAds
            )
        }
    }
}
