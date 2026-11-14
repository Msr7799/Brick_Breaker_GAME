/*
 * المسار: app/src/main/java/com/example/brick_breaker_ball/app/PlatformActions.kt
 * الوظيفة: واجهة إجراءات Android التي تطلبها اللعبة.
 * الحسابات: لا توجد معادلات لعب.
 */

package com.example.brick_breaker_ball.app

import com.example.brick_breaker_ball.gameplay.GameplayMode

/** Status and receipt verified by the backend using Bahrain's calendar, not the device clock. */
data class ServerDailyAward(val id: String, val coins: Int, val gems: Int, val streak: Int, val calendarDay: Long)
data class ServerDailyStatus(
    val serverDay: Long,
    val dayToClaim: Int,
    val claimedToday: Boolean,
    val completedDays: Int,
    val lastReward: ServerDailyAward?
)

/** Platform-owned actions that cannot be implemented inside the shared LibGDX layer. */
interface PlatformActions {
    fun shareGame()
    fun openGameplayVideo()
    fun setGameplayOrientation(mode: GameplayMode) = Unit
    val appUpdateCheckComplete: Boolean get() = true
    val availableAppUpdateVersionCode: Int? get() = null
    fun startAppUpdate() = Unit
    val cloudProgressEnabled: Boolean get() = false
    fun connectPlayerAccount() = Unit
    fun readCloudProgress(accountId: String, callback: (CloudProgressResult) -> Unit) =
        callback(CloudProgressResult(error = "Cloud save is unavailable"))
    fun writeCloudProgress(accountId: String, revision: Long, saveId: String, snapshot: String, callback: (CloudProgressResult) -> Unit) =
        callback(CloudProgressResult(error = "Cloud save is unavailable"))
    val serverDailyRewardsEnabled: Boolean get() = false
    val dailyRewardAccountId: String? get() = null
    fun fetchDailyRewardStatus(callback: (ServerDailyStatus?, String?) -> Unit) =
        callback(null, "Daily rewards server is not configured")
    fun claimDailyReward(callback: (ServerDailyStatus?, String?) -> Unit) =
        callback(null, "Daily rewards server is not configured")

    companion object {
        val UNAVAILABLE: PlatformActions = object : PlatformActions {
            override fun shareGame() = Unit
            override fun openGameplayVideo() = Unit
        }
    }
}
