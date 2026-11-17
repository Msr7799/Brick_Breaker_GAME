/*
 * المسار: app/src/main/java/com/example/brick_breaker_ball/campaign/Campaign.kt
 * الوظيفة: إعدادات اللاعب وتقدم المراحل وحفظهما.
 * الحسابات: احتساب النجوم والفتح مع صيانة مفاتيح الحفظ.
 */

package com.example.brick_breaker_ball.campaign

import com.badlogic.gdx.Gdx
import com.badlogic.gdx.Preferences
import com.example.brick_breaker_ball.cosmetics.CosmeticDefaults
import com.example.brick_breaker_ball.cosmetics.VolleyBallSelection
import com.example.brick_breaker_ball.gameplay.Ball
import com.example.brick_breaker_ball.gameplay.BallSize
import com.example.brick_breaker_ball.gameplay.BrickType
import com.example.brick_breaker_ball.gameplay.CampaignCatalog
import com.example.brick_breaker_ball.gameplay.GameplayMode
import com.example.brick_breaker_ball.gameplay.VolleyOrbCatalog
import com.example.brick_breaker_ball.gameplay.VolleyOrbDefinition
import com.example.brick_breaker_ball.gameplay.VolleySourceOrbDrops
import java.util.Random

data class WorldDefinition(val id: Int, val name: String, val subtitle: String, val backgroundRegion: String, val tintHex: String)
data class LevelDefinition(
    val id: Int = -1,
    val world: Int = 1,
    val name: String = "UNTITLED",
    val rows: Int = 8,
    val columns: Int = 9,
    val layout: List<String> = List(8) { "........." },
    val ballSpeed: Float = 600f,
    val lives: Int = 3,
    val threeStarScore: Int = 5000,
    val modifiers: Set<String> = emptySet(),
    val customId: String? = null,
    val dropRate: Float = .12f,
    val deathRailEnabled: Boolean = true,
    val bossHealth: Int = 5,
    val brickGroups: Map<String, Int> = emptyMap(),
    /** Canonical BrickType IDs for custom/new levels; layout remains as a legacy migration fallback. */
    val brickIds: Map<String, String> = emptyMap()
)

enum class GraphicsQuality { LOW, MEDIUM, HIGH }
enum class VolleyDifficulty(val healthMultiplier: Int) { NORMAL(1), HARD(2), EXTREME(4) }
data class VolleyOrbDropResult(val orb: VolleyOrbDefinition, val improved: Boolean)
internal const val DEFAULT_MASTER_VOLUME = .5f
internal const val DEFAULT_MUSIC_VOLUME = .5f
internal const val DEFAULT_SFX_VOLUME = .5f

data class GameSettings(
    var masterVolume: Float = DEFAULT_MASTER_VOLUME,
    var musicVolume: Float = DEFAULT_MUSIC_VOLUME,
    var sfxVolume: Float = DEFAULT_SFX_VOLUME,
    var haptics: Boolean = true,
    var reduceMotion: Boolean = false,
    var highContrastBall: Boolean = false,
    var colorBlind: Boolean = false,
    var leftHanded: Boolean = false,
    var relativeControl: Boolean = false,
    var sensitivity: Float = 1f,
    var quality: GraphicsQuality = GraphicsQuality.HIGH,
    var selectedBallSpriteName: String = CosmeticDefaults.BALL_SPRITE,
    var selectedBallGroupName: String = CosmeticDefaults.BALL_GROUP,
    var selectedVolleyBallSpriteName: String = VolleyBallSelection.STARTER_SPRITE,
    var selectedVolleyBallGroupName: String = VolleyBallSelection.STARTER_GROUP,
    var selectedBallBaseSize: BallSize = BallSize.DEFAULT,
    var gameplayMode: GameplayMode = GameplayMode.VOLLEY
)

class ProgressStore(private val prefs: Preferences = Gdx.app.getPreferences("brickbreakerball-progress-v1")) {
    companion object {
        const val ORB_LOADOUT_COUNT = 4
        const val ORB_SLOTS_PER_LOADOUT = 8
    }
    val settings = GameSettings()
    private val orbInventory = VolleyOrbInventory(prefs)

    // New and installed players both start this separate stat at level 1 / XP 0. Never
    // manufacture historical XP from stage stars, world unlocks, purchases or ball ownership.
    val combatPlayerLevel: Int get() = prefs.getInteger("volley_combat_player_level_v1", 1).coerceAtLeast(1)
    val combatPlayerExperience: Long get() = prefs.getLong("volley_combat_player_experience_v1", 0L).coerceAtLeast(0L)

