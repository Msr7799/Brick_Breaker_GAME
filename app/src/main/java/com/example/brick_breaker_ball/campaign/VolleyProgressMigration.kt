/*
 * المسار: app/src/main/java/com/example/brick_breaker_ball/campaign/VolleyProgressMigration.kt
 * الوظيفة: نقل تقدم الإصدارات السابقة إلى تقدم Volley.
 * الحسابات: مطابقة المراحل والنجوم دون فقد بيانات اللاعب.
 */

package com.example.brick_breaker_ball.campaign

import com.badlogic.gdx.Preferences

/** Volley-only migration to the authored variable-size world layout. */
object VolleyProgressMigration {
    const val CURRENT_SCHEMA = 8
    private const val SCHEMA_KEY = "volley_campaign_schema"
    private const val LEGACY_TOTAL_LEVELS = 90
    private const val PREVIOUS_TOTAL_LEVELS = 120
    private const val PREVIOUS_WORLDS = 8
    private const val PREVIOUS_LEVELS_PER_WORLD = 15
    private const val EIGHT_STAGE_WORLDS = 10
    private const val EIGHT_STAGE_LEVELS_PER_WORLD = 8
    private const val EIGHT_STAGE_TOTAL_LEVELS = EIGHT_STAGE_WORLDS * EIGHT_STAGE_LEVELS_PER_WORLD
    private val PREVIOUS_AUTHORED_WORLD_COUNTS = listOf(7, 6, 5, 6, 8, 10, 1, 12, 10)
    private val PREVIOUS_RANKED_WORLD_COUNTS = listOf(7, 10, 6, 10, 10, 10, 8, 10, 10)
    private val EIGHTY_EIGHT_WORLD_COUNTS = listOf(10, 10, 10, 10, 10, 10, 8, 10, 10)

    /**
     * Keeps Volley progress isolated while folding the previous 8x15 layout into 10x8. Scores and
     * stars from a world's stages 8..15 merge into its new final stage, so completed late stages
     * remain credited rather than leaking into the next world's IDs.
     */
    fun migrate(prefs: Preferences): Boolean {
        val schema = prefs.getInteger(SCHEMA_KEY, 1)
        if (schema >= CURRENT_SCHEMA) return false

        if (schema < 2) {
            if (prefs.contains("volley_unlocked")) {
                prefs.putInteger(
                    "volley_unlocked",
                    prefs.getInteger("volley_unlocked", 1).coerceIn(1, PREVIOUS_TOTAL_LEVELS)
                )
            }
            if (prefs.contains("volley_last_played_level")) {
                prefs.putInteger(
                    "volley_last_played_level",
                    prefs.getInteger("volley_last_played_level", 1).coerceIn(1, PREVIOUS_TOTAL_LEVELS)
                )
            }
            if (prefs.contains("volley_last_world_map_selection")) {
                val legacyWorld = prefs.getInteger("volley_last_world_map_selection", 1).coerceIn(1, 13)
                prefs.putInteger(
                    "volley_last_world_map_selection",
                    (1 + (legacyWorld - 1) * PREVIOUS_WORLDS / 13).coerceIn(1, PREVIOUS_WORLDS)
                )
            }
            prefs.putInteger("volley_migrated_from_total_levels", LEGACY_TOTAL_LEVELS)
        }

        if (schema < 3) {
            val migratedStars = mutableMapOf<Int, Int>()
            val migratedScores = mutableMapOf<Int, Int>()
            for (oldWorld in 1..PREVIOUS_WORLDS) {
                for (oldStage in 1..PREVIOUS_LEVELS_PER_WORLD) {
                    val oldLevel = (oldWorld - 1) * PREVIOUS_LEVELS_PER_WORLD + oldStage
                    val newLevel = (oldWorld - 1) * EIGHT_STAGE_LEVELS_PER_WORLD +
                        oldStage.coerceAtMost(EIGHT_STAGE_LEVELS_PER_WORLD)
                    val starKey = "volley_stars_$oldLevel"
                    val scoreKey = "volley_score_$oldLevel"
                    if (prefs.contains(starKey)) {
                        migratedStars[newLevel] = maxOf(
                            migratedStars[newLevel] ?: 0,
                            prefs.getInteger(starKey, 0).coerceIn(0, 3)
                        )
                    }
                    if (prefs.contains(scoreKey)) {
                        migratedScores[newLevel] = maxOf(
                            migratedScores[newLevel] ?: 0,
                            prefs.getInteger(scoreKey, 0)
                        )
                    }
                }
            }
            migratedStars.forEach { (level, stars) -> prefs.putInteger("volley_stars_$level", stars) }
            migratedScores.forEach { (level, score) -> prefs.putInteger("volley_score_$level", score) }

            if (prefs.contains("volley_unlocked")) {
                prefs.putInteger(
                    "volley_unlocked",
                    remapPreviousLevel(prefs.getInteger("volley_unlocked", 1))
                )
            }
            if (prefs.contains("volley_last_played_level")) {
                prefs.putInteger(
                    "volley_last_played_level",
                    remapPreviousLevel(prefs.getInteger("volley_last_played_level", 1))
                )
            }
            if (prefs.contains("volley_last_world_map_selection")) {
                prefs.putInteger(
                    "volley_last_world_map_selection",
                    prefs.getInteger("volley_last_world_map_selection", 1)
                        .coerceIn(1, VolleyStageRepository.WORLDS_COUNT)
                )
            }
        }

        if (schema < 4) migrateEightStageCampaign(prefs)
        if (schema < 5) {
            migrateCampaignLayout(
                prefs = prefs,
                oldWorldCounts = PREVIOUS_AUTHORED_WORLD_COUNTS,
                newWorldCounts = PREVIOUS_RANKED_WORLD_COUNTS
            )
        }
        if (schema < 6) {
            migrateCampaignLayout(
                prefs = prefs,
                oldWorldCounts = PREVIOUS_RANKED_WORLD_COUNTS,
                newWorldCounts = EIGHTY_EIGHT_WORLD_COUNTS
            )
        }
        if (schema < 7) {
            migrateCampaignLayout(
                prefs = prefs,
                oldWorldCounts = EIGHTY_EIGHT_WORLD_COUNTS,
                // Schema 7 still had ten image stages per world. Schema 8 expands each image
                // milestone into an independent imported stage; skipping this intermediate layout
                // would discard stars for old worlds 6..9 during the second remap.
                newWorldCounts = List(VolleyStageRepository.WORLDS_COUNT) { 10 },
                shiftOldMoonBoss = true
            )
        }
        if (schema < 8) migratePairedStageImages(prefs)

        prefs.putInteger(SCHEMA_KEY, CURRENT_SCHEMA)
            .flush()
        return true
    }

