/*
 * المسار: app/src/main/java/com/example/brick_breaker_ball/app/DevelopmentAccess.kt
 * الوظيفة: ضبط صلاحيات المطور في نسخ التطوير وQA.
 * الحسابات: شروط السماح مرتبطة بنوع النسخة والإعداد المحفوظ.
 */

package com.example.brick_breaker_ball.app

import com.badlogic.gdx.Gdx
import com.badlogic.gdx.Preferences
import com.example.brick_breaker_ball.BuildConfig
import com.example.brick_breaker_ball.gameplay.CampaignCatalog
import com.example.brick_breaker_ball.gameplay.GameplayMode

/** صلاحيات المطور متاحة من زر القائمة في QA فقط. */
class DevelopmentAccess(
    private val prefs: Preferences = Gdx.app.getPreferences(PREFERENCES_NAME),
    private val available: Boolean = DEVELOPER_ACCESS && BuildConfig.DEVELOPER_ACCESS_ALLOWED
) {
    val enabled: Boolean
        get() = available && prefs.getBoolean(ENABLED_KEY, false)

    fun toggle(): Boolean {
        if (!available) return false
        val next = !enabled
        prefs.putBoolean(ENABLED_KEY, next).flush()
        return next
    }

    fun canSelectLevel(
        levelId: Int,
        unlockedLevel: Int,
        mode: GameplayMode = GameplayMode.VOLLEY
    ): Boolean = levelId in 1..CampaignCatalog.totalLevels(mode) && (enabled || levelId <= unlockedLevel)

    fun canSelectWorld(
        world: Int,
        unlockedLevel: Int,
        mode: GameplayMode = GameplayMode.VOLLEY
    ): Boolean = world in 1..CampaignCatalog.worlds(mode).size &&
        canSelectLevel(CampaignCatalog.firstLevel(mode, world), unlockedLevel, mode)

    fun canUseCosmetic(owned: Boolean): Boolean = enabled || owned

    fun canMakeTestPurchase(productAvailable: Boolean, busy: Boolean): Boolean = !busy && (enabled || productAvailable)

    companion object {
        val DEVELOPER_ACCESS: Boolean
            get() = BuildConfig.BUILD_TYPE.equals("qa", ignoreCase = true) &&
                BuildConfig.DEVELOPER_ACCESS_ALLOWED

        private const val PREFERENCES_NAME = "brickbreakerball-development-access-v1"
        private const val ENABLED_KEY = "enabled"
        const val TEST_PRICE_LABEL = "DEV"
    }
}