    /**
     * Call once per killed objective, in order, using the same run/sequence after pause restore.
     * Even zero-XP kills must be delivered: source level-up checks occur per kill, not per batch.
     * Retain recent run high-watermarks rather than one receipt per enemy in cloud preferences.
     */
    @Synchronized
    fun recordCombatExperience(runId: String, killSequence: Int, amount: Int) {
        require(amount >= 0 && killSequence > 0)
        require(runId.length in 1..128 && runId.all { it.isLetterOrDigit() || it == '-' || it == '_' })
        val receipts = linkedMapOf<String, Int>()
        for (entry in prefs.getString("volley_combat_run_receipts_v1", "").lineSequence()) {
            val separator = entry.lastIndexOf(':')
            if (separator <= 0) continue
            val sequence = entry.substring(separator + 1).toIntOrNull() ?: continue
            if (sequence > 0) receipts[entry.substring(0, separator)] = sequence
        }
        if (killSequence <= (receipts[runId] ?: 0)) return
        val next = VolleyPlayerProgress.award(
            VolleyPlayerProgress.State(combatPlayerLevel, combatPlayerExperience), amount)
        receipts.remove(runId)
        receipts[runId] = killSequence
        while (receipts.size > 32) receipts.remove(receipts.keys.first())
        prefs.putInteger(VolleyPlayerProgress.SCHEMA_KEY, 1)
            .putInteger("volley_combat_player_level_v1", next.level)
            .putLong("volley_combat_player_experience_v1", next.experience)
            .putString("volley_combat_run_receipts_v1", receipts.entries.joinToString("\n") { "${it.key}:${it.value}" })
            .flush()
    }

    /** A firing pattern is a choice for the owned ball, not a global difficulty setting. */
    fun isShotgunEnabled(spriteName: String): Boolean =
        prefs.getBoolean("volley_ball_shotgun_$spriteName", false)

    fun setShotgunEnabled(spriteName: String, enabled: Boolean) {
        prefs.putBoolean("volley_ball_shotgun_$spriteName", enabled).flush()
    }

    /** Preserve the existing Volley preference keys for installed players. */
    private fun modeKey(key: String): String = "volley_$key"

    private fun modeKey(key: String, @Suppress("UNUSED_PARAMETER") mode: GameplayMode): String = "volley_$key"
    private fun difficultyKey(key: String): String = if (selectedDifficulty == VolleyDifficulty.NORMAL) {
        modeKey(key)
    } else {
        "volley_${selectedDifficulty.name.lowercase()}_$key"
    }

    var selectedDifficulty: VolleyDifficulty
        get() = runCatching { VolleyDifficulty.valueOf(prefs.getString("volley_difficulty", "NORMAL")) }
            .getOrDefault(VolleyDifficulty.NORMAL)
        set(value) {
            prefs.putString("volley_difficulty", value.name).flush()
            syncUnlockProgress()
        }

    val orbUpgradePoints: Int get() = prefs.getInteger("volley_orb_upgrade_points", 0)
    /** SDK earn or recovery of the same verified opportunity; one durable receipt per credit. */
    fun creditEarnedOrbPoint(receipt: String): Boolean {
        if (receipt.isBlank() || prefs.getBoolean("volley-orb-ad:$receipt", false)) return false
        // The earned callback grants a real Standard Orb copy alongside the old point.
        VolleySourceOrbDrops.pick(1, receipt.hashCode(), 0, 0)?.let { picked ->
            if (orbInventory.add(picked.id, Random(receipt.hashCode().toLong()), flush = false) == null)
                prefs.putString("volley-orb-ad-pending:$receipt", picked.id)
        }
        prefs.putInteger("volley_orb_upgrade_points", (orbUpgradePoints.toLong() + 1L).coerceAtMost(Int.MAX_VALUE.toLong()).toInt())
            .putBoolean("volley-orb-ad:$receipt", true).flush()
        return true
    }
    /** Bag-full rewards remain in the existing cloud-synced progress checkpoint until space opens. */
    fun deliverPendingRewardedOrbs(): Int {
        var delivered = 0
        prefs.get().toMap().forEach { (key, value) ->
            if (!key.startsWith("volley-orb-ad-pending:") || value !is String || !orbInventory.hasSpace) return@forEach
            val receipt = key.removePrefix("volley-orb-ad-pending:")
            if (orbInventory.add(value, Random(receipt.hashCode().toLong()), flush = false) != null) {
                prefs.remove(key)
                delivered++
            }
        }
        if (delivered > 0) prefs.flush()
        return delivered
    }
    val pendingRewardedOrbCount: Int get() = prefs.get().keys.count { it.startsWith("volley-orb-ad-pending:") }
    /** مكافأة واحدة لكل عقدة أورب داخل محاولة المرحلة، بدون إصدار Boosts قديمة. */
    fun creditStageOrbPoint(levelId: Int, sourceId: Int, turn: Int, naturalDrop: Boolean = false): Boolean {
        if (levelId !in 1..160 || sourceId < 0 || turn < 0) return false
        val receipt = if (naturalDrop) "volley-orb-drop:$levelId:$sourceId:$turn"
            else "volley-orb-stage:$levelId:$sourceId:$turn"
        if (prefs.getBoolean(receipt, false)) return false
        prefs.putInteger("volley_orb_upgrade_points", (orbUpgradePoints.toLong() + 1L).coerceAtMost(Int.MAX_VALUE.toLong()).toInt())
            .putBoolean(receipt, true).flush()
        return true
    }
    /** Record the exact broken source and roll one persistent Orb copy. */
    private fun orbDropReceipt(levelId: Int, sourceId: Int, turn: Int,
                               naturalDrop: Boolean, runId: String): String {
        val kind = if (naturalDrop) "drop" else "stage"
        val runSuffix = runId.takeIf { it.isNotBlank() }?.let { ":$it" }.orEmpty()
        return "volley-orb-$kind:$levelId:$sourceId:$turn$runSuffix"
    }
    fun hasStageOrbDropReceipt(levelId: Int, sourceId: Int, turn: Int,
                               naturalDrop: Boolean, runId: String = ""): Boolean =
        prefs.getBoolean(orbDropReceipt(levelId, sourceId, turn, naturalDrop, runId), false)

