/*
 * المسار: app/src/main/java/com/example/brick_breaker_ball/campaign/CampaignGemPolicy.kt
 * الوظيفة: قواعد منح الجواهر بعد اللعب.
 * الحسابات: حساب المكافأة وفق نتيجة المرحلة.
 */

package com.example.brick_breaker_ball.campaign

/** Deterministic first-clear gem payout. Bosses take precedence over fifth-stage milestones. */
internal fun campaignClearGems(levelId: Int, isBossStage: Boolean, firstClear: Boolean): Int = when {
    !firstClear || levelId <= 0 -> 0
    isBossStage -> 30
    levelId % 5 == 0 -> 3
    else -> 0
}
