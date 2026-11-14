package com.example.brick_breaker_ball.ui

import com.example.brick_breaker_ball.BrickBreakerGame
import com.example.brick_breaker_ball.campaign.VolleyDifficulty
import com.example.brick_breaker_ball.campaign.VolleyStageRepository
import com.example.brick_breaker_ball.cosmetics.BallAbilityProfile
import com.example.brick_breaker_ball.gameplay.VolleyGeneratorFeature
import com.example.brick_breaker_ball.gameplay.VolleyOrbEffects
import java.util.Random

/** Collection and detail use the same stage + active-orb count, without advancing a run. */
internal class BallPowerPreview(game: BrickBreakerGame, returnToPausedGame: Boolean) {
    private val paused = if (returnToPausedGame) game.volleyPausedSession.restore() else null
    private val level = paused?.first?.id ?: game.progress.unlockedLevel
    private val stage = VolleyGeneratorFeature.stageForLevel(level.coerceIn(1, VolleyStageRepository.TOTAL_LEVELS))
    private val orbBalls = VolleyOrbEffects(game.progress.equippedOrbs, Random(0), stage.levelId,
        game.progress.selectedDifficulty == VolleyDifficulty.EXTREME, combatBoost = 1.5f).startingBalls()

    fun balls(profile: BallAbilityProfile): Int =
        (paused?.second?.ballCount ?: (stage.startingBalls + profile.startingBalls + orbBalls)).coerceIn(1, 160)
    fun damage(profile: BallAbilityProfile): Int = profile.hitDamageForVolley(balls(profile))
}
