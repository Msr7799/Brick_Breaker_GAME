package com.example.brick_breaker_ball.ui

import com.badlogic.gdx.Gdx
import com.badlogic.gdx.Input
import com.badlogic.gdx.graphics.Color
import com.badlogic.gdx.math.Rectangle
import com.example.brick_breaker_ball.BrickBreakerGame

/** Resolve the account checkpoint before the first story/menu, without requiring a connection. */
class CloudSaveScreen(game: BrickBreakerGame, private val onClose: (() -> Unit)? = null) : ForgeScreen(game) {
    private var leaving = false
    private var summaryTimer = 2f
    private var localSummary = ""
    private var remoteSummary = ""
    private fun leave() {
        if (leaving) return
        leaving = true
        if (onClose != null) onClose.invoke() else game.openFirstEntryOrMenu()
    }
    override fun render(delta: Float) {
        val sync = game.cloudProgress
        if (onClose == null && sync.accountReady && sync.conflict == null) { leave(); return }
        beginStaticBackground(com.badlogic.gdx.graphics.g2d.TextureRegion(game.assets.splash), .74f)
        val panel = Rectangle(65f, 395f, 770f, 900f)
        game.assets.uiRenderer.drawGradientBorderPanel(batch, panel, ForgeUiRenderer.GradientStyle.PRIMARY, 7f)
        fittedText(game.assets.titleFont, if (sync.conflict != null) "CHOOSE YOUR SAVE" else "CLOUD SAVE", 105f, 1180f, 690f, .82f)
        fittedText(game.assets.bodyFont, sync.message, 100f, 1080f, 700f, .56f)
        val conflict = sync.conflict != null
        val first: Rectangle
        val second: Rectangle
        if (conflict) {
            summaryTimer += delta
            if (summaryTimer >= 1f) {
                summaryTimer = 0f
                localSummary = sync.localSummary()
                remoteSummary = sync.cloudSummary()
            }
            fittedText(game.assets.bodyFont, "THIS DEVICE: $localSummary", 120f, 970f, 660f, .67f)
            fittedText(game.assets.bodyFont, "CLOUD: $remoteSummary", 120f, 890f, 660f, .67f)
            fittedText(game.assets.smallFont, "Wallet and items stay together with the save you choose.", 100f, 815f, 700f, .47f)
            first = button("USE CLOUD SAVE", 135f, 650f, 630f, 105f, ForgeUiRenderer.GradientStyle.PRIMARY)
            second = button("KEEP THIS DEVICE", 135f, 495f, 630f, 105f, ForgeUiRenderer.GradientStyle.NEUTRAL)
        } else {
            fittedText(game.assets.bodyFont, "Your stages, balls, orbs and wallet follow your account.", 100f, 950f, 700f, .57f)
            fittedText(game.assets.smallFont, "Offline? Progress stays on this device until it syncs.", 100f, 870f, 700f, .48f)
            first = button("RETRY SYNC", 135f, 650f, 630f, 105f, ForgeUiRenderer.GradientStyle.PRIMARY)
            second = button(if (onClose != null) "BACK" else "CONTINUE OFFLINE", 135f, 495f, 630f, 105f, ForgeUiRenderer.GradientStyle.NEUTRAL)
        }
        batch.color = Color.WHITE
        end()
        if (leaving) return
        if (tapped(first)) {
            if (conflict) sync.chooseCloud() else {
                sync.retry()
                game.platformActions.connectPlayerAccount()
            }
        }
        else if (tapped(second)) { if (conflict) sync.chooseDevice() else leave() }
        else if (!conflict && (Gdx.input.isKeyJustPressed(Input.Keys.BACK) || Gdx.input.isKeyJustPressed(Input.Keys.ESCAPE))) leave()
        // Do not auto-timeout a restore: the player can explicitly continue offline.
    }
}
