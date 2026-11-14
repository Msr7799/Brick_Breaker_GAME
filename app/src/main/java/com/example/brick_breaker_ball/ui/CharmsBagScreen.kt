/*
 * المسار: app/src/main/java/com/example/brick_breaker_ball/ui/CharmsBagScreen.kt
 * الوظيفة: عرض الحقيبة واستخدام عناصرها.
 * الحسابات: حساب شبكة البطاقات وحدود اللمس والتمرير.
 */

package com.example.brick_breaker_ball.ui

import com.badlogic.gdx.Gdx
import com.badlogic.gdx.Input
import com.badlogic.gdx.graphics.Color
import com.badlogic.gdx.graphics.g2d.GlyphLayout
import com.badlogic.gdx.math.Rectangle
import com.badlogic.gdx.scenes.scene2d.utils.ScissorStack
import com.badlogic.gdx.utils.Align
import com.example.brick_breaker_ball.BrickBreakerGame
import com.example.brick_breaker_ball.campaign.VolleyStageRepository
import com.example.brick_breaker_ball.cosmetics.BallAbilityCatalog
import com.example.brick_breaker_ball.economy.SHOP_ELIGIBLE_TYPES
import com.example.brick_breaker_ball.gameplay.GameplayMode
import com.example.brick_breaker_ball.gameplay.PowerUpType
import com.example.brick_breaker_ball.gameplay.VolleyItemController
import com.example.brick_breaker_ball.gameplay.VolleyItemUseResult
import com.example.brick_breaker_ball.gameplay.VolleySession
import kotlin.math.abs

