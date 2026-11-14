/*
 * يقرأ قيم التمائم الموثقة ويحوّلها إلى تعريفات قابلة للاستخدام في محاكاة Volley.
 */
package com.example.brick_breaker_ball.gameplay

import com.badlogic.gdx.Gdx
import com.badlogic.gdx.files.FileHandle
import com.badlogic.gdx.utils.JsonReader
import java.io.File
import java.util.Collections
import java.util.Random
import kotlin.math.roundToInt

data class VolleyOrbDefinition(
    val id: String,
    val family: String,
    val rank: Int,
    val source: String,
    val stats: Map<String, Float>,
    val name: String = id,
    val rarity: String = "STANDARD",
    val onlyEquipOne: Boolean = false,
    val uid: String? = null,
    val bonusStats: Map<String, Float> = emptyMap(),
    val legacy: Boolean = false,
    val rollRanges: Map<String, VolleyOrbRollRange> = emptyMap(),
    val locked: Boolean = false,
    val isNew: Boolean = false,
    val imbued: Boolean = false,
    val acquiredAt: Long = 0L,
    /** Source Items.lvl_req, mapped to Volley's global campaign stage. */
    val stageRequirement: Int = 1
) {
    fun stat(name: String): Float = (stats[name] ?: 0f) + (bonusStats[name] ?: 0f)
}

data class VolleyOrbRollRange(val min: Float, val max: Float, val type: String = "flat") {
    fun roll(random: Random): Float {
        val lower = min.coerceAtMost(max)
        val upper = max.coerceAtLeast(min)
        val whole = lower == lower.roundToInt().toFloat() && upper == upper.roundToInt().toFloat()
        val scale = if (whole) 1 else 10
        val from = (lower * scale).roundToInt()
        val to = (upper * scale).roundToInt()
        return (from + random.nextInt(to - from + 1)).toFloat() / scale
    }
}

private data class OrbRollTable(val fixed: Map<String, Float>, val ranges: Map<String, VolleyOrbRollRange>,
                                val bonusCount: Int, val bonusPool: Map<String, VolleyOrbRollRange>)

object VolleyOrbCatalog {
    private const val ASSET = "volley-source/orbs.json"
    // These RPG bonuses need modes/effects that Volley does not expose. Never roll a dead bonus.
    private val unavailableBonusStats = setOf("goblin_ancient_debris_amount",
        "idle_enemy_health_reduction")
    // These entries require a separate mining, goblin, or randomly rolled item system.
    private val archiveFamilies = setOf("BLAST", "EXPLOSIVE_TEMP", "GOBLIN", "ODIN", "PICK",
        "PUTRID", "THOR", "MINERS", "TIME", "IDLE", "CHAOS")
    fun isPlayableFamily(family: String): Boolean = family !in archiveFamilies &&
        all.any { it.family == family && !it.legacy }
    private val rollTables = linkedMapOf<String, OrbRollTable>()
    val all: List<VolleyOrbDefinition> by lazy {
        val handle = if (Gdx.files != null) Gdx.files.internal(ASSET) else {
            val candidates = listOf(File("app/src/main/assets/$ASSET"), File("src/main/assets/$ASSET"))
            FileHandle(candidates.firstOrNull(File::isFile) ?: error("Missing $ASSET"))
        }
        val root = JsonReader().parse(handle)
        buildList {
            var node = root.get("orbs").child
            while (node != null) {
                val stats = linkedMapOf<String, Float>()
                var number = node.get("stats").child
                while (number != null) {
                    stats[number.name] = number.asFloat()
                    number = number.next
                }
                val id = node.getString("id")
                val fixed = linkedMapOf<String, Float>()
                val ranges = linkedMapOf<String, VolleyOrbRollRange>()
                var roll = node.get("rolls")?.child
                while (roll != null) {
                    if (roll.isObject) ranges[roll.name] = VolleyOrbRollRange(
                        roll.getFloat("min"), roll.getFloat("max"), roll.getString("type", "flat"))
                    else fixed[roll.name] = if (roll.isBoolean) { if (roll.asBoolean()) 1f else 0f } else roll.asFloat()
                    roll = roll.next
                }
                val bonusPool = linkedMapOf<String, VolleyOrbRollRange>()
                var bonus = node.get("bonusPool")?.child
                while (bonus != null) {
                    if (bonus.isObject && bonus.name !in unavailableBonusStats) bonusPool[bonus.name] = VolleyOrbRollRange(
                        bonus.getFloat("min"), bonus.getFloat("max"), bonus.getString("type", "flat"))
                    bonus = bonus.next
                }
                rollTables[id] = OrbRollTable(fixed, ranges, node.getInt("bonusCount", 0), bonusPool)
                add(VolleyOrbDefinition(id, node.getString("family"), node.getInt("rank"),
                    node.getString("source"), stats, node.getString("name", id),
                    node.getString("rarity", "STANDARD"), node.getBoolean("onlyEquipOne", false),
                    legacy = node.getBoolean("legacy", false),
                    stageRequirement = node.getInt("stageRequirement", 1)))
                node = node.next
            }
        }
    }

    fun byId(id: String?): VolleyOrbDefinition? = all.firstOrNull { it.id == id }
    fun families(): List<String> = all.map(VolleyOrbDefinition::family).distinct()
    fun atRank(family: String, rank: Int): VolleyOrbDefinition? = all.firstOrNull { it.family == family && it.rank == rank }
    fun nextRank(family: String, current: Int): VolleyOrbDefinition? = all.filter { it.family == family && it.rank > current }.minByOrNull { it.rank }

    fun rangesFor(id: String, bonusNames: Set<String> = emptySet()): Map<String, VolleyOrbRollRange> {
        all // Populate the source tables before lookup.
        val table = rollTables[id] ?: return emptyMap()
        return table.ranges + table.bonusPool.filterKeys { it in bonusNames }
    }

    fun bonusPool(id: String): Map<String, VolleyOrbRollRange> {
        all
        return rollTables[id]?.bonusPool.orEmpty()
    }

    /** Source orb_stats.gd rolls inclusive integers or one-decimal values and unique bonus names. */
    fun rollCopy(id: String, uid: String, random: Random): VolleyOrbDefinition? {
        val base = byId(id) ?: return null
        val table = rollTables[id] ?: return base.copy(uid = uid, isNew = true,
            acquiredAt = System.currentTimeMillis())
        val staticStats = table.fixed.toMutableMap()
        table.ranges.forEach { (name, range) -> staticStats[name] = range.roll(random) }
        val bonusStats = linkedMapOf<String, Float>()
        val bonusNames = table.bonusPool.keys.toMutableList()
        Collections.shuffle(bonusNames, random)
        bonusNames.take(table.bonusCount).forEach { name ->
            bonusStats[name] = table.bonusPool.getValue(name).roll(random)
        }
        return base.copy(uid = uid, stats = staticStats, bonusStats = bonusStats,
            rollRanges = rangesFor(id, bonusStats.keys), isNew = true, acquiredAt = System.currentTimeMillis())
    }
}
