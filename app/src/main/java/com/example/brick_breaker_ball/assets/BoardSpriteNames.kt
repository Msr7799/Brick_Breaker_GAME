/*
 * المسار: app/src/main/java/com/example/brick_breaker_ball/assets/BoardSpriteNames.kt
 * الوظيفة: أسماء مناطق اللوحات المعتمدة في العرض.
 * الحسابات: لا توجد معادلات.
 */

package com.example.brick_breaker_ball.assets

import java.util.Locale

/** Atlas keys use ASCII digits regardless of the device's display language. */
internal object BoardSpriteNames {
    fun bagSlot(index: Int): String = "bag_slot_%02d".format(Locale.ROOT, index)

    fun dailyRewardDay(day: Int, kind: String): String = "daily_reward_day_%02d_%s".format(Locale.ROOT, day, kind)
}
