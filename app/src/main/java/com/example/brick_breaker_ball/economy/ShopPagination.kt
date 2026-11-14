/*
 * المسار: app/src/main/java/com/example/brick_breaker_ball/economy/ShopPagination.kt
 * الوظيفة: تقسيم عروض المتجر إلى صفحات.
 * الحسابات: حساب عدد الصفحات وحدود كل صفحة.
 */

package com.example.brick_breaker_ball.economy

/** Four authored shelf positions per page. Empty tabs still show page 1 of 1. */
object ShopPagination {
    fun pageCount(itemCount: Int): Int = maxOf(1, (itemCount.coerceAtLeast(0) + 3) / 4)

    fun visibleRange(itemCount: Int, requestedPage: Int): IntRange {
        val page = requestedPage.coerceIn(0, pageCount(itemCount) - 1)
        val first = page * 4
        val last = minOf(first + 3, itemCount - 1)
        return if (first > last) IntRange.EMPTY else first..last
    }
}
