package com.example.brick_breaker_ball.economy

import android.content.Context
import android.os.Handler
import android.os.Looper
import com.example.brick_breaker_ball.BuildConfig
import com.google.firebase.auth.FirebaseAuth
import org.json.JSONArray
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL
import java.util.concurrent.Executors
import java.util.concurrent.atomic.AtomicBoolean

/** Additive SSV rollout: durable delivery journal, never blocks an SDK-earned reward on SSV. */
internal class RewardedSsvClient(context: Context, private val placement: String) {
    private val prefs = context.applicationContext.getSharedPreferences("bbb-rewarded-delivery-v1", Context.MODE_PRIVATE)
    private val origin = BuildConfig.REWARDS_API_BASE_URL.trimEnd('/')
    private val handler = Handler(Looper.getMainLooper())
    private var nextSyncAt = 0L
    private var syncCursor = 0
    data class Options(val userId: String, val customData: String)

    fun prepare(requestId: String, rewardContext: String, callback: (Options?) -> Unit) {
        val uid = currentUid()
        update(requestId) { put("uid", uid); put("placement", placement); put("context", rewardContext); put("createdAt", System.currentTimeMillis()) }
        val finished = AtomicBoolean(false)
        val finish: (Options?) -> Unit = { options ->
            if (finished.compareAndSet(false, true)) handler.post { callback(options) }
        }
        // A slow token/server cannot take away the player's existing rewarded-ad opportunity.
        handler.postDelayed({ finish(null) }, 7_000L)
        if (uid.isBlank() || !origin.startsWith("https://")) { finish(null); return }
        authenticated(uid) { token ->
            val result = post("opportunity", token, JSONObject().put("requestId", requestId)
                .put("placement", placement).put("context", rewardContext))
            val options = result?.takeIf { it.optString("mode") == "observe" && it.optString("userId") == uid }
                ?.let { Options(uid, it.getString("customData")) }
            if (options != null) update(requestId) { put("serverPrepared", true) }
            finish(options?.takeIf { currentUid() == uid })
        }
    }

    fun earned(requestId: String) {
        update(requestId) { put("sdkEarned", true) }
        sync(force = true)
    }

    fun playerId(): String = currentUid()

    fun acknowledge(requestId: String, outcome: String) {
        update(requestId) { put("delivered", true); put("outcome", outcome) }
        sync(force = true)
    }

    fun pending(rewardContext: String): List<String> {
        sync()
        val uid = currentUid()
        return records().filter { (_, row) ->
            row.optString("uid") == uid && row.optString("placement") == placement &&
                row.optString("context") == rewardContext && !row.optBoolean("delivered") &&
                (row.optBoolean("sdkEarned") || row.optBoolean("verified"))
        }.map { it.first }
    }

    private fun sync(force: Boolean = false) {
        val now = android.os.SystemClock.elapsedRealtime()
        synchronized(lock) {
            if (!force && now < nextSyncAt) return
            nextSyncAt = now + 15_000L
        }
        val uid = currentUid()
        if (uid.isBlank() || !origin.startsWith("https://")) return
        val candidates = records().filter { it.second.optString("uid") == uid &&
            it.second.optString("placement") == placement && it.second.optBoolean("serverPrepared") &&
            (!it.second.optBoolean("deliveredSynced") || !it.second.optBoolean("verified")) }
        if (candidates.isEmpty()) return
        // Rotate batches: abandoned, unearned opportunities must not starve newer earned rewards.
        val rows = synchronized(lock) {
            val batch = List(minOf(20, candidates.size)) { candidates[(syncCursor + it) % candidates.size] }
            syncCursor = (syncCursor + batch.size) % candidates.size
            batch
        }
        authenticated(uid) { token ->
            for ((id, row) in rows) {
                if (row.optBoolean("sdkEarned") && !row.optBoolean("earnedSynced")) {
                    if (post("delivery", token, JSONObject().put("requestId", id).put("state", "sdk_earned")) != null)
                        update(id) { put("earnedSynced", true) }
                }
                if (row.optBoolean("delivered") && !row.optBoolean("deliveredSynced")) {
                    if (post("delivery", token, JSONObject().put("requestId", id).put("state", "delivered")
                            .put("outcome", row.optString("outcome", "already_applied"))) != null)
                        update(id) { put("deliveredSynced", true) }
                }
            }
            val result = post("status", token, JSONObject().put("requestIds", JSONArray(rows.map { it.first })))
            val statuses = result?.optJSONArray("opportunities") ?: return@authenticated
            for (i in 0 until statuses.length()) {
                val status = statuses.getJSONObject(i)
                val id = status.getString("requestId")
                val local = rows.firstOrNull { it.first == id }?.second ?: continue
                if (status.optString("placement") != placement || status.optString("context") != local.optString("context")) continue
                if (status.optBoolean("verified")) update(id) { put("verified", true) }
                // Remote delivery telemetry cannot grant locally or suppress a still-undelivered reward.
            }
        }
    }

    private fun authenticated(uid: String, action: (String) -> Unit) {
        val user = FirebaseAuth.getInstance().currentUser ?: return
        if (user.uid != uid) return
        user.getIdToken(false).addOnCompleteListener { task ->
            val token = if (task.isSuccessful) task.result?.token else null
            if (token.isNullOrBlank() || currentUid() != uid) return@addOnCompleteListener
            io.execute { if (currentUid() == uid) runCatching { action(token) } }
        }
    }

    private fun post(endpoint: String, token: String, body: JSONObject): JSONObject? {
        var connection: HttpURLConnection? = null
        return try {
            connection = URL("$origin/api/rewards/ads/$endpoint").openConnection() as HttpURLConnection
            connection.instanceFollowRedirects = false
            connection.connectTimeout = 3_000
            connection.readTimeout = 3_000
            connection.requestMethod = "POST"
            connection.useCaches = false
            connection.doOutput = true
            connection.setRequestProperty("Authorization", "Bearer $token")
            connection.setRequestProperty("Content-Type", "application/json")
            connection.outputStream.use { it.write(body.toString().toByteArray(Charsets.UTF_8)) }
            if (connection.responseCode != 200) null else {
                val text = connection.inputStream.bufferedReader().use { it.readText() }
                JSONObject(text).takeIf { it.optBoolean("ok") }
            }
        } catch (_: Exception) { null } finally { connection?.disconnect() }
    }

    private fun currentUid(): String = FirebaseAuth.getInstance().currentUser?.uid.orEmpty()
    private fun records(): List<Pair<String, JSONObject>> = synchronized(lock) {
        prefs.all.mapNotNull { (key, value) ->
            if (value !is String) null else runCatching { key to JSONObject(value) }.getOrNull()
        }
    }
    private fun update(id: String, apply: JSONObject.() -> Unit) = synchronized(lock) {
        val row = runCatching { JSONObject(prefs.getString(id, "{}") ?: "{}") }.getOrDefault(JSONObject())
        row.apply()
        if (!prefs.edit().putString(id, row.toString()).commit()) {
            // Keep the pre-SSV SDK path working if device storage fails; recovery then needs retesting.
            android.util.Log.e("BBB-RewardedSSV", "Reward journal persistence failed")
        }
    }
    companion object {
        private val lock = Any()
        private val io = Executors.newSingleThreadExecutor()
    }
}