    fun creditStageOrbDrop(levelId: Int, sourceId: Int, turn: Int, naturalDrop: Boolean,
                           requestedRank: Int, runId: String = ""): VolleyOrbDropResult? {
        if (levelId !in 1..160 || sourceId < 0 || turn < 0) return null
        val receipt = orbDropReceipt(levelId, sourceId, turn, naturalDrop, runId)
        if (prefs.getBoolean(receipt, false)) return null
        val orb = VolleySourceOrbDrops.pick(levelId, sourceId, turn, requestedRank,
            extremeMode = selectedDifficulty == VolleyDifficulty.EXTREME) ?: return null
        val improved = orb.rank > orbRank(orb.family)
        val copy = orbInventory.add(orb.id, Random(levelId.toLong() * 31 + sourceId.toLong() * 17 +
            turn + runId.hashCode().toLong()),
            flush = false)
        prefs.putBoolean(receipt, true)
        if (copy != null) {
            prefs.putInteger("volley_orb_found_${orb.id}",
                (prefs.getInteger("volley_orb_found_${orb.id}", 0).toLong() + 1L)
                    .coerceAtMost(Int.MAX_VALUE.toLong()).toInt())
            if (improved) prefs.putInteger("volley_orb_rank_${orb.family}", orb.rank)
        } else prefs.putInteger("volley_orb_upgrade_points",
            (orbUpgradePoints.toLong() + 1L).coerceAtMost(Int.MAX_VALUE.toLong()).toInt())
        prefs.flush()
        return copy?.let { VolleyOrbDropResult(it, improved) }
    }

    /** A boss's first guaranteed reward is recorded separately from its repeat drops. */
    fun creditRepeatBossOrbDrops(levelId: Int, bossSourceId: Int, ranks: List<Int>): List<VolleyOrbDropResult> {
        if (levelId !in 1..VolleyStageRepository.TOTAL_LEVELS ||
            levelId != VolleyStageRepository.lastLevel(VolleyStageRepository.worldForLevel(levelId)) ||
            bossSourceId < 0 || ranks.size != 2) return emptyList()
        val key = "volley-boss-repeat-attempt:$levelId"
        val attempt = prefs.getInteger(key, 0) + 1
        prefs.putInteger(key, attempt).flush()
        return ranks.mapIndexedNotNull { slot, rank ->
            if (rank < 0) null else creditStageOrbDrop(levelId,
                1_000_000 + bossSourceId * 2 + slot, attempt, true, rank)
        }
    }

    val ownedOrbCopies: List<VolleyOrbDefinition> get() = orbInventory.all()
    val orbStashCount: Int get() = orbInventory.size
    fun orbCopy(uid: String): VolleyOrbDefinition? = orbInventory.find(uid)
    fun setOrbCopyLocked(uid: String, locked: Boolean): Boolean {
        val copy = orbInventory.find(uid) ?: return false
        return orbInventory.update(copy.copy(locked = locked))
    }
    fun markOrbCopySeen(uid: String): Boolean {
        val copy = orbInventory.find(uid) ?: return false
        return if (copy.isNew) orbInventory.update(copy.copy(isNew = false)) else true
    }

