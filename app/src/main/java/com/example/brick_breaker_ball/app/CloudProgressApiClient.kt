package com.example.brick_breaker_ball.app

import com.badlogic.gdx.Gdx
import com.example.brick_breaker_ball.BuildConfig
import com.google.firebase.auth.FirebaseAuth
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL
import java.io.ByteArrayOutputStream
import java.util.concurrent.Executors

/** Release uses the same Firebase ID token and trusted origin as daily rewards. */
class CloudProgressApiClient {
    private val executor = Executors.newSingleThreadExecutor()
    private val origin = BuildConfig.REWARDS_API_BASE_URL.trimEnd('/')
    val enabled: Boolean get() = origin.startsWith("https://")

    fun read(accountId: String, callback: (CloudProgressResult) -> Unit) = request(accountId, null, callback)
    fun write(accountId: String, revision: Long, saveId: String, snapshot: String, callback: (CloudProgressResult) -> Unit) =
        request(accountId, JSONObject().put("baseRevision", revision).put("saveId", saveId).put("snapshot", JSONObject(snapshot)).toString(), callback)

    private fun deliver(accountId: String, result: CloudProgressResult, callback: (CloudProgressResult) -> Unit) {
        Gdx.app?.postRunnable {
            if (FirebaseAuth.getInstance().currentUser?.uid == accountId) callback(result)
            else callback(CloudProgressResult(error = "Account changed. Retry cloud sync."))
        }
    }

    private fun request(accountId: String, body: String?, callback: (CloudProgressResult) -> Unit) {
        val user = FirebaseAuth.getInstance().currentUser
        if (!enabled || user == null || user.uid != accountId) {
            deliver(accountId, CloudProgressResult(error = "Connect your Play Games account to sync progress."), callback)
            return
        }
        user.getIdToken(false).addOnCompleteListener { task ->
            val token = if (task.isSuccessful) task.result?.token else null
            if (token.isNullOrBlank()) {
                deliver(accountId, CloudProgressResult(error = "Account verification failed. Retry sync."), callback)
                return@addOnCompleteListener
            }
            executor.execute {
                var connection: HttpURLConnection? = null
                try {
                    check(FirebaseAuth.getInstance().currentUser?.uid == accountId)
                    connection = URL("$origin/api/progress").openConnection() as HttpURLConnection
                    connection.connectTimeout = 6_000
                    connection.readTimeout = 9_000
                    connection.useCaches = false
                    connection.instanceFollowRedirects = false
                    connection.requestMethod = if (body == null) "GET" else "PUT"
                    connection.setRequestProperty("Authorization", "Bearer $token")
                    connection.setRequestProperty("Accept", "application/json")
                    if (body != null) {
                        val bytes = body.toByteArray(Charsets.UTF_8)
                        require(bytes.size <= 1024 * 1024)
                        connection.doOutput = true
                        connection.setRequestProperty("Content-Type", "application/json")
                        connection.setFixedLengthStreamingMode(bytes.size)
                        connection.outputStream.use { it.write(bytes) }
                    }
                    val code = connection.responseCode
                    if (code != 200 && code != 409) {
                        deliver(accountId, CloudProgressResult(error = if (code == 401) "Sign in to Play Games again." else "Cloud unavailable. Progress stays on this device."), callback)
                    } else {
                        val stream = requireNotNull(if (code == 409) connection.errorStream else connection.inputStream)
                        val bytes = stream.use { input ->
                            val out = ByteArrayOutputStream()
                            val buffer = ByteArray(8192)
                            while (true) {
                                val size = input.read(buffer)
                                if (size < 0) break
                                require(out.size() + size <= 1024 * 1024 + 2048)
                                out.write(buffer, 0, size)
                            }
                            out.toByteArray()
                        }
                        val json = JSONObject(String(bytes, Charsets.UTF_8))
                        check(code == 409 || json.optBoolean("ok"))
                        val snapshot = json.optJSONObject("snapshot")?.toString()
                        val record = CloudProgressRecord(json.getLong("revision"), json.getString("saveId"), snapshot, json.getLong("updatedAt"))
                        deliver(accountId, CloudProgressResult(record, conflict = code == 409), callback)
                    }
                } catch (_: Exception) {
                    deliver(accountId, CloudProgressResult(error = "Cloud unavailable. Progress stays on this device."), callback)
                } finally { connection?.disconnect() }
            }
        }
    }
    fun close() = executor.shutdownNow()
}
