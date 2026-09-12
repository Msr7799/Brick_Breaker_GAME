package com.example.brick_breaker_ball

import com.badlogic.gdx.Gdx
import com.badlogic.gdx.Preferences
import kotlin.math.ceil

sealed interface CosmeticUnlock {
    data class Ball(val ball: BallSpriteDefinition) : CosmeticUnlock
    data class Paddle(val style: PaddleStyleSet) : CosmeticUnlock
}

/**
 * Version 5 stores ownership per individual ball. The old theme/group ownership model is no
 * longer used; completed campaign stages safely reconstruct which balls should be available.
 */
class CosmeticOwnershipStore(private val prefs: Preferences = Gdx.app.getPreferences("brickbreakerball-cosmetic-ownership-v5")) {
    fun ownsBall(ballId: String): Boolean = prefs.getBoolean("ball:$ballId", false)
    fun ownsPaddleStyle(id: String): Boolean = prefs.getBoolean("paddle:$id", false)

    fun unlockBall(ballId: String): Boolean {
        if (ownsBall(ballId)) return false
        prefs.putBoolean("ball:$ballId", true).flush()
        return true
    }

    fun unlockPaddleStyle(id: String): Boolean {
        if (ownsPaddleStyle(id)) return false
        prefs.putBoolean("paddle:$id", true).flush()
        return true
    }
}

