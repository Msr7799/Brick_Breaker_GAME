package com.example.brick_breaker_ball.campaign

import com.badlogic.gdx.Gdx
import com.badlogic.gdx.Preferences
import com.badlogic.gdx.utils.JsonReader
import com.badlogic.gdx.utils.JsonWriter
import java.io.StringWriter
import java.security.MessageDigest

/** Only player state is backed up: no auth tokens, ad consent or developer settings. */
internal object CloudSaveCodec {
    val stores = linkedMapOf(
        "progress" to "brickbreakerball-progress-v1",
        "wallet" to "brickbreakerball-economy-v1",
        "items" to "brickbreakerball-volley-items-v1",
        "transactions" to "brickbreakerball-transactions-v1",
        "cosmetics" to "brickbreakerball-cosmetic-ownership-v5",
        "guide" to "brickbreakerball-hero-guide-v1",
        "controls" to "brickbreakerball-volley-controls-v1",
        "pendingRewards" to "brickbreakerball-pending-reward-v1"
    )
    data class Entry(val key: String, val type: String, val value: String)
    fun meta(): Preferences = Gdx.app.getPreferences("brickbreakerball-cloud-sync-v1")
    fun hash(value: String): String = MessageDigest.getInstance("SHA-256").digest(value.toByteArray(Charsets.UTF_8))
        .joinToString("") { "%02x".format(it.toInt() and 255) }

