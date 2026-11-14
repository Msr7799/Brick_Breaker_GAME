package com.example.brick_breaker_ball.campaign

import com.badlogic.gdx.Preferences
import com.example.brick_breaker_ball.gameplay.VolleyOrbCatalog
import com.example.brick_breaker_ball.gameplay.VolleyOrbDefinition
import com.example.brick_breaker_ball.gameplay.VolleyOrbRollRange
import java.util.Random
import java.util.UUID

/** Individual RPG-style Orb copies; old family/rank preferences remain readable. */
internal class VolleyOrbInventory(private val prefs: Preferences) {
    companion object {
        const val CAPACITY = 150 // Components/items.gd: maximum_stash_size
        private const val INDEX = "volley_orb_copy_uids_v1"
        private const val PREFIX = "volley_orb_copy_v1_"
        private const val META_PREFIX = "volley_orb_copy_meta_v2_"
        private const val RANGE_PREFIX = "volley_orb_copy_ranges_v2_"
        private const val SCHEMA_KEY = "volley_orb_copy_schema"
    }

    private var cachedIds: List<String>? = null
    private var cachedCopies: List<VolleyOrbDefinition>? = null
    private fun ids(): List<String> = cachedIds ?: prefs.getString(INDEX, "").split(',')
        .filter { it.isNotBlank() }.also { cachedIds = it }
    val size: Int get() = ids().size
    val hasSpace: Boolean get() = size < CAPACITY

    fun all(): List<VolleyOrbDefinition> = cachedCopies ?: ids().mapNotNull(::read).also { cachedCopies = it }

    fun find(uid: String): VolleyOrbDefinition? {
        if (uid !in ids()) return null
        return read(uid)
    }

    private fun read(uid: String): VolleyOrbDefinition? {
        val parts = prefs.getString(PREFIX + uid, "").split('|', limit = 3)
        if (parts.size != 3) return null
        val base = VolleyOrbCatalog.byId(parts[0]) ?: return null
        val stats = decode(parts[1])
        val bonus = decode(parts[2])
        val meta = prefs.getString(META_PREFIX + uid, "").split('|')
        val ranges = decodeRanges(prefs.getString(RANGE_PREFIX + uid, ""))
            .ifEmpty { VolleyOrbCatalog.rangesFor(base.id, bonus.keys) }
        return base.copy(uid = uid, stats = stats, bonusStats = bonus, rollRanges = ranges,
            locked = meta.getOrNull(0) == "1", isNew = meta.getOrNull(1) == "1",
            imbued = meta.getOrNull(2) == "1", acquiredAt = meta.getOrNull(3)?.toLongOrNull() ?: 0L)
    }

    fun add(itemId: String, random: Random, flush: Boolean = true): VolleyOrbDefinition? {
        if (!hasSpace) return null
        val uid = UUID.randomUUID().toString().replace("-", "")
        val copy = VolleyOrbCatalog.rollCopy(itemId, uid, random) ?: return null
        prefs.putString(PREFIX + uid, "${copy.id}|${encode(copy.stats)}|${encode(copy.bonusStats)}")
        prefs.putString(META_PREFIX + uid, encodeMeta(copy))
        prefs.putString(RANGE_PREFIX + uid, encodeRanges(copy.rollRanges))
        prefs.putInteger(SCHEMA_KEY, 2)
        val updated = ids() + uid
        prefs.putString(INDEX, updated.joinToString(","))
        cachedIds = updated
        cachedCopies = null
        if (flush) prefs.flush()
        return copy
    }

    fun update(copy: VolleyOrbDefinition, flush: Boolean = true): Boolean {
        val uid = copy.uid ?: return false
        if (uid !in ids() || VolleyOrbCatalog.byId(copy.id) == null) return false
        prefs.putString(PREFIX + uid, "${copy.id}|${encode(copy.stats)}|${encode(copy.bonusStats)}")
        prefs.putString(META_PREFIX + uid, encodeMeta(copy))
        prefs.putString(RANGE_PREFIX + uid, encodeRanges(copy.rollRanges))
        prefs.putInteger(SCHEMA_KEY, 2)
        cachedCopies = null
        if (flush) prefs.flush()
        return true
    }

    fun remove(uid: String): Boolean {
        if (uid !in ids()) return false
        val updated = ids().filterNot { it == uid }
        prefs.putString(INDEX, updated.joinToString(","))
        prefs.remove(PREFIX + uid)
        prefs.remove(META_PREFIX + uid)
        prefs.remove(RANGE_PREFIX + uid)
        cachedIds = updated
        cachedCopies = null
        prefs.flush()
        return true
    }

    private fun encodeMeta(copy: VolleyOrbDefinition): String =
        "${if (copy.locked) 1 else 0}|${if (copy.isNew) 1 else 0}|${if (copy.imbued) 1 else 0}|${copy.acquiredAt}"

    private fun encodeRanges(values: Map<String, VolleyOrbRollRange>): String =
        values.entries.joinToString(",") { (key, range) ->
            require(key.matches(Regex("[a-z0-9_]+")))
            require(range.type.matches(Regex("[a-z0-9_]+")))
            "$key=${range.min}~${range.max}~${range.type}"
        }

    private fun decodeRanges(raw: String): Map<String, VolleyOrbRollRange> = raw.split(',').mapNotNull { pair ->
        val equal = pair.indexOf('=')
        if (equal <= 0) return@mapNotNull null
        val parts = pair.substring(equal + 1).split('~')
        if (parts.size != 3) return@mapNotNull null
        val min = parts[0].toFloatOrNull() ?: return@mapNotNull null
        val max = parts[1].toFloatOrNull() ?: return@mapNotNull null
        pair.substring(0, equal) to VolleyOrbRollRange(min, max, parts[2])
    }.toMap()

    private fun encode(values: Map<String, Float>): String = if (values.isEmpty()) "-" else values.entries.joinToString(",") { (key, value) ->
        require(key.matches(Regex("[a-z0-9_]+")))
        "$key=$value"
    }

    private fun decode(raw: String): Map<String, Float> = raw.split(',').mapNotNull { pair ->
        val pos = pair.indexOf('=')
        if (pos <= 0) null else pair.substring(pos + 1).toFloatOrNull()?.let { pair.substring(0, pos) to it }
    }.toMap()
}
