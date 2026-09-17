package com.example.brick_breaker_ball

import com.badlogic.gdx.Gdx
import com.badlogic.gdx.Preferences

/** Developer tools are available only in Debug and QA builds and never change saved campaign progress. */
class DevelopmentAccess(
    private val prefs: Preferences = Gdx.app.getPreferences(PREFERENCES_NAME),
    private val available: Boolean = DEVELOPER_ACCESS && BuildConfig.DEVELOPER_ACCESS_ALLOWED,
) {
    val enabled: Boolean
        get() = available && prefs.getBoolean(ENABLED_KEY, false)

    fun toggle(): Boolean {
        if (!available) return false
        val next = !enabled
        prefs.putBoolean(ENABLED_KEY, next).flush()
        return next
    }

    fun canSelectLevel(levelId: Int, unlockedLevel: Int): Boolean =
        levelId in 1..LevelRepository.TOTAL_LEVELS && (enabled || levelId <= unlockedLevel)

    fun canSelectWorld(world: Int, unlockedLevel: Int): Boolean =
        world in 1..LevelRepository.worlds.size &&
            canSelectLevel(LevelRepository.firstLevel(world), unlockedLevel)

    fun canUseCosmetic(owned: Boolean): Boolean = enabled || owned

    fun canMakeTestPurchase(productAvailable: Boolean, busy: Boolean): Boolean =
        !busy && (enabled || productAvailable)

    companion object {
        // Set to true to include the Developer Access button in Debug and QA; false removes it.
        const val DEVELOPER_ACCESS = false
        private const val PREFERENCES_NAME = "brickbreakerball-development-access-v1"
        private const val ENABLED_KEY = "enabled"
        const val TEST_PRICE_LABEL = "DEV"
    }
}
