/*
 * المسار: app/src/main/java/com/example/brick_breaker_ball/app/Navigator.kt
 * الوظيفة: تنسيق الانتقال بين شاشات اللعبة.
 * الحسابات: توقيت الانتقالات وشفافيتها حسب الزمن.
 */

package com.example.brick_breaker_ball.app

import com.badlogic.gdx.Screen
import com.example.brick_breaker_ball.BrickBreakerGame
import com.example.brick_breaker_ball.ui.ScreenTransitionStyle

/** All routes pass through the same two-phase fade; no side-slide/zoom/edge-wipe path remains. */
class Navigator(private val game: BrickBreakerGame) {
    private var pending: Screen? = null
    val hasPendingRoute: Boolean get() = pending != null

    fun navigate(next: Screen, style: ScreenTransitionStyle = ScreenTransitionStyle.FADE_THROUGH) {
        @Suppress("UNUSED_VARIABLE")
        val transitionStyle = style
        if (game.screen === next || pending != null) return
        if (game.progress.settings.reduceMotion) {
            game.setScreen(next)
            return
        }
        if (!game.uiMotion.startExit()) return
        pending = next
    }

    /** Must be called after the black-out draw and not inside the outgoing screen's render(). */
    fun commitPending() {
        val destination = pending ?: return
        pending = null
        game.setScreen(destination)
        if (!game.progress.settings.reduceMotion) game.uiMotion.startEntrance()
    }
}
