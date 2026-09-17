package com.example.brick_breaker_ball

import com.badlogic.gdx.Gdx
import com.badlogic.gdx.Input
import com.badlogic.gdx.graphics.Color
import com.badlogic.gdx.graphics.g2d.TextureRegion
import com.badlogic.gdx.math.Rectangle
import com.badlogic.gdx.utils.Align
import kotlin.math.min

/** Full-screen power card for one ball. Every catalog ball uses this reusable screen. */
class BallPowerDetailScreen(
    game: BrickBreakerGame,
    private val ball: BallSpriteDefinition,
    private val returnToPausedGame: Boolean = false,
) : ForgeScreen(game) {
    private val profile = BallAbilityCatalog.profileForBall(ball)
    private var elapsed = 0f

    override fun render(delta: Float) {
        elapsed = (elapsed + delta.coerceAtMost(.1f)).coerceAtMost(1.2f)
        begin("world_4_zerog")
        title("BALL POWER", 1510f)

        val panel = Rectangle(65f, 390f, 770f, 940f)
        game.assets.uiRenderer.drawGradientBorderPanel(batch, panel, ForgeUiRenderer.GradientStyle.PRIMARY, 5f)

        val reveal = smooth(elapsed / .35f)
        val diameter = 270f * (.88f + .12f * reveal)
        game.assets.cosmetics.ballRegion(ball)?.let { region ->
            drawBallClean(region, 450f, 1040f, diameter, reveal)
        }

        fittedText(game.assets.titleFont, ball.name.uppercase(), 100f, 835f, 700f, .78f)
        game.assets.smallFont.color = ForgeUiPalette.primaryLight
        fittedText(game.assets.smallFont, "${profile.title}  •  ${profile.tierLabel}", 100f, 770f, 700f, .70f)
        game.assets.smallFont.color = Color.WHITE

        val statBox = Rectangle(155f, 655f, 590f, 82f)
        game.assets.uiRenderer.drawGradientPanel(batch, statBox, ForgeUiRenderer.GradientStyle.NEUTRAL)
        game.assets.bodyFont.color = ForgeUiPalette.gold
        fittedText(game.assets.bodyFont, profile.shortStat(), statBox.x + 15f, statBox.y + 55f, statBox.width - 30f, .70f)
        game.assets.bodyFont.color = Color.WHITE

        game.assets.smallFont.color = ForgeUiPalette.textSecondary
        game.assets.smallFont.draw(batch, profile.description.uppercase(), 130f, 605f, 640f, Align.center, true)
        game.assets.smallFont.color = Color.WHITE

        val owned = game.developmentAccess.canUseCosmetic(game.cosmeticProgression.ownsBall(ball))
        val equipped = game.progress.settings.selectedBallGroupName == ball.groupName &&
            game.progress.settings.selectedBallSpriteName == ball.spriteName
        val status = when {
            equipped -> "EQUIPPED"
            owned -> "UNLOCKED • READY TO EQUIP"
            else -> game.cosmeticProgression.ballRequirement(ball)
        }
        game.assets.smallFont.color = if (owned) ForgeUiPalette.primaryLight else ForgeUiPalette.textSecondary
        fittedText(game.assets.smallFont, status, 110f, 500f, 680f, .62f)
        game.assets.smallFont.color = Color.WHITE

        val equip = if (owned) {
            button(
                if (equipped) "EQUIPPED" else "EQUIP BALL",
                125f,
                245f,
                650f,
                96f,
                if (equipped) ForgeUiRenderer.GradientStyle.SUCCESS else ForgeUiRenderer.GradientStyle.PRIMARY,
            )
        } else null
        val back = button("BACK", 250f, 115f, 400f, 82f, ForgeUiRenderer.GradientStyle.NEUTRAL)
        end()

        if (equip != null && !equipped && tapped(equip)) {
            game.cosmeticProgression.equipBall(game.progress.settings, ball, game.developmentAccess.enabled)
            game.progress.saveSettings()
        }
        if (tapped(back) || Gdx.input.isKeyJustPressed(Input.Keys.BACK)) {
            game.setScreen(CustomizationScreen(game, returnToPausedGame, CustomizationTab.BALLS))
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

/** Full-screen power card for one paddle family: Normal + Weapon + Sticky together. */
class PaddlePowerDetailScreen(
    game: BrickBreakerGame,
    private val style: PaddleStyleSet,
    private val returnToPausedGame: Boolean = false,
) : ForgeScreen(game) {
    private val profile = PaddleAbilityCatalog.profileForNormalPaddle(style.normal.id)
    private var elapsed = 0f

    override fun render(delta: Float) {
        elapsed = (elapsed + delta.coerceAtMost(.1f)).coerceAtMost(1.5f)
        begin("world_4_zerog")
        title("PADDLE POWER", 1510f)
        fittedText(game.assets.titleFont, style.displayName.uppercase(), 70f, 1390f, 760f, .72f)

        val forms = listOf("NORMAL" to style.normal, "WEAPON" to style.weapon, "STICKY" to style.sticky)
        forms.forEachIndexed { index, (label, paddle) ->
            val t = smooth((elapsed - .10f - index * .12f) / .32f)
            val y = 1120f - index * 205f - (1f - t) * 28f
            val rect = Rectangle(95f, y, 710f, 165f)
            game.assets.uiRenderer.drawGradientBorderPanel(batch, rect, ForgeUiRenderer.GradientStyle.NEUTRAL, 4f)
            game.assets.smallFont.color = ForgeUiPalette.primaryLight
            fittedText(game.assets.smallFont, label, rect.x + 20f, rect.y + 134f, 150f, .62f)
            game.assets.smallFont.color = Color.WHITE
            game.assets.cosmetics.paddleRegion(paddle)?.let { region ->
                drawPaddleFit(region, rect.x + 175f, rect.y + 35f, 500f, 100f, t)
            }
        }

        val abilityY = 405f
        val abilityPanel = Rectangle(95f, abilityY, 710f, 245f)
        game.assets.uiRenderer.drawGradientBorderPanel(batch, abilityPanel, ForgeUiRenderer.GradientStyle.PRIMARY, 5f)
        game.assets.bodyFont.color = ForgeUiPalette.gold
        fittedText(game.assets.bodyFont, "${profile.title}  •  TIER ${profile.tier}", 120f, abilityY + 200f, 660f, .70f)
        game.assets.bodyFont.color = Color.WHITE
        game.assets.smallFont.color = ForgeUiPalette.textSecondary
        game.assets.smallFont.draw(batch, profile.description.uppercase(), 135f, abilityY + 145f, 630f, Align.center, true)
        game.assets.smallFont.color = Color.WHITE

        val owned = game.developmentAccess.canUseCosmetic(game.cosmeticProgression.ownsPaddle(style))
        val equipped = game.progress.settings.selectedPaddleId == style.normal.id &&
            game.progress.settings.selectedWeaponPaddleId == style.weapon.id &&
            game.progress.settings.selectedStickyPaddleId == style.sticky.id
        val status = when {
            equipped -> "EQUIPPED STYLE"
            owned -> "ALL 3 FORMS UNLOCKED"
            else -> game.cosmeticProgression.paddleRequirement(style)
        }
        game.assets.smallFont.color = if (owned) ForgeUiPalette.primaryLight else ForgeUiPalette.textSecondary
        fittedText(game.assets.smallFont, status, 120f, abilityY + 50f, 660f, .60f)
        game.assets.smallFont.color = Color.WHITE

        val equip = if (owned) {
            button(
                if (equipped) "EQUIPPED" else "EQUIP STYLE",
                125f,
                230f,
                650f,
                90f,
                if (equipped) ForgeUiRenderer.GradientStyle.SUCCESS else ForgeUiRenderer.GradientStyle.PRIMARY,
            )
        } else null
        val back = button("BACK", 250f, 105f, 400f, 78f, ForgeUiRenderer.GradientStyle.NEUTRAL)
        end()

        if (equip != null && !equipped && tapped(equip)) {
            game.cosmeticProgression.equipStyle(game.progress.settings, style, game.developmentAccess.enabled)
            game.progress.saveSettings()
        }
        if (tapped(back) || Gdx.input.isKeyJustPressed(Input.Keys.BACK)) {
            game.setScreen(CustomizationScreen(game, returnToPausedGame, CustomizationTab.PADDLES))
        }
    }

    private fun drawPaddleFit(region: TextureRegion, x: Float, y: Float, width: Float, height: Float, alpha: Float) {
        val scale = min(width / region.regionWidth, height / region.regionHeight)
        val drawWidth = region.regionWidth * scale
        val drawHeight = region.regionHeight * scale
        batch.color = Color(1f, 1f, 1f, alpha)
        batch.draw(region, x + (width - drawWidth) / 2f, y + (height - drawHeight) / 2f, drawWidth, drawHeight)
        batch.color = Color.WHITE
    }

    private fun smooth(value: Float): Float {
        val t = value.coerceIn(0f, 1f)
        return t * t * (3f - 2f * t)
    }
}
