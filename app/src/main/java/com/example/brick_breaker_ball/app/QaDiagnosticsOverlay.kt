/*
 * المسار: app/src/main/java/com/example/brick_breaker_ball/app/QaDiagnosticsOverlay.kt
 * الوظيفة: عرض معلومات تشخيص QA فوق اللعبة.
 * الحسابات: مواضع النصوص مشتقة من حدود الشاشة.
 */

package com.example.brick_breaker_ball.app

import com.badlogic.gdx.Gdx
import com.badlogic.gdx.Input
import com.badlogic.gdx.Screen
import com.badlogic.gdx.graphics.Color
import com.badlogic.gdx.graphics.OrthographicCamera
import com.badlogic.gdx.graphics.g2d.SpriteBatch
import com.badlogic.gdx.utils.Align
import com.badlogic.gdx.utils.viewport.ScreenViewport
import com.example.brick_breaker_ball.BrickBreakerGame
import com.example.brick_breaker_ball.BuildConfig
import java.util.Locale

/**
 * Debug/QA-only runtime HUD. Toggle with F8 on desktop/scrcpy keyboard.
 * It is completely inert in release builds.
 */
class QaDiagnosticsOverlay(private val game: BrickBreakerGame) {
    companion object {
        @Volatile private var accountState = "not_started"
        @Volatile private var rewardState = "not_requested"

        fun reportAccount(stage: String, code: String? = null) {
            if (!BuildConfig.QA_DIAGNOSTICS) return
            accountState = stage + (code?.let { " ($it)" } ?: "")
            android.util.Log.i("BrickBreakerAuth", "QA account stage=$accountState")
        }

        fun reportReward(stage: String, code: String? = null) {
            if (!BuildConfig.QA_DIAGNOSTICS) return
            rewardState = stage + (code?.let { " ($it)" } ?: "")
            android.util.Log.i("BrickBreakerDaily", "QA daily reward stage=$rewardState")
        }
    }

    private val camera = OrthographicCamera()
    private val viewport = ScreenViewport(camera)
    private val batch = SpriteBatch()
    private var enabled = false
    private var smoothedFrameMs = 16.6f
    private var worstFrameMs = 0f
    private var logCooldown = 0f

    fun render(screen: Screen?) {
        val screenName = screen?.javaClass?.simpleName ?: "NO_SCREEN"
        if (!BuildConfig.QA_DIAGNOSTICS) return
        if (Gdx.input.isKeyJustPressed(Input.Keys.F8)) enabled = !enabled

        val dt = Gdx.graphics.deltaTime.coerceIn(0f, .25f)
        val frameMs = dt * 1000f
        smoothedFrameMs += (frameMs - smoothedFrameMs) * .08f
        worstFrameMs = maxOf(worstFrameMs * .995f, frameMs)
        logCooldown = (logCooldown - dt).coerceAtLeast(0f)
        if (frameMs > 34f && logCooldown <= 0f) {
            Gdx.app.log("BB-Perf", "Frame spike ${"%.1f".format(Locale.ROOT, frameMs)} ms on $screenName")
            logCooldown = 1f
        }
        if (!enabled) return

        viewport.update(Gdx.graphics.width, Gdx.graphics.height, true)
        camera.position.set(viewport.worldWidth / 2f, viewport.worldHeight / 2f, 0f)
        camera.update()
        viewport.apply()
        batch.projectionMatrix = camera.combined
        batch.begin()

        val w = 360f
        val h = 208f
        val x = 12f
        val y = viewport.worldHeight - h - 12f
        batch.color = Color(0f, 0f, 0f, .78f)
        batch.draw(game.assets.solidUiRegion, x, y, w, h)
        batch.color = Color.WHITE

        val runtime = Runtime.getRuntime()
        val usedMb = (runtime.totalMemory() - runtime.freeMemory()) / (1024 * 1024)
        val text = buildString {
            append("QA  ").append(screenName).append('\n')
            append("FPS ").append(Gdx.graphics.framesPerSecond)
            append("   ").append("%.1f".format(Locale.ROOT, smoothedFrameMs)).append(" ms")
            append("   worst ").append("%.1f".format(Locale.ROOT, worstFrameMs)).append(" ms\n")
            append("RAM ").append(usedMb).append(" MB")
            append("   worldTex ").append(game.assets.residentWorldCardCount())
            append("   cards ").append(game.assets.residentWorldCardCount()).append('\n')
            append("Account ").append(accountState).append('\n')
            append("Daily ").append(rewardState).append('\n')
            append("F8 hide/show")
        }
        game.assets.smallFont.color = Color.WHITE
        game.assets.smallFont.draw(batch, text, x + 14f, y + h - 18f, w - 28f, Align.left, true)
        batch.end()
    }

    fun dispose() = batch.dispose()
}