    /** Places previously earned image milestones on the first new stage using that image. */
    private fun migratePairedStageImages(prefs: Preferences) {
        val oldCounts = List(9) { 10 }
        val newCounts = VolleyStageRepository.WORLD_STAGE_COUNTS
        val stars = mutableMapOf<Int, Int>()
        val scores = mutableMapOf<Int, Int>()
        for (oldLevel in 1..90) {
            val world = (oldLevel - 1) / 10
            val oldLocal = (oldLevel - 1) % 10 + 1
            val newLocal = when {
                oldLocal == 10 -> newCounts[world]
                world >= 7 && oldLocal >= 8 -> oldLocal + 7
                else -> oldLocal * 2 - 1
            }
            val newLevel = newCounts.take(world).sum() + newLocal
            if (prefs.contains("volley_stars_$oldLevel")) stars[newLevel] = prefs.getInteger("volley_stars_$oldLevel", 0)
            if (prefs.contains("volley_score_$oldLevel")) scores[newLevel] = prefs.getInteger("volley_score_$oldLevel", 0)
        }
        for (level in 1..160) {
            prefs.remove("volley_stars_$level")
            prefs.remove("volley_score_$level")
        }
        stars.forEach { (level, value) -> prefs.putInteger("volley_stars_$level", value) }
        scores.forEach { (level, value) -> prefs.putInteger("volley_score_$level", value) }
        fun remap(oldLevel: Int): Int {
            val safe = oldLevel.coerceIn(1, oldCounts.sum())
            val world = (safe - 1) / 10
            val local = (safe - 1) % 10 + 1
            val target = when {
                local == 10 -> newCounts[world]
                world >= 7 && local >= 8 -> local + 7
                else -> local * 2 - 1
            }
            return newCounts.take(world).sum() + target
        }
        for (key in listOf("volley_unlocked", "volley_last_played_level")) {
            if (prefs.contains(key)) prefs.putInteger(key, remap(prefs.getInteger(key, 1)))
        }
    }

