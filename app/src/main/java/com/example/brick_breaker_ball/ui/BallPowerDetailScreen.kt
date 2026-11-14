package com.example.brick_breaker_ball.ui

import com.badlogic.gdx.Gdx
import com.badlogic.gdx.Input
import com.badlogic.gdx.graphics.Color
import com.badlogic.gdx.graphics.g2d.TextureRegion
import com.badlogic.gdx.math.Rectangle
import com.badlogic.gdx.utils.Align
import com.example.brick_breaker_ball.BrickBreakerGame
import com.example.brick_breaker_ball.cosmetics.BallAbilityCatalog
import com.example.brick_breaker_ball.cosmetics.BallCollectionCatalog
import com.example.brick_breaker_ball.cosmetics.BallRarity
import com.example.brick_breaker_ball.cosmetics.BallSpriteDefinition
import com.example.brick_breaker_ball.cosmetics.VolleyBallSelection
import com.example.brick_breaker_ball.gameplay.GameplayMode

/** Inspect every ball, including locked entries, without changing the equipped loadout. */
class BallPowerDetailScreen(
    game: BrickBreakerGame,
    private val ball: BallSpriteDefinition,
    private val returnToPausedGame: Boolean = false,
    private val returnToShop: Boolean = false,
    private val shopReturnDestination: ShopReturnDestination = if (returnToPausedGame)
        ShopReturnDestination.PAUSED_GAME else ShopReturnDestination.MAIN_MENU,
    private val shopReturnMode: GameplayMode = game.progress.settings.gameplayMode,
    private val returnToCollection: (() -> Unit)? = null
) : ForgeScreen(game) {
    private val profile = BallAbilityCatalog.profileForBall(ball)
    private val offer = BallCollectionCatalog.offer(ball)
    private val preview by lazy { BallPowerPreview(game, returnToPausedGame) }
    private val help = VisualHelpOverlay()
    private var elapsed = 0f
    private val ink = Color(.035f, .055f, .09f, .98f)
    private val surface = Color(.065f, .095f, .145f, 1f)
    private val muted = Color(.59f, .67f, .77f, 1f)
    private val accent = when (offer.rarity) {
        BallRarity.NORMAL -> Color(.38f, .84f, 1f, 1f)
        BallRarity.RARE -> Color(.40f, .61f, 1f, 1f)
        BallRarity.LEGENDARY -> Color(1f, .77f, .32f, 1f)
        BallRarity.EPIC -> Color(.82f, .49f, 1f, 1f)
    }

    override fun render(delta: Float) {
        elapsed += delta.coerceIn(0f, .1f)
        help.update(delta, game.progress.settings.reduceMotion)
        beginStaticBackground(TextureRegion(game.assets.splash), .84f)
        val inset = safeInsets
        val usableW = visibleWidth - inset.left - inset.right - 48f
        val usableH = visibleHeight - inset.top - inset.bottom - 40f
        val scale = minOf(usableW / 820f, usableH / 1400f)
        val x = visibleLeft + inset.left + (visibleWidth - inset.left - inset.right - 820f * scale) / 2f
        val y = visibleBottom + inset.bottom + (visibleHeight - inset.top - inset.bottom - 1400f * scale) / 2f
        fun area(px: Float, py: Float, w: Float, h: Float) = Rectangle(x + px * scale, y + py * scale, w * scale, h * scale)
        fun label(text: String, px: Float, py: Float, w: Float, size: Float, color: Color = Color.WHITE) {
            val font = game.assets.hudLabelFont
            font.color = color
            fittedText(font, text, x + px * scale, y + py * scale, w * scale, size * scale)
            font.color = Color.WHITE
        }
        val screen = Rectangle(visibleLeft, visibleBottom, visibleWidth, visibleHeight)
        fill(area(0f, 0f, 820f, 1400f), ink)
        fill(area(0f, 1397f, 820f, 3f), accent)
        label("BALL DETAILS", 36f, 1348f, 650f, .58f, muted)
        val helpButton = area(724f, 1292f, 60f, 60f)
        VisualGameUi.helpButton(batch, game.assets, helpButton)
        label(offer.rarity.name, 120f, 1275f, 580f, .60f, accent)

        game.assets.cosmetics.ballRegion(ball)?.let { region ->
            val diameter = 234f * scale
            val ratio = minOf(diameter / region.regionWidth, diameter / region.regionHeight)
            val w = region.regionWidth * ratio
            val h = region.regionHeight * ratio
            batch.color = Color.WHITE
            batch.draw(region, x + 410f * scale - w / 2f, y + 1122f * scale - h / 2f, w, h)
        }
        label(ball.name.uppercase(), 32f, 958f, 756f, .88f)
        val owned = game.developmentAccess.canUseCosmetic(VolleyBallSelection.canUse(
            shopReturnMode, ball, game.cosmeticProgression.ownsBall(ball)))
        val equipped = VolleyBallSelection.isEquipped(game.progress.settings, ball)
        label(if (equipped) "EQUIPPED" else if (owned) "OWNED" else "PREVIEW · ${game.cosmeticProgression.ballRequirement(ball)}",
            36f, 912f, 748f, .43f, if (owned) accent else muted)

        val reveal = if (game.progress.settings.reduceMotion) 1f else (elapsed / .42f).coerceIn(0f, 1f)
        fun stat(py: Float, title: String, value: String, power: Float) {
            fill(area(32f, py, 756f, 112f), surface)
            label(title, 54f, py + 83f, 500f, .48f, muted)
            label(value, 560f, py + 83f, 200f, .70f)
            val gap = 5f
            val width = (706f - gap * 11) / 12f
            repeat(12) { i ->
                val lit = (power * 12f * reveal - i).coerceIn(0f, 1f)
                val color = Color(.13f, .19f, .26f, 1f).lerp(accent, lit)
                fill(area(54f + i * (width + gap), py + 22f, width, 16f), color)
            }
        }
        stat(762f, "HIT DAMAGE · STAGE ${preview.stageNumber}", "${preview.damage(profile)}", profile.baseHitDamage / 12f)
        stat(632f, "VOLLEY SIZE · +${profile.startingBalls} FROM BALL", "${preview.balls(profile)}", profile.startingBalls / 40f)

        fill(area(32f, 432f, 756f, 180f), surface)
        label(profile.title, 54f, 572f, 706f, .57f, accent)
        val font = game.assets.smallFont
        val oldX = font.data.scaleX
        val oldY = font.data.scaleY
        font.data.setScale(.47f * scale)
        font.color = Color(.83f, .88f, .94f, 1f)
        font.draw(batch, profile.description, x + 54f * scale, y + 529f * scale,
            706f * scale, Align.left, true)
        font.data.setScale(oldX, oldY)
        font.color = Color.WHITE

        val straight = area(32f, 290f, 367f, 120f)
        val spread = area(421f, 290f, 367f, 120f)
        val shotgun = owned && profile.shotgunAvailable && game.progress.isShotgunEnabled(ball.spriteName)
        fun mode(rect: Rectangle, selected: Boolean, icon: VisualGameUi.Icon, text: String, unlocked: Boolean) {
            fill(rect, if (selected) Color(accent).mul(.34f, .34f, .34f, 1f) else surface)
            if (selected) fill(Rectangle(rect.x, rect.y, rect.width, 3f * scale), accent)
            VisualGameUi.icon(batch, game.assets, icon,
                Rectangle(rect.x + 20f * scale, rect.y + 26f * scale, 72f * scale, 68f * scale), if (unlocked) 1f else .35f)
            label(text, (rect.x - x) / scale + 108f, (rect.y - y) / scale + 70f, 230f, .46f,
                if (unlocked) Color.WHITE else muted)
        }
        mode(straight, owned && !shotgun, VisualGameUi.Icon.STRAIGHT, "STRAIGHT", owned)
        mode(spread, shotgun, VisualGameUi.Icon.SPREAD, if (profile.shotgunAvailable) "SPREAD" else "SPREAD LOCKED", owned && profile.shotgunAvailable)
        val equip = area(32f, 166f, 756f, 100f)
        fill(equip, if (equipped) surface else accent)
        label(if (!owned) "VIEW PRICE" else if (equipped) "EQUIPPED" else "EQUIP BALL", 48f, 229f, 724f, .66f,
            if (equipped) accent else ink)
        val back = area(32f, 42f, 756f, 94f)
        fill(back, surface)
        label("BACK", 48f, 103f, 724f, .56f, muted)
        help.focus.set(area(32f, 632f, 756f, 242f))
        help.draw(batch, game.assets, screen, game.progress.settings.reduceMotion,
            balls = preview.balls(profile), damage = preview.damage(profile), spreadAvailable = profile.shotgunAvailable)
        end()

        if (help.active) {
            if (Gdx.input.isKeyJustPressed(Input.Keys.BACK)) help.tap(true)
            else if (Gdx.input.justTouched()) help.tap(VisualHelpOverlay.closeBounds(screen).contains(touchPoint().x, touchPoint().y))
            return
        }
        if (tapped(helpButton)) { help.open(VisualHelpOverlay.Topic.BALLS); return }
        if (owned) {
            if (tapped(straight)) game.progress.setShotgunEnabled(ball.spriteName, false)
            if (profile.shotgunAvailable && tapped(spread)) game.progress.setShotgunEnabled(ball.spriteName, true)
        }
        if (tapped(equip)) {
            if (!owned) game.navigate(ShopScreen(game, shopReturnDestination, shopReturnMode, "BALLS", ball.spriteName))
            else if (!equipped) {
                VolleyBallSelection.equip(game.progress.settings, ball)
                game.progress.saveSettings()
                game.uiFeedback.emit(UiFeedbackKind.CONFIRM)
            }
        }
        if (tapped(back) || Gdx.input.isKeyJustPressed(Input.Keys.BACK) || Gdx.input.isKeyJustPressed(Input.Keys.ESCAPE)) {
            if (returnToShop) game.navigate(ShopScreen(game, shopReturnDestination, shopReturnMode, "BALLS", ball.spriteName))
            else if (returnToCollection != null) returnToCollection.invoke()
            else game.navigate(CustomizationScreen(game, returnToPausedGame, initialBallSpriteName = ball.spriteName))
        }
    }

    private fun fill(rect: Rectangle, color: Color) {
        batch.color = color
        batch.draw(game.assets.solidUiRegion, rect.x, rect.y, rect.width, rect.height)
        batch.color = Color.WHITE
    }
}