    fun isOrbCopyEquipped(uid: String): Boolean = (1..ORB_LOADOUT_COUNT).any { loadout ->
        (0 until ORB_SLOTS_PER_LOADOUT).any { slot -> orbTokenInSlot(slot, loadout) == "copy:$uid" }
    }

    fun orbCopySaleValue(uid: String): Int? {
        val copy = orbInventory.find(uid) ?: return null
        if (copy.locked || isOrbCopyEquipped(uid)) return null
        return when (copy.rank) {
            0 -> 100; 1 -> 250; 2 -> 700; 3 -> 4_000
            4 -> 12_000; 5 -> 40_000; else -> 100_000
        }
    }

    /** The caller credits the wallet with an idempotent sale receipt before removing this copy. */
    fun sellOrbCopy(uid: String, credit: (String, Int) -> Boolean): Int? {
        val value = orbCopySaleValue(uid) ?: return null
        if (!credit(uid, value)) return null
        return if (orbInventory.remove(uid)) value else null
    }

    private val slotLevelRequirements = intArrayOf(0, 10, 25, 40, 60, 80, 0, 0)
    private val slotCoinCosts = intArrayOf(0, 800, 2_000, 4_000, 8_000, 15_000, 0, 0)
    fun orbSlotRequirement(slot: Int): Int = slotLevelRequirements.getOrElse(slot) { Int.MAX_VALUE }
    fun orbSlotCoinCost(slot: Int): Int = slotCoinCosts.getOrElse(slot) { Int.MAX_VALUE }
    fun isOrbSlotUnlocked(slot: Int): Boolean {
        if (slot !in 0 until ORB_SLOTS_PER_LOADOUT) return false
        if (slot == 0 || slot >= 6) return true
        if (prefs.getBoolean("volley_orb_slot_unlocked_${slot + 1}", false)) return true
        // Existing players had all eight slots available; never invalidate their equipped copies.
        if (prefs.contains("volley_equipped_orb_family")) return true
        return (1..ORB_LOADOUT_COUNT).any { loadout ->
            prefs.getString("volley_orb_loadout_${loadout}_slot_$slot", "").isNotBlank()
        }
    }
    fun unlockOrbSlot(slot: Int, chargeCoins: (Int, Int) -> Boolean): Boolean {
        if (slot !in 1..5 || isOrbSlotUnlocked(slot) || unlockedLevel < orbSlotRequirement(slot)) return false
        val cost = orbSlotCoinCost(slot)
        if (!chargeCoins(slot, cost)) return false
        prefs.putBoolean("volley_orb_slot_unlocked_${slot + 1}", true).flush()
        return true
    }

    fun orbForgeCost(uid: String): Int? {
        val copy = orbInventory.find(uid) ?: return null
        if (copy.locked || isOrbCopyEquipped(uid) || VolleyOrbCatalog.nextRank(copy.family, copy.rank) == null) return null
        return (copy.rank + 1).coerceAtLeast(1)
    }

    /** Replace an unequipped copy with a newly rolled copy of the next rarity. */
    fun forgeOrbCopy(uid: String, random: Random = Random()): VolleyOrbDefinition? {
        if (unlockedLevel < 15) return null
        val copy = orbInventory.find(uid) ?: return null
        val cost = orbForgeCost(uid) ?: return null
        if (orbUpgradePoints < cost) return null
        val next = VolleyOrbCatalog.nextRank(copy.family, copy.rank) ?: return null
        if (unlockedLevel < next.stageRequirement) return null
        val upgraded = VolleyOrbCatalog.rollCopy(next.id, uid, random)?.copy(
            acquiredAt = copy.acquiredAt, isNew = true) ?: return null
        if (!orbInventory.update(upgraded, flush = false)) return null
        prefs.putInteger("volley_orb_upgrade_points", orbUpgradePoints - cost)
        if (next.rank > orbRank(copy.family)) prefs.putInteger("volley_orb_rank_${copy.family}", next.rank)
        prefs.putInteger("volley_orb_found_${next.id}",
            (prefs.getInteger("volley_orb_found_${next.id}", 0).toLong() + 1L)
                .coerceAtMost(Int.MAX_VALUE.toLong()).toInt())
        prefs.flush()
        return upgraded
    }

    fun orbRerollCost(uid: String): Int? {
        val copy = orbInventory.find(uid) ?: return null
        if (copy.bonusStats.isEmpty() || copy.locked || isOrbCopyEquipped(uid)) return null
        return (copy.rank + 1).coerceAtLeast(1)
    }

