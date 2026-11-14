/*
 * المسار: app/src/main/java/com/example/brick_breaker_ball/economy/StorePurchaseConfirmation.kt
 * الوظيفة: تنسيق تأكيد عمليات الشراء.
 * الحسابات: شروط الانتقال بين حالات التأكيد.
 */

package com.example.brick_breaker_ball.economy

/** A deliberate, one-shot confirmation gate for every purchase initiated in the shop. */
data class StorePurchaseConfirmation(
    val itemName: String,
    val costLabel: String,
    val detail: String,
    val onConfirmed: () -> Unit
)

/**
 * Input from the underlying shop must be blocked while [pending] is non-null.
 * A confirmation can execute its action only once; cancelling never executes it.
 */
class StorePurchaseConfirmationController {
    var pending: StorePurchaseConfirmation? = null
        private set

    fun request(confirmation: StorePurchaseConfirmation): Boolean {
        if (pending != null) return false
        pending = confirmation
        return true
    }

    fun cancel() {
        pending = null
    }

    fun confirm(): Boolean {
        val accepted = pending ?: return false
        pending = null // Clear first so a repeated frame/touch cannot purchase twice.
        accepted.onConfirmed()
        return true
    }
}
