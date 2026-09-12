/*
 * ملاحظات صيانة الملف:
 * المسار: app/src/main/java/com/example/brick_breaker_ball/DevelopmentAccess.kt
 * المؤلف: mohamed alromaihi
 * الدوال الموجودة: `canSelectLevel`، `canSelectWorld`
 */

package com.example.brick_breaker_ball

import com.badlogic.gdx.Gdx
import com.badlogic.gdx.Preferences

/**
 * Development-only campaign navigation policy.
 *
 * This intentionally bypasses selection locks without changing saved completion,
 * scores, stars, or the canonical unlocked-level value.
 */
class DevelopmentAccess(
    private val prefs: Preferences = Gdx.app.getPreferences(PREFERENCES_NAME)
) {
    var enabled: Boolean
        get() = prefs.getBoolean(ENABLED_KEY, false)
        private set(value) {
            prefs.putBoolean(ENABLED_KEY, value).flush()
        }

    fun toggle(): Boolean {
        enabled = !enabled
        return enabled
    }

    /** ملاحظة صيانة: الدالة `canSelectLevel` تتحقق من الشرط المطلوب وتعيد نتيجة يمكن لبقية النظام الاعتماد عليها؛ راجع استدعاءاتها واختباراتها قبل تعديلها. */
    fun canSelectLevel(levelId: Int, unlockedLevel: Int): Boolean = levelId in 1..LevelRepository.TOTAL_LEVELS && (enabled || levelId <= unlockedLevel)

    /** ملاحظة صيانة: الدالة `canSelectWorld` تتحقق من الشرط المطلوب وتعيد نتيجة يمكن لبقية النظام الاعتماد عليها؛ راجع استدعاءاتها واختباراتها قبل تعديلها. */
    fun canSelectWorld(world: Int, unlockedLevel: Int): Boolean = world in 1..LevelRepository.worlds.size && canSelectLevel(LevelRepository.firstLevel(world), unlockedLevel)

    fun canUseCosmetic(owned: Boolean): Boolean = enabled || owned

    fun canMakeTestPurchase(productAvailable: Boolean, busy: Boolean): Boolean = !busy && (enabled || productAvailable)

    companion object {
        private const val PREFERENCES_NAME = "brickbreakerball-development-access-v1"
        private const val ENABLED_KEY = "enabled"
        const val TEST_PRICE_LABEL = "DEV"
    }
}
