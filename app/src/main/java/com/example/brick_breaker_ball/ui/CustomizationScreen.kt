/*
 * المسار: app/src/main/java/com/example/brick_breaker_ball/ui/CustomizationScreen.kt
 * الوظيفة: اختيار الكرة وتجهيزها من لوحة التخصيص.
 * الحسابات: قص قائمة الكرات وتمريرها ومواضع اللمس.
 */

package com.example.brick_breaker_ball.ui

import com.badlogic.gdx.Gdx
import com.badlogic.gdx.Input
import com.badlogic.gdx.graphics.Color
import com.badlogic.gdx.graphics.GL20
import com.badlogic.gdx.graphics.g2d.TextureRegion
import com.badlogic.gdx.math.Rectangle
import com.badlogic.gdx.math.Vector3
import com.example.brick_breaker_ball.BrickBreakerGame
import com.example.brick_breaker_ball.cosmetics.BallAbilityCatalog
import com.example.brick_breaker_ball.cosmetics.BallCollectionCatalog
import com.example.brick_breaker_ball.cosmetics.BallRarity
import com.example.brick_breaker_ball.cosmetics.BallSpriteDefinition
import com.example.brick_breaker_ball.cosmetics.VolleyBallSelection
import com.example.brick_breaker_ball.gameplay.Ball
import com.example.brick_breaker_ball.gameplay.GameplayMode

internal object CustomizationHitTesting {
    const val BALL_LIST_BOTTOM = 535f
    const val BALL_LIST_TOP = 1105f

    fun visibleBallCard(rect: Rectangle, clipBottom: Float = BALL_LIST_BOTTOM, clipTop: Float = BALL_LIST_TOP): Rectangle? {
        val bottom = maxOf(rect.y, clipBottom)
        val top = minOf(rect.y + rect.height, clipTop)
        if (top <= bottom) return null
        return Rectangle(rect.x, bottom, rect.width, top - bottom)
    }
}

