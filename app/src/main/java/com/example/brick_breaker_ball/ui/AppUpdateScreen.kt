package com.example.brick_breaker_ball.ui

import com.badlogic.gdx.Gdx
import com.badlogic.gdx.Input
import com.badlogic.gdx.graphics.Color
import com.badlogic.gdx.graphics.GL20
import com.badlogic.gdx.math.Rectangle
import com.example.brick_breaker_ball.BrickBreakerGame
import com.example.brick_breaker_ball.BuildConfig

/** Optional launch screen shown only when Google Play reports a newer build. */
class AppUpdateScreen(game: BrickBreakerGame, private val availableVersionCode: Int) : ForgeScreen(game) {
    private var leaving = false

    private fun continueToMenu() {
        if (leaving) return
        leaving = true
        game.openFirstEntryOrMenu()
    }

    override fun render(delta: Float) {
        if (!leaving && game.platformActions.appUpdateCheckComplete &&
            game.platformActions.availableAppUpdateVersionCode == null
        ) {
            continueToMenu()
            return
        }

        Gdx.gl.glClearColor(0f, 0f, 0f, 1f)
        Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT)
        viewport.apply()
        batch.projectionMatrix = camera.combined
        batch.begin()
        batch.color = Color.WHITE
        drawFullscreen(game.assets.worldFallbackBg(1))
        batch.setColor(0.01f, 0.02f, 0.04f, .72f)
        batch.draw(game.assets.solidUiRegion, visibleLeft, visibleBottom, visibleWidth, visibleHeight)
        batch.color = Color.WHITE

        val panel = Rectangle(72f, 410f, 756f, 780f)
        game.assets.uiRenderer.drawGradientBorderPanel(
            batch, panel, ForgeUiRenderer.GradientStyle.PRIMARY, 7f
        )

        val titleColor = game.assets.titleFont.color.cpy()
        game.assets.titleFont.color = ForgeUiPalette.textPrimary
        fittedText(game.assets.titleFont, "UPDATE AVAILABLE", 115f, 1080f, 670f, .88f, 100f)
        game.assets.titleFont.color = titleColor

        val bodyColor = game.assets.bodyFont.color.cpy()
        game.assets.bodyFont.color = ForgeUiPalette.textPrimary
        fittedText(
            game.assets.bodyFont,
            "CURRENT VERSION  ${BuildConfig.VERSION_NAME}",
            135f, 960f, 630f, .76f
        )
        fittedText(
            game.assets.bodyFont,
            "NEW BUILD $availableVersionCode IS READY ON GOOGLE PLAY",
            115f, 894f, 670f, .68f
        )
        fittedText(
            game.assets.bodyFont,
            "You can keep playing or update now.",
            130f, 825f, 640f, .70f
        )
        game.assets.bodyFont.color = bodyColor

        val continueButton = button(
            "CONTINUE OLD VERSION", 150f, 650f, 600f, 112f,
            ForgeUiRenderer.GradientStyle.NEUTRAL
        )
        val updateButton = button(
            "UPDATE NOW", 150f, 485f, 600f, 112f,
            ForgeUiRenderer.GradientStyle.PRIMARY
        )
        end()

        if (leaving) return
        when {
            Gdx.input.isKeyJustPressed(Input.Keys.BACK) ||
                Gdx.input.isKeyJustPressed(Input.Keys.ESCAPE) || tapped(continueButton) -> continueToMenu()
            tapped(updateButton) -> game.platformActions.startAppUpdate()
        }
    }
}
