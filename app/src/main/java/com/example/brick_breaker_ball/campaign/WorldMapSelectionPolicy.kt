/*
 * المسار: app/src/main/java/com/example/brick_breaker_ball/campaign/WorldMapSelectionPolicy.kt
 * الوظيفة: تحديد العالم والمرحلة القابلين للفتح.
 * الحسابات: مقارنة التقدم بحدود فتح العالم.
 */

package com.example.brick_breaker_ball.campaign

import com.example.brick_breaker_ball.app.DevelopmentAccess

/**
 * World-map preview policy.
 *
 * A player may preview any authored world card/background, including a locked world. Actual stage
 * launch remains protected by DevelopmentAccess/ProgressStore. Keeping preview selection separate
 * from campaign unlock state prevents locked cards from feeling unresponsive.
 */
object WorldMapSelectionPolicy {
    fun previewWorld(requestedWorld: Int, totalWorlds: Int): Int = requestedWorld.coerceIn(1, totalWorlds.coerceAtLeast(1))
}
