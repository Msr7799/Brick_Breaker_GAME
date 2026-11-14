/*
 * المسار: economy/OrbShopPricing.kt
 * الوظيفة: تسعير ترقيات Orbs الجديدة بعملات اللعبة. الأسعار قرارات توازن محلية.
 */
package com.example.brick_breaker_ball.economy

object OrbShopPricing {
    fun coins(targetRank: Int): Int = 300 + targetRank.coerceIn(0, 50) * 180
    fun gems(targetRank: Int): Int = 5 + targetRank.coerceIn(0, 50) * 3
}
