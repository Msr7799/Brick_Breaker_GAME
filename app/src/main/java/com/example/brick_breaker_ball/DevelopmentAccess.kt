package com.example.brick_breaker_ball

/**
 * Development-only campaign navigation policy.
 *
 * This intentionally bypasses selection locks without changing saved completion,
 * scores, stars, or the canonical unlocked-level value.
 */
object DevelopmentAccess {
    const val ALL_CAMPAIGN_LEVELS_SELECTABLE = true

    fun canSelectLevel(levelId: Int, unlockedLevel: Int): Boolean =
        ALL_CAMPAIGN_LEVELS_SELECTABLE || levelId <= unlockedLevel

    fun canSelectWorld(world: Int, unlockedLevel: Int): Boolean =
        canSelectLevel(LevelRepository.firstLevel(world), unlockedLevel)
}
