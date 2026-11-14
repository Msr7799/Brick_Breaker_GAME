package com.example.brick_breaker_ball.ui

import com.example.brick_breaker_ball.BrickBreakerGame
import com.example.brick_breaker_ball.cosmetics.BallSpriteDefinition
import com.example.brick_breaker_ball.gameplay.GameplayMode

/** Shop artwork and collection entry points now display the same complete showcase. */
class BallPowerDetailScreen(
    game: BrickBreakerGame,
    ball: BallSpriteDefinition,
    returnToPausedGame: Boolean = false,
    returnToShop: Boolean = false,
    shopReturnDestination: ShopReturnDestination = if (returnToPausedGame)
        ShopReturnDestination.PAUSED_GAME else ShopReturnDestination.MAIN_MENU,
    shopReturnMode: GameplayMode = game.progress.settings.gameplayMode,
    returnToCollection: (() -> Unit)? = null
) : BallShowcaseScreen(game, ball.spriteName, returnToPausedGame, shopReturnDestination, shopReturnMode,
    onBack = { selected ->
        when {
            returnToShop -> game.navigate(ShopScreen(game, shopReturnDestination, shopReturnMode, "BALLS", selected.spriteName))
            returnToCollection != null -> returnToCollection.invoke()
            else -> game.navigate(CustomizationScreen(game, returnToPausedGame, initialBallSpriteName = selected.spriteName))
        }
    })