    /** RPG rerolls a bonus into a different, not already present, stat from its rarity pool. */
    fun rerollOrbBonus(uid: String, bonusName: String, random: Random = Random()): VolleyOrbDefinition? {
        if (unlockedLevel < 50) return null
        val copy = orbInventory.find(uid) ?: return null
        val cost = orbRerollCost(uid) ?: return null
        if (orbUpgradePoints < cost || bonusName !in copy.bonusStats) return null
        val candidates = VolleyOrbCatalog.bonusPool(copy.id).filterKeys {
            it != bonusName && it !in copy.bonusStats
        }.toList()
        if (candidates.isEmpty()) return null
        val (newName, range) = candidates[random.nextInt(candidates.size)]
        val bonus = linkedMapOf<String, Float>()
        copy.bonusStats.forEach { (name, value) -> bonus[if (name == bonusName) newName else name] =
            if (name == bonusName) range.roll(random) else value }
        val updated = copy.copy(bonusStats = bonus,
            rollRanges = (copy.rollRanges - bonusName) + (newName to range))
        if (!orbInventory.update(updated, flush = false)) return null
        prefs.putInteger("volley_orb_upgrade_points", orbUpgradePoints - cost).flush()
        return updated
    }

    fun orbImbueCost(uid: String): Int? {
        val copy = orbInventory.find(uid) ?: return null
        if (copy.locked || isOrbCopyEquipped(uid) || copy.bonusStats.size >= 7) return null
        return (copy.rank + 2).coerceAtLeast(2)
    }

    fun imbueOrbBonus(uid: String, random: Random = Random()): VolleyOrbDefinition? {
        if (unlockedLevel < 60) return null
        val copy = orbInventory.find(uid) ?: return null
        val cost = orbImbueCost(uid) ?: return null
        if (orbUpgradePoints < cost) return null
        val candidates = VolleyOrbCatalog.bonusPool(copy.id).filterKeys { it !in copy.bonusStats }.toList()
        if (candidates.isEmpty()) return null
        val (name, range) = candidates[random.nextInt(candidates.size)]
        val updated = copy.copy(bonusStats = copy.bonusStats + (name to range.roll(random)),
            rollRanges = copy.rollRanges + (name to range), imbued = true)
        if (!orbInventory.update(updated, flush = false)) return null
        prefs.putInteger("volley_orb_upgrade_points", orbUpgradePoints - cost).flush()
        return updated
    }
    fun orbByToken(token: String?): VolleyOrbDefinition? = when {
        token == null -> null
        token.startsWith("copy:") -> orbInventory.find(token.removePrefix("copy:"))
        else -> VolleyOrbCatalog.byId(token)
    }

    fun orbFoundCount(id: String): Int = prefs.getInteger("volley_orb_found_$id", 0)
    fun orbRank(family: String): Int = prefs.getInteger(
        "volley_orb_rank_$family", if (VolleyOrbCatalog.atRank(family, 0) != null) 0 else -1
    )
    var selectedOrbLoadout: Int
        get() = prefs.getInteger("volley_orb_loadout", 1).coerceIn(1, ORB_LOADOUT_COUNT)
        set(value) { prefs.putInteger("volley_orb_loadout", value.coerceIn(1, ORB_LOADOUT_COUNT)).flush() }

    fun orbTokenInSlot(slot: Int, loadout: Int = selectedOrbLoadout): String? {
        require(slot in 0 until ORB_SLOTS_PER_LOADOUT)
        require(loadout in 1..ORB_LOADOUT_COUNT)
        val legacy = if (loadout == 1 && slot == 0) {
            prefs.getString("volley_equipped_orb_family", "CRIT")
                .takeIf(VolleyOrbCatalog::isPlayableFamily) ?: "CRIT"
        } else ""
        return prefs.getString("volley_orb_loadout_${loadout}_slot_$slot", legacy).takeIf { it.isNotBlank() }
    }

    fun orbFamilyInSlot(slot: Int, loadout: Int = selectedOrbLoadout): String? {
        val token = orbTokenInSlot(slot, loadout) ?: return null
        return if (token.startsWith("copy:")) orbByToken(token)?.family else token
    }

    fun orbInSlot(slot: Int, loadout: Int = selectedOrbLoadout): VolleyOrbDefinition? {
        val token = orbTokenInSlot(slot, loadout) ?: return null
        return if (token.startsWith("copy:")) orbByToken(token)
            else VolleyOrbCatalog.atRank(token, orbRank(token))
    }

    val equippedOrbs: List<VolleyOrbDefinition>
        get() {
            val uniqueFamilies = mutableSetOf<String>()
            return (0 until ORB_SLOTS_PER_LOADOUT).mapNotNull { slot -> orbInSlot(slot) }
                .filter { VolleyOrbCatalog.isPlayableFamily(it.family) }
                .filter { it.family !in setOf("LUCK", "SECONDW") || uniqueFamilies.add(it.family) }
        }