    private fun remapPreviousLevel(level: Int): Int {
        val safeLevel = level.coerceIn(1, PREVIOUS_TOTAL_LEVELS)
        val oldWorld = (safeLevel - 1) / PREVIOUS_LEVELS_PER_WORLD + 1
        val oldStage = (safeLevel - 1) % PREVIOUS_LEVELS_PER_WORLD + 1
        return (
            (oldWorld - 1) * EIGHT_STAGE_LEVELS_PER_WORLD +
                oldStage.coerceAtMost(EIGHT_STAGE_LEVELS_PER_WORLD)
            )
            .coerceIn(1, EIGHT_STAGE_TOTAL_LEVELS)
    }

    /** Folds the former 10x8 campaign into the original nine supplied worlds. */
    private fun migrateEightStageCampaign(prefs: Preferences) {
        migrateCampaignLayout(
            prefs = prefs,
            oldWorldCounts = List(EIGHT_STAGE_WORLDS) { EIGHT_STAGE_LEVELS_PER_WORLD },
            newWorldCounts = PREVIOUS_AUTHORED_WORLD_COUNTS
        )
    }

    /**
     * Moves global stage keys by their stable world/stage meaning. If a world becomes shorter, its
     * removed tail merges into that world's new final stage; newly inserted stages never steal old
     * scores or stars from a following world.
     */
    private fun migrateCampaignLayout(
        prefs: Preferences,
        oldWorldCounts: List<Int>,
        newWorldCounts: List<Int>,
        shiftOldMoonBoss: Boolean = false
    ) {
        val migratedStars = mutableMapOf<Int, Int>()
        val migratedScores = mutableMapOf<Int, Int>()
        val oldTotal = oldWorldCounts.sum()
        val newTotal = newWorldCounts.sum()
        for (oldLevel in 1..oldTotal) {
            val newLevel = remapCampaignLevel(oldLevel, oldWorldCounts, newWorldCounts, shiftOldMoonBoss)
            val starKey = "volley_stars_$oldLevel"
            val scoreKey = "volley_score_$oldLevel"
            if (prefs.contains(starKey)) {
                migratedStars[newLevel] = maxOf(
                    migratedStars[newLevel] ?: 0,
                    prefs.getInteger(starKey, 0).coerceIn(0, 3)
                )
            }
            if (prefs.contains(scoreKey)) {
                migratedScores[newLevel] = maxOf(
                    migratedScores[newLevel] ?: 0,
                    prefs.getInteger(scoreKey, 0)
                )
            }
        }

        for (oldLevel in 1..maxOf(PREVIOUS_TOTAL_LEVELS, oldTotal, newTotal)) {
            prefs.remove("volley_stars_$oldLevel")
            prefs.remove("volley_score_$oldLevel")
        }
        migratedStars.forEach { (level, stars) -> prefs.putInteger("volley_stars_$level", stars) }
        migratedScores.forEach { (level, score) -> prefs.putInteger("volley_score_$level", score) }

        if (prefs.contains("volley_unlocked")) {
            prefs.putInteger(
                "volley_unlocked",
                remapCampaignLevel(prefs.getInteger("volley_unlocked", 1), oldWorldCounts, newWorldCounts, shiftOldMoonBoss)
            )
        }
        if (prefs.contains("volley_last_played_level")) {
            prefs.putInteger(
                "volley_last_played_level",
                remapCampaignLevel(
                    prefs.getInteger("volley_last_played_level", 1),
                    oldWorldCounts,
                    newWorldCounts,
                    shiftOldMoonBoss
                )
            )
        }
        if (prefs.contains("volley_last_world_map_selection")) {
            prefs.putInteger(
                "volley_last_world_map_selection",
                prefs.getInteger("volley_last_world_map_selection", 1).coerceIn(1, newWorldCounts.size)
            )
        }
    }

    private fun remapCampaignLevel(
        level: Int,
        oldWorldCounts: List<Int>,
        newWorldCounts: List<Int>,
        shiftOldMoonBoss: Boolean = false
    ): Int {
        val safeLevel = level.coerceIn(1, oldWorldCounts.sum())
        var oldWorld = 0
        var oldWorldStart = 1
        while (oldWorld < oldWorldCounts.lastIndex && safeLevel >= oldWorldStart + oldWorldCounts[oldWorld]) {
            oldWorldStart += oldWorldCounts[oldWorld]
            oldWorld++
        }
        if (oldWorld > newWorldCounts.lastIndex) return newWorldCounts.sum()
        val oldStage = safeLevel - oldWorldStart + 1
        val newStage = (if (shiftOldMoonBoss && oldWorld == 6 && oldStage == 8) 10 else oldStage)
            .coerceAtMost(newWorldCounts[oldWorld])
        return newWorldCounts.take(oldWorld).sum() + newStage
    }
}
