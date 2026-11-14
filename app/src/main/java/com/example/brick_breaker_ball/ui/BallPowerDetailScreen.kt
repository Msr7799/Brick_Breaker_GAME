/*
 * المسار: app/src/main/java/com/example/brick_breaker_ball/ui/BallPowerDetailScreen.kt
 * الوظيفة: عرض تفاصيل الكرة وقدراتها.
 * الحسابات: تحجيم المعاينة والنص حسب مساحة البطاقة.
 */

package com.example.brick_breaker_ball.ui

import com.badlogic.gdx.Gdx
import com.badlogic.gdx.Input
import com.badlogic.gdx.graphics.Color
import com.badlogic.gdx.graphics.g2d.TextureRegion
import com.badlogic.gdx.math.Rectangle
import com.example.brick_breaker_ball.BrickBreakerGame
import com.example.brick_breaker_ball.cosmetics.BallAbilityCatalog
import com.example.brick_breaker_ball.cosmetics.BallCollectionCatalog
import com.example.brick_breaker_ball.cosmetics.BallSpriteDefinition
import com.example.brick_breaker_ball.cosmetics.VolleyBallSelection
import kotlin.math.min

/** Full-screen power card for one ball. Every catalog ball uses this reusable screen. */
class BallPowerDetailScreen(
    game: BrickBreakerGame,
    private val ball: BallSpriteDefinition,
    private val returnToPausedGame: Boolean = false
) : ForgeScreen(game) {
    private val profile = BallAbilityCatalog.profileForBall(ball)
    private val collectionOffer = BallCollectionCatalog.offer(ball)
    private val preview by lazy { BallPowerPreview(game, returnToPausedGame) }
    private var elapsed = 0f
    private val visualHelp = VisualHelpOverlay()

    override fun render(delta: Float) {
        visualHelp.update(delta, game.progress.settings.reduceMotion)
        elapsed = (elapsed + delta.coerceAtMost(.1f)).coerceAtMost(1.2f)
        begin("world_4_zerog")
        val insets = safeInsets
        val safeHeight = visibleHeight - insets.top - insets.bottom
        val boardAreaHeight = minOf(1470f, safeHeight - 100f)
        val bounds = Rectangle(
            visibleLeft + insets.left + 24f,
            visibleBottom + insets.bottom + (safeHeight - boardAreaHeight) / 2f,
            visibleWidth - insets.left - insets.right - 48f,
            boardAreaHeight
        )
        val boardAtlas = game.assets.boards2
        val buttons = game.assets.ballCollectionButtons
        val board = boardAtlas.contain("scroll_board", bounds)
        val scale = board.width / 560f
        fun bx(value: Float) = board.x + value * scale
        fun by(value: Float) = board.y + value * scale
        fun bs(value: Float) = value * scale
        batch.color = Color.WHITE
        batch.draw(boardAtlas.requireRegion("scroll_board"), board.x, board.y, board.width, board.height)
        fittedText(game.assets.titleFont, "BALLS", bx(72f), by(850f), bs(354f), .80f)
        val badge = Rectangle(bx(105f), by(745f), bs(350f), bs(48f))
        batch.draw(buttons.requireRegion("tab_dark"), badge.x, badge.y, badge.width, badge.height)
        val rarityStars = collectionOffer.rarity.ordinal + 1
        repeat(rarityStars) { index ->
            VisualGameUi.icon(batch, game.assets, VisualGameUi.Icon.STAR,
                Rectangle(bx(280f - rarityStars * 19f + index * 38f), by(752f), bs(32f), bs(32f)))
        }

        val reveal = if (game.progress.settings.reduceMotion) 1f else smooth(elapsed / .35f)
        val diameter = bs(140f) * (.88f + .12f * reveal)
        game.assets.cosmetics.ballRegion(ball)?.let { region ->
            drawBallClean(region, bx(280f), by(645f), diameter, reveal)
        }

        fittedText(game.assets.titleFont, ball.name.uppercase(), bx(65f), by(530f), bs(430f), .76f)
        val previewBalls = preview.balls(profile)
        VisualGameUi.panel(batch, game.assets, Rectangle(bx(64f), by(330f), bs(432f), bs(150f)))
        VisualGameUi.icon(batch, game.assets, VisualGameUi.Icon.BALLS,
            Rectangle(bx(88f), by(384f), bs(65f), bs(65f)))
        VisualGameUi.number(batch, game.assets, "$previewBalls",
            Rectangle(bx(150f), by(389f), bs(100f), bs(56f)), 1.20f)
        VisualGameUi.icon(batch, game.assets, VisualGameUi.Icon.HIT,
            Rectangle(bx(286f), by(384f), bs(65f), bs(65f)))
        VisualGameUi.number(batch, game.assets, "${preview.damage(profile)}",
            Rectangle(bx(350f), by(389f), bs(100f), bs(56f)), 1.20f)
        VisualGameUi.meter(batch, game.assets,
            Rectangle(bx(90f), by(350f), bs(380f), bs(20f)), preview.damage(profile), reveal)
        // The special ability is shown with a single symbol; its details are optional help.
        val specialIcon = when (profile.kind) {
            com.example.brick_breaker_ball.cosmetics.BallAbilityKind.INFERNO_BURST -> VisualGameUi.Icon.BLAST
            com.example.brick_breaker_ball.cosmetics.BallAbilityKind.ARC_CHAIN -> VisualGameUi.Icon.BLAST
            com.example.brick_breaker_ball.cosmetics.BallAbilityKind.ARMOR_BREAKER -> VisualGameUi.Icon.SHIELD
            com.example.brick_breaker_ball.cosmetics.BallAbilityKind.FORTUNE_CORE -> VisualGameUi.Icon.COIN
            com.example.brick_breaker_ball.cosmetics.BallAbilityKind.MOMENTUM_CORE -> VisualGameUi.Icon.BALLS
            else -> VisualGameUi.Icon.HIT
        }
        VisualGameUi.icon(batch, game.assets, specialIcon,
            Rectangle(bx(112f), by(271f), bs(48f), bs(48f)))
        if (profile.openingBreakerEveryVolleys > 0) {
            VisualGameUi.number(batch, game.assets, "1 / ${profile.openingBreakerEveryVolleys}",
                Rectangle(bx(180f), by(273f), bs(130f), bs(42f)), .68f)
            VisualGameUi.icon(batch, game.assets, VisualGameUi.Icon.BLAST,
                Rectangle(bx(330f), by(271f), bs(48f), bs(48f)))
        }

        val owned = game.developmentAccess.canUseCosmetic(
            VolleyBallSelection.canUse(game.progress.settings.gameplayMode, ball, game.cosmeticProgression.ownsBall(ball))
        )
        val equipped = VolleyBallSelection.isEquipped(game.progress.settings, ball)
        if (equipped || !owned) VisualGameUi.icon(batch, game.assets,
            if (owned) VisualGameUi.Icon.CHECK else VisualGameUi.Icon.LOCK,
            Rectangle(bx(440f), by(571f), bs(48f), bs(48f)))

        val straight = Rectangle(bx(82f), by(174f), bs(191f), bs(80f))
        val shotgun = Rectangle(bx(287f), by(174f), bs(191f), bs(80f))
        val shotgunSelected = owned && profile.shotgunAvailable && game.progress.isShotgunEnabled(ball.spriteName)
        VisualGameUi.modeButton(batch, game.assets, straight, false,
            owned && !shotgunSelected, owned)
        VisualGameUi.modeButton(batch, game.assets, shotgun, true,
            shotgunSelected, owned && profile.shotgunAvailable)

        val equip = Rectangle(bx(82f), by(88f), bs(396f), bs(76f))
        VisualGameUi.actionButton(batch, game.assets, equip, owned && !equipped)
        VisualGameUi.icon(batch, game.assets, when {
            !owned -> VisualGameUi.Icon.COIN
            equipped -> VisualGameUi.Icon.CHECK
            else -> VisualGameUi.Icon.EQUIP
        }, Rectangle(equip.x + equip.width * .38f, equip.y + bs(10f), equip.width * .24f, equip.height - bs(20f)))
        val back = Rectangle(bx(145f), by(24f), bs(270f), bs(60f))
        batch.draw(buttons.requireRegion("wide_dark"), back.x, back.y, back.width, back.height)
        VisualGameUi.icon(batch, game.assets, VisualGameUi.Icon.BACK,
            Rectangle(back.x + back.width * .34f, back.y + bs(6f), back.width * .32f, back.height - bs(12f)))
        val helpButton = Rectangle(bx(450f), by(832f), bs(62f), bs(62f))
        VisualGameUi.helpButton(batch, game.assets, helpButton)
        val helpScreen = Rectangle(visibleLeft, visibleBottom, visibleWidth, visibleHeight)
        visualHelp.focus.set(when (visualHelp.page) {
            2 -> Rectangle(straight.x, straight.y, shotgun.x + shotgun.width - straight.x, straight.height)
            3 -> equip
            else -> Rectangle(bx(64f), by(330f), bs(432f), bs(150f))
        })
        visualHelp.draw(batch, game.assets, helpScreen, game.progress.settings.reduceMotion,
            balls = previewBalls, damage = preview.damage(profile), spreadAvailable = profile.shotgunAvailable)
        end()

        if (visualHelp.active) {
            if (Gdx.input.isKeyJustPressed(Input.Keys.BACK)) visualHelp.tap(true)
            else if (Gdx.input.justTouched()) visualHelp.tap(
                VisualHelpOverlay.closeBounds(helpScreen).contains(touchPoint().x, touchPoint().y))
            return
        }
        if (tapped(helpButton)) {
            visualHelp.open(VisualHelpOverlay.Topic.BALLS)
            return
        }


        if (owned && profile.shotgunAvailable) {
            if (tapped(straight)) game.progress.setShotgunEnabled(ball.spriteName, false)
            if (tapped(shotgun)) game.progress.setShotgunEnabled(ball.spriteName, true)
        }

        if (tapped(equip)) {
            if (owned && !equipped) {
                VolleyBallSelection.equip(game.progress.settings, ball)
                game.progress.saveSettings()
            } else if (!owned) {
                game.navigate(
                    ShopScreen(
                        game,
                        if (returnToPausedGame) ShopReturnDestination.PAUSED_GAME else ShopReturnDestination.MAIN_MENU,
                        game.progress.settings.gameplayMode,
                        "BALLS"
                    )
                )
            }
        }
        if (tapped(back) || Gdx.input.isKeyJustPressed(Input.Keys.BACK)) {
            game.navigate(CustomizationScreen(game, returnToPausedGame))
        }
    }

    private fun drawBallClean(region: TextureRegion, centerX: Float, centerY: Float, diameter: Float, alpha: Float) {
        // No glow/shadow panel is drawn here. This intentionally removes the faint square halo
        // that used to appear around transparent ball PNGs in the unlock/detail preview.
        val side = min(region.regionWidth, region.regionHeight)
        val xOffset = ((region.regionWidth - side) / 2).coerceAtLeast(0)
        val yOffset = ((region.regionHeight - side) / 2).coerceAtLeast(0)
        val square = TextureRegion(region, xOffset, yOffset, side, side)
        batch.color = Color(1f, 1f, 1f, alpha)
        batch.draw(square, centerX - diameter / 2f, centerY - diameter / 2f, diameter, diameter)
        batch.color = Color.WHITE
    }

    private fun smooth(value: Float): Float {
        val t = value.coerceIn(0f, 1f)
        return t * t * (3f - 2f * t)
    }
}