    val equippedOrb: VolleyOrbDefinition?
        get() = equippedOrbs.firstOrNull()

    fun equipOrb(family: String, slot: Int = 0): Boolean {
        if (!isOrbSlotUnlocked(slot)) return false
        if (!VolleyOrbCatalog.isPlayableFamily(family)) return false
        if (VolleyOrbCatalog.atRank(family, orbRank(family)) == null) return false
        val loadout = selectedOrbLoadout
        for (other in 0 until ORB_SLOTS_PER_LOADOUT) {
            if (other != slot && (orbTokenInSlot(other, loadout) == family ||
                family in setOf("LUCK", "SECONDW") && orbFamilyInSlot(other, loadout) == family)) {
                prefs.putString("volley_orb_loadout_${loadout}_slot_$other", "")
            }
        }
        prefs.putString("volley_orb_loadout_${loadout}_slot_$slot", family)
        if (loadout == 1 && slot == 0) prefs.putString("volley_equipped_orb_family", family)
        prefs.flush()
        return true
    }

    fun equipOrbCopy(uid: String, slot: Int): Boolean {
        if (!isOrbSlotUnlocked(slot)) return false
        val copy = orbInventory.find(uid) ?: return false
        if (!VolleyOrbCatalog.isPlayableFamily(copy.family)) return false
        val loadout = selectedOrbLoadout
        for (other in 0 until ORB_SLOTS_PER_LOADOUT) {
            if (other == slot) continue
            val equipped = orbInSlot(other, loadout)
            if (orbTokenInSlot(other, loadout) == "copy:$uid" ||
                (copy.onlyEquipOne && equipped?.id == copy.id) ||
                (copy.family in setOf("LUCK", "SECONDW") && equipped?.family == copy.family)) {
                prefs.putString("volley_orb_loadout_${loadout}_slot_$other", "")
            }
        }
        prefs.putString("volley_orb_loadout_${loadout}_slot_$slot", "copy:$uid")
        if (loadout == 1 && slot == 0) prefs.putString("volley_equipped_orb_family", "")
        prefs.flush()
        return true
    }

    fun unequipOrb(slot: Int): Boolean {
        if (slot !in 0 until ORB_SLOTS_PER_LOADOUT || orbTokenInSlot(slot) == null) return false
        prefs.putString("volley_orb_loadout_${selectedOrbLoadout}_slot_$slot", "")
        if (selectedOrbLoadout == 1 && slot == 0) prefs.putString("volley_equipped_orb_family", "")
        prefs.flush()
        return true
    }
    /** تمنح رتبة Orb بمقابل عملات/جواهر بعد التأكد من أن الرتبة موجودة.
     * لا نمس أرصدة أو مشتريات الـBoosts القديمة حتى لا تضيع حقوق المستخدم. */
    fun buyOrbRank(family: String, charge: () -> Boolean): Boolean {
        if (!VolleyOrbCatalog.isPlayableFamily(family)) return false
        val next = VolleyOrbCatalog.nextRank(family, orbRank(family)) ?: return false
        if (unlockedLevel < next.stageRequirement) return false
        if (!orbInventory.hasSpace) return false
        if (!charge()) return false
        if (orbInventory.add(next.id, Random(System.nanoTime()), flush = false) == null) return false
        prefs.putInteger("volley_orb_rank_$family", next.rank).flush()
        return true
    }

    fun upgradeOrb(family: String): Boolean {
        if (!VolleyOrbCatalog.isPlayableFamily(family)) return false
        val current = orbRank(family)
        val next = VolleyOrbCatalog.nextRank(family, current) ?: return false
        if (unlockedLevel < next.stageRequirement) return false
        val cost = next.rank - current
        if (orbUpgradePoints < cost || !orbInventory.hasSpace) return false
        if (orbInventory.add(next.id, Random(System.nanoTime()), flush = false) == null) return false
        prefs.putInteger("volley_orb_rank_$family", next.rank)
            .putInteger("volley_orb_upgrade_points", orbUpgradePoints - cost)
            .flush()
        return true
    }

    var unlockedLevel: Int
        get() = prefs.getInteger(
            difficultyKey("unlocked"),
            1
        )
            .coerceIn(1, CampaignCatalog.totalLevels(settings.gameplayMode))
        private set(value) {
            prefs.putInteger(difficultyKey("unlocked"), value.coerceIn(1, CampaignCatalog.totalLevels(settings.gameplayMode))).flush()
        }

    val hasStartedGame: Boolean
        get() = prefs.getBoolean(modeKey("has_started_game"), false)

