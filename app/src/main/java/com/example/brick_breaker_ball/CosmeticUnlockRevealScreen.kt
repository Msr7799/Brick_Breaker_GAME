package com.example.brick_breaker_ball

import com.badlogic.gdx.Gdx
import com.badlogic.gdx.graphics.Color
import com.badlogic.gdx.graphics.g2d.TextureRegion
import com.badlogic.gdx.math.MathUtils
import com.badlogic.gdx.math.Rectangle
import com.badlogic.gdx.utils.Align
import kotlin.math.min

enum class CosmeticRevealKind { BALL, PADDLE_STYLE, SUMMARY }

/** Pure flow state used by the screen and unit tests. */
class CosmeticRevealFlow(unlocks: List<CosmeticUnlock>, val reducedMotion: Boolean) {
    private val ordered = unlocks.sortedBy { if (it is CosmeticUnlock.Ball) 0 else 1 }
    val steps: List<CosmeticRevealKind> = buildList {
        if (ordered.any { it is CosmeticUnlock.Ball }) add(CosmeticRevealKind.BALL)
        if (ordered.any { it is CosmeticUnlock.Paddle }) add(CosmeticRevealKind.PADDLE_STYLE)
        if (size > 1) add(CosmeticRevealKind.SUMMARY)
    }
    var index = 0
        private set
    val current: CosmeticRevealKind? get() = steps.getOrNull(index)
    val complete: Boolean get() = index >= steps.size
    fun advance() {
        if (!complete) index++
    }
    fun skip() {
        index = steps.size
    }
}

class CosmeticUnlockRevealScreen(game: BrickBreakerGame, private val unlocks: List<CosmeticUnlock>, private val onComplete: () -> Unit) : ForgeScreen(game) {
    private val flow = CosmeticRevealFlow(unlocks, game.progress.settings.reduceMotion)
    private var elapsed = 0f
    private var finished = false
    private val ballUnlock get() = unlocks.filterIsInstance<CosmeticUnlock.Ball>().firstOrNull()
    private val paddleUnlock get() = unlocks.filterIsInstance<CosmeticUnlock.Paddle>().firstOrNull()

    override fun render(delta: Float) {
        elapsed += min(delta, .1f)
        begin(2)
        batch.color = ForgeUiPalette.glassOverlay
        drawFullscreenPanel(game.assets.ui.findRegion("panel"))
        batch.color = Color.WHITE
        if (!flow.reducedMotion) drawEnergy()

        val continueButton: Rectangle
        val equipButton: Rectangle?
        when (flow.current) {
            CosmeticRevealKind.BALL -> {
                val unlock = requireNotNull(ballUnlock)
                title("NEW BALL UNLOCKED", 1470f)
                revealPanel(unlock.ball.name.uppercase(), "NEW BALL AVAILABLE")
                drawBall(unlock.ball)
                val equipped = isBallEquipped(unlock.ball)
                equipButton = button(
                    if (equipped) "EQUIPPED" else "EQUIP NOW",
                    85f,
                    210f,
                    340f,
                    92f,
                    if (equipped) ForgeUiRenderer.GradientStyle.SUCCESS else ForgeUiRenderer.GradientStyle.PRIMARY,
                )
                continueButton = button("CONTINUE", 475f, 210f, 340f, 92f)
            }

            CosmeticRevealKind.PADDLE_STYLE -> {
                val unlock = requireNotNull(paddleUnlock)
                title("NEW PADDLE STYLE UNLOCKED", 1470f)
                revealPanel(unlock.style.displayName.uppercase(), "STYLE SET UNLOCKED  •  3 FORMS INCLUDED")
                drawPaddleStyle(unlock.style)
                val equipped = isStyleEquipped(unlock.style)
                equipButton = button(
                    if (equipped) "EQUIPPED" else "EQUIP NOW",
                    85f,
                    210f,
                    340f,
                    92f,
                    if (equipped) ForgeUiRenderer.GradientStyle.SUCCESS else ForgeUiRenderer.GradientStyle.PRIMARY,
                )
                continueButton = button("CONTINUE", 475f, 210f, 340f, 92f)
            }

            CosmeticRevealKind.SUMMARY -> {
                title("COLLECTION UPDATED", 1420f)
                revealPanel("REWARDS SECURED", "+1 BALL  •  +1 PADDLE STYLE")
                game.assets.bodyFont.draw(batch, "Everything is saved and ready to equip.", 80f, 780f, 740f, Align.center, true)
                equipButton = null
                continueButton = button("CONTINUE", 175f, 250f, 550f, 100f)
            }

            null -> {
                end()
                finish()
                return
            }
        }
        val skip = button("SKIP", 690f, 80f, 160f, 64f)
        end()

        if (elapsed >= .35f && equipButton != null && tapped(equipButton)) equipCurrent()
        if (elapsed >= .55f && tapped(continueButton)) advance()
        if (tapped(skip)) {
            flow.skip()
            finish()
        }
    }