/** Campaign milestones unlock one ball at a time plus complete paddle style trios. */
class CosmeticProgressionService(
    balls: List<BallSpriteDefinition>,
    paddles: List<PaddleStyleSet>,
    private val ownership: CosmeticOwnershipStore
) {
    /** Exact gameplay order: starter first, then one new ball for each early cleared stage. */
    val balls: List<BallSpriteDefinition> = starterFirst(
        balls.sortedBy(BallSpriteDefinition::index),
        balls.firstOrNull { it.groupName == CosmeticDefaults.BALL_GROUP && it.spriteName == CosmeticDefaults.BALL_SPRITE }
    )

    val paddleStyles: List<PaddleStyleSet> = starterFirst(
        paddles,
        paddles.firstOrNull { it.normal.id == CosmeticDefaults.PADDLE_ID }
    )

    private val starterBall: BallSpriteDefinition? = this.balls.firstOrNull()

    /**
     * Ball #1 is always available. Completing a new campaign stage unlocks the next ball
     * in the ordered catalog until the entire ball collection is earned.
     */
    private val ballUnlockStages: Map<String, Int> = this.balls.drop(1).mapIndexed { index, ball ->
        ball.id to (index + 1).coerceIn(1, LevelRepository.TOTAL_LEVELS)
    }.toMap()

    /** The first earned paddle style intentionally shares Stage 1 for a combined reveal. */
    private val paddleUnlockStages: Map<String, Int> = buildMap {
        val earned = paddleStyles.drop(1)
        earned.forEachIndexed { index, style ->
            val stage = if (index == 0) {
                1
            } else {
                1 + ceil(index * (LevelRepository.TOTAL_LEVELS - 1).toDouble() / (earned.size - 1).coerceAtLeast(1)).toInt()
            }
            put(style.id, stage.coerceIn(1, LevelRepository.TOTAL_LEVELS))
        }
    }

    fun reconcile(progress: ProgressStore) {
        starterBall?.let { ownership.unlockBall(it.id) }
        starterPaddle()?.let { ownership.unlockPaddleStyle(it.id) }

        balls.drop(1).forEach { ball ->
            val stage = ballUnlockStages[ball.id] ?: return@forEach
            if (progress.stars(stage) > 0) ownership.unlockBall(ball.id)
        }
        paddleStyles.drop(1).forEach { style ->
            val stage = paddleUnlockStages[style.id] ?: return@forEach
            if (progress.stars(stage) > 0) ownership.unlockPaddleStyle(style.id)
        }
        ensureEquippedOwned(progress)
    }

    /** Call after ProgressStore.complete(); every returned entry is already durably owned. */
    fun onCampaignResult(levelId: Int, previousStars: Int, progress: ProgressStore): List<CosmeticUnlock> {
        if (previousStars > 0 || progress.stars(levelId) <= 0) return emptyList()
        val unlocked = mutableListOf<CosmeticUnlock>()

        balls.drop(1).firstOrNull { ballUnlockStages[it.id] == levelId }?.let { ball ->
            if (ownership.unlockBall(ball.id)) unlocked += CosmeticUnlock.Ball(ball)
        }
        paddleStyles.drop(1).firstOrNull { paddleUnlockStages[it.id] == levelId }?.let { style ->
            if (ownership.unlockPaddleStyle(style.id)) unlocked += CosmeticUnlock.Paddle(style)
        }
        ensureEquippedOwned(progress)
        return unlocked
    }

    fun ownsBall(definition: BallSpriteDefinition): Boolean = ownership.ownsBall(definition.id)
    fun ownsPaddle(style: PaddleStyleSet): Boolean = ownership.ownsPaddleStyle(style.id)
    fun styleForPaddle(paddleId: String): PaddleStyleSet? = PaddleStyleCatalog.styleForPaddle(paddleStyles, paddleId)

    fun ownedBallCount(): Int = balls.count(::ownsBall)
    fun totalBallCount(): Int = balls.size
    fun ownedPaddleCount(): Int = paddleStyles.count(::ownsPaddle)
    fun totalPaddleCount(): Int = paddleStyles.size

    fun firstOwnedBall(): BallSpriteDefinition? = balls.firstOrNull(::ownsBall)
    fun firstOwnedPaddle(): PaddleStyleSet? = paddleStyles.firstOrNull(::ownsPaddle)

    fun ballRequirement(definition: BallSpriteDefinition): String = when (definition) {
        starterBall -> "STARTER BALL"
        else -> ballUnlockStages[definition.id]?.let { "COMPLETE STAGE $it" } ?: "LOCKED"
    }

    fun ballUnlockStage(definition: BallSpriteDefinition): Int? = ballUnlockStages[definition.id]
    fun paddleUnlockStage(style: PaddleStyleSet): Int? = paddleUnlockStages[style.id]

    fun paddleRequirement(style: PaddleStyleSet): String = when (style) {
        starterPaddle() -> "STARTER"
        else -> paddleUnlockStages[style.id]?.let { "COMPLETE STAGE $it" } ?: "COLLECTION REWARD"
    }

    fun nextLockedBall(): Pair<BallSpriteDefinition, Int>? = balls.drop(1).asSequence()
        .filterNot(::ownsBall)
        .mapNotNull { ball -> ballUnlockStages[ball.id]?.let { ball to it } }
        .minByOrNull { it.second }

    fun nextLockedPaddle(): Pair<PaddleStyleSet, Int>? = paddleStyles.drop(1).asSequence()
        .filterNot(::ownsPaddle)
        .mapNotNull { style -> paddleUnlockStages[style.id]?.let { style to it } }
        .minByOrNull { it.second }

    fun equipBall(settings: GameSettings, definition: BallSpriteDefinition, developmentAccess: Boolean = false) {
        require(developmentAccess || ownsBall(definition)) { "Cannot equip a locked ball" }
        settings.selectedBallGroupName = definition.groupName
        settings.selectedBallSpriteName = definition.spriteName
    }

    fun equipStyle(settings: GameSettings, style: PaddleStyleSet, developmentAccess: Boolean = false) {
        require(developmentAccess || ownsPaddle(style)) { "Cannot equip a locked paddle style" }
        settings.selectedPaddleId = style.normal.id
        settings.selectedWeaponPaddleId = style.weapon.id
        settings.selectedStickyPaddleId = style.sticky.id
    }

    private fun starterPaddle(): PaddleStyleSet? = paddleStyles.firstOrNull()

    private fun ensureEquippedOwned(progress: ProgressStore) {
        val selected = balls.firstOrNull {
            it.groupName == progress.settings.selectedBallGroupName && it.spriteName == progress.settings.selectedBallSpriteName
        }
        if (selected == null || !ownsBall(selected)) firstOwnedBall()?.let { equipBall(progress.settings, it) }

        val selectedStyle = styleForPaddle(progress.settings.selectedPaddleId)
        if (selectedStyle == null || !ownsPaddle(selectedStyle)) {
            firstOwnedPaddle()?.let { equipStyle(progress.settings, it) }
        } else if (progress.settings.selectedWeaponPaddleId != selectedStyle.weapon.id ||
            progress.settings.selectedStickyPaddleId != selectedStyle.sticky.id
        ) {
            equipStyle(progress.settings, selectedStyle)
        }
        progress.saveSettings()
    }

    private fun <T> starterFirst(items: List<T>, starter: T?): List<T> = when {
        starter == null || items.firstOrNull() == starter -> items
        else -> listOf(starter) + items.filterNot { it == starter }
    }
}
