package com.example.brick_breaker_ball.app

import com.example.brick_breaker_ball.BuildConfig
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL
import java.util.concurrent.Executors

/** Exchanges a single-use Play Games code for a Firebase token on the trusted server. */
class AccountServerHttpException(val statusCode: Int) :
    IllegalStateException("Account server rejected Play Games code ($statusCode)")

class PlayGamesBridgeClient {
    private val executor = Executors.newSingleThreadExecutor()
    private val origin = BuildConfig.REWARDS_API_BASE_URL.trimEnd('/')

    fun exchange(authCode: String, callback: (String?, Exception?) -> Unit) {
        if (!origin.startsWith("https://")) {
            QaDiagnosticsOverlay.reportAccount("account_server_not_configured")
            callback(null, IllegalStateException("Account server is not configured"))
            return
        }
        executor.execute {
            var connection: HttpURLConnection? = null
            var responseReceived = false
            try {
                QaDiagnosticsOverlay.reportAccount("account_server_request")
                connection = URL("$origin/api/auth/play-games").openConnection() as HttpURLConnection
                connection.requestMethod = "POST"
                connection.connectTimeout = 6_000
                connection.readTimeout = 12_000
                connection.useCaches = false
                connection.doOutput = true
                connection.setRequestProperty("Content-Type", "application/json")
                connection.setRequestProperty("Accept", "application/json")
                connection.outputStream.use { stream ->
                    stream.write(JSONObject().put("authCode", authCode).toString().toByteArray(Charsets.UTF_8))
                }
                val responseCode = connection.responseCode
                responseReceived = true
                if (responseCode != HttpURLConnection.HTTP_OK) {
                    QaDiagnosticsOverlay.reportAccount("account_server_http_failed", responseCode.toString())
                    throw AccountServerHttpException(responseCode)
                }
                val response = JSONObject(connection.inputStream.bufferedReader().use { it.readText() })
                val customToken = response.optString("firebaseCustomToken")
                if (!response.optBoolean("ok") || customToken.isBlank()) {
                    QaDiagnosticsOverlay.reportAccount("account_server_invalid_response")
                    throw IllegalStateException("Account server returned no Firebase token")
                }
                QaDiagnosticsOverlay.reportAccount("account_server_ready")
                callback(customToken, null)
            } catch (error: Exception) {
                if (!responseReceived) {
                    QaDiagnosticsOverlay.reportAccount("account_server_request_failed", error.javaClass.simpleName)
                }
                callback(null, error)
            } finally {
                connection?.disconnect()
            }
        }
    }

    fun close() = executor.shutdownNow()
}