class CustomizationScreen(
    game: BrickBreakerGame,
    private val returnToPausedGame: Boolean = false,
    private val returnToBag: Boolean = false,
    private val bagReturnDestination: ShopReturnDestination = ShopReturnDestination.MAIN_MENU,
    private val bagReturnMode: GameplayMode = game.progress.settings.gameplayMode,
    private val bagReturnToShop: Boolean = false,
    private val returnToShop: Boolean = false,
    private val shopReturnDestination: ShopReturnDestination = ShopReturnDestination.MAIN_MENU,
    private val shopReturnMode: GameplayMode = game.progress.settings.gameplayMode,
    private val initialBallSpriteName: String? = null
) : ForgeScreen(game) {
    private var ballRarityFilter = BallCollectionCatalog.offer(initialBallSpriteName
        ?: VolleyBallSelection.selectionSprite(game.progress.settings)).rarity
    private val powerPreview by lazy { BallPowerPreview(game, returnToPausedGame) }
    private val visualHelp = VisualHelpOverlay()
    private var ballScrollOffset = 0f
    private var previewBallKey: String? = initialBallSpriteName?.let { sprite ->
        game.assets.cosmetics.balls.firstOrNull { it.spriteName == sprite }?.let { "${it.groupName}/${it.spriteName}" }
    }
    private var initialFocusPending = initialBallSpriteName != null
    private var boardTouchActive = false
    private var boardTouchMoved = false
    private var boardTouchStartY = 0f
    private var boardTouchLastY = 0f
    private var boardScrollVelocity = 0f
    private val boardContentArea = Rectangle()
    private var boardBallRowStep = 154f
    private var boardBallMaxScroll = 0f

    private fun ballsForThisMode(): List<BallSpriteDefinition> = VolleyBallSelection.inDisplayOrder(game.assets.cosmetics.balls, game.progress.settings.gameplayMode)
        .filter { BallCollectionCatalog.offer(it).rarity == ballRarityFilter }

    private fun isAvailable(ball: BallSpriteDefinition): Boolean = game.developmentAccess.canUseCosmetic(
        VolleyBallSelection.canUse(game.progress.settings.gameplayMode, ball, game.cosmeticProgression.ownsBall(ball))
    )

    override fun show() {
        installMouseWheelHandler(::handleMouseWheel)
    }
    override fun hide() {
        removeMouseWheelHandler()
    }
    override fun render(delta: Float) {
        renderBallsBoard(delta)
    }

    /** Always equip the same selected entry that the preview is showing. */
    private fun selectedBallDefinition(): BallSpriteDefinition? {
        val balls = ballsForThisMode()
        val settings = game.progress.settings
        return previewBallKey?.let { key -> balls.firstOrNull { "${it.groupName}/${it.spriteName}" == key } }
            ?: balls.firstOrNull { VolleyBallSelection.isEquipped(settings, it) && isAvailable(it) }
            ?: balls.firstOrNull(::isAvailable)
    }

    /** Ball selection uses the authored scroll board while retaining the existing progression rules. */
    private fun renderBallsBoard(delta: Float) {
        visualHelp.update(delta, game.progress.settings.reduceMotion)
        beginStaticBackground(com.badlogic.gdx.graphics.g2d.TextureRegion(game.assets.splash), .68f)
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
        val titleArea = atlas.placement("scroll_board", "scroll_board_title_area", board)
        val content = atlas.placement("scroll_board", "scroll_board_content_area", board)
        val back = atlas.placement("scroll_board", "scroll_board_back_button_area", board)
        val use = atlas.placement("scroll_board", "scroll_board_use_button_area", board)
        fittedText(
            game.assets.titleFont,
            "BALLS",
            titleArea.x + 12f,
            titleArea.y + titleArea.height * .68f,
            titleArea.width * .94f,
            .72f
        )
        // Two wide rows leave enough room for LEGENDARY and make the labels readable on phones.
        val rarityTabGap = 10f
        val rarityTabHeight = minOf(board.height * .049f, 78f)
        val rarityTabWidth = (content.width - rarityTabGap) / 2f
        val ballListArea = Rectangle(
            content.x,
            content.y,
            content.width,
            content.height - rarityTabHeight - 16f
        )
        boardContentArea.set(ballListArea)
        val rarityTabs = BallRarity.entries.mapIndexed { index, rarity ->
            val column = index % 2
            val row = index / 2
            val rect = Rectangle(
                content.x + column * (rarityTabWidth + rarityTabGap),
                content.y + content.height + 6f - row * (rarityTabHeight + 6f),
                rarityTabWidth,
                rarityTabHeight
            )
            VisualGameUi.panel(batch, game.assets, rect, ballRarityFilter == rarity)
            val stars = rarity.ordinal + 1
            val side = minOf(rect.height * .60f, rect.width / 6f)
            repeat(stars) { star ->
                VisualGameUi.icon(batch, game.assets, VisualGameUi.Icon.STAR,
                    Rectangle(rect.x + (rect.width - stars * side) / 2f + star * side,
                        rect.y + rect.height * .20f, side, side))
            }
            rarity to rect
        }

        val balls = ballsForThisMode()
        val rowHeight = minOf(142f, content.height * .19f)
        val rowStep = rowHeight + 12f
        val maxScroll = ((balls.size - 1) * rowStep + rowHeight + 20f - ballListArea.height)
            .coerceAtLeast(0f)
        boardBallRowStep = rowStep
        boardBallMaxScroll = maxScroll
        if (initialFocusPending) {
            val selectedIndex = balls.indexOfFirst { it.spriteName == initialBallSpriteName }
            ballScrollOffset = (selectedIndex.coerceAtLeast(0) * rowStep).coerceIn(0f, maxScroll)
            initialFocusPending = false
        }
        if (!boardTouchActive && boardScrollVelocity != 0f && !game.progress.settings.reduceMotion) {
            ballScrollOffset = (ballScrollOffset + boardScrollVelocity * delta).coerceIn(0f, maxScroll)
            boardScrollVelocity *= (1f - 8f * delta.coerceIn(0f, .1f)).coerceAtLeast(0f)
            if (kotlin.math.abs(boardScrollVelocity) < 8f || ballScrollOffset == 0f || ballScrollOffset == maxScroll) {
                boardScrollVelocity = 0f
            }
        }
        ballScrollOffset = ballScrollOffset.coerceIn(0f, maxScroll)

        batch.flush()
        val clipBottom = Vector3(ballListArea.x, ballListArea.y, 0f)
        val clipTop = Vector3(
            ballListArea.x + ballListArea.width,
            ballListArea.y + ballListArea.height,
            0f
        )
        viewport.project(clipBottom)
        viewport.project(clipTop)
        Gdx.gl.glEnable(GL20.GL_SCISSOR_TEST)
        Gdx.gl.glScissor(
            clipBottom.x.toInt(),
            clipBottom.y.toInt(),
            (clipTop.x - clipBottom.x).toInt().coerceAtLeast(1),
            (clipTop.y - clipBottom.y).toInt().coerceAtLeast(1)
        )
        balls.forEachIndexed { index, definition ->
            val row = Rectangle(
                ballListArea.x + 10f,
                ballListArea.y + ballListArea.height - rowHeight - 10f - index * rowStep + ballScrollOffset,
                ballListArea.width - 20f,
                rowHeight
            )
            if (row.y + row.height < ballListArea.y || row.y > ballListArea.y + ballListArea.height) return@forEachIndexed
            val key = "${definition.groupName}/${definition.spriteName}"
            val owned = isAvailable(definition)
            val equipped = owned && VolleyBallSelection.isEquipped(game.progress.settings, definition)
            val selected = previewBallKey == key || (previewBallKey == null && equipped)
            batch.setColor(
                if (selected) .95f else .70f,
                if (selected) .86f else .70f,
                if (selected) .65f else .70f,
                if (selected) .32f else .15f
            )
            batch.draw(game.assets.solidUiRegion, row.x, row.y, row.width, row.height)
            batch.color = Color.WHITE
            val iconBox = Rectangle(row.x + 12f, row.y + 8f, rowHeight - 16f, rowHeight - 16f)
            val iconFrame = atlas.contain("large_stone_item_panel", iconBox)
            batch.draw(
                atlas.requireRegion("large_stone_item_panel"),
                iconFrame.x,
                iconFrame.y,
                iconFrame.width,
                iconFrame.height
            )
            game.assets.cosmetics.ballRegion(definition)?.let {
                drawBallSquare(
                    it,
                    iconBox.x + iconBox.width * .20f,
                    iconBox.y + iconBox.height * .20f,
                    iconBox.width * .60f
                )
            }
            val textX = iconBox.x + iconBox.width + 15f
            val textWidth = (row.x + row.width - textX - 12f).coerceAtLeast(20f)
            fittedText(
                game.assets.hudLabelFont,
                definition.name.uppercase(),
                textX,
                row.y + row.height * .82f,
                textWidth,
                .62f
            )
            val offer = BallCollectionCatalog.offer(definition)
            val ownershipText = when {
                equipped -> "EQUIPPED"
                owned -> "OWNED"
                offer.coins != null -> "${offer.coins} COINS" + (offer.stage?.let { " / STAGE $it" } ?: "")
                offer.gems != null -> "${offer.gems} GEMS"
                else -> game.cosmeticProgression.ballRequirement(definition)
            }
            game.assets.smallFont.color = if (owned) Color(.55f, .90f, 1f, 1f) else Color(.98f, .82f, .48f, 1f)
            fittedText(game.assets.smallFont, ownershipText, textX, row.y + row.height * .56f, textWidth, .42f)
            game.assets.smallFont.color = Color.WHITE
            val ability = BallAbilityCatalog.profileForBall(definition)
            VisualGameUi.stats(batch, game.assets,
                Rectangle(textX, row.y + row.height * .12f, textWidth, row.height * .33f),
                powerPreview.balls(ability), powerPreview.damage(ability), power = ability.baseHitDamage)
            if (equipped || !owned) VisualGameUi.icon(batch, game.assets,
                if (owned) VisualGameUi.Icon.CHECK else VisualGameUi.Icon.LOCK,
                Rectangle(iconBox.x + iconBox.width * .65f, iconBox.y, iconBox.width * .34f, iconBox.height * .34f))
        }
        batch.flush()
        Gdx.gl.glDisable(GL20.GL_SCISSOR_TEST)

        val chosen = selectedBallDefinition()
        val chosenOwned = chosen != null && isAvailable(chosen)
        val canEquip = chosen != null && isAvailable(chosen) &&
            !VolleyBallSelection.isEquipped(game.progress.settings, chosen)
        batch.draw(atlas.requireRegion("ui_button_red_blank"), back.x, back.y, back.width, back.height)
        batch.draw(
            atlas.requireRegion(if (canEquip || !chosenOwned) "ui_button_green_blank" else "ui_button_dark_blank"),
            use.x,
            use.y,
            use.width,
            use.height
        )
        VisualGameUi.icon(batch, game.assets, VisualGameUi.Icon.BACK,
            Rectangle(back.x + back.width * .30f, back.y + back.height * .12f,
                back.width * .40f, back.height * .76f))
        VisualGameUi.icon(batch, game.assets, when {
            !chosenOwned -> VisualGameUi.Icon.COIN
            canEquip -> VisualGameUi.Icon.EQUIP
            else -> VisualGameUi.Icon.CHECK
        }, Rectangle(use.x + use.width * .30f, use.y + use.height * .12f,
            use.width * .40f, use.height * .76f))
        if (chosen != null) {
            val ability = BallAbilityCatalog.profileForBall(chosen)
            VisualGameUi.stats(batch, game.assets,
                Rectangle(content.x + 16f, back.y + back.height + 10f, content.width - 32f, 44f),
                powerPreview.balls(ability), powerPreview.damage(ability), power = ability.baseHitDamage)
        }
        val helpButton = Rectangle(titleArea.x + titleArea.width - 64f,
            titleArea.y + titleArea.height * .30f, 64f, 64f)
        VisualGameUi.helpButton(batch, game.assets, helpButton)
        val helpScreen = Rectangle(visibleLeft, visibleBottom, visibleWidth, visibleHeight)
        visualHelp.focus.set(if (visualHelp.page == 0) ballListArea else if (visualHelp.page == 3) use else content)
        val helpAbility = chosen?.let(BallAbilityCatalog::profileForBall) ?: BallAbilityCatalog.default
        visualHelp.draw(batch, game.assets, helpScreen, game.progress.settings.reduceMotion,
            balls = powerPreview.balls(helpAbility), damage = powerPreview.damage(helpAbility),
            spreadAvailable = helpAbility.shotgunAvailable)
        end()

        if (visualHelp.active) {
            boardTouchActive = false
            boardScrollVelocity = 0f
            if (Gdx.input.isKeyJustPressed(Input.Keys.BACK) || Gdx.input.isKeyJustPressed(Input.Keys.ESCAPE)) visualHelp.tap(true)
            else if (Gdx.input.justTouched()) visualHelp.tap(VisualHelpOverlay.closeBounds(helpScreen).contains(touchPoint().x, touchPoint().y))
            return
        }
        if (tapped(helpButton)) {
            visualHelp.open(VisualHelpOverlay.Topic.BALLS)
            return
        }
        if (Gdx.input.isKeyJustPressed(Input.Keys.BACK) || Gdx.input.isKeyJustPressed(Input.Keys.ESCAPE)) {
            leaveCustomization()
            return
        }
        val touch = touchPoint()
        if (Gdx.input.justTouched()) {
            val rarityHit = rarityTabs.firstOrNull { it.second.contains(touch.x, touch.y) }
            when {
                back.contains(touch.x, touch.y) -> {
                    leaveCustomization()
                    return
                }

                rarityHit != null -> {
                    ballRarityFilter = rarityHit.first
                    ballScrollOffset = 0f
                    previewBallKey = null
                    boardScrollVelocity = 0f
                    return
                }

                use.contains(touch.x, touch.y) -> {
                    if (canEquip) {
                        VolleyBallSelection.equip(game.progress.settings, chosen)
                        game.progress.saveSettings()
                        game.uiFeedback.emit(UiFeedbackKind.CONFIRM)
                    } else if (!chosenOwned) {
                        game.navigate(
                            ShopScreen(
                                game,
                                if (returnToPausedGame) ShopReturnDestination.PAUSED_GAME else ShopReturnDestination.MAIN_MENU,
                                game.progress.settings.gameplayMode,
                                "BALLS",
                                chosen?.spriteName
                            )
                        )
                    }
                    return
                }

                ballListArea.contains(touch.x, touch.y) -> {
                    boardTouchActive = true
                    boardTouchMoved = false
                    boardTouchStartY = touch.y
                    boardTouchLastY = touch.y
                    boardScrollVelocity = 0f
                }
            }
        }
        if (boardTouchActive && Gdx.input.isTouched) {
            // Rows use +offset in their Y position, so the content follows the finger.
            val drag = touch.y - boardTouchLastY
            ballScrollOffset = (ballScrollOffset + drag).coerceIn(0f, maxScroll)
            if (kotlin.math.abs(touch.y - boardTouchStartY) > 20f) boardTouchMoved = true
            if (boardTouchMoved) boardScrollVelocity = (drag / delta.coerceAtLeast(.016f)).coerceIn(-1500f, 1500f)
            boardTouchLastY = touch.y
        } else if (boardTouchActive) {
            if (!boardTouchMoved && ballListArea.contains(touch.x, touch.y)) {
                val fromTop = ballListArea.y + ballListArea.height - 10f - touch.y + ballScrollOffset
                val index = (fromTop / rowStep).toInt()
                if (index in balls.indices && fromTop % rowStep <= rowHeight) {
                    val selectedBall = balls[index]
                    val key = "${selectedBall.groupName}/${selectedBall.spriteName}"
                    if (previewBallKey == key) {
                        game.navigate(BallPowerDetailScreen(game, selectedBall, returnToPausedGame,
                            returnToCollection = {
                                game.navigate(CustomizationScreen(game, returnToPausedGame, returnToBag,
                                    bagReturnDestination, bagReturnMode, bagReturnToShop, returnToShop,
                                    shopReturnDestination, shopReturnMode, selectedBall.spriteName))
                            }))
                    } else {
                        previewBallKey = key
                    }
                }
            }
            boardTouchActive = false
        }
    }

    private fun leaveCustomization() {
        game.progress.saveSettings()
        when {
            returnToBag -> game.navigate(VolleyOrbsScreen(game, bagReturnDestination, bagReturnMode, bagReturnToShop))
            returnToShop -> game.navigate(ShopScreen(game, shopReturnDestination, shopReturnMode, "BALLS",
                selectedBallDefinition()?.spriteName))
            returnToPausedGame -> game.resumePausedGame(showLoading = false)
            else -> game.openMenu()
        }
    }

    private fun handleMouseWheel(amountY: Float): Boolean {
        if (amountY == 0f) return false
        val point = touchPoint()
        if (!boardContentArea.contains(point.x, point.y)) return false
        ballScrollOffset = (ballScrollOffset + amountY * boardBallRowStep * 2f)
            .coerceIn(0f, boardBallMaxScroll)
        boardScrollVelocity = 0f
        return true
    }

    /** رسم الكرة داخل المربع مع الحفاظ على أبعاد الصورة الأصلية. */
    private fun drawBallSquare(region: TextureRegion, x: Float, y: Float, diameter: Float) {
        val scale = minOf(diameter / region.regionWidth, diameter / region.regionHeight)
        val drawWidth = region.regionWidth * scale
        val drawHeight = region.regionHeight * scale
        batch.color = Color.WHITE
        batch.draw(region, x + (diameter - drawWidth) / 2f, y + (diameter - drawHeight) / 2f, drawWidth, drawHeight)
    }
}
