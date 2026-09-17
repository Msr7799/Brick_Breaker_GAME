/*
 * ملاحظات صيانة الملف:
 * المسار: app/src/main/java/com/example/brick_breaker_ball/CustomizationScreen.kt
 * المؤلف: mohamed alromaihi
 * دالة قص لمس الكور: `visibleBallCard`
 * الدوال الموجودة: `visiblePaddleCard`، `render`، `drawBalls`، `drawPaddles`، `selectedPaddleId`، `setSelectedPaddleId`، `maxPaddleScroll`، `beginPaddleClip`، `endPaddleClip`، `leaveCustomization`، `handleTouch`، `changePage`، `compactButton`، `panel`، `drawFit`، `drawBallSquare`، `drawSmallScaled`
 */

package com.example.brick_breaker_ball

import com.badlogic.gdx.Gdx
import com.badlogic.gdx.Input
import com.badlogic.gdx.graphics.Color
import com.badlogic.gdx.graphics.GL20
import com.badlogic.gdx.graphics.g2d.TextureRegion
import com.badlogic.gdx.math.Rectangle
import com.badlogic.gdx.math.Vector3
import com.badlogic.gdx.utils.Align
import kotlin.math.abs

enum class CustomizationTab { BALLS, PADDLES }
private enum class PaddleFilter { NORMAL, WEAPON, STICKY }

internal object CustomizationHitTesting {
    const val BALL_LIST_BOTTOM = 535f
    const val BALL_LIST_TOP = 1185f
    const val PADDLE_LIST_BOTTOM = 500f
    const val PADDLE_LIST_TOP = 1150f

    /** ملاحظة صيانة: الدالة `visiblePaddleCard` تنفّذ مسؤولية محلية يعتمد عليها هذا الجزء من اللعبة؛ راجع استدعاءاتها واختباراتها قبل تعديلها. */
    fun visiblePaddleCard(rect: Rectangle): Rectangle? {
        val bottom = maxOf(rect.y, PADDLE_LIST_BOTTOM)
        val top = minOf(rect.y + rect.height, PADDLE_LIST_TOP)
        if (top <= bottom) return null
        return Rectangle(rect.x, bottom, rect.width, top - bottom)
    }

    /** يقصر لمس كرة التخصيص على الجزء الظاهر داخل حاوية التمرير. */
    fun visibleBallCard(rect: Rectangle): Rectangle? {
        val bottom = maxOf(rect.y, BALL_LIST_BOTTOM)
        val top = minOf(rect.y + rect.height, BALL_LIST_TOP)
        if (top <= bottom) return null
        return Rectangle(rect.x, bottom, rect.width, top - bottom)
    }
}