    fun capture(): String = encode(stores.mapValues { (_, name) ->
        Gdx.app.getPreferences(name).get().map { (key, value) ->
            val type = when (value) { is String -> "s"; is Boolean -> "b"; is Int -> "i"; is Long -> "l"; is Float -> "f"; else -> error("Unsupported preference type") }
            Entry(key, type, value.toString())
        }
    })
    fun empty(): String = encode(stores.mapValues { emptyList() })
    fun canonical(snapshot: String): String = encode(decode(snapshot))
    private fun encode(sections: Map<String, List<Entry>>): String {
        val out = StringWriter()
        JsonWriter(out).use { writer ->
            writer.setOutputType(JsonWriter.OutputType.json)
            writer.`object`().name("schema").value(1).name("sections").array()
            for ((name, entries) in sections.toSortedMap()) {
                writer.`object`().name("name").value(name).name("entries").array()
                for (entry in entries.sortedBy { it.key }) writer.`object`().name("key").value(entry.key)
                    .name("type").value(entry.type).name("value").value(entry.value).pop()
                writer.pop().pop()
            }
            writer.pop().pop()
        }
        return out.toString().also { require(it.toByteArray(Charsets.UTF_8).size < 1024 * 1024) }
    }
    fun decode(snapshot: String): Map<String, List<Entry>> {
        require(snapshot.toByteArray(Charsets.UTF_8).size <= 1024 * 1024)
        val root = JsonReader().parse(snapshot)
        require(root.getInt("schema") == 1)
        val result = linkedMapOf<String, List<Entry>>()
        var section = root.get("sections").child
        var count = 0
        while (section != null) {
            val name = section.getString("name")
            require(name in stores && name !in result)
            val entries = mutableListOf<Entry>()
            val keys = mutableSetOf<String>()
            var node = section.get("entries").child
            while (node != null) {
                val item = Entry(node.getString("key"), node.getString("type"), node.getString("value"))
                require(++count <= 12000 && item.key.length in 1..1024 && keys.add(item.key) && item.value.length <= 262144)
                when (item.type) {
                    "s" -> Unit
                    "b" -> require(item.value == "true" || item.value == "false")
                    "i" -> item.value.toInt()
                    "l" -> item.value.toLong()
                    "f" -> require(item.value.toFloat().isFinite())
                    else -> error("Unknown preference type")
                }
                entries += item
                node = node.next
            }
            result[name] = entries
            section = section.next
        }
        // Early schema-1 backups did not include the aiming tutorial marker.
        if ("controls" !in result) result["controls"] = emptyList()
        require(result.keys == stores.keys)
        return result
    }
    /** Finish a journal before creating any in-memory store after process death. */
    fun recover() {
        val pending = meta().getString("apply_snapshot", "")
        if (pending.isNotBlank()) apply(pending, meta().getString("apply_account", ""))
    }
    fun apply(snapshot: String, account: String) {
        require(account.isNotBlank())
        val incoming = decode(snapshot) // Validate everything before clearing a single key.
        val metadata = meta()
        val localProgress = Gdx.app.getPreferences(stores.getValue("progress"))
        val preserveCombat = metadata.getString("bound_account", "") == account &&
            incoming.getValue("progress").none { it.key == VolleyPlayerProgress.SCHEMA_KEY }
        val decoded = if (preserveCombat) {
            val combat = localProgress.get().filterKeys { it.startsWith(VolleyPlayerProgress.PREFERENCE_PREFIX) }
                .map { (key, value) ->
                    val type = when (value) {
                        is String -> "s"; is Boolean -> "b"; is Int -> "i"; is Long -> "l"; is Float -> "f"
                        else -> error("Unsupported preference type")
                    }
                    Entry(key, type, value.toString())
                }.toMutableList()
            if (combat.isNotEmpty() && combat.none { it.key == VolleyPlayerProgress.SCHEMA_KEY }) {
                combat += Entry(VolleyPlayerProgress.SCHEMA_KEY, "i", "1")
            }
            if (combat.isEmpty()) incoming else incoming + ("progress" to
                (incoming.getValue("progress").filterNot {
                    it.key.startsWith(VolleyPlayerProgress.PREFERENCE_PREFIX)
                } + combat))
        } else incoming
        // Journal the effective snapshot, including same-account compatibility preservation.
        // Recovery must replay that exact state even if a process dies after prefs.clear().
        val effectiveSnapshot = encode(decoded)
        decode(effectiveSnapshot)
        metadata.putString("apply_snapshot", effectiveSnapshot).putString("apply_account", account).flush()
        for ((name, entries) in decoded) {
            val prefs = Gdx.app.getPreferences(stores.getValue(name))
            prefs.clear()
            for (item in entries) when (item.type) {
                "s" -> prefs.putString(item.key, item.value)
                "b" -> prefs.putBoolean(item.key, item.value.toBoolean())
                "i" -> prefs.putInteger(item.key, item.value.toInt())
                "l" -> prefs.putLong(item.key, item.value.toLong())
                "f" -> prefs.putFloat(item.key, item.value.toFloat())
            }
            prefs.flush()
        }
        metadata.putString("bound_account", account)
        metadata.remove("apply_snapshot")
        metadata.remove("apply_account")
        metadata.flush()
    }
    fun meaningful(snapshot: String): Boolean {
        val sections = decode(snapshot)
        return sections.getValue("progress").any {
            (it.key.contains("stars_") && (it.value.toIntOrNull() ?: 0) > 0) ||
                (it.key.endsWith("has_started_game") && it.value == "true") ||
                (it.key == "volley_orb_copy_uids_v1" && it.value.isNotBlank()) ||
                (it.key == "volley_combat_player_level_v1" && (it.value.toIntOrNull() ?: 1) > 1) ||
                (it.key == "volley_combat_player_experience_v1" && (it.value.toLongOrNull() ?: 0L) > 0L)
        } || sections.getValue("wallet").any { (it.key == "coins" && it.value != "600") || (it.key == "gems" && it.value != "25") || (it.type == "b" && it.value == "true") } ||
            sections.getValue("items").any { (it.value.toIntOrNull() ?: 0) > 0 } ||
            sections.getValue("transactions").any { it.type == "b" && it.value == "true" } ||
            sections.getValue("cosmetics").count { it.key.startsWith("ball:") && it.value == "true" } > 1
    }
    fun summary(snapshot: String): String {
        val entries = decode(snapshot).getValue("progress")
        val stars = entries.filter { it.key.contains("stars_") }.sumOf { (it.value.toIntOrNull() ?: 0).coerceIn(0, 3) }
        val wallet = decode(snapshot).getValue("wallet").associate { it.key to it.value }
        return "$stars stars | ${wallet["coins"] ?: "600"} coins | ${wallet["gems"] ?: "25"} gems"
    }
}
