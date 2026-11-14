package com.example.brick_breaker_ball.ui

import com.badlogic.gdx.math.Rectangle
import com.example.brick_breaker_ball.BrickBreakerGame
import com.example.brick_breaker_ball.gameplay.GameplayMode

/** Retained for existing geometry callers; the showcase has no scrolling collection board. */
internal object CustomizationHitTesting {
    const val BALL_LIST_BOTTOM = 535f
    const val BALL_LIST_TOP = 1105f
    fun visibleBallCard(rect: Rectangle, clipBottom: Float = BALL_LIST_BOTTOM,
                        clipTop: Float = BALL_LIST_TOP): Rectangle? {
        val bottom = maxOf(rect.y, clipBottom)
        val top = minOf(rect.y + rect.height, clipTop)
        return if (top <= bottom) null else Rectangle(rect.x, bottom, rect.width, top - bottom)
    }
}

/** Preserve every existing entry/return route while using the shared full-size ball showcase. */
class CustomizationScreen(
    game: BrickBreakerGame,
    returnToPausedGame: Boolean = false,
    returnToBag: Boolean = false,
    bagReturnDestination: ShopReturnDestination = ShopReturnDestination.MAIN_MENU,
    bagReturnMode: GameplayMode = game.progress.settings.gameplayMode,
    bagReturnToShop: Boolean = false,
    returnToShop: Boolean = false,
    shopReturnDestination: ShopReturnDestination = ShopReturnDestination.MAIN_MENU,
    shopReturnMode: GameplayMode = game.progress.settings.gameplayMode,
    initialBallSpriteName: String? = null
) : BallShowcaseScreen(game, initialBallSpriteName, returnToPausedGame,
    if (returnToPausedGame) ShopReturnDestination.PAUSED_GAME else shopReturnDestination,
    if (returnToBag) bagReturnMode else shopReturnMode,
    onBack = { selected ->
        when {
            returnToBag -> game.navigate(VolleyOrbsScreen(game, bagReturnDestination, bagReturnMode, bagReturnToShop))
            returnToShop -> game.navigate(ShopScreen(game, shopReturnDestination, shopReturnMode, "BALLS", selected.spriteName))
            returnToPausedGame -> game.resumePausedGame(showLoading = false)
            else -> game.openMenu()
        }
    })
