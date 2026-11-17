package com.example.brick_breaker_ball.ui

import com.example.brick_breaker_ball.BrickBreakerGame
import com.example.brick_breaker_ball.campaign.VolleyDifficulty
import com.example.brick_breaker_ball.campaign.VolleyStageRepository
import com.example.brick_breaker_ball.campaign.VolleySourceHealth
import com.example.brick_breaker_ball.campaign.VolleyPlayerProgress
import com.example.brick_breaker_ball.gameplay.VolleyReferenceCombat
import com.example.brick_breaker_ball.cosmetics.BallAbilityCatalog
import com.example.brick_breaker_ball.cosmetics.VolleyBallSelection
import com.example.brick_breaker_ball.cosmetics.BallAbilityProfile
import com.example.brick_breaker_ball.gameplay.VolleyGeneratorFeature
import com.example.brick_breaker_ball.gameplay.VolleyOrbEffects
import com.example.brick_breaker_ball.gameplay.VolleyModifier
import java.util.Random

/** Rules-aware, read-only preview; a paused legacy run retains its original combat rules. */
internal class BallPowerPreview(game: BrickBreakerGame, returnToPausedGame: Boolean) {
    private val paused = if (returnToPausedGame) game.volleyPausedSession.restore() else null
    private val level = paused?.first?.id ?: game.progress.unlockedLevel
    private val stage = VolleyGeneratorFeature.stageForLevel(level.coerceIn(1, VolleyStageRepository.TOTAL_LEVELS))
    private val equippedBonus = BallAbilityCatalog.profileForSprite(
        VolleyBallSelection.selectionSprite(game.progress.settings)).startingBalls
    val usesReferenceCombat: Boolean = paused?.second?.let { it.sourceCombatRulesVersion == 1 && stage.sourceScene != null }
        ?: (stage.sourceScene != null)
    val playerLevel: Int = if (usesReferenceCombat)
        paused?.second?.sourceCombatPlayerLevel ?: game.progress.combatPlayerLevel
        else game.progress.combatPlayerLevel
    val playerExperience: Long = game.progress.combatPlayerExperience
    val accountPlayerLevel: Int = game.progress.combatPlayerLevel
    val nextLevelExperience: Long = VolleyPlayerProgress.requiredExperience(game.progress.combatPlayerLevel)
    private val equippedOrbs = game.progress.equippedOrbs
    val stageNumber: Int get() = stage.levelId
    val campaignAttack: Int get() = if (usesReferenceCombat || stage.sourceScene == null) 1
        else VolleySourceHealth.ballAttack(stage.levelId, 0)
    val nextAttackStage: Int? get() = ((stage.levelId - 1) / 12 * 12 + 13)
        .takeIf { !usesReferenceCombat && stage.sourceScene != null && it <= VolleyStageRepository.TOTAL_LEVELS }
    private val orbBalls = VolleyOrbEffects(game.progress.equippedOrbs, Random(0), stage.levelId,
        game.progress.selectedDifficulty == VolleyDifficulty.EXTREME, combatBoost = 1.5f).startingBalls()

    fun balls(profile: BallAbilityProfile): Int {
        val snapshot = paused?.second
        if (usesReferenceCombat) {
            val total = VolleyReferenceCombat.startingBalls(playerLevel, equippedOrbs)
            val orbBonus = total - VolleyReferenceCombat.startingBalls(playerLevel, emptyList())
            return if (snapshot != null)
                (snapshot.ballCount.toLong() + orbBonus - (snapshot.orbStartingBonus ?: orbBonus))
                    .coerceIn(1L, Int.MAX_VALUE.toLong()).toInt()
            else total
        }
        return (if (snapshot != null) snapshot.ballCount -
            (snapshot.ballStartingBonus ?: equippedBonus) + profile.startingBalls +
            orbBalls - (snapshot.orbStartingBonus ?: orbBalls)
        else stage.startingBalls + profile.startingBalls + orbBalls).coerceIn(1, 160)
    }

    fun damage(profile: BallAbilityProfile): Int {
        val megaBonus = if (paused?.second?.activeModifiers?.contains(VolleyModifier.MEGA) == true) 1 else 0
        if (usesReferenceCombat) return (VolleyReferenceCombat.baseDamage(playerLevel, equippedOrbs).toLong() +
            (paused?.second?.bonusDamageFlat ?: 0) + megaBonus).coerceIn(1L, Int.MAX_VALUE.toLong()).toInt()
        return (profile.hitDamageForVolley(balls(profile)) + (paused?.second?.bonusDamageFlat ?: 0) + megaBonus) * campaignAttack
    }
}
