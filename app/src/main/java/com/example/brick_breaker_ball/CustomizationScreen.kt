package com.example.brick_breaker_ball

import com.badlogic.gdx.Gdx
import com.badlogic.gdx.Input
import com.badlogic.gdx.graphics.Color
import com.badlogic.gdx.graphics.GL20
import com.badlogic.gdx.graphics.g2d.TextureRegion
import com.badlogic.gdx.math.Rectangle
import com.badlogic.gdx.utils.Align
import kotlin.math.abs

private enum class CustomizationTab { BALLS, PADDLES }
private enum class PaddleFilter { ALL, NORMAL, WEAPON, STICKY }

class CustomizationScreen(game: BrickBreakerGame, private val returnToPausedGame: Boolean = false) : ForgeScreen(game) {
    private var tab = CustomizationTab.BALLS
    private var ballGroupIndex = game.assets.cosmetics.ballGroups.indexOfFirst {
        it.groupName == game.progress.settings.selectedBallGroupName
    }.coerceAtLeast(0)
    private var paddleFilter = PaddleFilter.ALL
    private var paddleScrollOffset = 0f
    private var touchActive = false
    private var touchMoved = false
    private var touchStartX = 0f
    private var touchStartY = 0f
    private var touchLastX = 0f
    private var touchLastY = 0f

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
        handleTouch(buildList {
            add(ballsTab to { tab = CustomizationTab.BALLS })
            add(paddlesTab to { tab = CustomizationTab.PADDLES })
            addAll(hitTargets)
            add(back to ::leaveCustomization)
        })
    }

    private fun drawBalls(): List<Pair<Rectangle, () -> Unit>> {
        val actions = mutableListOf<Pair<Rectangle, () -> Unit>>()
        val groups = game.assets.cosmetics.ballGroups
        if (groups.isEmpty()) {
            game.assets.bodyFont.draw(batch, "BALL SPRITES UNAVAILABLE\nUSING CLASSIC BALL", 100f, 920f, 700f, Align.center, true)
            return actions
        }
        ballGroupIndex = ballGroupIndex.coerceIn(groups.indices)
        val group = groups[ballGroupIndex]
        game.assets.bodyFont.draw(batch, group.groupName.replace('_', ' ').uppercase(), 0f, 1280f, 900f, Align.center, false)
        drawSmallScaled("SWIPE UP / DOWN TO CHANGE GROUP", 80f, 1215f, 740f, .68f)

        val settings = game.progress.settings
        group.sprites.forEachIndexed { index, definition ->
            val col = index % 4; val row = index / 4
            val rect = Rectangle(35f + col * 215f, 1025f - row * 205f, 185f, 180f)
            val selected = settings.selectedBallGroupName == definition.groupName && settings.selectedBallSpriteName == definition.spriteName
            panel(rect, selected)
            game.assets.cosmetics.ballRegion(definition)?.let { drawFit(it, rect.x + 50f, rect.y + 58f, 85f, 85f) }
            drawSmallScaled(definition.name.replace('_', ' ').uppercase(), rect.x + 8f, rect.y + 38f, rect.width - 16f, .68f)
            actions += rect to {
                settings.selectedBallGroupName = definition.groupName
                settings.selectedBallSpriteName = definition.spriteName
                game.progress.saveSettings()
            }
        }

        val selected = game.assets.cosmetics.selectedBall(settings.selectedBallGroupName, settings.selectedBallSpriteName)
            ?: group.sprites.first()
        val region = game.assets.cosmetics.ballRegion(selected)
        drawSmallScaled("SELECTED PREVIEW", 35f, 695f, 250f, .72f)
        if (region != null) drawFit(region, 95f, 500f, 130f, 130f)
        drawSmallScaled(selected.name.replace('_', ' ').uppercase(), 35f, 475f, 250f, .72f)
        drawSmallScaled("REAL SIZE COMPARISON", 305f, 695f, 560f, .78f)
        val sizes = listOf(BallSize.SMALL to "SMALL", BallSize.DEFAULT to "NORMAL", BallSize.LARGE to "LARGE")
        sizes.forEachIndexed { index, (size, label) ->
            val centerX = 390f + index * 190f
            val displayDiameter = size.radius * 3.4f
            if (region != null) drawFit(region, centerX - displayDiameter / 2f, 535f - displayDiameter / 2f, displayDiameter, displayDiameter)
            drawSmallScaled("$label  R=${size.radius.toInt()}", centerX - 85f, 450f, 170f, .58f)
            val button = compactButton(label, centerX - 82f, 335f, 164f, 72f, settings.selectedBallBaseSize == size)
            actions += button to { settings.selectedBallBaseSize = size; game.progress.saveSettings() }
        }
        return actions
    }

    private fun drawPaddles(): List<Pair<Rectangle, () -> Unit>> {
        val actions = mutableListOf<Pair<Rectangle, () -> Unit>>()
        val settings = game.progress.settings
        PaddleFilter.entries.forEachIndexed { index, filter ->
            val rect = compactButton(filter.name, 28f + index * 217f, 1215f, 195f, 70f, paddleFilter == filter)
            actions += rect to { paddleFilter = filter; paddleScrollOffset = 0f }
        }
        val filtered = game.assets.cosmetics.paddles.filter { paddleFilter == PaddleFilter.ALL || it.groupId.equals(paddleFilter.name, true) }
        if (filtered.isEmpty()) {
            game.assets.bodyFont.draw(batch, "PADDLE SPRITES UNAVAILABLE\nUSING CLASSIC PADDLE", 100f, 900f, 700f, Align.center, true)
            return actions
        }
        paddleScrollOffset = paddleScrollOffset.coerceIn(0f, maxPaddleScroll(filtered.size))
        drawSmallScaled("SCROLL UP / DOWN TO BROWSE ALL ${filtered.size} PADDLES", 70f, 1170f, 760f, .62f)
        beginPaddleClip()
        filtered.forEachIndexed { index, definition ->
            val col = index % 2; val row = index / 2
            val rect = Rectangle(40f + col * 430f, 980f - row * 190f + paddleScrollOffset, 390f, 164f)
            panel(rect, settings.selectedPaddleId == definition.id)
            game.assets.cosmetics.paddleRegion(definition)?.let { drawFit(it, rect.x + 24f, rect.y + 64f, rect.width - 48f, 75f) }
            drawSmallScaled(definition.displayNameEn.uppercase(), rect.x + 12f, rect.y + 43f, rect.width - 24f, .72f)
            actions += rect to { settings.selectedPaddleId = definition.id; game.progress.saveSettings() }
        }
        endPaddleClip()
        val selected = game.assets.cosmetics.selectedPaddle(settings.selectedPaddleId)
        game.assets.smallFont.draw(batch, "SELECTED PADDLE", 0f, 475f, 900f, Align.center, false)
        selected?.let { definition ->
            game.assets.cosmetics.paddleRegion(definition)?.let { drawFit(it, 145f, 340f, 610f, 105f) }
            game.assets.smallFont.draw(batch, definition.displayNameEn.uppercase(), 100f, 320f, 700f, Align.center, false)
        }
        return actions
    }

    private fun maxPaddleScroll(itemCount: Int): Float {
        val rows = (itemCount + 1) / 2
        return ((rows - 1) * 190f - 480f).coerceAtLeast(0f)
    }

    private fun beginPaddleClip() {
        batch.flush()
        val screenY = viewport.screenY + (500f / 1600f * viewport.screenHeight).toInt()
        val screenHeight = (650f / 1600f * viewport.screenHeight).toInt()
        Gdx.gl.glEnable(GL20.GL_SCISSOR_TEST)
        Gdx.gl.glScissor(viewport.screenX, screenY, viewport.screenWidth, screenHeight)
    }

    private fun endPaddleClip() {
        batch.flush()
        Gdx.gl.glDisable(GL20.GL_SCISSOR_TEST)
    }

    private fun leaveCustomization() {
        game.progress.saveSettings()
        if (returnToPausedGame) game.resumePausedGame() else game.openMenu()
    }

    private fun handleTouch(actions: List<Pair<Rectangle, () -> Unit>>) {
        val isTouched = Gdx.input.isTouched
        if (Gdx.input.justTouched()) {
            val point = touchPoint()
            touchActive = true
            touchMoved = false
            touchStartX = point.x; touchStartY = point.y
            touchLastX = point.x; touchLastY = point.y
        }
        if (touchActive && isTouched) {
            val point = touchPoint()
            if (tab == CustomizationTab.PADDLES) {
                val filteredCount = game.assets.cosmetics.paddles.count {
                    paddleFilter == PaddleFilter.ALL || it.groupId.equals(paddleFilter.name, true)
                }
                paddleScrollOffset = (paddleScrollOffset + point.y - touchLastY)
                    .coerceIn(0f, maxPaddleScroll(filteredCount))
            }
            touchLastX = point.x; touchLastY = point.y
            if (abs(point.y - touchStartY) > 28f || abs(point.x - touchStartX) > 28f) touchMoved = true
        } else if (touchActive) {
            val verticalDistance = touchLastY - touchStartY
            if (tab == CustomizationTab.BALLS && touchMoved && abs(verticalDistance) > 80f && abs(verticalDistance) > abs(touchLastX - touchStartX)) {
                changePage(if (verticalDistance > 0f) 1 else -1)
            } else if (!touchMoved) {
                actions.firstOrNull { it.first.contains(touchLastX, touchLastY) }?.second?.invoke()
            }
            touchActive = false
        }
    }

    private fun changePage(direction: Int) {
        if (tab == CustomizationTab.BALLS) {
            val lastIndex = game.assets.cosmetics.ballGroups.lastIndex
            if (lastIndex >= 0) ballGroupIndex = (ballGroupIndex + direction).coerceIn(0, lastIndex)
        }
    }

    private fun compactButton(text: String, x: Float, y: Float, width: Float, height: Float, selected: Boolean): Rectangle {
        val rect = Rectangle(x, y, width, height)
        batch.color = if (selected) Color(.2f, 1f, .65f, 1f) else Color(.25f, .8f, 1f, .9f)
        batch.draw(game.assets.ui.findRegion("button_primary"), x, y, width, height)
        batch.color = Color.WHITE
        game.assets.smallFont.draw(batch, text, x + 5f, y + height * .64f, width - 10f, Align.center, false)
        return rect
    }

    private fun panel(rect: Rectangle, selected: Boolean) {
        batch.color = if (selected) Color(.15f, 1f, .62f, 1f) else Color(.16f, .58f, .78f, .72f)
        batch.draw(game.assets.ui.findRegion("button_primary"), rect.x, rect.y, rect.width, rect.height)
        batch.color = Color.WHITE
    }

    private fun drawFit(region: TextureRegion, x: Float, y: Float, width: Float, height: Float) {
        val scale = minOf(width / region.regionWidth, height / region.regionHeight)
        val drawWidth = region.regionWidth * scale
        val drawHeight = region.regionHeight * scale
        batch.color = Color.WHITE
        batch.draw(region, x + (width - drawWidth) / 2f, y + (height - drawHeight) / 2f, drawWidth, drawHeight)
    }

    private fun drawSmallScaled(text: String, x: Float, baselineY: Float, width: Float, scale: Float) {
        val data = game.assets.smallFont.data
        val oldX = data.scaleX; val oldY = data.scaleY
        data.setScale(scale)
        game.assets.smallFont.draw(batch, text, x, baselineY, width, Align.center, false)
        data.setScale(oldX, oldY)
    }
}
