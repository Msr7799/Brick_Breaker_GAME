/*
 * المسار: app/src/main/java/com/example/brick_breaker_ball/MonetizationServices.kt
 * المؤلف: mohamed alromaihi
 * الدوال: purchase, price, reconcile, dispose, show, preload, unavailable, earn, close.
 */
package com.example.brick_breaker_ball

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

    /** يعرض الإعلان الجاهز، ولا يمنح شيئاً عند الإغلاق أو الفشل. */
    fun show(callback: (RewardedAdResult) -> Unit)

    /** يحمّل الإعلان التالي أو يعيد المحاولة بعد فشل الشبكة. */
    fun preload() {}

    /** يظهر فقط عندما تطلب منصة الخصوصية من الناشر توفير نقطة دخول للمستخدم. */
    val privacyOptionsRequired: Boolean get() = false

    /** يفتح خيارات الخصوصية الخاصة بالإعلانات عندما تكون مطلوبة. */
    fun showPrivacyOptions(callback: (String?) -> Unit = {}) { callback(null) }

    /** يحدّث حالة الموافقة عند بداية جلسة التطبيق. */
    fun refreshConsent() {}

    /** يلغي المراجع والطلبات المرتبطة بالنشاط. */
    fun dispose() {}
}

/** يمنع تكرار إشعار المكافأة أو إصدار إغلاق سلبي بعد استحقاقها. */
class RewardOpportunity(private val token: String) {
    private var terminal = false

    /** يسمح بمنحة واحدة، وفقط عند استدعاء رد SDK الخاص بالاستحقاق. */
    fun earn(): RewardedAdResult.Earned? {
        if (terminal) return null
        terminal = true
        return RewardedAdResult.Earned(token)
    }

    /** إغلاق بلا استحقاق لا يضيف عناصر ويُنهي فرصة المكافأة. */
    fun close(): RewardedAdResult? {
        if (terminal) return null
        terminal = true
        return RewardedAdResult.ClosedWithoutReward
    }
}

data class MonetizationServices(
    val purchaseGateway: PurchaseGateway,
    val rewardedReviveAdGateway: RewardedAdGateway,
    val rewardedTalismanAdGateway: RewardedAdGateway,
) {
    /** Privacy settings use the revive gateway as the primary AdMob/UMP owner. */
    val rewardedAdGateway: RewardedAdGateway get() = rewardedReviveAdGateway

    fun dispose() {
        purchaseGateway.dispose()
        rewardedReviveAdGateway.dispose()
        rewardedTalismanAdGateway.dispose()
    }

    companion object {
        /** الفشل الآمن هو الافتراضي، ولا توجد بوابة تمنح نجاحاً وهمياً في التطبيق. */
        fun unavailable(): MonetizationServices {
            val unavailableAds = object : RewardedAdGateway {
                override fun show(callback: (RewardedAdResult) -> Unit) =
                    callback(RewardedAdResult.Failed("Ads unavailable"))
            }
            return MonetizationServices(
                purchaseGateway = object : PurchaseGateway {
                    override fun purchase(product: ShopProduct, callback: (PurchaseResult) -> Unit) =
                        callback(PurchaseResult.Failed("Store unavailable"))
                },
                rewardedReviveAdGateway = unavailableAds,
                rewardedTalismanAdGateway = unavailableAds,
            )
        }
    }
}
