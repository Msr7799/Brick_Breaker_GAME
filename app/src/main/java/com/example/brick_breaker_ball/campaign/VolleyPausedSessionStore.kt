/*
 * المسار: app/src/main/java/com/example/brick_breaker_ball/campaign/VolleyPausedSessionStore.kt
 * الوظيفة: حفظ جلسة Volley واستعادتها مع نسخة احتياطية وفحص سلامة البيانات.
 * الحسابات: يحافظ على صيغة Java Serialization القديمة لتوافق الجلسات المحفوظة.
 */

package com.example.brick_breaker_ball.campaign

import com.badlogic.gdx.Gdx
import com.badlogic.gdx.Preferences
import com.badlogic.gdx.utils.Base64Coder
import com.example.brick_breaker_ball.gameplay.VolleySession
import com.example.brick_breaker_ball.gameplay.VolleySessionSnapshot
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.io.ObjectInputStream
import java.io.ObjectOutputStream
import java.security.MessageDigest

/**
 * Keeps the old on-device format readable. Each new save checksums its payload and retains the
 * previous verified save, so an interrupted/corrupt write does not automatically erase progress.
 */
class VolleyPausedSessionStore(
    private val prefs: Preferences = Gdx.app.getPreferences("brickbreakerball-volley-paused-v1")
) {
    companion object {
        private const val VERSION = 2
        private const val MAX_SNAPSHOT_BYTES = 16 * 1024 * 1024
        private const val VALID = "valid"
        private const val SNAPSHOT = "snapshot"
        private const val SNAPSHOT_HASH = "snapshot_sha256"
        private const val LEVEL = "level"
        private const val REVIVES = "rewarded_revives_used"
    }

    fun hasPausedGame(): Boolean = prefs.getBoolean(VALID, false) &&
        (prefs.getInteger("version", 0) == VERSION || prefs.getBoolean("backup_valid", false))

    /** Read a level for the menu without requiring the whole snapshot to deserialize. */
    fun pausedLevelId(): Int? {
        if (!hasPausedGame()) return null
        val level = prefs.getInteger(LEVEL, 0).takeIf { it > 0 }
        return level ?: prefs.getInteger("backup_level", 0).takeIf { it > 0 }
    }

    fun save(levelId: Int, snapshot: VolleySessionSnapshot, rewardedRevivesUsed: Int = 0) {
        require(levelId > 0 && snapshot.version == VERSION)
        snapshot.evolution?.requireValid()
        require(snapshot.randomState == null || snapshot.randomState in 0..281474976710655L)
        val bytes = ByteArrayOutputStream().use { output ->
            ObjectOutputStream(output).use { it.writeObject(snapshot) }
            output.toByteArray()
        }
        require(bytes.size in 1..MAX_SNAPSHOT_BYTES) { "Paused session is too large" }
        val encoded = Base64Coder.encode(bytes).concatToString()
        val hash = sha256(bytes)

        // Preserve only a verified earlier save. Never replace a good backup with damaged data.
        if (loadRecord(SNAPSHOT, SNAPSHOT_HASH, LEVEL) != null) {
            prefs.putString("backup_snapshot", prefs.getString(SNAPSHOT, ""))
                .putString("backup_sha256", prefs.getString(SNAPSHOT_HASH, ""))
                .putInteger("backup_level", prefs.getInteger(LEVEL, 0))
                .putInteger("backup_revives", prefs.getInteger(REVIVES, 0))
                .putBoolean("backup_valid", true)
        }
        prefs.putInteger("version", VERSION)
            .putInteger(LEVEL, levelId)
            .putInteger(REVIVES, rewardedRevivesUsed.coerceIn(0, VolleySession.MAX_REWARDED_REVIVES))
            .putString(SNAPSHOT, encoded)
            .putString(SNAPSHOT_HASH, hash)
            .putBoolean(VALID, true)
            .flush()
    }

    fun restore(): Pair<LevelDefinition, VolleySessionSnapshot>? {
        if (!hasPausedGame()) return null
        loadRecord(SNAPSHOT, SNAPSHOT_HASH, LEVEL)?.let { return it }
        val recovered = if (prefs.getBoolean("backup_valid", false)) {
            loadRecord("backup_snapshot", "backup_sha256", "backup_level")
        } else null
        if (recovered != null) {
            // Restore the working copy of the older checkpoint after a partially written save.
            prefs.putString(SNAPSHOT, prefs.getString("backup_snapshot", ""))
                .putString(SNAPSHOT_HASH, prefs.getString("backup_sha256", ""))
                .putInteger(LEVEL, prefs.getInteger("backup_level", 0))
                .putInteger(REVIVES, prefs.getInteger("backup_revives", 0))
                .putInteger("version", VERSION)
                .putBoolean(VALID, true)
                .flush()
            return recovered
        }
        // Intentionally do NOT clear bad records here: they may be recoverable by a future update.
        return null
    }

    private fun loadRecord(
        snapshotKey: String,
        hashKey: String,
        levelKey: String
    ): Pair<LevelDefinition, VolleySessionSnapshot>? = runCatching {
        val encoded = prefs.getString(snapshotKey, "")
        require(encoded.isNotBlank() && encoded.length <= MAX_SNAPSHOT_BYTES * 2)
        val bytes = Base64Coder.decode(encoded)
        require(bytes.size in 1..MAX_SNAPSHOT_BYTES)
        val storedHash = prefs.getString(hashKey, "")
        // Older version-2 saves had no checksum: keep them backward compatible.
        require(storedHash.isBlank() || storedHash == sha256(bytes))
        val snapshot = ObjectInputStream(ByteArrayInputStream(bytes)).use {
            it.readObject() as VolleySessionSnapshot
        }
        require(snapshot.version == VERSION)
        snapshot.evolution?.requireValid()
        require(snapshot.randomState == null || snapshot.randomState in 0..281474976710655L)
        val levelId = prefs.getInteger(levelKey, 0)
        require(levelId > 0)
        VolleyStageRepository.levelDefinition(levelId) to snapshot
    }.getOrNull()

    private fun sha256(bytes: ByteArray): String = MessageDigest.getInstance("SHA-256")
        .digest(bytes).joinToString("") { "%02x".format(it.toInt() and 0xff) }

    fun rewardedRevivesUsed(): Int = prefs.getInteger(REVIVES, 0)
        .coerceIn(0, VolleySession.MAX_REWARDED_REVIVES)

    fun clear() {
        prefs.clear()
        prefs.flush()
    }
}
