package com.example.brick_breaker_ball

import com.badlogic.gdx.Gdx
import com.badlogic.gdx.Input
import com.badlogic.gdx.graphics.Color
import com.badlogic.gdx.graphics.GL20
import com.badlogic.gdx.math.Rectangle
import com.badlogic.gdx.math.Vector3
import com.badlogic.gdx.utils.Align
import kotlin.math.abs

/**
 * Full-screen home for owned gameplay charms and the power-up guide.
 *
 * The main-menu version is deliberately read-only. The paused-game version restores the
 * persisted session and only consumes a charm through GameSession.useItem(), preserving the
 * existing atomic activation/consumption contract.
 */
class CharmsBagScreen(
    game: BrickBreakerGame,
    private val returnDestination: ShopReturnDestination,
) : ForgeScreen(game) {
    private val pausedState = if (returnDestination == ShopReturnDestination.PAUSED_GAME) {
        game.pausedSession.restore()
    } else {
        null
    }
    private val pointer = Vector3()
    private var selectedCharm = 0
    private var selectedGuidePowerUp = 0
    private var guideMode = false
    private var scrollOffset = 0f
    private var touchActive = false
    private var touchMoved = false
    private var touchStartX = 0f
    private var touchStartY = 0f
    private var touchLastX = 0f
    private var touchLastY = 0f
    private var feedback = ""

    override fun show() {
        installMouseWheelHandler(::handleMouseWheel)
    }

    override fun hide() {
        removeMouseWheelHandler()
    }

    override fun render(delta: Float) {
        begin()

        game.assets.uiRenderer.drawGradientPanel(
            batch,
            Rectangle(42f, 190f, 816f, 1190f),
            ForgeUiRenderer.GradientStyle.NEUTRAL,
        )

        game.assets.pauseTitleFont.color = ForgeUiPalette.textPrimary
        game.assets.pauseTitleFont.draw(batch, "CHARMS BAG", 0f, TITLE_BASELINE, 900f, Align.center, false)
        game.assets.pauseTitleFont.color = Color.WHITE
        game.assets.uiRenderer.drawGradientProgress(
            batch,
            Rectangle(150f, TITLE_RULE_Y, 600f, 4f),
            ForgeUiRenderer.GradientStyle.PRIMARY,
        )

        val charmsTab = charmsTabRect()
        val guideTab = guideTabRect()
        drawTab(charmsTab, "MY CHARMS 14", !guideMode)
        drawTab(guideTab, "GUIDE 20", guideMode)

        val entries = entries()
        scrollOffset = scrollOffset.coerceIn(0f, maxScroll(entries.size))
        beginListClip()
        entries.forEachIndexed(::drawCharmCard)
        endListClip()

        drawSelectedCharm(entries)
        val actions = drawActions()

        game.assets.smallFont.color = ForgeUiPalette.muted
        drawDescription("DRAG THE LIST TO SCROLL", 0f, SCROLL_HINT_BASELINE, 900f, .72f, Align.center)
        game.assets.smallFont.color = Color.WHITE

        if (feedback.isNotEmpty()) {
            game.assets.smallFont.color = ForgeUiPalette.crimsonLight
            drawDescription(feedback, 82f, 214f, 736f, .72f, Align.center)
            game.assets.smallFont.color = Color.WHITE
        }

        end()
        handleInput(actions)
    }

    private fun drawSelectedCharm(entries: List<PowerUpType>) {
        val selectedType = selectedType(entries)
        val category = PowerUpCatalog.definitions.getValue(selectedType).category
        val info = PowerUpInfoRepository.info(selectedType)
        val detailRect = Rectangle(82f, DETAIL_Y, 736f, DETAIL_HEIGHT)
        game.assets.uiRenderer.drawGradientBorderPanel(
            batch,
            detailRect,
            ForgeUiRenderer.GradientStyle.PRIMARY,
            4f,
        )

        val iconPanel = Rectangle(detailRect.x + 14f, detailRect.y + 23f, 104f, 104f)
        game.assets.uiRenderer.drawGradientPanel(batch, iconPanel, ForgeUiRenderer.GradientStyle.NEUTRAL)
        drawIcon(selectedType, iconPanel.x + 5f, iconPanel.y + 5f, 94f)

        val textX = detailRect.x + 138f
        val textWidth = detailRect.width - 158f
        game.assets.hudLabelFont.color = ForgeUiPalette.primaryLight
        game.assets.hudLabelFont.draw(batch, info.fullName, textX, detailRect.y + 118f, textWidth, Align.left, false)
        game.assets.hudLabelFont.color = Color.WHITE

        game.assets.smallFont.color = ForgeUiPalette.textSecondary
        drawDescription(info.description, textX, detailRect.y + 84f, textWidth, .80f)
        game.assets.smallFont.color = Color.WHITE

        if (guideMode) {
            val availability = when {
                selectedType in SHOP_ELIGIBLE_TYPES -> "POSITIVE CHARM - CAN BE OWNED / USED"
                category == PowerUpCategory.BAD -> "HAZARD DROP - NOT SOLD"
                category == PowerUpCategory.SPECIAL -> "SPECIAL DROP - NOT SOLD"
                else -> "GAMEPLAY DROP"
            }
            game.assets.smallFont.color = when {
                selectedType in SHOP_ELIGIBLE_TYPES -> ForgeUiPalette.successLight
                category == PowerUpCategory.BAD -> ForgeUiPalette.crimsonLight
                category == PowerUpCategory.SPECIAL -> ForgeUiPalette.primaryLight
                else -> ForgeUiPalette.textSecondary
            }
            drawDescription(availability, textX, detailRect.y + 42f, textWidth, .68f)
            game.assets.smallFont.color = Color.WHITE
        }
    }

    private fun drawActions(): BagActions {
        val pausedGame = returnDestination == ShopReturnDestination.PAUSED_GAME
        val use = if (pausedGame) Rectangle(35f, ACTION_Y, 190f, ACTION_HEIGHT) else null
        val back = if (pausedGame) Rectangle(245f, ACTION_Y, 190f, ACTION_HEIGHT) else Rectangle(110f, ACTION_Y, 200f, ACTION_HEIGHT)
        val shop = if (pausedGame) Rectangle(455f, ACTION_Y, 190f, ACTION_HEIGHT) else Rectangle(350f, ACTION_Y, 200f, ACTION_HEIGHT)
        val customize = if (pausedGame) Rectangle(665f, ACTION_Y, 200f, ACTION_HEIGHT) else Rectangle(590f, ACTION_Y, 200f, ACTION_HEIGHT)

        use?.let { rect ->
            val canUse = !guideMode && pausedState?.second?.canActivatePowerUp(selectedType()) == true &&
                game.boosterInventory.count(selectedType()) > 0
            drawAction(rect, "USE", if (canUse) ForgeUiRenderer.GradientStyle.SUCCESS else ForgeUiRenderer.GradientStyle.DISABLED)
        }
        drawAction(back, "BACK", ForgeUiRenderer.GradientStyle.NEUTRAL)
        drawAction(shop, "SHOP", ForgeUiRenderer.GradientStyle.PRIMARY)
        drawAction(customize, "CUSTOMIZE", ForgeUiRenderer.GradientStyle.NEUTRAL)
        return BagActions(use, back, shop, customize)
    }

    private fun drawAction(rect: Rectangle, label: String, style: ForgeUiRenderer.GradientStyle) {
        game.assets.uiRenderer.drawGradientButton(batch, rect, style)
        game.assets.buttonFont.color = ForgeUiPalette.textPrimary
        fittedText(game.assets.buttonFont, label, rect.x + 8f, rect.y + 55f, rect.width - 16f, .78f)
        game.assets.buttonFont.color = Color.WHITE
    }

    private fun drawTab(rect: Rectangle, label: String, selected: Boolean) {
        game.assets.uiRenderer.drawGradientButton(
            batch,
            rect,
            if (selected) ForgeUiRenderer.GradientStyle.PRIMARY else ForgeUiRenderer.GradientStyle.NEUTRAL,
        )
        game.assets.hudLabelFont.color = if (selected) ForgeUiPalette.textPrimary else ForgeUiPalette.textSecondary
        game.assets.hudLabelFont.draw(batch, label, rect.x, rect.y + 47f, rect.width, Align.center, false)
        game.assets.hudLabelFont.color = Color.WHITE
    }

    private fun drawCharmCard(index: Int, type: PowerUpType) {
        val rect = cardRect(index)
        val selectedIndex = if (guideMode) selectedGuidePowerUp else selectedCharm
        val selected = index == selectedIndex
        val category = PowerUpCatalog.definitions.getValue(type).category
        val stripeStyle = when (category) {
            PowerUpCategory.BAD -> ForgeUiRenderer.GradientStyle.CRIMSON
            PowerUpCategory.SPECIAL -> ForgeUiRenderer.GradientStyle.PRIMARY
            PowerUpCategory.GOOD -> ForgeUiRenderer.GradientStyle.SUCCESS
        }

        if (selected) {
            game.assets.uiRenderer.drawGradientBorderPanel(batch, rect, ForgeUiRenderer.GradientStyle.PRIMARY, 5f)
        } else {
            game.assets.uiRenderer.drawGradientPanel(batch, rect, ForgeUiRenderer.GradientStyle.NEUTRAL)
        }
        game.assets.uiRenderer.drawGradientProgress(batch, Rectangle(rect.x, rect.y, 9f, rect.height), stripeStyle)

        val iconPanel = Rectangle(rect.x + 18f, rect.y + 24f, 100f, 100f)
        game.assets.uiRenderer.drawGradientPanel(batch, iconPanel, ForgeUiRenderer.GradientStyle.NEUTRAL)
        drawIcon(type, iconPanel.x + 5f, iconPanel.y + 5f, 90f)

        val chipWidth = 102f
        val chipX = rect.x + rect.width - chipWidth - 18f
        val textX = rect.x + 136f
        val textWidth = chipX - textX - 14f
        val info = PowerUpInfoRepository.info(type)

        game.assets.hudLabelFont.color = if (selected) ForgeUiPalette.primaryLight else ForgeUiPalette.textPrimary
        game.assets.hudLabelFont.draw(batch, info.shortName, textX, rect.y + 122f, textWidth, Align.left, false)
        game.assets.hudLabelFont.color = Color.WHITE
        game.assets.smallFont.color = ForgeUiPalette.textSecondary
        drawDescription(info.description, textX, rect.y + 86f, textWidth, .78f)
        game.assets.smallFont.color = Color.WHITE

        val chipRect = Rectangle(chipX, rect.y + 34f, chipWidth, 80f)
        game.assets.uiRenderer.drawGradientPanel(batch, chipRect, ForgeUiRenderer.GradientStyle.NEUTRAL)
        game.assets.uiRenderer.drawGradientProgress(batch, Rectangle(chipRect.x, chipRect.y, chipRect.width, 4f), stripeStyle)

        if (type in SHOP_ELIGIBLE_TYPES) {
            val owned = game.boosterInventory.count(type)
            game.assets.hudLabelFont.color = if (owned > 0) ForgeUiPalette.successLight else ForgeUiPalette.muted
            game.assets.hudLabelFont.draw(batch, "x$owned", chipRect.x, rect.y + 84f, chipRect.width, Align.center, false)
            game.assets.hudLabelFont.color = Color.WHITE
            game.assets.smallFont.color = ForgeUiPalette.muted
            drawDescription("OWNED", chipRect.x, rect.y + 53f, chipRect.width, .64f, Align.center)
        } else {
            game.assets.smallFont.color = when (category) {
                PowerUpCategory.BAD -> ForgeUiPalette.crimsonLight
                PowerUpCategory.SPECIAL -> ForgeUiPalette.primaryLight
                PowerUpCategory.GOOD -> ForgeUiPalette.textSecondary
            }
            drawDescription(if (category == PowerUpCategory.BAD) "HAZARD" else "DROP", chipRect.x, rect.y + 78f, chipRect.width, .66f, Align.center)
        }
        game.assets.smallFont.color = Color.WHITE
    }

    private fun handleInput(actions: BagActions) {
        if (Gdx.input.isTouched || Gdx.input.justTouched()) {
            pointer.set(Gdx.input.x.toFloat(), Gdx.input.y.toFloat(), 0f)
            viewport.unproject(pointer)
        }
        if (Gdx.input.isKeyJustPressed(Input.Keys.ESCAPE) || Gdx.input.isKeyJustPressed(Input.Keys.BACK)) {
            goBack()
            return
        }

        if (Gdx.input.justTouched()) {
            when {
                charmsTabRect().contains(pointer.x, pointer.y) -> {
                    guideMode = false
                    resetScrollTouch()
                    return
                }
                guideTabRect().contains(pointer.x, pointer.y) -> {
                    guideMode = true
                    resetScrollTouch()
                    return
                }
                actions.use?.contains(pointer.x, pointer.y) == true -> {
                    useSelectedCharm()
                    return
                }
                actions.back.contains(pointer.x, pointer.y) -> {
                    goBack()
                    return
                }
                actions.shop.contains(pointer.x, pointer.y) -> {
                    game.setScreen(ShopScreen(game, returnDestination))
                    return
                }
                actions.customize.contains(pointer.x, pointer.y) -> {
                    game.setScreen(CustomizationScreen(game, returnDestination == ShopReturnDestination.PAUSED_GAME))
                    return
                }
                pointer.y in LIST_BOTTOM..LIST_TOP -> {
                    touchActive = true
                    touchMoved = false
                    touchStartX = pointer.x
                    touchStartY = pointer.y
                    touchLastX = pointer.x
                    touchLastY = pointer.y
                }
            }
        }

        if (touchActive && Gdx.input.isTouched) {
            scrollOffset = (scrollOffset + pointer.y - touchLastY).coerceIn(0f, maxScroll(entries().size))
            touchLastX = pointer.x
            touchLastY = pointer.y
            if (abs(pointer.y - touchStartY) > 20f || abs(pointer.x - touchStartX) > 20f) touchMoved = true
        } else if (touchActive) {
            if (!touchMoved) {
                entries().indices.firstOrNull { cardRect(it).contains(touchLastX, touchLastY) }?.let { index ->
                    if (guideMode) selectedGuidePowerUp = index else selectedCharm = index
                    feedback = ""
                }
            }
            touchActive = false
        }
    }

    private fun handleMouseWheel(amountY: Float): Boolean {
        if (amountY == 0f) return false
        pointer.set(Gdx.input.x.toFloat(), Gdx.input.y.toFloat(), 0f)
        viewport.unproject(pointer)
        if (pointer.y !in LIST_BOTTOM..LIST_TOP) return false
        scrollOffset = (scrollOffset + amountY * CARD_STEP).coerceIn(0f, maxScroll(entries().size))
        return true
    }

    private fun useSelectedCharm() {
        if (guideMode) return
        val (level, session) = pausedState ?: return
        when (val result = session.useItem(selectedType(), game.boosterInventory)) {
            is BoosterUseResult.Applied -> {
                game.pausedSession.save(level, session)
                goBack()
            }
            is BoosterUseResult.Rejected -> feedback = result.reason
        }
    }

    private fun goBack() {
        if (returnDestination == ShopReturnDestination.PAUSED_GAME) game.resumePausedGame() else game.openMenu()
    }

    private fun entries(): List<PowerUpType> = if (guideMode) PowerUpCatalog.classicOrderedTypes else SHOP_ELIGIBLE_TYPES

    private fun selectedType(entries: List<PowerUpType> = entries()): PowerUpType {
        val index = if (guideMode) selectedGuidePowerUp else selectedCharm
        return entries[index.coerceIn(entries.indices)]
    }

    private fun charmsTabRect() = Rectangle(82f, TAB_Y, 345f, TAB_HEIGHT)
    private fun guideTabRect() = Rectangle(473f, TAB_Y, 345f, TAB_HEIGHT)
    private fun cardRect(index: Int) = Rectangle(80f, LIST_TOP - CARD_HEIGHT - LIST_INSET - index * CARD_STEP + scrollOffset, 740f, CARD_HEIGHT)

    private fun maxScroll(count: Int): Float {
        if (count <= 0) return 0f
        val contentHeight = LIST_INSET * 2f + CARD_HEIGHT + (count - 1) * CARD_STEP
        return (contentHeight - (LIST_TOP - LIST_BOTTOM)).coerceAtLeast(0f)
    }

    private fun resetScrollTouch() {
        scrollOffset = 0f
        touchActive = false
        feedback = ""
    }

    private fun beginListClip() {
        batch.flush()
        val bottom = Vector3(0f, LIST_BOTTOM, 0f)
        val top = Vector3(900f, LIST_TOP, 0f)
        viewport.project(bottom)
        viewport.project(top)
        Gdx.gl.glEnable(GL20.GL_SCISSOR_TEST)
        Gdx.gl.glScissor(
            bottom.x.toInt(),
            bottom.y.toInt(),
            (top.x - bottom.x).toInt().coerceAtLeast(1),
            (top.y - bottom.y).toInt().coerceAtLeast(1),
        )
    }

    private fun endListClip() {
        batch.flush()
        Gdx.gl.glDisable(GL20.GL_SCISSOR_TEST)
    }

    private fun drawIcon(type: PowerUpType, x: Float, y: Float, size: Float) {
        batch.color = Color.WHITE
        batch.draw(game.assets.gameplayAtlas.powerUpIcon(type), x, y, size, size)
    }

    private fun drawDescription(text: String, x: Float, y: Float, width: Float, scale: Float, align: Int = Align.left) {
        val oldX = game.assets.smallFont.data.scaleX
        val oldY = game.assets.smallFont.data.scaleY
        game.assets.smallFont.data.setScale(scale)
        game.assets.smallFont.draw(batch, text, x, y, width, align, true)
        game.assets.smallFont.data.setScale(oldX, oldY)
    }

    private data class BagActions(
        val use: Rectangle?,
        val back: Rectangle,
        val shop: Rectangle,
        val customize: Rectangle,
    )

    private companion object {
        const val TITLE_BASELINE = 1340f
        const val TITLE_RULE_Y = 1278f
        const val TAB_Y = 1185f
        const val TAB_HEIGHT = 70f
        const val LIST_BOTTOM = 555f
        const val LIST_TOP = 1158f
        const val LIST_INSET = 12f
        const val CARD_HEIGHT = 148f
        const val CARD_STEP = 160f
        const val SCROLL_HINT_BASELINE = 536f
        const val DETAIL_Y = 360f
        const val DETAIL_HEIGHT = 150f
        const val ACTION_Y = 260f
        const val ACTION_HEIGHT = 82f
    }
}