/** The authored 3 x 4 Bag board, backed by the canonical inventory of the selected mode. */
class CharmsBagScreen(
    game: BrickBreakerGame,
    private val returnDestination: ShopReturnDestination,
    private val bagMode: GameplayMode = game.progress.settings.gameplayMode,
    private val returnToShop: Boolean = false,
    initialGuideMode: Boolean = false
) : ForgeScreen(game) {
    private val itemInventory = game.volleyItemInventory
    private val volleyPausedState by lazy {
        if (returnDestination == ShopReturnDestination.PAUSED_GAME) {
            game.volleyPausedSession.restore()
        } else {
            null
        }
    }
    private val volleySession by lazy {
        volleyPausedState?.let { (level, snapshot) ->
            val stage = VolleyStageRepository.forLevel(level.id)
            val settings = game.progress.settings
            val ballAbility = game.assets.cosmetics.selectedBall(
                settings.selectedVolleyBallGroupName, settings.selectedVolleyBallSpriteName
            )?.let(BallAbilityCatalog::profileForBall) ?: BallAbilityCatalog.default
            VolleySession(stage, stage.world, ballAbility,
                reduceMotion = settings.reduceMotion,
                difficulty = game.progress.selectedDifficulty,
                equippedOrbs = game.progress.equippedOrbs).also { session ->
                session.restore(snapshot)
                session.restoreRewardedRevivesUsed(game.volleyPausedSession.rewardedRevivesUsed())
                session.resume()
            }
        }
    }

    private var guideMode = initialGuideMode
    private var selectedCharm = rememberedCharm
    private var selectedGuidePowerUp = rememberedGuidePowerUp
    private var feedback = ""
    private val preview = CharmTutorial()
    private var previewWaitType: PowerUpType? = null
    private var previewWaitElapsed = 0f
    private var pressedIndex = -1
    private var touchStartY = 0f
    private var lastTouchY = 0f
    private var dragged = false
    private var inventoryTouchActive = false
    private var armPressPending = false
    private var scrollOffset = rememberedScroll
    private var scrollTarget = rememberedScroll
    private var scrollVelocity = 0f
    private var inventoryMaxScroll = 0f
    private var inventoryAreaForWheel = Rectangle()
    private val textLayout = GlyphLayout()
    private val visualHelp = VisualHelpOverlay()
    private val itemArt by lazy { com.example.brick_breaker_ball.assets.VolleyAssetCatalog(game.assets.volley) }

    override fun show() {
        installMouseWheelHandler { amountY ->
            if (visualHelp.active || amountY == 0f || inventoryMaxScroll <= 0f ||
                !inventoryAreaForWheel.contains(touchPoint().x, touchPoint().y)
            ) {
                false
            } else {
                scrollTarget = (scrollTarget + amountY * inventoryAreaForWheel.height * .30f)
                    .coerceIn(0f, inventoryMaxScroll)
                true
            }
        }
    }

    override fun hide() = removeMouseWheelHandler()

    override fun render(delta: Float) {
        visualHelp.update(delta, game.progress.settings.reduceMotion)
        previewWaitType?.let { type ->
            previewWaitElapsed += delta.coerceAtLeast(0f)
            if (previewWaitElapsed >= PREVIEW_DELAY_SECONDS) {
                preview.show(type, CharmLessonStatus.PREVIEW)
                previewWaitType = null
            }
        }
        preview.animate(delta)
        val stage = game.volleyPausedSession.pausedLevelId()?.let(VolleyStageRepository::forLevel)
        beginStaticBackground(
            stage?.let(game.assets::volleyStageBackground) ?: game.assets.worldMapImage(1, bagMode),
            .62f
        )
        val insets = safeInsets
        val available = Rectangle(
            visibleLeft + insets.left + 20f,
            visibleBottom + insets.bottom + 20f,
            visibleWidth - insets.left - insets.right - 40f,
            visibleHeight - insets.top - insets.bottom - 40f
        )
        val atlas = game.assets.boards2
        val board = atlas.contain("scroll_board", available)
        batch.color = Color.WHITE
        batch.draw(atlas.requireRegion("scroll_board"), board.x, board.y, board.width, board.height)

        val entries = entries()
        val contentX = board.x + board.width * .10f
        val contentW = board.width * .80f
        val inventoryArea = Rectangle(contentX, board.y + board.height * .235f,
            contentW, board.height * .455f)
        val gap = board.width * .018f
        val cardW = (contentW - gap * 2f) / 3f
        val cardH = board.height * .100f
        val rowStep = cardH + board.height * .012f
        val slots = entries.indices.map { index ->
            Rectangle(contentX + index % 3 * (cardW + gap),
                inventoryArea.y + inventoryArea.height - (index / 3 + 1) * rowStep,
                cardW, cardH)
        }
        val maxScroll = (((entries.size + 2) / 3) * rowStep - inventoryArea.height)
            .coerceAtLeast(0f)
        inventoryMaxScroll = maxScroll
        inventoryAreaForWheel.set(inventoryArea)
        scrollTarget = scrollTarget.coerceIn(0f, maxScroll)
        scrollOffset = if (game.progress.settings.reduceMotion) {
            scrollTarget
        } else {
            scrollOffset + (scrollTarget - scrollOffset) * (delta.coerceAtLeast(0f) * 13f).coerceIn(0f, 1f)
        }
        batch.flush()
        val scissors = Rectangle()
        ScissorStack.calculateScissors(camera, batch.transformMatrix, inventoryArea, scissors)
        if (ScissorStack.pushScissors(scissors)) {
            try {
                drawInventory(entries, slots)
            } finally {
                batch.flush()
                ScissorStack.popScissors()
            }
        }

        val back = Rectangle(contentX, board.y + board.height * .075f,
            contentW * .47f, board.height * .055f)
        val use = Rectangle(contentX + contentW * .53f, back.y,
            contentW * .47f, back.height)
        val controlsY = board.y + board.height * .725f
        val controlH = board.height * .048f
        val controls = listOf(
            Rectangle(contentX, controlsY, contentW * .315f, controlH),
            Rectangle(contentX + contentW * .342f, controlsY, contentW * .315f, controlH),
            Rectangle(contentX + contentW * .684f, controlsY, contentW * .315f, controlH),
            Rectangle(contentX, board.y + board.height * .816f,
                contentW * .20f, board.height * .05f),
            Rectangle(contentX + contentW * .84f, board.y + board.height * .816f,
                contentW * .16f, board.height * .05f)
        )
        drawText("BAG", contentX + contentW * .20f, board.y + board.height * .855f,
            contentW * .64f, Color.WHITE, .94f)
        listOf("ITEMS", "ORBS", "SHOP", "BALLS", "?").forEachIndexed { i, label ->
            val active = i == 0 && !guideMode || i == 4 && guideMode
            drawBagTab(controls[i], label, active)
        }

        val selected = entries.getOrNull((if (guideMode) selectedGuidePowerUp else selectedCharm).coerceIn(entries.indices))
        drawText(selected?.name?.replace('_', ' ') ?: "SELECT AN ITEM",
            contentX, board.y + board.height * .191f, contentW, Color.WHITE, .58f)
        drawText(if (selected == null) "" else "x${itemInventory.count(selected)}",
            contentX, board.y + board.height * .166f, contentW, Color.GOLD, .48f)
        if (feedback.isNotBlank()) {
            drawText(
                feedback,
                contentX,
                board.y + board.height * .143f,
                contentW,
                Color.SCARLET,
                .62f
            )
        }

        if (maxScroll > 0f) {
            val track = Rectangle(
                inventoryArea.x + inventoryArea.width - 5f,
                inventoryArea.y + 5f,
                3f,
                inventoryArea.height - 10f
            )
            val thumbHeight = (
                track.height * inventoryArea.height /
                    (inventoryArea.height + maxScroll)
                ).coerceAtLeast(28f)
            val thumbY = track.y + (track.height - thumbHeight) * (1f - scrollOffset / maxScroll)
            batch.setColor(.83f, .54f, .24f, .72f)
            batch.draw(game.assets.solidUiRegion, track.x, thumbY, track.width, thumbHeight)
            batch.color = Color.WHITE
        }

        val canUse = selected != null && !guideMode && itemInventory.count(selected) > 0 &&
            returnDestination == ShopReturnDestination.PAUSED_GAME &&
            volleySession?.let { VolleyItemController(it, itemInventory).canUseVolleyItem(selected).usable } == true
        batch.draw(atlas.requireRegion("ornate_button_blank"), back.x, back.y, back.width, back.height)
        VisualGameUi.icon(batch, game.assets, VisualGameUi.Icon.BACK,
            Rectangle(back.x + back.width * .32f, back.y + back.height * .13f, back.width * .36f, back.height * .74f))
        VisualGameUi.panel(batch, game.assets, use, canUse)
        VisualGameUi.icon(batch, game.assets, if (canUse) VisualGameUi.Icon.EQUIP else VisualGameUi.Icon.LOCK,
            Rectangle(use.x + use.width * .32f, use.y + use.height * .13f, use.width * .36f, use.height * .74f))
        if (armPressPending && Gdx.input.isTouched && use.contains(touchPoint().x, touchPoint().y)) {
            batch.setColor(.67f, .67f, .67f, 1f)
            batch.draw(atlas.requireRegion("ornate_button_blank"), use.x, use.y, use.width, use.height)
            batch.color = Color.WHITE
        }
        preview.type?.let { type ->
            CharmTutorialRenderer.draw(
                batch, game.assets, type, CharmLessonStatus.PREVIEW,
                preview.elapsed, game.progress.settings.reduceMotion,
                visibleLeft, visibleBottom, visibleWidth, visibleHeight
            )
        }
        val helpScreen = Rectangle(visibleLeft, visibleBottom, visibleWidth, visibleHeight)
        visualHelp.focus.set(if (visualHelp.page <= 1) inventoryArea else use)
        visualHelp.draw(batch, game.assets, helpScreen, game.progress.settings.reduceMotion,
            selected?.let { itemArt.itemIcon(it) })
        end()
        if (visualHelp.active) {
            inventoryTouchActive = false
            previewWaitType = null
            if (Gdx.input.isKeyJustPressed(Input.Keys.BACK) || Gdx.input.isKeyJustPressed(Input.Keys.ESCAPE)) visualHelp.tap(true)
            else if (Gdx.input.justTouched()) visualHelp.tap(VisualHelpOverlay.closeBounds(helpScreen).contains(touchPoint().x, touchPoint().y))
            return
        }
        handleInput(slots, inventoryArea, back, use, controls, entries.size, canUse, maxScroll, delta)
    }

    private fun slotAt(index: Int, slots: List<Rectangle>): Rectangle {
        val template = slots[index]
        return Rectangle(template.x, template.y + scrollOffset, template.width, template.height)
    }

    private fun drawBagTab(rect: Rectangle, label: String, active: Boolean) {
        VisualGameUi.panel(batch, game.assets, rect, active)
        if (label == "?") VisualGameUi.helpButton(batch, game.assets, rect)
        else VisualGameUi.icon(batch, game.assets, when (label) {
            "ITEMS" -> VisualGameUi.Icon.GRID
            "ORBS" -> VisualGameUi.Icon.ORB
            "SHOP" -> VisualGameUi.Icon.COIN
            else -> VisualGameUi.Icon.BALLS
        }, Rectangle(rect.x + rect.width * .32f, rect.y + rect.height * .10f,
            rect.width * .36f, rect.height * .80f))
    }

    private fun drawInventory(entries: List<PowerUpType>, slots: List<Rectangle>) {
        entries.forEachIndexed { index, type ->
            val slot = slotAt(index, slots)
            val count = itemInventory.count(type)
            val selected = index == if (guideMode) selectedGuidePowerUp else selectedCharm
            if (selected) batch.color = Color.GOLD
            else batch.setColor(.48f, .32f, .21f, 1f)
            batch.draw(game.assets.solidUiRegion, slot.x, slot.y, slot.width, slot.height)
            batch.setColor(.11f, .075f, .055f, .97f)
            batch.draw(game.assets.solidUiRegion, slot.x + 2f, slot.y + 2f,
                slot.width - 4f, slot.height - 4f)
            batch.color = Color.WHITE
            val side = minOf(slot.width * .68f, slot.height * .68f)
            batch.draw(itemArt.itemIcon(type), slot.x + (slot.width - side) / 2f,
                slot.y + slot.height * .26f, side, side)
            VisualGameUi.number(batch, game.assets, "x$count",
                Rectangle(slot.x + 8f, slot.y + slot.height * .025f, slot.width - 16f, slot.height * .22f), .52f)
        }
    }

    private fun handleInput(
        slots: List<Rectangle>,
        inventoryArea: Rectangle,
        back: Rectangle,
        use: Rectangle,
        controls: List<Rectangle>,
        count: Int,
        canUse: Boolean,
        maxScroll: Float,
        delta: Float
    ) {
        if (preview.active) {
            if (Gdx.input.isKeyJustPressed(Input.Keys.BACK) || Gdx.input.isKeyJustPressed(Input.Keys.ESCAPE) ||
                (Gdx.input.justTouched() && CharmTutorialRenderer.skipButton(visibleLeft, visibleBottom, visibleWidth, visibleHeight).contains(touchPoint().x, touchPoint().y))
            ) {
                preview.skip()
            }
            return
        }
        if (Gdx.input.isKeyJustPressed(Input.Keys.BACK) || Gdx.input.isKeyJustPressed(Input.Keys.ESCAPE)) {
            goBack()
            return
        }
        val point = touchPoint()
        if (armPressPending) {
            if (!Gdx.input.isTouched) {
                armPressPending = false
                if (use.contains(point.x, point.y) && canUse) useSelectedCharm()
            }
            return
        }
        if (Gdx.input.justTouched()) {
            when {
                back.contains(point.x, point.y) -> {
                    goBack()
                    return
                }

                use.contains(point.x, point.y) -> {
                    armPressPending = canUse
                    return
                }

                controls[0].contains(point.x, point.y) -> {
                    guideMode = false
                    scrollOffset = 0f
                    scrollTarget = 0f
                    previewWaitType = null
                    feedback = ""
                    return
                }

                controls[1].contains(point.x, point.y) -> {
                    rememberView()
                    game.setScreen(VolleyOrbsScreen(game, returnDestination, bagMode, returnToShop))
                    return
                }

                controls[2].contains(point.x, point.y) -> {
                    rememberView()
                    game.navigate(ShopScreen(game, returnDestination, bagMode))
                    return
                }

                controls[3].contains(point.x, point.y) -> {
                    rememberView()
                    game.navigate(
                        CustomizationScreen(
                            game,
                            returnToPausedGame = returnDestination == ShopReturnDestination.PAUSED_GAME,
                            returnToBag = true,
                            bagReturnDestination = returnDestination,
                            bagReturnMode = bagMode,
                            bagReturnToShop = returnToShop
                        )
                    )
                    return
                }

                controls[4].contains(point.x, point.y) -> {
                    preview.skip()
                    visualHelp.open(VisualHelpOverlay.Topic.ITEMS)
                    guideMode = false
                    scrollOffset = 0f
                    scrollTarget = 0f
                    previewWaitType = null
                    feedback = ""
                    return
                }

                else -> {
                    inventoryTouchActive = inventoryArea.contains(point.x, point.y)
                    pressedIndex = if (inventoryTouchActive) {
                        (0 until count).firstOrNull {
                            slotAt(it, slots).contains(point.x, point.y)
                        } ?: -1
                    } else {
                        -1
                    }
                    touchStartY = point.y
                    lastTouchY = point.y
                    dragged = false
                    scrollVelocity = 0f
                }
            }
        }
        if (inventoryTouchActive && Gdx.input.isTouched) {
            if (abs(point.y - touchStartY) > 14f) {
                dragged = true
                previewWaitType = null
            }
            val dy = point.y - lastTouchY
            if (dragged && maxScroll > 0f) {
                // viewport.unproject gives an increasing Y for an upward swipe.
                // Increasing content offset moves lower inventory rows into the clipped window.
                scrollTarget = bagScrollAfterDrag(scrollTarget, dy, maxScroll)
                scrollOffset = scrollTarget
                scrollVelocity = dy / delta.coerceAtLeast(.016f)
            }
            lastTouchY = point.y
        } else if (inventoryTouchActive) {
            if (dragged) {
                if (!game.progress.settings.reduceMotion) {
                    scrollTarget = (scrollTarget + scrollVelocity * .10f).coerceIn(0f, maxScroll)
                }
            } else if (pressedIndex >= 0 && inventoryArea.contains(point.x, point.y) &&
                slotAt(pressedIndex, slots).contains(point.x, point.y)
            ) {
                if (guideMode) selectedGuidePowerUp = pressedIndex else selectedCharm = pressedIndex
                previewWaitType = entries()[pressedIndex]
                previewWaitElapsed = 0f
                feedback = ""
            }
            inventoryTouchActive = false
            pressedIndex = -1
        }
    }

    private fun useSelectedCharm() {
        if (guideMode) return
        previewWaitType = null
        val type = selectedType()
        val level = volleyPausedState?.first ?: return
        val session = volleySession ?: return
        when (val result = VolleyItemController(session, itemInventory).tryUseVolleyItem(type)) {
            is VolleyItemUseResult.Applied -> {
                session.pause()
                val prior = volleyPausedState?.second
                game.volleyPausedSession.save(level.id,
                    session.snapshot().copy(earnedOrbUids = prior?.earnedOrbUids,
                        orbRunId = prior?.orbRunId), session.rewardedRevivesUsed)
                goBack()
            }

            is VolleyItemUseResult.Rejected -> feedback = result.reason
        }
    }

    private fun goBack() {
        rememberView()
        when {
            returnToShop -> game.navigate(ShopScreen(game, returnDestination, bagMode))
            returnDestination == ShopReturnDestination.PAUSED_GAME -> game.resumePausedGame(showLoading = false)
            else -> game.openMenu()
        }
    }

    private fun rememberView() {
        rememberedCharm = selectedCharm
        rememberedGuidePowerUp = selectedGuidePowerUp
        rememberedScroll = scrollOffset
    }

    private fun entries(): List<PowerUpType> = SHOP_ELIGIBLE_TYPES
    private fun selectedType(): PowerUpType = entries()[selectedCharm.coerceIn(entries().indices)]
    private fun drawText(text: String, x: Float, y: Float, width: Float, color: Color, scale: Float) {
        val font = game.assets.smallFont
        val oldColor = Color(font.color)
        val sx = font.data.scaleX
        val sy = font.data.scaleY
        font.color = color
        font.data.setScale(scale * uiTextScale)
        textLayout.setText(font, text)
        if (textLayout.width > width && textLayout.width > 0f) {
            font.data.setScale(scale * uiTextScale * width / textLayout.width)
        }
        font.draw(batch, text, x, y, width, Align.center, false)
        font.data.setScale(sx, sy)
        font.color = oldColor
    }

    private companion object {
        var rememberedCharm = 0
        var rememberedGuidePowerUp = 0
        var rememberedScroll = 0f
        const val PREVIEW_DELAY_SECONDS = 3f
    }
}
