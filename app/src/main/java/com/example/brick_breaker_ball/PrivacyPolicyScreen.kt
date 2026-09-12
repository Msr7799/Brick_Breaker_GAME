package com.example.brick_breaker_ball

import com.badlogic.gdx.Gdx
import com.badlogic.gdx.Input
import com.badlogic.gdx.graphics.Color
import com.badlogic.gdx.graphics.GL20
import com.badlogic.gdx.graphics.g2d.GlyphLayout
import com.badlogic.gdx.math.Rectangle
import com.badlogic.gdx.math.Vector3
import com.badlogic.gdx.utils.Align
import kotlin.math.abs

/** In-game copy of the same privacy policy that should be published at BuildConfig.PRIVACY_POLICY_URL. */
class PrivacyPolicyScreen(
    game: BrickBreakerGame,
    private val returnToPausedGame: Boolean = false,
) : ForgeScreen(game) {
    private companion object {
        const val PANEL_LEFT = 55f
        const val PANEL_RIGHT = 845f
        const val PANEL_BOTTOM = 245f
        const val PANEL_TOP = 1370f
        const val TEXT_LEFT = 92f
        const val TEXT_WIDTH = 716f
        const val TEXT_TOP = 1325f
        const val TEXT_BOTTOM = 285f
    }

    private val policyText = Gdx.files.internal("privacy/privacy_policy.txt").readString("UTF-8")
    private var scrollOffset = 0f
    private var touchActive = false
    private var touchMoved = false
    private var touchStartY = 0f
    private var touchLastY = 0f

    override fun render(delta: Float) {
        begin("world_2_crystal")
        title("PRIVACY POLICY", 1515f)

        val panel = Rectangle(PANEL_LEFT, PANEL_BOTTOM, PANEL_RIGHT - PANEL_LEFT, PANEL_TOP - PANEL_BOTTOM)
        game.assets.uiRenderer.drawGradientBorderPanel(
            batch,
            panel,
            ForgeUiRenderer.GradientStyle.NEUTRAL,
            4f,
        )

        val oldScaleX = game.assets.smallFont.data.scaleX
        val oldScaleY = game.assets.smallFont.data.scaleY
        game.assets.smallFont.data.setScale(.62f)
        val layout = GlyphLayout(
            game.assets.smallFont,
            policyText,
            ForgeUiPalette.textPrimary,
            TEXT_WIDTH,
            Align.left,
            true,
        )
        val visibleHeight = TEXT_TOP - TEXT_BOTTOM
        val maxScroll = (layout.height - visibleHeight + 40f).coerceAtLeast(0f)
        scrollOffset = scrollOffset.coerceIn(0f, maxScroll)

        beginTextClip()
        game.assets.smallFont.color = ForgeUiPalette.textPrimary
        game.assets.smallFont.draw(
            batch,
            policyText,
            TEXT_LEFT,
            TEXT_TOP + scrollOffset,
            TEXT_WIDTH,
            Align.left,
            true,
        )
        game.assets.smallFont.color = Color.WHITE
        endTextClip()
        game.assets.smallFont.data.setScale(oldScaleX, oldScaleY)

        if (maxScroll > 0f) {
            drawScrollTrack(maxScroll)
        }

        val onlineUrl = BuildConfig.PRIVACY_POLICY_URL.trim()
        val back: Rectangle
        val online: Rectangle?
        if (onlineUrl.isNotEmpty()) {
            back = button("BACK", 105f, 105f, 320f, 82f)
            online = button("OPEN ONLINE", 475f, 105f, 320f, 82f)
        } else {
            back = button("BACK", 250f, 105f, 400f, 82f)
            online = null
            game.assets.smallFont.color = ForgeUiPalette.textSecondary
            fittedText(
                game.assets.smallFont,
                "PUBLISH PRIVACY_POLICY.md AND SET privacyPolicyUrl BEFORE RELEASE",
                90f,
                218f,
                720f,
                .48f,
            )
            game.assets.smallFont.color = Color.WHITE
        }
        end()

        if (Gdx.input.isKeyJustPressed(Input.Keys.BACK)) {
            goBack()
            return
        }

        handleScroll(maxScroll)
        when {
            tapped(back) -> goBack()
            online != null && tapped(online) -> Gdx.net.openURI(onlineUrl)
        }
    }

    private fun beginTextClip() {
        batch.flush()
        val bottomLeft = Vector3(PANEL_LEFT + 12f, TEXT_BOTTOM, 0f)
        val topRight = Vector3(PANEL_RIGHT - 12f, TEXT_TOP + 12f, 0f)
        viewport.project(bottomLeft)
        viewport.project(topRight)
        Gdx.gl.glEnable(GL20.GL_SCISSOR_TEST)
        Gdx.gl.glScissor(
            bottomLeft.x.toInt(),
            bottomLeft.y.toInt(),
            (topRight.x - bottomLeft.x).toInt().coerceAtLeast(1),
            (topRight.y - bottomLeft.y).toInt().coerceAtLeast(1),
        )
    }

    private fun endTextClip() {
        batch.flush()
        Gdx.gl.glDisable(GL20.GL_SCISSOR_TEST)
    }

    private fun handleScroll(maxScroll: Float) {
        val touched = Gdx.input.isTouched
        if (Gdx.input.justTouched()) {
            val point = touchPoint()
            if (point.x in PANEL_LEFT..PANEL_RIGHT && point.y in PANEL_BOTTOM..PANEL_TOP) {
                touchActive = true
                touchMoved = false
                touchStartY = point.y
                touchLastY = point.y
            }
        }
        if (touchActive && touched) {
            val point = touchPoint()
            scrollOffset = (scrollOffset + point.y - touchLastY).coerceIn(0f, maxScroll)
            touchLastY = point.y
            if (abs(point.y - touchStartY) > 18f) touchMoved = true
        } else if (touchActive) {
            touchActive = false
        }
    }

    private fun drawScrollTrack(maxScroll: Float) {
        val trackX = 819f
        val trackY = TEXT_BOTTOM
        val trackHeight = TEXT_TOP - TEXT_BOTTOM
        val thumbHeight = (trackHeight * .20f).coerceAtLeast(88f)
        val travel = trackHeight - thumbHeight
        val ratio = if (maxScroll <= 0f) 0f else (scrollOffset / maxScroll).coerceIn(0f, 1f)
        val thumbY = trackY + travel * (1f - ratio)
        batch.color = Color(1f, 1f, 1f, .18f)
        batch.draw(game.assets.ui.findRegion("panel"), trackX, trackY, 8f, trackHeight)
        batch.color = ForgeUiPalette.primaryLight
        batch.draw(game.assets.ui.findRegion("panel"), trackX - 2f, thumbY, 12f, thumbHeight)
        batch.color = Color.WHITE
    }

    private fun goBack() {
        if (returnToPausedGame) game.setScreen(SettingsScreen(game, true)) else game.setScreen(SettingsScreen(game))
    }
}