    var lastPlayedLevel: Int
        get() = prefs.getInteger(modeKey("last_played_level"), 1)
            .coerceIn(1, CampaignCatalog.totalLevels(settings.gameplayMode))
        private set(value) {
            prefs.putInteger(modeKey("last_played_level"), value.coerceIn(1, CampaignCatalog.totalLevels(settings.gameplayMode))).flush()
        }

    var lastWorldMapSelection: Int
        get() = prefs.getInteger(modeKey("last_world_map_selection"), 1)
        private set(value) {
            prefs.putInteger(modeKey("last_world_map_selection"), value).flush()
        }

    val currentPlayingWorld: Int
        get() = CampaignCatalog.worldForLevel(settings.gameplayMode, lastPlayedLevel)
            .coerceIn(1, CampaignCatalog.worlds(settings.gameplayMode).last().id)

    val highestUnlockedWorld: Int
        get() = computeAccessibleWorld()

    init {
        loadSettings()
        VolleyProgressMigration.migrate(prefs)
        // Mark snapshots made by the new app even before the first kill. This adds no XP.
        if (!prefs.contains(VolleyPlayerProgress.SCHEMA_KEY)) {
            prefs.putInteger(VolleyPlayerProgress.SCHEMA_KEY, 1).flush()
        }
        syncUnlockProgress()
    }

    /** ملاحظة صيانة: الدالة `stars` تنفّذ مسؤولية محلية يعتمد عليها هذا الجزء من اللعبة؛ راجع استدعاءاتها واختباراتها قبل تعديلها. */
    fun stars(level: Int) = stars(level, settings.gameplayMode)

    fun stars(level: Int, mode: GameplayMode) = prefs.getInteger("${difficultyKey("stars")}_$level", 0)

    /** Read legacy milestones once so earned cosmetics survive removal of the old campaign. */
    fun legacyClassicStars(level: Int): Int = prefs.getInteger("stars_$level", 0)

    /** ملاحظة صيانة: الدالة `bestScore` تنفّذ مسؤولية محلية يعتمد عليها هذا الجزء من اللعبة؛ راجع استدعاءاتها واختباراتها قبل تعديلها. */
    fun bestScore(level: Int) = prefs.getInteger("${difficultyKey("score")}_$level", 0)
    fun totalStars(): Int = (1..CampaignCatalog.totalLevels(settings.gameplayMode)).sumOf(::stars)
    fun bossCompleted(world: Int, mode: GameplayMode = settings.gameplayMode): Boolean = prefs.getBoolean("${difficultyKey("boss_complete")}_$world", false)

    /** ملاحظة صيانة: الدالة `markGameStarted` تحفظ البيانات أو تضيفها إلى الحالة المعتمدة في النظام؛ راجع استدعاءاتها واختباراتها قبل تعديلها. */
    fun markGameStarted() {
        if (!hasStartedGame) {
            prefs.putBoolean(modeKey("has_started_game"), true).flush()
        }
    }

    fun markLevelPlayed(level: Int) {
        lastPlayedLevel = level.coerceIn(1, CampaignCatalog.totalLevels(settings.gameplayMode))
    }

    fun rememberWorldMapSelection(world: Int) {
        lastWorldMapSelection = world.coerceIn(1, highestUnlockedWorld)
    }

    fun isWorldUnlocked(world: Int): Boolean = world in 1..highestUnlockedWorld

    /** Every stage in an unlocked world is selectable, regardless of its stage number. */
    fun isLevelUnlocked(level: Int): Boolean = level in 1..CampaignCatalog.totalLevels(settings.gameplayMode) &&
        CampaignCatalog.worldForLevel(settings.gameplayMode, level) <= highestUnlockedWorld

    fun nextRecommendedLevel(): Int {
        val world = highestUnlockedWorld
        val first = CampaignCatalog.firstLevel(settings.gameplayMode, world)
        val last = CampaignCatalog.lastLevel(settings.gameplayMode, world)
        return (first..last).firstOrNull { stars(it) == 0 } ?: last
    }

    private fun computeAccessibleWorld(): Int {
        var accessibleWorld = 1
        val worlds = CampaignCatalog.worlds(settings.gameplayMode)
        for (world in 1 until worlds.size) {
            val first = CampaignCatalog.firstLevel(settings.gameplayMode, world)
            val last = CampaignCatalog.lastLevel(settings.gameplayMode, world)
            val currentWorldComplete = (first..last).all { stars(it) > 0 }
            if (!currentWorldComplete) break
            accessibleWorld = world + 1
        }
        return accessibleWorld.coerceIn(1, worlds.last().id)
    }