class CustomizationScreen(
    game: BrickBreakerGame,
    private val returnToPausedGame: Boolean = false,
    initialTab: CustomizationTab = CustomizationTab.BALLS,
) : ForgeScreen(game) {
    private companion object {
        val WEAPON_PREVIEW_EXPAND_SCALE =
            (GameSession.BASE_PADDLE_WIDTH + GameSession.PADDLE_EXPAND_STEP) / GameSession.BASE_PADDLE_WIDTH
        const val BALL_CARD_HEIGHT = 190f
        const val BALL_ROW_SPACING = 218f
        const val BALL_START_Y = 995f
    }

    private var tab = initialTab
    private var paddleFilter = PaddleFilter.NORMAL
    private var ballScrollOffset = 0f
    private var paddleScrollOffset = 0f
    private var touchActive = false
    private var touchMoved = false
    private var touchScrollEnabled = false
    private var touchStartX = 0f
    private var touchStartY = 0f
    private var touchLastX = 0f
    private var touchLastY = 0f

    override fun show() {
        installMouseWheelHandler(::handleMouseWheel)
    }

    override fun hide() {
        removeMouseWheelHandler()
    }

    /** ملاحظة صيانة: الدالة `render` تنفّذ العقد الموروث وتربط دورة حياة المكوّن بسلوك هذا الملف؛ راجع استدعاءاتها واختباراتها قبل تعديلها. */
    override fun render(delta: Float) {
        begin("world_4_zerog")
        title("CUSTOMIZE PADDLE & BALL", 1530f)
        val ballsTab = compactButton("BALLS", 75f, 1350f, 350f, 88f, tab == CustomizationTab.BALLS)
        val paddlesTab = compactButton("PADDLES", 475f, 1350f, 350f, 88f, tab == CustomizationTab.PADDLES)
        val hitTargets = if (tab == CustomizationTab.BALLS) drawBalls() else drawPaddles()
        val back = compactButton("SAVE & BACK", 250f, 45f, 400f, 80f, false)
        end()

        if (Gdx.input.isKeyJustPressed(Input.Keys.BACK)) {
            leaveCustomization()
            return
        }
        handleTouch(
            buildList {
                add(ballsTab to { tab = CustomizationTab.BALLS; ballScrollOffset = 0f })
                add(paddlesTab to { tab = CustomizationTab.PADDLES; paddleScrollOffset = 0f })
                add(back to ::leaveCustomization)
                addAll(hitTargets)
            }
        )
    }

    /** Draws every ball as its own progression item; the old set/theme pages are removed. */
    private fun drawBalls(): List<Pair<Rectangle, () -> Unit>> {
        val actions = mutableListOf<Pair<Rectangle, () -> Unit>>()
        val balls = game.assets.cosmetics.balls
        if (balls.isEmpty()) {
            game.assets.bodyFont.draw(batch, "BALL SPRITES UNAVAILABLE\nUSING CLASSIC BALL", 100f, 920f, 700f, Align.center, true)
            return actions
        }

        val settings = game.progress.settings
        val ownedCount = game.cosmeticProgression.ownedBallCount()
        val nextUnlock = game.cosmeticProgression.nextLockedBall()
        val nextText = nextUnlock?.let { "NEXT BALL: STAGE ${it.second}" } ?: "ALL BALLS UNLOCKED"

        game.assets.bodyFont.draw(batch, "BALL COLLECTION", 0f, 1295f, 900f, Align.center, false)
        drawSmallScaled("$ownedCount/${balls.size} UNLOCKED  •  $nextText", 55f, 1238f, 790f, .62f)

        val cardWidth = 250f
        val cardHeight = BALL_CARD_HEIGHT
        val startX = 55f
        val startY = BALL_START_Y
        val colGap = 20f
        val rowSpacing = BALL_ROW_SPACING
        val rows = if (balls.isEmpty()) 0 else (balls.size + 2) / 3
        ballScrollOffset = ballScrollOffset.coerceIn(0f, maxBallScroll(rows))

        beginBallClip()
        balls.forEachIndexed { index, definition ->
            val col = index % 3
            val row = index / 3
            val rect = Rectangle(
                startX + col * (cardWidth + colGap),
                startY - row * rowSpacing + ballScrollOffset,
                cardWidth,
                cardHeight
            )
            if (rect.y + rect.height < CustomizationHitTesting.BALL_LIST_BOTTOM || rect.y > CustomizationHitTesting.BALL_LIST_TOP) return@forEachIndexed
            val owned = game.developmentAccess.canUseCosmetic(game.cosmeticProgression.ownsBall(definition))
            val selected = owned && settings.selectedBallGroupName == definition.groupName &&
                settings.selectedBallSpriteName == definition.spriteName

            panel(rect, selected)
            val previewX = rect.x + (rect.width - 92f) / 2f
            game.assets.cosmetics.ballRegion(definition)?.let {
                drawBallSquare(it, previewX, rect.y + 72f, 92f)
            }
            if (!owned) drawLockBadge(previewX + 54f, rect.y + 126f, 38f)
            drawSmallScaled(definition.name.uppercase(), rect.x + 10f, rect.y + 54f, rect.width - 20f, .54f)
            if (owned) {
                game.assets.smallFont.color = if (selected) ForgeUiPalette.primaryLight else ForgeUiPalette.textSecondary
                drawSmallScaled(if (selected) "EQUIPPED • TAP FOR POWER" else "TAP FOR POWER", rect.x + 10f, rect.y + 23f, rect.width - 20f, .44f)
                game.assets.smallFont.color = Color.WHITE
                CustomizationHitTesting.visibleBallCard(rect)?.let { hitRect ->
                    actions += hitRect to {
                        game.setScreen(BallPowerDetailScreen(game, definition, returnToPausedGame))
                    }
                }
            } else {
                game.assets.smallFont.color = ForgeUiPalette.textSecondary
                drawSmallScaled(game.cosmeticProgression.ballRequirement(definition), rect.x + 10f, rect.y + 23f, rect.width - 20f, .44f)
                game.assets.smallFont.color = Color.WHITE
                CustomizationHitTesting.visibleBallCard(rect)?.let { hitRect ->
                    actions += hitRect to {
                        game.setScreen(BallPowerDetailScreen(game, definition, returnToPausedGame))
                    }
                }
            }
        }

        endBallClip()

        val selected = game.assets.cosmetics.selectedBall(settings.selectedBallGroupName, settings.selectedBallSpriteName)
            ?.takeIf { game.developmentAccess.canUseCosmetic(game.cosmeticProgression.ownsBall(it)) }
            ?: if (game.developmentAccess.enabled) balls.firstOrNull() else game.cosmeticProgression.firstOwnedBall()
        val region = selected?.let(game.assets.cosmetics::ballRegion)

        drawSmallScaled("SELECTED PREVIEW", 35f, 490f, 250f, .72f)
        if (region != null && selected != null) {
            drawBallSquare(region, 105f, 315f, 110f)
            drawSmallScaled(selected.name.uppercase(), 35f, 285f, 250f, .62f)
            val selectedAbility = BallAbilityCatalog.profileForBall(selected)
            game.assets.smallFont.color = ForgeUiPalette.primaryLight
            drawSmallScaled("${selectedAbility.title} • TIER ${selectedAbility.tier}", 35f, 255f, 250f, .46f)
            game.assets.smallFont.color = Color.WHITE
        }
        drawSmallScaled("REAL SIZE COMPARISON", 305f, 490f, 560f, .76f)
        drawSmallScaled("POWER-UP SIZE PREVIEW", 305f, 450f, 560f, .64f)
        game.assets.smallFont.color = ForgeUiPalette.textSecondary
        drawSmallScaled("Every run starts at NORMAL size", 305f, 416f, 560f, .50f)
        game.assets.smallFont.color = Color.WHITE
        if (region != null) {
            val sizes = listOf(BallSize.SMALL to "SMALL", BallSize.DEFAULT to "NORMAL", BallSize.LARGE to "LARGE")
            sizes.forEachIndexed { index, (size, label) ->
                val centerX = 390f + index * 190f
                val displayDiameter = size.radius * 2.8f
                drawBallSquare(region, centerX - displayDiameter / 2f, 330f - displayDiameter / 2f, displayDiameter)
                drawSmallScaled("$label  R=${size.radius.toInt()}", centerX - 85f, 240f, 170f, .56f)
            }
        }
        return actions
    }

    /** ملاحظة صيانة: الدالة `drawPaddles` ترسم العناصر المطلوبة مع الحفاظ على ترتيب طبقات العرض؛ راجع استدعاءاتها واختباراتها قبل تعديلها. */
    private fun drawPaddles(): List<Pair<Rectangle, () -> Unit>> {
        val actions = mutableListOf<Pair<Rectangle, () -> Unit>>()
        val settings = game.progress.settings
        PaddleFilter.entries.forEachIndexed { index, filter ->
            val rect = compactButton(filter.name, 45f + index * 285f, 1215f, 240f, 70f, paddleFilter == filter)
            actions += rect to {
                paddleFilter = filter
                paddleScrollOffset = 0f
            }
        }

        // Keep the same style order in Normal / Weapon / Sticky. The starter family is first
        // in CosmeticProgressionService, therefore the three default paddles are always card #1.
        val filtered = orderedPaddles(paddleFilter)
        if (filtered.isEmpty()) {
            game.assets.bodyFont.draw(batch, "PADDLE SPRITES UNAVAILABLE\nUSING CLASSIC PADDLE", 100f, 900f, 700f, Align.center, true)
            return actions
        }

        paddleScrollOffset = paddleScrollOffset.coerceIn(0f, maxPaddleScroll(filtered.size))
        val nextUnlock = game.cosmeticProgression.nextLockedPaddle()
        val nextText = nextUnlock?.let { "NEXT: STAGE ${it.second}" } ?: "ALL UNLOCKED"
        val groupCount = game.assets.cosmetics.paddles.map(PaddleSpriteDefinition::sourceAtlas).distinct().size
        drawSmallScaled(
            "$groupCount GROUPS  •  ${game.cosmeticProgression.ownedPaddleCount()}/${game.cosmeticProgression.totalPaddleCount()} SETS UNLOCKED  •  $nextText",
            55f,
            1170f,
            790f,
            .58f
        )

        beginPaddleClip()
        filtered.forEachIndexed { index, definition ->
            val col = index % 2
            val row = index / 2
            val rect = Rectangle(40f + col * 430f, 980f - row * 190f + paddleScrollOffset, 390f, 164f)
            val style = game.cosmeticProgression.styleForPaddle(definition.id)
            val owned = style != null &&
                game.developmentAccess.canUseCosmetic(game.cosmeticProgression.ownsPaddle(style))
            val selected = owned && selectedPaddleId(settings) == definition.id
            panel(rect, selected)

            batch.color = Color.WHITE
            game.assets.cosmetics.paddleRegion(definition)?.let {
                drawFit(
                    it,
                    rect.x + 24f,
                    rect.y + 70f,
                    rect.width - 48f,
                    68f,
                    horizontalScale = if (definition.groupId == "weapon") WEAPON_PREVIEW_EXPAND_SCALE else 1f
                )
            }
            if (!owned) drawLockBadge(rect.x + rect.width - 73f, rect.y + 91f, 46f)
            drawSmallScaled(definition.displayNameEn.uppercase(), rect.x + 12f, rect.y + 48f, rect.width - 24f, .64f)
            if (owned) {
                game.assets.smallFont.color = if (selected) ForgeUiPalette.primaryLight else ForgeUiPalette.textSecondary
                drawSmallScaled(if (selected) "EQUIPPED • TAP FOR POWER" else "TAP FOR POWER", rect.x + 12f, rect.y + 21f, rect.width - 24f, .50f)
                game.assets.smallFont.color = Color.WHITE
            } else {
                game.assets.smallFont.color = ForgeUiPalette.primaryLight
                val requirement = style?.let(game.cosmeticProgression::paddleRequirement) ?: "LOCKED"
                drawSmallScaled(requirement, rect.x + 20f, rect.y + 21f, rect.width - 40f, .50f)
                game.assets.smallFont.color = Color.WHITE
            }

            CustomizationHitTesting.visiblePaddleCard(rect)?.let { hitRect ->
                if (style != null) {
                    actions += hitRect to {
                        game.setScreen(PaddlePowerDetailScreen(game, style, returnToPausedGame))
                    }
                }
            }
        }
        endPaddleClip()

        val selected = game.assets.cosmetics.selectedPaddle(selectedPaddleId(settings))
        game.assets.smallFont.draw(batch, "SELECTED ${paddleFilter.name} PADDLE", 0f, 475f, 900f, Align.center, false)
        selected?.let { definition ->
            val style = game.cosmeticProgression.styleForPaddle(definition.id)
            if (style != null && game.developmentAccess.canUseCosmetic(game.cosmeticProgression.ownsPaddle(style))) {
                game.assets.cosmetics.paddleRegion(definition)?.let {
                    drawFit(
                        it,
                        145f,
                        340f,
                        610f,
                        105f,
                        horizontalScale = if (definition.groupId == "weapon") WEAPON_PREVIEW_EXPAND_SCALE else 1f
                    )
                }
                game.assets.smallFont.draw(batch, definition.displayNameEn.uppercase(), 100f, 320f, 700f, Align.center, false)
                val ability = PaddleAbilityCatalog.profileForNormalPaddle(style.normal.id)
                game.assets.smallFont.color = ForgeUiPalette.primaryLight
                drawSmallScaled("${ability.title}  •  TIER ${ability.tier}", 100f, 275f, 700f, .62f)
                game.assets.smallFont.color = ForgeUiPalette.textSecondary
                drawSmallScaled(ability.description.uppercase(), 85f, 235f, 730f, .50f)
                game.assets.smallFont.color = Color.WHITE
            }
        }
        return actions
    }

    /** Stable style ordering shared by all three visual-state filters. */
    private fun orderedPaddles(filter: PaddleFilter): List<PaddleSpriteDefinition> = game.cosmeticProgression.paddleStyles.map { style ->
        when (filter) {
            PaddleFilter.NORMAL -> style.normal
            PaddleFilter.WEAPON -> style.weapon
            PaddleFilter.STICKY -> style.sticky
        }
    }

    /** ملاحظة صيانة: الدالة `selectedPaddleId` تنفّذ مسؤولية محلية يعتمد عليها هذا الجزء من اللعبة؛ راجع استدعاءاتها واختباراتها قبل تعديلها. */
    private fun selectedPaddleId(settings: GameSettings): String = when (paddleFilter) {
        PaddleFilter.NORMAL -> settings.selectedPaddleId
        PaddleFilter.WEAPON -> settings.selectedWeaponPaddleId
        PaddleFilter.STICKY -> settings.selectedStickyPaddleId
    }

    /** ملاحظة صيانة: الدالة `setSelectedPaddleId` تنفّذ مسؤولية محلية يعتمد عليها هذا الجزء من اللعبة؛ راجع استدعاءاتها واختباراتها قبل تعديلها. */
    private fun setSelectedPaddleId(settings: GameSettings, id: String) {
        when (paddleFilter) {
            PaddleFilter.NORMAL -> settings.selectedPaddleId = id
            PaddleFilter.WEAPON -> settings.selectedWeaponPaddleId = id
            PaddleFilter.STICKY -> settings.selectedStickyPaddleId = id
        }
    }

    /** ملاحظة صيانة: الدالة `maxBallScroll` تنفّذ مسؤولية محلية يعتمد عليها هذا الجزء من اللعبة؛ راجع استدعاءاتها واختباراتها قبل تعديلها. */
    private fun maxBallScroll(rows: Int): Float =
        ((rows - 1) * BALL_ROW_SPACING - (BALL_START_Y - CustomizationHitTesting.BALL_LIST_BOTTOM)).coerceAtLeast(0f)

    /** ملاحظة صيانة: الدالة `beginBallClip` تنفّذ مسؤولية محلية يعتمد عليها هذا الجزء من اللعبة؛ راجع استدعاءاتها واختباراتها قبل تعديلها. */
    private fun beginBallClip() {
        batch.flush()
        val bottom = Vector3(0f, CustomizationHitTesting.BALL_LIST_BOTTOM, 0f)
        val top = Vector3(900f, CustomizationHitTesting.BALL_LIST_TOP, 0f)
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

    /** ملاحظة صيانة: الدالة `endBallClip` تنفّذ مسؤولية محلية يعتمد عليها هذا الجزء من اللعبة؛ راجع استدعاءاتها واختباراتها قبل تعديلها. */
    private fun endBallClip() {
        batch.flush()
        Gdx.gl.glDisable(GL20.GL_SCISSOR_TEST)
    }

    /** ملاحظة صيانة: الدالة `maxPaddleScroll` تنفّذ مسؤولية محلية يعتمد عليها هذا الجزء من اللعبة؛ راجع استدعاءاتها واختباراتها قبل تعديلها. */
    private fun maxPaddleScroll(itemCount: Int): Float {
        val rows = (itemCount + 1) / 2
        return ((rows - 1) * 190f - 480f).coerceAtLeast(0f)
    }

    /** ملاحظة صيانة: الدالة `beginPaddleClip` تنفّذ مسؤولية محلية يعتمد عليها هذا الجزء من اللعبة؛ راجع استدعاءاتها واختباراتها قبل تعديلها. */
    private fun beginPaddleClip() {
        batch.flush()
        val bottom = Vector3(0f, CustomizationHitTesting.PADDLE_LIST_BOTTOM, 0f)
        val top = Vector3(900f, CustomizationHitTesting.PADDLE_LIST_TOP, 0f)
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

    /** ملاحظة صيانة: الدالة `endPaddleClip` تنفّذ مسؤولية محلية يعتمد عليها هذا الجزء من اللعبة؛ راجع استدعاءاتها واختباراتها قبل تعديلها. */
    private fun endPaddleClip() {
        batch.flush()
        Gdx.gl.glDisable(GL20.GL_SCISSOR_TEST)
    }

    /** ملاحظة صيانة: الدالة `leaveCustomization` تنفّذ مسؤولية محلية يعتمد عليها هذا الجزء من اللعبة؛ راجع استدعاءاتها واختباراتها قبل تعديلها. */
    private fun leaveCustomization() {
        game.progress.saveSettings()
        if (returnToPausedGame) game.resumePausedGame() else game.openMenu()
    }

    /** ملاحظة صيانة: الدالة `handleTouch` تعالج الحدث أو الطلب وتحدّث الحالة المرتبطة به؛ راجع استدعاءاتها واختباراتها قبل تعديلها. */
    private fun handleTouch(actions: List<Pair<Rectangle, () -> Unit>>) {
        val isTouched = Gdx.input.isTouched
        if (Gdx.input.justTouched()) {
            val point = touchPoint()
            touchActive = true
            touchMoved = false
            touchStartX = point.x
            touchStartY = point.y
            touchLastX = point.x
            touchLastY = point.y
            touchScrollEnabled = when (tab) {
                CustomizationTab.BALLS -> point.y in CustomizationHitTesting.BALL_LIST_BOTTOM..CustomizationHitTesting.BALL_LIST_TOP
                CustomizationTab.PADDLES -> point.y in CustomizationHitTesting.PADDLE_LIST_BOTTOM..CustomizationHitTesting.PADDLE_LIST_TOP
            }
        }
        if (touchActive && isTouched) {
            val point = touchPoint()
            if (touchScrollEnabled) {
                when (tab) {
                    CustomizationTab.BALLS -> {
                        val rows = (game.assets.cosmetics.balls.size + 2) / 3
                        ballScrollOffset = (ballScrollOffset + point.y - touchLastY)
                            .coerceIn(0f, maxBallScroll(rows))
                    }

                    CustomizationTab.PADDLES -> {
                        val filteredCount = game.cosmeticProgression.paddleStyles.size
                        paddleScrollOffset = (paddleScrollOffset + point.y - touchLastY)
                            .coerceIn(0f, maxPaddleScroll(filteredCount))
                    }
                }
            }
            touchLastX = point.x
            touchLastY = point.y
            if (abs(point.y - touchStartY) > 28f || abs(point.x - touchStartX) > 28f) touchMoved = true
        } else if (touchActive) {
            if (!touchMoved) {
                actions.firstOrNull { it.first.contains(touchLastX, touchLastY) }?.second?.invoke()
            }
            touchActive = false
            touchScrollEnabled = false
        }
    }

    private fun handleMouseWheel(amountY: Float): Boolean {
        if (amountY == 0f) return false
        val point = touchPoint()
        when (tab) {
            CustomizationTab.BALLS -> {
                if (point.y !in CustomizationHitTesting.BALL_LIST_BOTTOM..CustomizationHitTesting.BALL_LIST_TOP) return false
                val rows = (game.assets.cosmetics.balls.size + 2) / 3
                ballScrollOffset = (ballScrollOffset + amountY * 230f).coerceIn(0f, maxBallScroll(rows))
            }
            CustomizationTab.PADDLES -> {
                if (point.y !in CustomizationHitTesting.PADDLE_LIST_BOTTOM..CustomizationHitTesting.PADDLE_LIST_TOP) return false
                val filteredCount = game.cosmeticProgression.paddleStyles.size
                paddleScrollOffset = (paddleScrollOffset + amountY * 190f).coerceIn(0f, maxPaddleScroll(filteredCount))
            }
        }
        return true
    }


    /** ملاحظة صيانة: الدالة `compactButton` تنفّذ مسؤولية محلية يعتمد عليها هذا الجزء من اللعبة؛ راجع استدعاءاتها واختباراتها قبل تعديلها. */
    private fun compactButton(text: String, x: Float, y: Float, width: Float, height: Float, selected: Boolean): Rectangle {
        val rect = Rectangle(x, y, width, height)
        game.assets.uiRenderer.drawGradientButton(
            batch,
            rect,
            if (selected) ForgeUiRenderer.GradientStyle.PRIMARY else ForgeUiRenderer.GradientStyle.NEUTRAL,
        )
        game.assets.smallFont.color = if (selected) ForgeUiPalette.textPrimary else ForgeUiPalette.textSecondary
        game.assets.smallFont.draw(batch, text, x + 5f, y + height * .64f, width - 10f, Align.center, false)
        game.assets.smallFont.color = Color.WHITE
        return rect
    }

    /** ملاحظة صيانة: الدالة `panel` تنفّذ مسؤولية محلية يعتمد عليها هذا الجزء من اللعبة؛ راجع استدعاءاتها واختباراتها قبل تعديلها. */
    private fun panel(rect: Rectangle, selected: Boolean) {
        if (selected) {
            game.assets.uiRenderer.drawGradientBorderPanel(batch, rect, ForgeUiRenderer.GradientStyle.PRIMARY, 5f)
        } else {
            game.assets.uiRenderer.drawGradientPanel(batch, rect, ForgeUiRenderer.GradientStyle.NEUTRAL)
        }
    }

    /** ملاحظة صيانة: الدالة `drawFit` ترسم العناصر المطلوبة مع الحفاظ على ترتيب طبقات العرض؛ راجع استدعاءاتها واختباراتها قبل تعديلها. */
    private fun drawFit(
        region: TextureRegion,
        x: Float,
        y: Float,
        width: Float,
        height: Float,
        horizontalScale: Float = 1f
    ) {
        val scale = minOf(width / region.regionWidth, height / region.regionHeight)
        val drawWidth = (region.regionWidth * scale * horizontalScale).coerceAtMost(width)
        val drawHeight = region.regionHeight * scale
        batch.color = Color.WHITE
        batch.draw(region, x + (width - drawWidth) / 2f, y + (height - drawHeight) / 2f, drawWidth, drawHeight)
    }

    /** ملاحظة صيانة: الدالة `drawBallSquare` ترسم العناصر المطلوبة مع الحفاظ على ترتيب طبقات العرض؛ راجع استدعاءاتها واختباراتها قبل تعديلها. */
    private fun drawBallSquare(region: TextureRegion, x: Float, y: Float, diameter: Float) {
        val scale = minOf(diameter / region.regionWidth, diameter / region.regionHeight)
        val drawWidth = region.regionWidth * scale
        val drawHeight = region.regionHeight * scale
        batch.color = Color.WHITE
        batch.draw(region, x + (diameter - drawWidth) / 2f, y + (diameter - drawHeight) / 2f, drawWidth, drawHeight)
    }

    /** ملاحظة صيانة: الدالة `drawSmallScaled` ترسم العناصر المطلوبة مع الحفاظ على ترتيب طبقات العرض؛ راجع استدعاءاتها واختباراتها قبل تعديلها. */
    private fun drawSmallScaled(text: String, x: Float, baselineY: Float, width: Float, scale: Float) {
        val data = game.assets.smallFont.data
        val oldX = data.scaleX
        val oldY = data.scaleY
        data.setScale(scale)
        game.assets.smallFont.draw(batch, text, x, baselineY, width, Align.center, false)
        data.setScale(oldX, oldY)
    }
}
