package com.example.brick_breaker_ball.ui

import com.badlogic.gdx.Gdx
import com.badlogic.gdx.Input
import com.badlogic.gdx.graphics.Color
import com.badlogic.gdx.graphics.g2d.GlyphLayout
import com.badlogic.gdx.math.Rectangle
import com.badlogic.gdx.scenes.scene2d.utils.ScissorStack
import com.badlogic.gdx.utils.Align
import com.example.brick_breaker_ball.BrickBreakerGame
import com.example.brick_breaker_ball.campaign.ProgressStore
import com.example.brick_breaker_ball.gameplay.GameplayMode
import com.example.brick_breaker_ball.gameplay.VolleyOrbDefinition
import kotlin.math.abs
import kotlin.math.ceil

/** Owned RPG Orb copies and the selected eight-slot loadout. Catalog previews live in Shop. */
class VolleyOrbsScreen(
    game: BrickBreakerGame,
    private val returnDestination: ShopReturnDestination,
    private val bagMode: GameplayMode,
    private val returnToShop: Boolean,
    guideFromShop: Boolean = false
) : ForgeScreen(game) {
    private companion object {
        var rememberedUid: String? = null
        var rememberedSlot = 0
        var rememberedFilter = 0
        var rememberedRaritySort = false
        var rememberedScroll = 0f
    }
    private val actions = mutableListOf<Pair<Rectangle, () -> Unit>>()
    private val copyHits = mutableListOf<Pair<Rectangle, String>>()
    private val bonusHits = mutableListOf<Pair<Rectangle, String>>()
    private val measure = GlyphLayout()
    private var selectedUid: String? = rememberedUid
    private var selectedSlot = rememberedSlot
    private var filter = rememberedFilter
    private var raritySort = rememberedRaritySort
    private var scroll = rememberedScroll
    private var maxScroll = 0f
    private var detailScroll = 0f
    private var detailMaxScroll = 0f
    private var detailOpen = false
    private var forgeOpen = false
    private var confirmSale = false
    private var multiSelect = false
    private var confirmBulkSale = false
    private var optionsOpen = false
    private var optionsActionStart = 0
    private var optionsElapsed = 0f
    private var feedbackTime = 0f
    private val selectedUids = linkedSetOf<String>()
    private var selectedBonus: String? = null
    private var feedback = ""
        set(value) {
            field = value
            feedbackTime = if (value.isEmpty()) 0f else 3.5f
        }
    private var touchInCollection = false
    private var touchStartY = 0f
    private var lastTouchY = 0f
    private var dragged = false
    private var collectionArea = Rectangle()
    private var detailArea = Rectangle()
    private var detailActionStart = 0
    private var confirmActionStart = 0
    private var forgeActionStart = 0
    private var bulkActionStart = 0
    private var guidePage = if (guideFromShop) 0 else -1
    private val guideAnimation = GuideAnimation()
    private var skipGuideAfterExit = false
    private var guideIsReplay = false
    private var guideFeedback = ""
    private val guideFocus = Rectangle()
    private var selectionPulseTime = 0f
    private val selectionPulseRect = Rectangle()

    init {
        if (guideFromShop) {
            filter = 0
            scroll = 0f
            selectedSlot = (0 until ProgressStore.ORB_SLOTS_PER_LOADOUT)
                .firstOrNull(game.progress::isOrbSlotUnlocked) ?: 0
        }
    }

    override fun show() {
        installMouseWheelHandler { amountY ->
            if (guidePage >= 0 || optionsOpen) return@installMouseWheelHandler true
            val point = touchPoint()
            when {
                amountY == 0f -> false
                detailOpen && !forgeOpen && !confirmSale && detailArea.contains(point.x, point.y) -> {
                    detailScroll = (detailScroll + amountY * detailArea.height * .22f)
                        .coerceIn(0f, detailMaxScroll)
                    true
                }
                !detailOpen && !confirmBulkSale && collectionArea.contains(point.x, point.y) -> {
                    scroll = (scroll + amountY * collectionArea.height * .28f)
                        .coerceIn(0f, maxScroll)
                    true
                }
                else -> false
            }
        }
    }

    override fun hide() = removeMouseWheelHandler()

    override fun render(delta: Float) {
        selectionPulseTime = (selectionPulseTime - delta.coerceIn(0f, .05f)).coerceAtLeast(0f)
        optionsElapsed = if (optionsOpen) optionsElapsed + delta.coerceIn(0f, .05f) else 0f
        feedbackTime = (feedbackTime - delta.coerceIn(0f, .05f)).coerceAtLeast(0f)
        if (feedbackTime == 0f && !detailOpen && !confirmBulkSale) feedback = ""
        if (guidePage >= 0) {
            if (!game.uiMotion.active) guideAnimation.update(delta, game.progress.settings.reduceMotion)
            if (guideAnimation.exitFinished) {
                val lastPage = if (game.progress.orbStashCount == 0) 1 else 3
                if (skipGuideAfterExit || guidePage >= lastPage) {
                    guidePage = -1
                    if (!guideIsReplay) game.heroGuideProgress.completeShopOrbs()
                } else guidePage++
                guideFeedback = ""
                guideAnimation.reset()
            }
        }
        beginStaticBackground(game.assets.worldMapImage(1, bagMode), .62f)
        val insets = safeInsets
        val area = Rectangle(visibleLeft + insets.left + 20f, visibleBottom + insets.bottom + 20f,
            visibleWidth - insets.left - insets.right - 40f,
            visibleHeight - insets.top - insets.bottom - 40f)
        val atlas = game.assets.boards2
        val board = atlas.contain("scroll_board", area)
        batch.color = Color.WHITE
        batch.draw(atlas.requireRegion("scroll_board"), board.x, board.y, board.width, board.height)
        actions.clear()
        copyHits.clear()
        bonusHits.clear()
        val x = board.x + board.width * .07f
        val w = board.width * .86f
        val h = board.height
        val copies = visibleCopies()
        if ((!detailOpen && copies.none { it.uid == selectedUid }) ||
            (selectedUid != null && game.progress.orbCopy(selectedUid!!) == null)) {
            selectedUid = copies.firstOrNull()?.uid
        }
        val selected = selectedUid?.let(game.progress::orbCopy)
        label("ORBS", x + w * .14f, board.y + h * .871f, w * .63f, 1.05f, Color.WHITE)
        VisualGameUi.number(batch, game.assets, "${game.progress.orbStashCount}/150",
            Rectangle(x + w * .26f, board.y + h * .793f, w * .48f, h * .043f), .68f)
        val helpButton = Rectangle(x + w * .85f, board.y + h * .831f, h * .057f, h * .057f)
        VisualGameUi.helpButton(batch, game.assets, helpButton)
        actions += helpButton to {
            guidePage = 0; guideIsReplay = true; skipGuideAfterExit = false; guideAnimation.reset()
            guideFeedback = ""
            detailOpen = false; forgeOpen = false; confirmSale = false; confirmBulkSale = false
            optionsOpen = false
            multiSelect = false; selectedUids.clear()
            filter = 0; scroll = 0f
            selectedSlot = (0 until ProgressStore.ORB_SLOTS_PER_LOADOUT)
                .firstOrNull(game.progress::isOrbSlotUnlocked) ?: 0
            touchInCollection = false
        }

        val tabY = board.y + h * .718f
        tab(Rectangle(x, tabY, w * .32f, h * .051f), "ORBS", true) {}
        tab(Rectangle(x + w * .34f, tabY, w * .32f, h * .051f), "BALLS", false) {
            rememberView()
            game.navigate(CustomizationScreen(game,
                returnToPausedGame = returnDestination == ShopReturnDestination.PAUSED_GAME,
                returnToBag = true, bagReturnDestination = returnDestination,
                bagReturnMode = bagMode, bagReturnToShop = returnToShop))
        }
        tab(Rectangle(x + w * .68f, tabY, w * .32f, h * .051f), "SHOP", false) {
            rememberView()
            game.navigate(ShopScreen(game, returnDestination, bagMode, initialTabName = "ORBS"))
        }

        val equippedCount = (0 until ProgressStore.ORB_SLOTS_PER_LOADOUT).count {
            game.progress.orbInSlot(it) != null
        }
        sectionTitle("EQUIPPED $equippedCount/${ProgressStore.ORB_SLOTS_PER_LOADOUT}", x, board.y + h * .684f, w * .72f)
        label("SET ${game.progress.selectedOrbLoadout}", x + w * .73f,
            board.y + h * .684f, w * .27f, .43f, Color.LIGHT_GRAY)
        repeat(ProgressStore.ORB_SLOTS_PER_LOADOUT) { index ->
            val row = index / 4
            val column = index % 4
            val slot = Rectangle(x + column * w / 4f + 2f,
                board.y + h * (.600f - row * .082f), w / 4f - 4f, h * .075f)
            val orb = game.progress.orbInSlot(index)
            val unlocked = game.progress.isOrbSlotUnlocked(index)
            tile(slot, orb, index == selectedSlot, unlocked,
                if (unlocked) "${index + 1}  ${orb?.family ?: "EMPTY"}" else "${index + 1}  LOCKED")
            actions += slot to {
                if (!multiSelect) {
                    selectedSlot = index
                    if (selectedUid == null) selectedUid = orb?.uid
                    showFeedback(if (!unlocked) slotHint(index) else "SLOT ${index + 1} SELECTED")
                }
            }
        }

        val collectionTitle = if (multiSelect) "SELECT TO SELL" else when (filter) {
            1 -> "NEW ORBS"
            2 -> "IN USE"
            3 -> "LOCKED ORBS"
            else -> "YOUR ORBS"
        }
        sectionTitle("$collectionTitle ${copies.size}", x, board.y + h * .480f, w * .65f)
        labeledAction(Rectangle(x + w * .68f, board.y + h * .461f, w * .32f, h * .043f),
            "MORE", null, false) {
            optionsOpen = true; optionsElapsed = 0f; touchInCollection = false
        }
        collectionArea.set(x, board.y + h * .216f, w, h * .235f)
        drawCollection(copies)
        val summary = when {
            feedback.isNotEmpty() -> feedback
            multiSelect -> "${selectedUids.size} SELECTED"
            !game.progress.isOrbSlotUnlocked(selectedSlot) -> slotHint(selectedSlot)
            selected != null -> "${selected.family}  -  SLOT ${selectedSlot + 1}"
            else -> "CHOOSE AN ORB"
        }
        label(summary, x + 4f, board.y + h * .184f, w - 8f, .46f, Color.GOLD)
        button(Rectangle(x, board.y + h * .105f, w * .49f, h * .060f),
            if (multiSelect) "SELL ${selectedUids.size}" else if (game.progress.isOrbSlotUnlocked(selectedSlot)) {
                if (game.progress.orbInSlot(selectedSlot) != null &&
                    (selected?.uid == null || game.progress.orbInSlot(selectedSlot)?.uid == selected.uid))
                    "REMOVE" else "EQUIP"
            } else "UNLOCK SLOT", !multiSelect || selectedUids.isNotEmpty()) {
            if (multiSelect) {
                if (selectedUids.any { game.progress.orbCopySaleValue(it) != null }) confirmBulkSale = true
                else showFeedback("CHOOSE ORBS NOT IN USE OR LOCKED")
            } else if (!game.progress.isOrbSlotUnlocked(selectedSlot)) {
                showFeedback(when {
                    game.progress.unlockedLevel < game.progress.orbSlotRequirement(selectedSlot) -> slotHint(selectedSlot)
                    game.progress.unlockOrbSlot(selectedSlot, game.economy::spendCoinsForOrbSlot) -> "SLOT ${selectedSlot + 1} OPEN"
                    else -> "MORE COINS NEEDED"
                })
            } else if (selected?.uid == null) {
                showFeedback(if (game.progress.unequipOrb(selectedSlot)) "REMOVED FROM SLOT ${selectedSlot + 1}"
                    else "CHOOSE AN ORB")
            }
            else if (game.progress.orbInSlot(selectedSlot)?.uid == selected.uid) {
                showFeedback(if (game.progress.unequipOrb(selectedSlot)) "REMOVED" else "SLOT EMPTY")
            } else if (game.progress.equipOrbCopy(selected.uid, selectedSlot)) {
                showFeedback("EQUIPPED IN SLOT ${selectedSlot + 1}")
                game.uiFeedback.emit(UiFeedbackKind.CONFIRM)
            } else {
                showFeedback("CANNOT EQUIP")
                game.uiFeedback.emit(UiFeedbackKind.ERROR)
            }
        }
        button(Rectangle(x + w * .51f, board.y + h * .105f, w * .49f, h * .060f),
            if (multiSelect) "CANCEL" else "DETAILS", false) { if (multiSelect) {
                multiSelect = false; selectedUids.clear()
            } else if (selected != null) { detailOpen = true; detailScroll = 0f;
                selectedBonus = selected.bonusStats.keys.firstOrNull(); game.progress.markOrbCopySeen(selected.uid!!) } }
        button(Rectangle(x, board.y + h * .040f, w, h * .052f), "BACK", false) { goBack() }

        optionsActionStart = actions.size
        if (optionsOpen) drawOptions(board)
        detailActionStart = actions.size
        if (detailOpen && selected != null) {
            if (forgeOpen) drawForge(board, selected) else drawDetails(board, selected)
        }
        bulkActionStart = actions.size
        if (confirmBulkSale) drawBulkConfirmation(board)
        if (selectionPulseTime > 0f && !game.progress.settings.reduceMotion) {
            HeroGuideRenderer.drawFocus(batch, game.assets, selectionPulseRect,
                (.30f - selectionPulseTime) * 2f, false, selectionPulseTime / .30f)
        }
        if (guidePage >= 0) {
            guideFocus.set(when (guidePage) {
                0 -> if (game.progress.orbStashCount == 0) collectionArea else
                    Rectangle(x + (selectedSlot % 4) * w / 4f + 2f,
                        board.y + h * (.600f - (selectedSlot / 4) * .082f), w / 4f - 4f, h * .075f)
                1 -> if (game.progress.orbStashCount == 0)
                    Rectangle(x + w * .68f, tabY, w * .32f, h * .051f) else collectionArea
                2 -> if (game.progress.orbInSlot(selectedSlot)?.uid == selectedUid)
                    Rectangle(x + (selectedSlot % 4) * w / 4f + 2f,
                        board.y + h * (.600f - (selectedSlot / 4) * .082f), w / 4f - 4f, h * .075f)
                else Rectangle(x, board.y + h * .105f, w * .49f, h * .060f)
                else -> Rectangle(x + w * .51f, board.y + h * .105f, w * .49f, h * .060f)
            })
            val screen = Rectangle(visibleLeft, visibleBottom, visibleWidth, visibleHeight)
            VisualHelpOverlay.drawLesson(batch, game.assets, screen, VisualHelpOverlay.Topic.ORBS,
                guidePage, if (game.progress.orbStashCount == 0) 2 else 4, guideAnimation,
                guideFocus, game.progress.settings.reduceMotion,
                selected?.takeIf { game.assets.orbIconsLoaded() }?.let { game.assets.orbIcon(it.id) },
                VisualGameUi.orbEffect(selected?.family ?: ""),
                emptyCollection = game.progress.orbStashCount == 0, lessonKey = guideCaptionKey(),
                orbFamily = selected?.family, actionRequired = guideRequiresAction(),
                statusText = guideFeedback.takeIf { it.isNotEmpty() })
        }
        end()
        handleInput()
    }

    private fun animateSelection(rect: Rectangle) {
        selectionPulseRect.set(rect)
        selectionPulseTime = if (game.progress.settings.reduceMotion) 0f else .30f
    }

    private fun showFeedback(message: String) { feedback = message }

    private fun guideCaptionKey(): String = if (game.progress.orbStashCount == 0) {
        "empty_$guidePage"
    } else when (guidePage) {
        2 -> if (game.progress.orbInSlot(selectedSlot)?.uid == selectedUid) "orbs_equipped" else "orbs_2"
        else -> "orbs_$guidePage"
    }

    private fun sectionTitle(title: String, x: Float, y: Float, width: Float) {
        label(title, x + 4f, y, width - 8f, .50f, Color.WHITE)
    }

    /** Less common controls stay behind one named button instead of six unexplained icons. */
    private fun drawOptions(board: Rectangle) {
        batch.setColor(.04f, .025f, .02f, .80f)
        batch.draw(game.assets.solidUiRegion, board.x, board.y, board.width, board.height)
        batch.color = Color.WHITE
        val reveal = if (game.progress.settings.reduceMotion) 1f else
            (optionsElapsed / .18f).coerceIn(0f, 1f).let { it * it * (3f - 2f * it) }
        val box = Rectangle(board.x + board.width * .08f,
            board.y + board.height * (.19f - .025f * (1f - reveal)),
            board.width * .84f, board.height * .56f)
        VisualGameUi.panel(batch, game.assets, box)
        val x = box.x + box.width * .07f
        val w = box.width * .86f
        val h = box.height
        label("BAG OPTIONS", x, box.y + h * .935f, w, .72f, Color.GOLD)
        // Currencies are relevant here and in Forge, rather than a second unlabeled header row.
        VisualGameUi.icon(batch, game.assets, VisualGameUi.Icon.PLUS,
            Rectangle(x, box.y + h * .81f, h * .055f, h * .055f))
        label("${game.progress.orbUpgradePoints} POINTS", x + w * .09f,
            box.y + h * .851f, w * .40f, .43f, Color.WHITE)
        VisualGameUi.icon(batch, game.assets, VisualGameUi.Icon.COIN,
            Rectangle(x + w * .53f, box.y + h * .81f, h * .055f, h * .055f))
        label("${game.economy.coins}", x + w * .62f, box.y + h * .851f,
            w * .36f, .43f, Color.WHITE)
        label("LOADOUT", x, box.y + h * .775f, w, .45f, Color.LIGHT_GRAY)
        repeat(ProgressStore.ORB_LOADOUT_COUNT) { index ->
            val preset = index + 1
            tab(Rectangle(x + index * w / 4f + 2f, box.y + h * .665f,
                w / 4f - 4f, h * .09f), "SET $preset", game.progress.selectedOrbLoadout == preset) {
                game.progress.selectedOrbLoadout = preset
                feedback = ""; selectedUids.clear()
                selectedUid = game.progress.orbInSlot(selectedSlot)?.uid
            }
        }
        label("SHOW", x, box.y + h * .62f, w, .45f, Color.LIGHT_GRAY)
        val filters = listOf("ALL", "NEW", "IN USE", "LOCKED")
        val icons = listOf(VisualGameUi.Icon.GRID, VisualGameUi.Icon.STAR,
            VisualGameUi.Icon.CHECK, VisualGameUi.Icon.LOCK)
        filters.forEachIndexed { index, title ->
            labeledAction(Rectangle(x + (index % 2) * w * .51f,
                box.y + h * (.50f - (index / 2) * .105f), w * .49f, h * .095f),
                title, icons[index], filter == index) {
                filter = index; scroll = 0f; feedback = ""; selectedUids.clear()
            }
        }
        labeledAction(Rectangle(x, box.y + h * .265f, w, h * .10f),
            if (raritySort) "SORT: RARITY" else "SORT: NEWEST", VisualGameUi.Icon.SORT, false) {
            raritySort = !raritySort; scroll = 0f
        }
        labeledAction(Rectangle(x, box.y + h * .10f, w * .49f, h * .11f),
            if (multiSelect) "CANCEL SALE" else "SELL ORBS", VisualGameUi.Icon.SELL, false) {
            multiSelect = !multiSelect; selectedUids.clear(); optionsOpen = false
            feedback = ""
        }
        labeledAction(Rectangle(x + w * .51f, box.y + h * .10f, w * .49f, h * .11f),
            "DONE", VisualGameUi.Icon.CHECK, true) { optionsOpen = false }
    }

    private fun visibleCopies(): List<VolleyOrbDefinition> {
        val activeUids = if (filter == 2) (0 until ProgressStore.ORB_SLOTS_PER_LOADOUT)
            .mapNotNull { game.progress.orbInSlot(it)?.uid }.toSet() else emptySet()
        val items = game.progress.ownedOrbCopies.filter { copy -> when (filter) {
            1 -> copy.isNew
            2 -> copy.uid?.let { it in activeUids } == true
            3 -> copy.locked
            else -> true
        } }
        return if (raritySort) items.sortedWith(compareByDescending<VolleyOrbDefinition> { it.rank }
            .thenByDescending { it.acquiredAt }) else items.sortedWith(
            compareByDescending<VolleyOrbDefinition> { it.acquiredAt }.thenByDescending { it.uid })
    }

    private fun activeSlot(uid: String): Int? =
        (0 until ProgressStore.ORB_SLOTS_PER_LOADOUT).firstOrNull {
            game.progress.orbInSlot(it)?.uid == uid
        }

    private fun drawCollection(copies: List<VolleyOrbDefinition>) {
        batch.color = Color.WHITE
        batch.draw(game.assets.boards2.requireRegion("scroll_board_content_area"),
            collectionArea.x, collectionArea.y, collectionArea.width, collectionArea.height)
        val cols = 2
        val cellW = collectionArea.width / cols
        val cellH = collectionArea.height * .49f
        maxScroll = (kotlin.math.ceil(copies.size / cols.toFloat()).toInt() * cellH - collectionArea.height)
            .coerceAtLeast(0f)
        scroll = scroll.coerceIn(0f, maxScroll)
        batch.flush()
        val scissors = Rectangle()
        ScissorStack.calculateScissors(camera, batch.transformMatrix, collectionArea, scissors)
        if (ScissorStack.pushScissors(scissors)) {
            try {
                if (copies.isEmpty()) {
                    VisualGameUi.icon(batch, game.assets, VisualGameUi.Icon.ORB,
                        Rectangle(collectionArea.x + collectionArea.width * .41f,
                            collectionArea.y + collectionArea.height * .44f,
                            collectionArea.width * .18f, collectionArea.height * .35f), .4f)
                    label(if (filter == 0) "NO ORBS YET" else "NO MATCHES",
                        collectionArea.x, collectionArea.y + collectionArea.height * .32f,
                        collectionArea.width, .50f, Color.LIGHT_GRAY)
                }
                copies.forEachIndexed { index, orb ->
                    val row = index / cols
                    val column = index % cols
                    val rect = Rectangle(collectionArea.x + column * cellW + 3f,
                        collectionArea.y + collectionArea.height - (row + 1) * cellH + scroll + 3f,
                        cellW - 6f, cellH - 6f)
                    if (rect.y + rect.height < collectionArea.y || rect.y > collectionArea.y + collectionArea.height) return@forEachIndexed
                    val equippedSlot = orb.uid?.let(::activeSlot)
                    tile(rect, orb, if (multiSelect) orb.uid in selectedUids else orb.uid == selectedUid, true,
                        "${orb.family} ${if (orb.locked) "LOCK" else if (orb.isNew) "NEW" else "R${orb.rank}"}", equippedSlot)
                    orb.uid?.let { copyHits += rect to it }
                }
            } finally { batch.flush(); ScissorStack.popScissors() }
        }
        if (maxScroll > 0f) {
            val trackHeight = collectionArea.height
            val thumbHeight = (trackHeight * trackHeight / (trackHeight + maxScroll)).coerceAtLeast(18f)
            batch.setColor(1f, .78f, .35f, .7f)
            batch.draw(game.assets.solidUiRegion, collectionArea.x + collectionArea.width - 3f,
                collectionArea.y + trackHeight - thumbHeight - scroll / maxScroll * (trackHeight - thumbHeight),
                3f, thumbHeight)
            batch.color = Color.WHITE
        }
    }

    private fun drawDetails(board: Rectangle, orb: VolleyOrbDefinition) {
        val x = board.x + board.width * .055f
        val y = board.y + board.height * .115f
        val w = board.width * .89f
        val h = board.height * .76f
        batch.setColor(.08f, .045f, .035f, .97f)
        batch.draw(game.assets.solidUiRegion, x, y, w, h)
        batch.color = Color.WHITE
        label(orb.name, x + 10f, y + h * .94f, w - 20f, .65f, Color.GOLD)
        label("${orb.rarity}  |  ${orb.uid?.let(::activeSlot)?.let { "EQUIPPED: SLOT ${it + 1}" } ?: "NOT EQUIPPED"}",
            x + 10f, y + h * .89f, w - 20f, .43f, Color.WHITE)
        val equipped = game.progress.orbInSlot(selectedSlot)
        label("SLOT ${selectedSlot + 1}: ${equipped?.name ?: "EMPTY"}",
            x + 10f, y + h * .856f, w - 20f, .42f, Color.LIGHT_GRAY)
        val active = orb.uid?.let(::activeSlot) != null
        val effectSide = h * .055f
        VisualGameUi.icon(batch, game.assets, VisualGameUi.orbEffect(orb.family),
            Rectangle(x + 12f, y + h * .77f, effectSide, effectSide))
        label(orbActivationSummary(orb), x + effectSide + 18f, y + h * .811f,
            w - effectSide - 30f, .44f, Color.GOLD)
        label(if (active) "EQUIPPED - APPLIES IN GAMEPLAY" else "EQUIP IN A SLOT TO USE THIS EFFECT",
            x + effectSide + 18f, y + h * .778f, w - effectSide - 30f, .36f, Color.LIGHT_GRAY)
        detailArea.set(x + 10f, y + h * .28f, w - 20f, h * .475f)
        batch.setColor(.15f, .10f, .07f, .9f)
        batch.draw(game.assets.solidUiRegion, detailArea.x, detailArea.y, detailArea.width, detailArea.height)
        batch.color = Color.WHITE
        val entries = (orb.stats.entries.map { it.key to it.value } +
            orb.bonusStats.entries.map { it.key to it.value })
            .filter { it.second != 0f }
        val lineH = h * .048f
        detailMaxScroll = (entries.size * lineH - detailArea.height).coerceAtLeast(0f)
        detailScroll = detailScroll.coerceIn(0f, detailMaxScroll)
        batch.flush()
        val scissors = Rectangle()
        ScissorStack.calculateScissors(camera, batch.transformMatrix, detailArea, scissors)
        if (ScissorStack.pushScissors(scissors)) {
            try {
                entries.forEachIndexed { index, (name, value) ->
                    val lineY = detailArea.y + detailArea.height - (index + 1) * lineH + detailScroll
                    if (lineY + lineH < detailArea.y || lineY > detailArea.y + detailArea.height) return@forEachIndexed
                    val range = orb.rollRanges[name]
                    val isBonus = name in orb.bonusStats
                    val amount = if (value == value.toInt().toFloat()) value.toInt().toString() else value.toString()
                    val rangeText = range?.let {
                        val quality = if (it.max > it.min)
                            (((value - it.min) / (it.max - it.min)) * 100f).toInt().coerceIn(0, 100)
                        else 100
                        " [${it.min}-${it.max}] ${if (quality >= 100) "MAX" else "Q$quality%"}"
                    }.orEmpty()
                    val comparison = equipped?.takeIf { it.uid != orb.uid }?.let {
                        val delta = orb.stat(name) - it.stat(name)
                        if (delta > 0f) " +${delta}" else if (delta < 0f) " $delta" else ""
                    }.orEmpty()
                    val prefix = if (isBonus) "BONUS " else ""
                    val selected = isBonus && selectedBonus == name
                    val unit = if (range?.type == "percent") "%" else ""
                    label("${if (selected) "> " else ""}$prefix${name.replace('_', ' ').uppercase()}: $amount$unit$rangeText$comparison",
                        detailArea.x + 5f, lineY + lineH * .72f, detailArea.width - 10f,
                        .43f, if (selected) Color.GOLD else Color.WHITE)
                    if (isBonus) bonusHits += Rectangle(detailArea.x, lineY, detailArea.width, lineH) to name
                }
            } finally { batch.flush(); ScissorStack.popScissors() }
        }
        val buttonH = h * .069f
        val row = y + h * .135f
        val cellW = (w - 30f) / 4f
        fun action(index: Int, title: String, callback: () -> Unit) {
            button(Rectangle(x + 6f + index * (cellW + 6f), row, cellW, buttonH), title, false, callback)
        }
        action(0, if (orb.locked) "UNLOCK" else "LOCK") {
            game.progress.setOrbCopyLocked(orb.uid!!, !orb.locked)
            feedback = if (orb.locked) "UNLOCKED" else "LOCKED"
        }
        action(1, "SELL") {
            if (game.progress.orbCopySaleValue(orb.uid!!) != null) confirmSale = true
            else feedback = "LOCKED OR EQUIPPED ORB CANNOT BE SOLD"
        }
        action(2, "FORGE") {
            forgeOpen = true
            feedback = ""
        }
        action(3, "CLOSE") { detailOpen = false; confirmSale = false }
        confirmActionStart = actions.size
        if (confirmSale) {
            val box = Rectangle(x + w * .08f, y + h * .37f, w * .84f, h * .26f)
            batch.setColor(.06f, .035f, .025f, .98f)
            batch.draw(game.assets.solidUiRegion, box.x, box.y, box.width, box.height)
            batch.color = Color.WHITE
            label("SELL FOR ${game.progress.orbCopySaleValue(orb.uid!!) ?: 0} COINS?",
                box.x + 8f, box.y + box.height * .78f, box.width - 16f, .55f, Color.GOLD)
            button(Rectangle(box.x + 8f, box.y + 8f, box.width * .45f, box.height * .34f), "CANCEL", false) {
                confirmSale = false
            }
            button(Rectangle(box.x + box.width * .53f, box.y + 8f, box.width * .45f, box.height * .34f), "SELL", true) {
                val sold = game.progress.sellOrbCopy(orb.uid!!, game.economy::creditOrbSale)
                feedback = if (sold == null) "LOCKED OR EQUIPPED ORB CANNOT BE SOLD" else "SOLD FOR $sold COINS"
                if (sold != null) { selectedUid = null; detailOpen = false }
                confirmSale = false
            }
        }
    }

    /** A separate Forge page keeps crafting controls and their costs out of the Bag details. */
    private fun drawForge(board: Rectangle, orb: VolleyOrbDefinition) {
        val uid = orb.uid ?: return
        val x = board.x + board.width * .055f
        val y = board.y + board.height * .115f
        val w = board.width * .89f
        val h = board.height * .76f
        batch.setColor(.08f, .045f, .035f, .98f)
        batch.draw(game.assets.solidUiRegion, x, y, w, h)
        batch.color = Color.WHITE
        label("ORB FORGE", x + 10f, y + h * .94f, w - 20f, .72f, Color.GOLD)
        label("${orb.name}  •  ${orb.rarity}", x + 10f, y + h * .87f, w - 20f, .51f, Color.WHITE)
        label("AVAILABLE POINTS: ${game.progress.orbUpgradePoints}",
            x + 10f, y + h * .81f, w - 20f, .48f, Color.WHITE)
        val forgeCost = game.progress.orbForgeCost(uid)
        val rerollCost = game.progress.orbRerollCost(uid)
        val imbueCost = game.progress.orbImbueCost(uid)
        fun forgeRow(top: Float, title: String, explanation: String, callback: () -> Unit) {
            val row = Rectangle(x + 10f, y + h * top, w - 20f, h * .14f)
            batch.setColor(.16f, .10f, .07f, .94f)
            batch.draw(game.assets.solidUiRegion, row.x, row.y, row.width, row.height)
            batch.color = Color.WHITE
            label(explanation, row.x + 6f, row.y + row.height * .73f,
                row.width - 12f, .46f, Color.LIGHT_GRAY)
            button(Rectangle(row.x + row.width * .18f, row.y + 4f,
                row.width * .64f, row.height * .43f), title, false, callback)
        }
        forgeRow(.59f, "FORGE", "NEXT RANK • STAGE 15 • ${forgeCost ?: "N/A"} POINTS") {
            val updated = game.progress.forgeOrbCopy(uid)
            feedback = if (updated != null) "FORGED ${updated.rarity}" else "REQUIRES POINTS AND AN UNLOCKED, UNEQUIPPED ORB"
        }
        forgeRow(.40f, "REROLL", "CHANGE BONUS • STAGE 50 • ${rerollCost ?: "N/A"} POINTS") {
            val updated = selectedBonus?.let { game.progress.rerollOrbBonus(uid, it) }
            feedback = if (updated != null) "BONUS REROLLED" else "SELECT A BONUS AND CHECK THE REQUIREMENTS"
            selectedBonus = updated?.bonusStats?.keys?.firstOrNull()
        }
        forgeRow(.21f, "IMBUE", "ADD ONE BONUS • STAGE 60 • ${imbueCost ?: "N/A"} POINTS") {
            feedback = if (game.progress.imbueOrbBonus(uid) != null) "BONUS ADDED"
                else "REQUIRES AN OPEN BONUS SLOT AND POINTS"
        }
        label("BONUS: ${selectedBonus?.replace('_', ' ')?.uppercase() ?: "NONE"}",
            x + 10f, y + h * .17f, w - 20f, .43f, Color.WHITE)
        label(feedback, x + 10f, y + h * .12f, w - 20f, .43f, Color.GOLD)
        button(Rectangle(x + w * .24f, y + h * .025f, w * .52f, h * .061f), "BACK TO DETAILS", false) {
            forgeOpen = false
        }
        forgeActionStart = detailActionStart
    }

    private fun drawBulkConfirmation(board: Rectangle) {
        val box = Rectangle(board.x + board.width * .11f, board.y + board.height * .37f,
            board.width * .78f, board.height * .22f)
        batch.setColor(.06f, .035f, .025f, .98f)
        batch.draw(game.assets.solidUiRegion, box.x, box.y, box.width, box.height)
        batch.color = Color.WHITE
        val eligible = selectedUids.mapNotNull { uid -> game.progress.orbCopySaleValue(uid)?.let { uid to it } }
        label("SELL ${eligible.size} ORBS FOR ${eligible.sumOf { it.second }} COINS?",
            box.x + 8f, box.y + box.height * .77f, box.width - 16f, .51f, Color.GOLD)
        button(Rectangle(box.x + 8f, box.y + 8f, box.width * .45f, box.height * .34f), "CANCEL", false) {
            confirmBulkSale = false
        }
        button(Rectangle(box.x + box.width * .53f, box.y + 8f, box.width * .45f, box.height * .34f),
            "CONFIRM", true) {
            val sold = eligible.mapNotNull { (uid, _) -> game.progress.sellOrbCopy(uid, game.economy::creditOrbSale) }
            feedback = "SOLD ${sold.size} ORBS FOR ${sold.sum()} COINS"
            confirmBulkSale = false
            multiSelect = false
            selectedUids.clear()
            selectedUid = null
        }
    }

    private fun tile(rect: Rectangle, orb: VolleyOrbDefinition?, selected: Boolean,
                     unlocked: Boolean, caption: String, activeSlot: Int? = null) {
        // Gold is the current selection; a green check/ring means actually equipped. Never conflate them.
        VisualGameUi.panel(batch, game.assets, rect, selected)
        if (activeSlot != null) VisualGameUi.equippedRing(batch, game.assets, rect)
        val isSlot = caption.firstOrNull()?.isDigit() == true
        val size = minOf(rect.height * .62f, rect.width * if (isSlot) .60f else .34f)
        val iconBox = Rectangle(if (isSlot) rect.x + (rect.width - size) / 2f else rect.x + rect.width * .06f,
            rect.y + rect.height * .25f, size, size)
        if (orb != null && game.assets.orbIconsLoaded()) {
            batch.color = if (unlocked) Color.WHITE else Color.GRAY
            batch.draw(game.assets.orbIcon(orb.id), iconBox.x, iconBox.y, iconBox.width, iconBox.height)
            batch.color = Color.WHITE
            if (!isSlot) {
                label(orb.family, rect.x + rect.width * .42f, rect.y + rect.height * .76f,
                    rect.width * .54f, .48f, Color.WHITE)
                label(orb.rarity, rect.x + rect.width * .42f, rect.y + rect.height * .52f,
                    rect.width * .54f, .33f, Color.GOLD)
                val state = when {
                    multiSelect && orb.uid in selectedUids -> "SELECTED"
                    activeSlot != null -> "SLOT ${activeSlot + 1}"
                    orb.locked -> "LOCKED"
                    orb.isNew -> "NEW"
                    orb.rank > 0 -> "+${orb.rank}"
                    else -> ""
                }
                label(state, rect.x + rect.width * .42f, rect.y + rect.height * .27f,
                    rect.width * .54f, .36f, if (activeSlot != null) Color.LIME else Color.GOLD)
            }
        } else VisualGameUi.icon(batch, game.assets,
            if (unlocked) VisualGameUi.Icon.PLUS else VisualGameUi.Icon.LOCK, iconBox, if (unlocked) .35f else .8f)
        if (isSlot) {
            VisualGameUi.number(batch, game.assets, caption.substringBefore(' '),
                Rectangle(rect.x + 5f, rect.y + rect.height * .68f, rect.width * .18f, rect.height * .23f), .36f)
            label(if (orb != null) orb.family else if (unlocked) "EMPTY" else "LOCKED",
                rect.x + 4f, rect.y + rect.height * .20f, rect.width - 8f,
                .32f, if (orb != null) Color.WHITE else Color.LIGHT_GRAY)
            if (orb != null) {
                // A filled slot IS an equipped orb: mark it without relying on color alone.
                VisualGameUi.icon(batch, game.assets, VisualGameUi.Icon.CHECK,
                    Rectangle(rect.x + rect.width * .76f, rect.y + rect.height * .68f,
                        rect.width * .20f, rect.height * .28f))
            }
        }
        if (activeSlot != null || (multiSelect && orb?.uid in selectedUids)) {
            VisualGameUi.icon(batch, game.assets, VisualGameUi.Icon.CHECK,
                Rectangle(rect.x + rect.width * .82f, rect.y + rect.height * .73f, rect.width * .14f, rect.height * .23f))
        }
        if (!isSlot && orb?.locked == true) VisualGameUi.icon(batch, game.assets, VisualGameUi.Icon.LOCK,
            Rectangle(rect.x + 3f, rect.y + rect.height * .70f, rect.width * .21f, rect.height * .25f))
    }

    private fun labeledAction(rect: Rectangle, title: String, icon: VisualGameUi.Icon?,
                              selected: Boolean, action: () -> Unit) {
        VisualGameUi.panel(batch, game.assets, rect, selected)
        val side = minOf(rect.height * .63f, rect.width * .23f)
        if (icon != null) VisualGameUi.icon(batch, game.assets, icon,
            Rectangle(rect.x + rect.width * .08f, rect.y + (rect.height - side) / 2f, side, side))
        val padding = rect.width * .08f
        val textLeft = if (icon != null) rect.x + padding + side + rect.width * .035f else rect.x + padding
        VisualGameUi.number(batch, game.assets, title,
            Rectangle(textLeft, rect.y + rect.height * .20f,
                rect.x + rect.width - padding - textLeft, rect.height * .60f), .59f,
            if (title == "REMOVE") Color.SALMON else Color.WHITE)
        actions += rect to action
    }

    private fun tab(rect: Rectangle, title: String, selected: Boolean, action: () -> Unit) {
        if (title.startsWith("SET ")) {
            labeledAction(rect, title, null, selected, action)
        } else labeledAction(rect, title, when (title) {
            "SHOP" -> VisualGameUi.Icon.COIN
            "BALLS" -> VisualGameUi.Icon.BALLS
            else -> VisualGameUi.Icon.ORB
        }, selected, action)
    }

    private fun button(rect: Rectangle, title: String, selected: Boolean, action: () -> Unit) {
        val simpleIcon = when (title) {
            "BACK" -> VisualGameUi.Icon.BACK
            "EQUIP", "REMOVE" -> if (title == "EQUIP") VisualGameUi.Icon.EQUIP else VisualGameUi.Icon.CLOSE
            "DETAILS" -> VisualGameUi.Icon.DETAILS
            "UNLOCK SLOT" -> VisualGameUi.Icon.LOCK
            else -> null
        }
        if (simpleIcon != null) {
            labeledAction(rect, title, simpleIcon, selected && title != "REMOVE", action)
            return
        }
        val art = if (rect.width > rect.height * 3.2f) "ornate_button_blank"
            else "stone_button_style_02"
        batch.setColor(if (selected) 1f else .87f, if (selected) .94f else .85f,
            if (selected) .76f else .80f, 1f)
        batch.draw(game.assets.boards2.requireRegion(art), rect.x, rect.y, rect.width, rect.height)
        batch.color = Color.WHITE
        label(title, rect.x + 5f, rect.y + rect.height * .70f, rect.width - 10f,
            .57f, if (selected) Color.GOLD else Color.WHITE)
        actions += rect to action
    }

    private fun label(value: String, x: Float, y: Float, width: Float, scale: Float, color: Color) {
        val font = game.assets.smallFont
        val oldColor = Color(font.color)
        val oldX = font.data.scaleX
        val oldY = font.data.scaleY
        font.color = color
        val desired = scale * uiTextScale
        font.data.setScale(desired)
        measure.setText(font, value)
        font.data.setScale(desired * minOf(1f, width / measure.width.coerceAtLeast(1f)))
        font.draw(batch, value, x, y, width, Align.center, false)
        font.data.setScale(oldX, oldY)
        font.color = oldColor
    }

    private fun slotHint(slot: Int): String = if (game.progress.unlockedLevel < game.progress.orbSlotRequirement(slot))
        "UNLOCK AT STAGE ${game.progress.orbSlotRequirement(slot)}"
    else "${game.progress.orbSlotCoinCost(slot)} COINS TO UNLOCK"

    private fun orbActivationSummary(orb: VolleyOrbDefinition): String {
        val flatDamage = orb.stat("damage").toInt() + orb.stat("additional_damage").toInt()
        // Reference combat adds every equipped ORB's raw count, then floors the combined total.
        // The old 1.5x display overstated READY and other ORBs without changing combat.
        val bonusBalls = orb.referenceStat("increased_ball_count").coerceAtLeast(0.0)
        val bonusBallsLabel = bonusBalls.toString().removeSuffix(".0")
        val main = when (orb.family) {
            "READY" -> "+$bonusBallsLabel START BALLS"
            "CRIT" -> "HIT CHANCE: CRITICAL DAMAGE"
            "EMPOWER" -> "HIT CHANCE: BOOST LATER HITS"
            "EXPLOSIVE" -> "HIT CHANCE: NEARBY BLAST"
            "FETID" -> "HIT CHANCE: POISON OVER TIME"
            "HAMMER" -> "HIT CHANCE: CHAIN HAMMER"
            "LIGHTNING" -> "HIT CHANCE: LIGHTNING STRIKE"
            "REAPER" -> "HIT CHANCE: REAPER STRIKES"
            "SPLIT" -> "HIT CHANCE: THREE EXTRA TARGETS"
            "LUCK" -> "DROPS +${orb.stat("increased_drop_rate").toInt()}%, COINS +${orb.stat("goldfind").toInt()}%"
            "SECONDW" -> "ONE WALL RESCUE PER RUN"
            "SHARP" -> "DIRECT HIT DAMAGE BOOST"
            "SLAYING" -> "FINISH LOW-HP NON-BOSS BRICKS"
            "WARRIOR" -> "STAGE DAMAGE + BALLS"
            else -> when {
                flatDamage > 0 -> "+${ceil(flatDamage * 1.5f).toInt()} FLAT HIT DAMAGE"
                bonusBalls > 0.0 -> "+$bonusBallsLabel START BALLS"
                else -> "PASSIVE VOLLEY EFFECT"
            }
        }
        return if (bonusBalls > 0.0 && orb.family !in setOf("READY", "WARRIOR"))
            "$main  |  +$bonusBallsLabel BALLS" else main
    }

    private fun handleInput() {
        if (guidePage >= 0) {
            touchInCollection = false
            if (game.uiMotion.active || guideAnimation.exiting) return
            val point = touchPoint()
            val screen = Rectangle(visibleLeft, visibleBottom, visibleWidth, visibleHeight)
            if (Gdx.input.isKeyJustPressed(Input.Keys.BACK) || Gdx.input.isKeyJustPressed(Input.Keys.ESCAPE) ||
                (Gdx.input.justTouched() && VisualHelpOverlay.closeBounds(screen).contains(point.x, point.y))) {
                skipGuideAfterExit = true
                guideAnimation.beginExit()
            } else if (Gdx.input.justTouched()) handleGuideTap(point.x, point.y)
            return
        }
        if (Gdx.input.isKeyJustPressed(Input.Keys.BACK) || Gdx.input.isKeyJustPressed(Input.Keys.ESCAPE)) {
            when {
                optionsOpen -> optionsOpen = false
                confirmBulkSale -> confirmBulkSale = false
                confirmSale -> confirmSale = false
                forgeOpen -> forgeOpen = false
                detailOpen -> detailOpen = false
                multiSelect -> { multiSelect = false; selectedUids.clear() }
                else -> goBack()
            }
            return
        }
        val point = touchPoint()
        if (Gdx.input.justTouched()) {
            if (!confirmSale && !confirmBulkSale && detailOpen && !forgeOpen && detailArea.contains(point.x, point.y)) {
                touchInCollection = true
                touchStartY = point.y
                lastTouchY = point.y
                dragged = false
                return
            }
            if (!optionsOpen && !detailOpen && !confirmBulkSale && collectionArea.contains(point.x, point.y)) {
                touchInCollection = true
                touchStartY = point.y
                lastTouchY = point.y
                dragged = false
                return
            }
            val availableActions = when {
                confirmBulkSale -> actions.drop(bulkActionStart)
                confirmSale -> actions.drop(confirmActionStart)
                forgeOpen -> actions.drop(forgeActionStart)
                detailOpen -> actions.drop(detailActionStart)
                optionsOpen -> actions.drop(optionsActionStart).take(detailActionStart - optionsActionStart)
                else -> actions
            }
            availableActions.asReversed().firstOrNull { it.first.contains(point.x, point.y) }?.let {
                animateSelection(it.first)
                it.second.invoke()
            }
        }
        if (touchInCollection && Gdx.input.isTouched) {
            if (abs(point.y - touchStartY) > 12f) dragged = true
            if (dragged) {
                if (detailOpen) detailScroll = (detailScroll + point.y - lastTouchY)
                    .coerceIn(0f, detailMaxScroll)
                else scroll = (scroll + point.y - lastTouchY).coerceIn(0f, maxScroll)
            }
            lastTouchY = point.y
        } else if (touchInCollection) {
            if (!dragged) {
                if (detailOpen) bonusHits.firstOrNull { it.first.contains(point.x, point.y) }?.let {
                    selectedBonus = it.second
                } else copyHits.firstOrNull { it.first.contains(point.x, point.y) }?.let {
                    animateSelection(it.first)
                    if (multiSelect) {
                        if (game.progress.orbCopySaleValue(it.second) != null) {
                            if (!selectedUids.add(it.second)) selectedUids.remove(it.second)
                            feedback = "${selectedUids.size} SELECTED"
                        } else feedback = "IN USE OR LOCKED - CANNOT SELL"
                    } else {
                        selectedUid = it.second
                        activeSlot(it.second)?.let { selectedSlot = it }
                        feedback = ""
                    }
                }
            }
            touchInCollection = false
        }
        if (!game.assets.orbIconsLoaded()) game.assets.orbIcon("CRIT0")
    }

    private fun guideRequiresAction(): Boolean = game.progress.orbStashCount > 0 &&
        !(guidePage == 2 && game.progress.orbInSlot(selectedSlot)?.uid == selectedUid)

    /** Only the highlighted, reversible action can pass through the Old Man's overlay. */
    private fun handleGuideTap(x: Float, y: Float) {
        if (guideAnimation.elapsed < .15f) return
        val lines = game.assets.guideHelpText.lines(guideCaptionKey(),
            selectedUid?.let(game.progress::orbCopy)?.family)
        if (!guideRequiresAction()) {
            if (guideAnimation.revealOrContinue(lines)) guideAnimation.beginExit()
            return
        }
        if (!guideFocus.contains(x, y)) {
            guideAnimation.revealOrContinue(lines)
            return
        }
        if (guidePage == 1) {
            val hit = copyHits.firstOrNull { it.first.contains(x, y) } ?: return
            selectedUid = hit.second
            animateSelection(hit.first)
            guideAnimation.beginExit()
            return
        }
        val action = actions.firstOrNull { it.first.contains(x, y) } ?: return
        action.second.invoke()
        val completed = when (guidePage) {
            0 -> game.progress.isOrbSlotUnlocked(selectedSlot)
            2 -> selectedUid != null && game.progress.orbInSlot(selectedSlot)?.uid == selectedUid
            3 -> detailOpen
            else -> false
        }
        if (completed) guideAnimation.beginExit()
        else {
            guideFeedback = feedback
            // A conflicting loadout must leave a way to choose another Orb without skipping.
            if (guidePage == 2) {
                guidePage = 1
                guideAnimation.reset()
            }
        }
    }

    private fun goBack() {
        rememberView()
        when {
            returnToShop -> game.navigate(ShopScreen(game, returnDestination, bagMode, initialTabName = "ORBS"))
            returnDestination == ShopReturnDestination.PAUSED_GAME -> game.resumePausedGame(showLoading = false)
            else -> game.openMenu()
        }
    }

    private fun rememberView() {
        rememberedUid = selectedUid
        rememberedSlot = selectedSlot
        rememberedFilter = filter
        rememberedRaritySort = raritySort
        rememberedScroll = scroll
    }
}
