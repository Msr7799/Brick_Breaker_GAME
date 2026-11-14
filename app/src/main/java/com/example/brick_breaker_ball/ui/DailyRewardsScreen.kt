/*
 * المسار: app/src/main/java/com/example/brick_breaker_ball/ui/DailyRewardsScreen.kt
 * الوظيفة: واجهة المكافأة اليومية.
 * الحسابات: توقيت الاستحقاق ومواضع بطاقات المكافآت.
 */

package com.example.brick_breaker_ball.ui

import com.badlogic.gdx.Gdx
import com.badlogic.gdx.Input
import com.badlogic.gdx.graphics.Color
import com.badlogic.gdx.math.Rectangle
import com.badlogic.gdx.utils.Align
import com.example.brick_breaker_ball.BrickBreakerGame
import com.example.brick_breaker_ball.assets.BoardSpriteNames
import com.example.brick_breaker_ball.app.ServerDailyStatus
import com.example.brick_breaker_ball.economy.EconomyStore
import com.example.brick_breaker_ball.gameplay.GameplayMode

/** The board is the complete authored calendar; embedded frames are hit/layout guides only. */
class DailyRewardsScreen(
    game: BrickBreakerGame,
    private val shopReturnDestination: ShopReturnDestination? = null,
    private val shopMode: GameplayMode = game.progress.settings.gameplayMode
) : ForgeScreen(game) {
    private var claiming = false
    private var loading = true
    private var statusRequestId = 0
    private var errorMessage: String? = null
    private var serverStatus: ServerDailyStatus? = null
    private var statusAccountId: String? = null

    private fun leaveCalendar() {
        val destination = shopReturnDestination
        if (destination == null) {
            game.navigate(MainMenuScreen(game), ScreenTransitionStyle.FADE_THROUGH)
        } else {
            game.navigate(ShopScreen(game, destination, shopMode, "FREE"), ScreenTransitionStyle.FADE_THROUGH)
        }
    }

    init { fetchStatus() }

    fun refreshAfterAccountConnected() {
        if (!claiming) fetchStatus()
    }

    private fun fetchStatus() {
        val requestId = ++statusRequestId
        val accountId = game.platformActions.dailyRewardAccountId
        statusAccountId = accountId
        if (!game.cloudAccountReady) {
            loading = false
            errorMessage = "Return to the main menu to sync your cloud save first."
            return
        }
        loading = true
        errorMessage = null
        game.platformActions.fetchDailyRewardStatus { status, error ->
            if (requestId == statusRequestId && accountId == game.platformActions.dailyRewardAccountId && game.cloudAccountReady) {
                loading = false
                serverStatus = status
                errorMessage = error
                status?.let { game.markDailyRewardPromptSeen(it.serverDay) }
                status?.lastReward?.let { game.economy.creditServerDailyReward(it) }
            }
        }
    }

    override fun render(delta: Float) {
        beginStaticBackground(com.badlogic.gdx.graphics.g2d.TextureRegion(game.assets.splash), .74f)
        val insets = safeInsets
        val safe = Rectangle(
            visibleLeft + insets.left + 20f,
            visibleBottom + insets.bottom + 18f,
            visibleWidth - insets.left - insets.right - 40f,
            visibleHeight - insets.top - insets.bottom - 36f
        )
        val atlas = game.assets.boards
        val board = atlas.contain("daily_reward_board", safe)
        batch.color = Color.WHITE
        batch.draw(atlas.requireRegion("daily_reward_board"), board.x, board.y, board.width, board.height)

        val status = serverStatus
        var availableRect: Rectangle? = null
        for (day in 1..EconomyStore.DAILY_REWARD_DAYS) {
            val name = BoardSpriteNames.dailyRewardDay(
                day,
                when (day) {
                    7, 14 -> "gift"
                    3, 6, 10, 13 -> "gems"
                    else -> "coins"
                }
            )
            val tile = atlas.placement("daily_reward_board", name, board)
            val claimed = status != null && day <= status.completedDays
            val available = status != null && day == status.dayToClaim && !status.claimedToday
            if (available) availableRect = tile

            // Preserve the original day art. A translucent wash and short label convey state.
            if (!claimed && !available) {
                batch.setColor(0f, 0f, 0f, .48f)
                batch.draw(
                    game.assets.solidUiRegion,
                    tile.x + 3f,
                    tile.y + 3f,
                    tile.width - 6f,
                    tile.height - 6f
                )
                batch.color = Color.WHITE
            }
            val reward = game.economy.dailyRewardForDay(day)
            val amount = if (day == 7 || day == 14) {
                "+${reward.coins} C  +${reward.gems} G"
            } else if (name.endsWith("gems")) {
                "+${reward.gems} GEMS"
            } else {
                "+${reward.coins} COINS"
            }
            val oldColor = Color(game.assets.smallFont.color)
            game.assets.smallFont.color = when {
                available -> Color.GOLD
                claimed -> Color.GREEN
                else -> Color.LIGHT_GRAY
            }
            val oldScaleX = game.assets.smallFont.data.scaleX
            val oldScaleY = game.assets.smallFont.data.scaleY
            game.assets.smallFont.data.setScale((if (isLandscape) .45f else .53f) * uiTextScale)
            game.assets.smallFont.draw(
                batch,
                amount,
                tile.x + 2f,
                tile.y + tile.height * .23f,
                tile.width - 4f,
                Align.center,
                false
            )
            game.assets.smallFont.draw(
                batch,
                when {
                    claimed -> "CLAIMED"
                    available -> "TODAY"
                    else -> "LOCKED"
                },
                tile.x + 2f,
                tile.y + tile.height * .09f,
                tile.width - 4f,
                Align.center,
                false
            )
            game.assets.smallFont.data.setScale(oldScaleX, oldScaleY)
            game.assets.smallFont.color = oldColor
        }

        availableRect?.let { tile ->
            val pulse = selectionPulse()
            batch.setColor(1f, .77f, .15f, .25f + pulse * .19f)
            batch.draw(
                game.assets.particles.findRegion("glow"),
                tile.x - 7f,
                tile.y - 7f,
                tile.width + 14f,
                tile.height + 14f
            )
            batch.color = Color.WHITE
        }

        val skip = atlas.placement("daily_reward_board", "daily_reward_skip_button", board)
        // The authored SKIP is already in the complete board. Do not draw its crop twice.
        val claim = Rectangle(
            board.x + board.width * .16f,
            board.y + board.height * .035f,
            board.width * .68f,
            board.height * .064f
        )
        val label = when {
            loading -> "LOADING..."
            claiming -> "CLAIMING..."
            errorMessage != null -> "RETRY CONNECTION"
            status?.claimedToday == true -> "COLLECTED TODAY"
            status == null -> "RETRY"
            else -> "CLAIM REWARD"
        }
        val buttonArt = game.assets.boards2.requireRegion(if (status?.claimedToday == true || loading || claiming) "ui_button_dark_blank" else "ui_button_green_blank")
        batch.draw(buttonArt, claim.x, claim.y, claim.width, claim.height)
        fittedText(
            game.assets.buttonFont,
            label,
            claim.x + 10f,
            claim.y + claim.height * .67f,
            claim.width - 20f,
            .66f
        )
        errorMessage?.let { error ->
            fittedText(game.assets.smallFont, error.take(90), board.x + 12f, claim.y + claim.height + 23f, board.width - 24f, .44f)
        }
        end()

        when {
            tapped(skip) || Gdx.input.isKeyJustPressed(Input.Keys.BACK) ||
                Gdx.input.isKeyJustPressed(Input.Keys.ESCAPE) ->
                leaveCalendar()

            !loading && !claiming && tapped(claim) -> {
                if (errorMessage != null || serverStatus == null) {
                    fetchStatus()
                } else if (status?.claimedToday != true && game.cloudAccountReady && statusAccountId == game.platformActions.dailyRewardAccountId) {
                    val accountId = statusAccountId
                    claiming = true
                    game.platformActions.claimDailyReward { next, error ->
                        claiming = false
                        if (accountId != game.platformActions.dailyRewardAccountId || !game.cloudAccountReady) {
                            errorMessage = "Account changed. Return to the main menu to sync."
                            return@claimDailyReward
                        }
                        if (next == null) {
                            errorMessage = error ?: "Reward server unavailable"
                        } else {
                            serverStatus = next
                            val award = next.lastReward
                            if (award != null && game.economy.creditServerDailyReward(award)) {
                                game.pendingDailyWalletReward = com.example.brick_breaker_ball.economy.DailyWalletReward(award.coins, award.gems, award.streak)
                                game.assets.playLevelUp(game.progress.settings.masterVolume * game.progress.settings.sfxVolume)
                                if (game.progress.settings.haptics) Gdx.input.vibrate(32)
                            }
                            leaveCalendar()
                        }
                    }
                }
            }
        }
    }
}
