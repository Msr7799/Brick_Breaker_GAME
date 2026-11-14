/*
 * المسار: app/src/main/java/com/example/brick_breaker_ball/economy/EnglishPlayPrice.kt
 * الوظيفة: عرض مبلغ Google Play برمز العملة الدولي وأرقام إنجليزية في كل لغات الجهاز.
 * الحسابات: تحويل الوحدات المصغرة (المليون) إلى المبلغ مع دقة العملة الرسمية.
 */

package com.example.brick_breaker_ball.economy

import java.math.BigDecimal
import java.text.NumberFormat
import java.util.Currency
import java.util.Locale

/** المبلغ ورمز العملة يأتيان من العرض الفعلي في Google Play، لا من سعر ثابت في اللعبة. */
fun formatEnglishPlayPrice(currencyCode: String, amountMicros: Long): String {
    val currency = Currency.getInstance(currencyCode)
    val fractionDigits = currency.defaultFractionDigits.coerceAtLeast(0)
    val number = NumberFormat.getNumberInstance(Locale.US).apply {
        minimumFractionDigits = fractionDigits
        maximumFractionDigits = fractionDigits
    }
    return "${currency.currencyCode} ${number.format(BigDecimal.valueOf(amountMicros, 6))}"
}
