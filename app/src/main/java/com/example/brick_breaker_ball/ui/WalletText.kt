package com.example.brick_breaker_ball.ui

import com.badlogic.gdx.graphics.Color
import com.badlogic.gdx.graphics.g2d.SpriteBatch
import com.badlogic.gdx.math.Rectangle
import com.example.brick_breaker_ball.assets.GameAssets
import java.util.Locale

/** Short wallet labels; purchases and saved balances keep their exact integer values. */
internal object WalletText {
    fun compact(value: Int): String {
        val amount = value.coerceAtLeast(0).toLong()
        if (amount < 10_000L) return amount.toString()
        val (unit, suffix) = when {
            amount >= 1_000_000_000L -> 1_000_000_000L to "B"
            amount >= 1_000_000L -> 1_000_000L to "M"
            else -> 1_000L to "K"
        }
        val tenths = amount * 10L / unit
        val whole = tenths / 10L
        val fraction = tenths % 10L
        return if (whole >= 100L || fraction == 0L) "$whole$suffix" else "$whole.$fraction$suffix"
    }

    fun full(value: Int): String = String.format(Locale.US, "%,d", value.coerceAtLeast(0))
}

/** A temporary, non-modal balance peek keeps + buttons and navigation available. */
internal class WalletBalancePeek {
    private var showCoins = false
    private var timeLeft = 0f
    private var elapsed = 0f

    fun show(coins: Boolean) {
        showCoins = coins
        timeLeft = 4f
        elapsed = 0f
    }

    fun update(delta: Float) {
        val step = delta.coerceIn(0f, .1f)
        timeLeft = (timeLeft - step).coerceAtLeast(0f)
        elapsed += step
    }

    fun draw(batch: SpriteBatch, assets: GameAssets, bounds: Rectangle,
             coins: Int, gems: Int, reduceMotion: Boolean) {
        if (timeLeft <= 0f) return
        val reveal = if (reduceMotion) 1f else (elapsed / .14f).coerceIn(0f, 1f)
        val rect = Rectangle(bounds.x, bounds.y + (1f - reveal) * 12f, bounds.width, bounds.height)
        assets.uiRenderer.drawGradientBorderPanel(batch, rect, ForgeUiRenderer.GradientStyle.NEUTRAL, 3f)
        batch.color = Color.WHITE
        val side = rect.height * .62f
        val icon = if (showCoins) assets.currencyCoinIcon else assets.currencyGemIcon
        batch.draw(icon, rect.x + 15f, rect.y + (rect.height - side) / 2f, side, side)
        val text = "${if (showCoins) "COINS" else "GEMS"}: ${WalletText.full(if (showCoins) coins else gems)}"
        VisualGameUi.number(batch, assets, text,
            Rectangle(rect.x + side + 25f, rect.y + rect.height * .14f,
                rect.width - side - 40f, rect.height * .72f), .72f)
    }
}