    private fun revealPanel(name: String, subtitle: String) {
        val pulse = if (flow.reducedMotion) 1f else .82f + .18f * MathUtils.sin(elapsed * 4.2f)
        batch.color = Color(ForgeUiPalette.purple.r, ForgeUiPalette.purple.g, ForgeUiPalette.purple.b, pulse)
        batch.draw(game.assets.ui.findRegion("panel"), 65f, 430f, 770f, 820f)
        batch.color = ForgeUiPalette.glassPanel
        batch.draw(game.assets.ui.findRegion("panel"), 71f, 436f, 758f, 808f)
        batch.color = Color.WHITE
        fittedText(game.assets.titleFont, name, 95f, 1170f, 710f, .82f)
        game.assets.smallFont.color = ForgeUiPalette.maroon
        fittedText(game.assets.smallFont, subtitle, 110f, 1115f, 680f, .66f)
        game.assets.smallFont.color = Color.WHITE
    }

    private fun drawBall(ball: BallSpriteDefinition) {
        val appearAt = if (flow.reducedMotion) 0f else .18f
        val alpha = ((elapsed - appearAt) / .35f).coerceIn(0f, 1f)
        val size = 248f * (.84f + .16f * alpha)
        val centerX = 450f
        val centerY = 830f
        batch.color = Color(1f, 1f, 1f, alpha)
        game.assets.cosmetics.ballRegion(ball)?.let { region ->
            drawBallPreview(region, centerX, centerY, size)
        }
        batch.color = Color.WHITE
        fittedText(game.assets.smallFont, ball.name.uppercase(), 150f, 630f, 600f, .76f)
        val ability = BallAbilityCatalog.profileForBall(ball)
        game.assets.smallFont.color = ForgeUiPalette.primaryLight
        fittedText(game.assets.smallFont, "${ability.title} • TIER ${ability.tier} • ${ability.shortStat()}", 105f, 590f, 690f, .56f)
        game.assets.smallFont.color = ForgeUiPalette.textSecondary
        fittedText(game.assets.smallFont, "THIS BALL IS NOW READY TO EQUIP", 120f, 548f, 660f, .54f)
        game.assets.smallFont.color = Color.WHITE
    }

    private fun drawPaddleStyle(style: PaddleStyleSet) {
        val forms = listOf("NORMAL" to style.normal, "WEAPON" to style.weapon, "STICKY" to style.sticky)
        forms.forEachIndexed { index, (label, paddle) ->
            val appearAt = if (flow.reducedMotion) 0f else .2f + index * .22f
            val alpha = ((elapsed - appearAt) / .35f).coerceIn(0f, 1f)
            val cardY = 865f - index * 180f
            val cardX = 125f
            val cardWidth = 650f
            val cardHeight = 120f
            batch.color = Color(ForgeUiPalette.glassPanel.r, ForgeUiPalette.glassPanel.g, ForgeUiPalette.glassPanel.b, .35f + .35f * alpha)
            batch.draw(game.assets.ui.findRegion("panel"), cardX, cardY, cardWidth, cardHeight)

            game.assets.smallFont.color = Color(ForgeUiPalette.maroon.r, ForgeUiPalette.maroon.g, ForgeUiPalette.maroon.b, alpha)
            fittedText(game.assets.smallFont, label, cardX + 20f, cardY + 84f, 170f, .58f)
            game.assets.smallFont.color = Color.WHITE

            batch.color = Color(1f, 1f, 1f, alpha)
            game.assets.cosmetics.paddleRegion(paddle)?.let { region ->
                val availableWidth = 420f
                val availableHeight = 68f
                val scale = min(availableWidth / region.regionWidth, availableHeight / region.regionHeight)
                val w = region.regionWidth * scale
                val h = region.regionHeight * scale
                val drawX = cardX + 205f + (availableWidth - w) / 2f
                val drawY = cardY + (cardHeight - h) / 2f
                batch.draw(region, drawX, drawY, w, h)
            }
            batch.color = Color.WHITE
        }
        val ability = PaddleAbilityCatalog.profileForNormalPaddle(style.normal.id)
        game.assets.smallFont.color = ForgeUiPalette.primaryLight
        fittedText(game.assets.smallFont, "${ability.title} • TIER ${ability.tier}", 120f, 505f, 660f, .56f)
        game.assets.smallFont.color = ForgeUiPalette.textSecondary
        fittedText(game.assets.smallFont, ability.description.uppercase(), 120f, 465f, 660f, .46f)
        game.assets.smallFont.color = Color.WHITE
    }

