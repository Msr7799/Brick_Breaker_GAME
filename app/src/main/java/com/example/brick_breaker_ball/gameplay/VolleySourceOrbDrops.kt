package com.example.brick_breaker_ball.gameplay

import java.util.Random

/** RPG item-drop rarity gates and OrbStats' uniform family picker for a given rarity. */
object VolleySourceOrbDrops {
    /** Shared by the earned-ad picker and the offer's complete outcome disclosure. */
    fun rewardedStandardPool(): List<VolleyOrbDefinition> = eligibleAtRank(0)

    private fun eligibleAtRank(rank: Int): List<VolleyOrbDefinition> = VolleyOrbCatalog.all.filter {
        it.rank == rank && !it.legacy && VolleyOrbCatalog.isPlayableFamily(it.family)
    }

    fun minimumDropStage(rank: Int): Int = when (rank) {
        0 -> 1
        1 -> 1
        2 -> 31
        3 -> 61
        // RPG reserves legendary and ancient random drops for endless rewards.
        else -> Int.MAX_VALUE
    }

    fun pick(levelId: Int, @Suppress("UNUSED_PARAMETER") sourceId: Int,
             @Suppress("UNUSED_PARAMETER") turn: Int, requestedRank: Int,
             random: Random = Random(), extremeMode: Boolean = false): VolleyOrbDefinition? {
        val maximumRank = requestedRank.coerceIn(0, 5)
        for (rank in maximumRank downTo 0) {
            if (!extremeMode && levelId < minimumDropStage(rank)) continue
            // Item level requirements do not restrict this uniform family picker.
            val eligible = if (rank == 0) rewardedStandardPool() else eligibleAtRank(rank)
            if (eligible.isNotEmpty()) {
                return eligible[random.nextInt(eligible.size)]
            }
        }
        return null
    }
}
