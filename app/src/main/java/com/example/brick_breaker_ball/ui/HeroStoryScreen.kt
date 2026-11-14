package com.example.brick_breaker_ball.ui

import com.badlogic.gdx.Gdx
import com.badlogic.gdx.Input
import com.badlogic.gdx.graphics.Color
import com.badlogic.gdx.math.Rectangle
import com.example.brick_breaker_ball.BrickBreakerGame
import com.example.brick_breaker_ball.gameplay.VolleyGeneratorFeature

/** One-time prologue after the splash, before the first playable stage. */
class HeroStoryScreen(game: BrickBreakerGame) : ForgeScreen(game) {
    private val firstStage = VolleyGeneratorFeature.stageForLevel(1)
    private val pages = listOf(
        listOf("I'M EMBER, YOUR GUIDE.", "NINE WORLDS ARE WAITING FOR US!"),
        listOf("BREAK BRICKS AND CLEAR EACH WORLD.", "A FINAL BOSS GUARDS EVERY EXIT."),
        listOf("GATHER BALLS, ITEMS AND ORBS.", "LET'S BEGIN IN THE FIRST WORLD!")
    )
    private var page = 0
    private var leaving = false
    private var finishAfterExit = false
    private val guideAnimation = GuideAnimation()

    private fun beginJourney() {
        if (leaving) return
        leaving = true
        game.heroGuideProgress.completeStory()
        game.play(1)
    }

    override fun render(delta: Float) {
        if (!game.uiMotion.active) guideAnimation.update(delta, game.progress.settings.reduceMotion)
        if (guideAnimation.exitFinished) {
            if (finishAfterExit || page == pages.lastIndex) { beginJourney(); return }
            page++
            guideAnimation.reset()
        }
        beginStaticBackground(game.assets.volleyStageBackground(firstStage), .28f)
        val screen = Rectangle(visibleLeft, visibleBottom, visibleWidth, visibleHeight)
        HeroGuideRenderer.drawDialogue(batch, game.assets, screen, pages[page], visibleBottom + visibleHeight * .36f,
            "TAP TO CONTINUE  |  ${page + 1}/${pages.size}",
            animation = guideAnimation, reduceMotion = game.progress.settings.reduceMotion)
        val oldColor = Color(game.assets.titleFont.color)
        game.assets.titleFont.color = Color.WHITE
        fittedText(game.assets.titleFont, "THE NINE WORLDS", visibleLeft + 60f, visibleBottom + visibleHeight - 195f, visibleWidth - 120f, .9f)
        game.assets.titleFont.color = oldColor
        HeroGuideRenderer.drawSkip(batch, game.assets, screen)
        end()

        if (leaving || game.uiMotion.active || guideAnimation.exiting) return
        val skip = Gdx.input.justTouched() && HeroGuideRenderer.skipBounds(screen).contains(touchPoint().x, touchPoint().y)
        if (Gdx.input.isKeyJustPressed(Input.Keys.BACK) || Gdx.input.isKeyJustPressed(Input.Keys.ESCAPE) || skip) {
            finishAfterExit = true
            guideAnimation.beginExit()
        } else if (Gdx.input.justTouched()) {
            if (guideAnimation.revealOrContinue(pages[page])) guideAnimation.beginExit()
        }
    }
}
