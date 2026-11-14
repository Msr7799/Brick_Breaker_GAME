/*
 * المسار: app/src/main/java/com/example/brick_breaker_ball/gameplay/GameplayMode.kt
 * الوظيفة: تعريف وضع اللعب المعتمد.
 * الحسابات: لا توجد معادلات.
 */

package com.example.brick_breaker_ball.gameplay

import com.example.brick_breaker_ball.campaign.VolleyStageRepository
import com.example.brick_breaker_ball.campaign.WorldDefinition

/** The only playable campaign. Keep the enum for persisted mode compatibility. */
enum class GameplayMode {
    VOLLEY;

    val displayName: String
        get() = "VOLLEY"
}

object CampaignCatalog {
    fun worlds(mode: GameplayMode): List<WorldDefinition> = VolleyStageRepository.worlds

    fun levelsPerWorld(mode: GameplayMode): Int = VolleyStageRepository.LEVELS_PER_WORLD

    fun totalLevels(mode: GameplayMode): Int = VolleyStageRepository.TOTAL_LEVELS

    fun firstLevel(mode: GameplayMode, world: Int): Int = VolleyStageRepository.firstLevel(world)

    fun lastLevel(mode: GameplayMode, world: Int): Int = VolleyStageRepository.lastLevel(world)

    fun worldLevelCount(mode: GameplayMode, world: Int): Int = VolleyStageRepository.worldLevelCount(world)

    fun worldForLevel(mode: GameplayMode, level: Int): Int = VolleyStageRepository.worldForLevel(level)
}