    private fun drawBallPreview(region: TextureRegion, centerX: Float, centerY: Float, diameter: Float) {
        val previewRegion = croppedBallPreviewRegion(region)
        batch.draw(previewRegion, centerX - diameter / 2f, centerY - diameter / 2f, diameter, diameter)
    }

    private fun croppedBallPreviewRegion(region: TextureRegion): TextureRegion {
        val side = min(region.regionWidth, region.regionHeight)
        val xOffset = ((region.regionWidth - side) / 2).coerceAtLeast(0)
        val yOffset = ((region.regionHeight - side) / 2).coerceAtLeast(0)
        return TextureRegion(region, xOffset, yOffset, side, side)
    }

    private fun drawEnergy() {
        val glow = game.assets.particles.findRegion("glow")
        repeat(14) { index ->
            val phase = elapsed * (35f + index) + index * 71f
            val x = 40f + ((index * 137f + phase * .7f) % 820f)
            val y = 300f + ((index * 211f + phase) % 1050f)
            val alpha = .18f + .20f * MathUtils.sin(elapsed * 3f + index)
            batch.color = if (index % 3 == 0) {
                Color(ForgeUiPalette.primaryLight.r, ForgeUiPalette.primaryLight.g, ForgeUiPalette.primaryLight.b, alpha)
            } else {
                Color(ForgeUiPalette.crimsonLight.r, ForgeUiPalette.crimsonLight.g, ForgeUiPalette.crimsonLight.b, alpha)
            }
            batch.draw(glow, x, y, 30f, 30f)
        }
        batch.color = Color.WHITE
    }

    private fun isBallEquipped(ball: BallSpriteDefinition): Boolean =
        game.progress.settings.selectedBallGroupName == ball.groupName &&
            game.progress.settings.selectedBallSpriteName == ball.spriteName

    private fun isStyleEquipped(style: PaddleStyleSet): Boolean =
        game.progress.settings.selectedPaddleId == style.normal.id &&
            game.progress.settings.selectedWeaponPaddleId == style.weapon.id &&
            game.progress.settings.selectedStickyPaddleId == style.sticky.id

    private fun equipCurrent() {
        when (flow.current) {
            CosmeticRevealKind.BALL -> ballUnlock?.ball?.let {
                if (isBallEquipped(it)) {
                    game.cosmeticProgression.firstOwnedBall()?.let { starter ->
                        game.cosmeticProgression.equipBall(game.progress.settings, starter)
                    }
                } else {
                    game.cosmeticProgression.equipBall(game.progress.settings, it)
                }
            }

            CosmeticRevealKind.PADDLE_STYLE -> paddleUnlock?.style?.let {
                if (isStyleEquipped(it)) {
                    game.cosmeticProgression.firstOwnedPaddle()?.let { starter ->
                        game.cosmeticProgression.equipStyle(game.progress.settings, starter)
                    }
                } else {
                    game.cosmeticProgression.equipStyle(game.progress.settings, it)
                }
            }

            else -> Unit
        }
        game.progress.saveSettings()
    }

    private fun advance() {
        flow.advance()
        elapsed = 0f
        if (flow.complete) finish()
    }
    private fun finish() {
        if (!finished) {
            finished = true
            onComplete()
        }
    }
}