    private fun syncUnlockProgress() {
        // Existing menus and DevelopmentAccess use unlockedLevel as an inclusive boundary.
        // Persist the last stage of the current world so all of its stages open together.
        unlockedLevel = CampaignCatalog.lastLevel(settings.gameplayMode, computeAccessibleWorld())
    }

    /** ملاحظة صيانة: الدالة `complete` تنفّذ مسؤولية محلية يعتمد عليها هذا الجزء من اللعبة؛ راجع استدعاءاتها واختباراتها قبل تعديلها. */
    fun complete(level: Int, score: Int, stars: Int) {
        val firstClear = this.stars(level) == 0
        prefs.putInteger("${difficultyKey("stars")}_$level", maxOf(this.stars(level), stars))
            .putInteger("${difficultyKey("score")}_$level", maxOf(bestScore(level), score))
        val world = CampaignCatalog.worldForLevel(settings.gameplayMode, level)
        if (level == CampaignCatalog.lastLevel(settings.gameplayMode, world)) {
            prefs.putBoolean("${difficultyKey("boss_complete")}_$world", true)
            if (firstClear) prefs.putInteger("volley_orb_upgrade_points", orbUpgradePoints + 1)
        }
        prefs.flush()
        syncUnlockProgress()
    }

    /** ملاحظة صيانة: الدالة `saveSettings` تحفظ البيانات أو تضيفها إلى الحالة المعتمدة في النظام؛ راجع استدعاءاتها واختباراتها قبل تعديلها. */
    fun saveSettings() {
        with(settings) {
            prefs.putFloat("master", masterVolume).putFloat("music", musicVolume).putFloat("sfx", sfxVolume).putBoolean("haptics", haptics).putBoolean("motion", reduceMotion).putBoolean("contrast", highContrastBall).putBoolean("colorblind", colorBlind).putBoolean("left", leftHanded).putBoolean("relative", relativeControl).putFloat("sensitivity", sensitivity).putString("quality", quality.name)
                .putString("selected_ball_sprite", selectedBallSpriteName).putString("selected_ball_group", selectedBallGroupName)
                .putString("volley_selected_ball_sprite", selectedVolleyBallSpriteName)
                .putString("volley_selected_ball_group", selectedVolleyBallGroupName)
                // مفاتيح البادل القديمة لا تُكتب ولا تُمسح؛ تبقى بيانات المستخدم محفوظة في Preferences.
                .putString("selected_ball_size", BallSize.DEFAULT.name)
                .putString("gameplay_mode", gameplayMode.name).flush()
        }
    }

    /** ملاحظة صيانة: الدالة `loadSettings` تقرأ البيانات المطلوبة أو تسترجعها بصيغة مناسبة للاستخدام؛ راجع استدعاءاتها واختباراتها قبل تعديلها. */
    private fun loadSettings() {
        with(settings) {
            masterVolume = prefs.getFloat("master", DEFAULT_MASTER_VOLUME).coerceIn(0f, 1f)
            musicVolume = prefs.getFloat("music", DEFAULT_MUSIC_VOLUME).coerceIn(0f, 1f)
            sfxVolume = prefs.getFloat("sfx", DEFAULT_SFX_VOLUME).coerceIn(0f, 1f)
            haptics = prefs.getBoolean("haptics", true)
            reduceMotion = prefs.getBoolean("motion", false)
            highContrastBall = prefs.getBoolean("contrast", false)
            colorBlind = prefs.getBoolean("colorblind", false)
            leftHanded = prefs.getBoolean("left", false)
            relativeControl = prefs.getBoolean("relative", false)
            sensitivity = prefs.getFloat("sensitivity", 1f)
            quality = runCatching { GraphicsQuality.valueOf(prefs.getString("quality", "HIGH")) }.getOrDefault(GraphicsQuality.HIGH)
            selectedBallSpriteName = prefs.getString("selected_ball_sprite", CosmeticDefaults.BALL_SPRITE)
            selectedBallGroupName = prefs.getString("selected_ball_group", CosmeticDefaults.BALL_GROUP)
            selectedVolleyBallSpriteName = prefs.getString("volley_selected_ball_sprite", VolleyBallSelection.STARTER_SPRITE)
            selectedVolleyBallGroupName = prefs.getString("volley_selected_ball_group", VolleyBallSelection.STARTER_GROUP)
            // Ball size is gameplay-owned: every run starts Normal and size talismans change it temporarily.
            selectedBallBaseSize = BallSize.DEFAULT
            gameplayMode = GameplayMode.VOLLEY
            if (prefs.getString("gameplay_mode", "") != GameplayMode.VOLLEY.name) {
                prefs.putString("gameplay_mode", GameplayMode.VOLLEY.name).flush()
            }
        }
    }
}
