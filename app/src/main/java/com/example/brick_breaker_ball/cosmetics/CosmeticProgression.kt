/*
 * المسار: app/src/main/java/com/example/brick_breaker_ball/cosmetics/CosmeticProgression.kt
 * الوظيفة: ملكية الكرات وتقدم فتحها وتجهيزها.
 * الحسابات: اشتقاق الفتح من النجوم ومرحلة الحملة.
 */

package com.example.brick_breaker_ball.cosmetics

import com.badlogic.gdx.Gdx
import com.badlogic.gdx.Preferences
import com.example.brick_breaker_ball.campaign.GameSettings
import com.example.brick_breaker_ball.campaign.ProgressStore
import com.example.brick_breaker_ball.gameplay.Ball

sealed interface CosmeticUnlock {
    data class Ball(val ball: BallSpriteDefinition) : CosmeticUnlock
}

/**
 * Version 5 stores ownership per individual ball. The old theme/group ownership model is no
 * longer used; completed campaign stages safely reconstruct which balls should be available.
 */
class CosmeticOwnershipStore(private val prefs: Preferences = Gdx.app.getPreferences("brickbreakerball-cosmetic-ownership-v5")) {
    fun ownsBall(ballId: String): Boolean = prefs.getBoolean("ball:$ballId", false)

    fun unlockBall(ballId: String): Boolean {
        if (ownsBall(ballId)) return false
        prefs.putBoolean("ball:$ballId", true).flush()
        return true
    }
}

/** مراحل Volley تفتح الكرات فقط؛ مفاتيح ملكية البادل القديمة تبقى محفوظة في Preferences دون استخدامها. */
class CosmeticProgressionService(
    balls: List<BallSpriteDefinition>,
    private val ownership: CosmeticOwnershipStore
) {
    /** Exact gameplay order: starter first, then the remaining balls are spread across the full campaign. */
    val balls: List<BallSpriteDefinition> = starterFirst(
        balls.sortedBy(BallSpriteDefinition::index),
        balls.firstOrNull { it.groupName == CosmeticDefaults.BALL_GROUP && it.spriteName == CosmeticDefaults.BALL_SPRITE }
    )

    private val starterBall: BallSpriteDefinition? = this.balls.firstOrNull()

    /** Paid balls have no campaign milestone, including every Epic frame. */
    private val ballUnlockStages: Map<String, Int> = balls.mapNotNull { ball ->
        BallCollectionCatalog.offer(ball).stage?.let { ball.id to it }
    }.toMap()

    fun reconcile(progress: ProgressStore, developmentAccess: Boolean = false) {
        starterBall?.let { ownership.unlockBall(it.id) }
        balls.forEach { ball ->
            val stage = ballUnlockStages[ball.id] ?: return@forEach
            if (progress.stars(stage) > 0 || progress.legacyClassicStars(stage) > 0) ownership.unlockBall(ball.id)
        }
        if (!developmentAccess) ensureEquippedOwned(progress)
    }

    /** Call after ProgressStore.complete(); every returned entry is already durably owned. */
    fun onCampaignResult(
        levelId: Int,
        previousStars: Int,
        progress: ProgressStore,
        developmentAccess: Boolean = false
    ): List<CosmeticUnlock> {
        if (previousStars > 0 || progress.stars(levelId) <= 0) return emptyList()
        val unlocked = mutableListOf<CosmeticUnlock>()

        balls.filter { ballUnlockStages[it.id] == levelId }.forEach { ball ->
            if (ownership.unlockBall(ball.id)) unlocked += CosmeticUnlock.Ball(ball)
        }
        if (!developmentAccess) ensureEquippedOwned(progress)
        return unlocked
    }

    fun ownsBall(definition: BallSpriteDefinition): Boolean = ownership.ownsBall(definition.id)
    fun ownedBallCount(): Int = balls.count(::ownsBall)
    fun totalBallCount(): Int = balls.size

    fun firstOwnedBall(): BallSpriteDefinition? = balls.firstOrNull(::ownsBall)

    fun ballRequirement(definition: BallSpriteDefinition): String {
        if (definition == starterBall) return "STARTER BALL"
        val offer = BallCollectionCatalog.offer(definition)
        return when {
            offer.stage != null -> "COMPLETE STAGE ${offer.stage}"
            offer.coins != null -> "${offer.coins} COINS"
            offer.rarity == BallRarity.EPIC -> "1000 GEMS OR PLAY STORE"
            offer.gems != null -> "${offer.gems} GEMS"
            else -> "LOCKED"
        }
    }

    fun ballUnlockStage(definition: BallSpriteDefinition): Int? = ballUnlockStages[definition.id]
    fun nextLockedBall(): Pair<BallSpriteDefinition, Int>? = balls.drop(1).asSequence()
        .filterNot(::ownsBall)
        .mapNotNull { ball -> ballUnlockStages[ball.id]?.let { ball to it } }
        .minByOrNull { it.second }

    fun equipBall(settings: GameSettings, definition: BallSpriteDefinition, developmentAccess: Boolean = false) {
        require(developmentAccess || ownsBall(definition)) { "Cannot equip a locked ball" }
        settings.selectedBallGroupName = definition.groupName
        settings.selectedBallSpriteName = definition.spriteName
    }

    private fun ensureEquippedOwned(progress: ProgressStore) {
        val selected = balls.firstOrNull {
            it.groupName == progress.settings.selectedBallGroupName && it.spriteName == progress.settings.selectedBallSpriteName
        }
        if (selected == null || !ownsBall(selected)) firstOwnedBall()?.let { equipBall(progress.settings, it) }

        progress.saveSettings()
    }

    private fun <T> starterFirst(items: List<T>, starter: T?): List<T> = when {
        starter == null || items.firstOrNull() == starter -> items
        else -> listOf(starter) + items.filterNot { it == starter }
    }
}
