/*
 * المسار: app/src/main/java/com/example/brick_breaker_ball/app/DailyRewardApiClient.kt
 * الوظيفة: طلب حالة المكافأة واستلامها من الباك إند باستخدام Firebase ID Token.
 * الأمان: الوقت والمكافأة من السيرفر فقط؛ لا منح محلي عند فشل الاتصال.
 */
package com.example.brick_breaker_ball.app

import com.badlogic.gdx.Gdx
import com.example.brick_breaker_ball.BuildConfig
import com.google.firebase.auth.FirebaseAuth
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL
import java.util.concurrent.Executors

class DailyRewardApiClient {
    private val executor = Executors.newSingleThreadExecutor()
    private val origin = BuildConfig.REWARDS_API_BASE_URL.trimEnd('/')
    val enabled: Boolean get() = origin.startsWith("https://")

    fun status(callback: (ServerDailyStatus?, String?) -> Unit) = request(false, callback)
    fun claim(callback: (ServerDailyStatus?, String?) -> Unit) = request(true, callback)

    private fun deliver(callback: (ServerDailyStatus?, String?) -> Unit, status: ServerDailyStatus?, error: String?) {
        // The LibGDX game state must only be mutated on its rendering thread.
        Gdx.app?.postRunnable { callback(status, error) }
    }

    private fun request(claim: Boolean, callback: (ServerDailyStatus?, String?) -> Unit) {
        val operation = if (claim) "claim" else "status"
        if (!enabled) {
            QaDiagnosticsOverlay.reportReward("${operation}_server_not_configured")
            deliver(callback, null, "Daily rewards server is not configured")
            return
        }
        val user = FirebaseAuth.getInstance().currentUser
        if (user == null) {
            QaDiagnosticsOverlay.reportReward("${operation}_no_firebase_user")
            deliver(callback, null, "Sign in to Play Games, then retry")
            return
        }
        QaDiagnosticsOverlay.reportReward("${operation}_getting_id_token")
        user.getIdToken(false).addOnCompleteListener { task ->
            val token = if (task.isSuccessful) task.result?.token else null
            if (token.isNullOrBlank()) {
                QaDiagnosticsOverlay.reportReward("${operation}_id_token_failed", task.exception?.javaClass?.simpleName)
                deliver(callback, null, "Unable to verify your account. Retry.")
                return@addOnCompleteListener
            }
            executor.execute {
                var connection: HttpURLConnection? = null
                try {
                    QaDiagnosticsOverlay.reportReward("${operation}_requesting_server")
                    val path = if (claim) "/api/rewards/daily/claim" else "/api/rewards/daily/status"
                    connection = URL(origin + path).openConnection() as HttpURLConnection
                    connection.connectTimeout = 6_000
                    connection.readTimeout = 9_000
                    connection.useCaches = false
                    connection.requestMethod = if (claim) "POST" else "GET"
                    connection.setRequestProperty("Authorization", "Bearer $token")
                    connection.setRequestProperty("Accept", "application/json")
                    if (claim) {
                        connection.doOutput = true
                        connection.setRequestProperty("Content-Type", "application/json")
                        connection.outputStream.use { it.write("{}".toByteArray(Charsets.UTF_8)) }
                    }
                    val responseCode = connection.responseCode
                    if (responseCode != 200) {
                        QaDiagnosticsOverlay.reportReward("${operation}_http_failed", responseCode.toString())
                        deliver(callback, null, when (responseCode) {
                            401, 403 -> "Account verification expired. Sign in again."
                            else -> "Rewards server unavailable. Retry later."
                        })
                        // Non-200 responses have no trustworthy reward payload.
                    } else {
                        val body = connection.inputStream.bufferedReader().use { it.readText() }
                        val json = JSONObject(body)
                        check(json.optBoolean("ok"))
                        val award = json.optJSONObject("lastReward")?.let { item ->
                            ServerDailyAward(item.getString("id"), item.getInt("coins"), item.getInt("gems"),
                                item.getInt("streak"), item.getLong("calendarDay"))
                        }
                        val status = ServerDailyStatus(json.getLong("serverDay"), json.getInt("dayToClaim"), json.getBoolean("claimedToday"),
                            json.getInt("completedDays"), award)
                        QaDiagnosticsOverlay.reportReward("${operation}_ready")
                        deliver(callback, status, null)
                    }
                } catch (error: Exception) {
                    QaDiagnosticsOverlay.reportReward("${operation}_request_failed", error.javaClass.simpleName)
                    deliver(callback, null, "Connection failed. Check internet and retry.")
                } finally {
                    connection?.disconnect()
                }
            }
        }
    }
    fun close() = executor.shutdownNow()
}
