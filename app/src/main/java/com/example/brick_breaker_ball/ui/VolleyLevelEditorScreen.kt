/*
 * المسار: app/src/main/java/com/example/brick_breaker_ball/ui/VolleyLevelEditorScreen.kt
 * الوظيفة: واجهة محرر مراحل Volley.
 * الحسابات: تحويل لمسات الشبكة إلى صفوف وأعمدة.
 */

package com.example.brick_breaker_ball.ui

import com.badlogic.gdx.Gdx
import com.badlogic.gdx.Input
import com.badlogic.gdx.graphics.Color
import com.badlogic.gdx.graphics.g2d.BitmapFont
import com.badlogic.gdx.graphics.g2d.GlyphLayout
import com.badlogic.gdx.graphics.g2d.TextureRegion
import com.badlogic.gdx.math.Rectangle
import com.badlogic.gdx.scenes.scene2d.utils.ScissorStack
import com.example.brick_breaker_ball.BrickBreakerGame
import com.example.brick_breaker_ball.assets.VolleyPieceVisual
import com.example.brick_breaker_ball.assets.VolleyWorldArt
import com.example.brick_breaker_ball.campaign.VolleyBrickSeed
import com.example.brick_breaker_ball.campaign.VolleyPickupSeed
import com.example.brick_breaker_ball.campaign.VolleyStageRepository
import com.example.brick_breaker_ball.gameplay.Brick
import com.example.brick_breaker_ball.gameplay.BrickType
import com.example.brick_breaker_ball.gameplay.VolleyEditorDraft
import com.example.brick_breaker_ball.gameplay.VolleyEditorRepository
import com.example.brick_breaker_ball.gameplay.VolleyGeneratedLevel
import com.example.brick_breaker_ball.gameplay.VolleyGeneratedLevelJson
import com.example.brick_breaker_ball.gameplay.VolleyGeneratedLevelValidator
import com.example.brick_breaker_ball.gameplay.VolleyGeneratorFeature
import com.example.brick_breaker_ball.gameplay.VolleyGeneratorSamples
import com.example.brick_breaker_ball.gameplay.VolleyGeneratorSpec
import com.example.brick_breaker_ball.gameplay.VolleyLevelGenerator
import com.example.brick_breaker_ball.gameplay.VolleyLevelProbe
import com.example.brick_breaker_ball.gameplay.VolleyPattern
import com.example.brick_breaker_ball.gameplay.VolleyPieceShape
import java.util.ArrayDeque

/** Portrait Volley editor with the same 11 x 9 collision grid as the ranked campaign. */
class VolleyLevelEditorScreen(
    game: BrickBreakerGame,
    initialDraft: VolleyEditorDraft = VolleyEditorDraft()
) : ForgeScreen(game) {
    private companion object {
        const val GRID_LEFT = 132f
        const val GRID_RIGHT = 768f
        const val GRID_TOP = 1150f
        const val GRID_PITCH_Y = 50f
        const val GRID_BRICK_HEIGHT = 46f
        const val GRID_GAP = 4f
    }
    private enum class Tool { PAINT, ERASE, FILL, BALL }
    private enum class DrawerTab { TOOLS, BRICKS, GENERATOR }
    private data class Swatch(
        val type: BrickType,
        val label: String,
        val spriteName: String? = null,
        val shape: VolleyPieceShape = VolleyPieceShape.RECTANGLE
    )

    private val repository = VolleyEditorRepository()
    private var draft = initialDraft.duplicate()
    private val undo = ArrayDeque<VolleyEditorDraft>()
    private val redo = ArrayDeque<VolleyEditorDraft>()
    private var selectedType = VolleyStageRepository.editorMaterials(draft.world).first()
    private var selectedSpriteName: String? = null
    private var selectedShape = VolleyPieceShape.RECTANGLE
    private var selectedGroup = 1
    private var tool = Tool.PAINT
    private var drawerTab = DrawerTab.BRICKS
    private var toolsScroll = 0f
    private var bricksScroll = 0f
    private var generatorScroll = 0f
    private var generatorSpec = VolleyGeneratorSpec()
    private var sampleIndex = 0
    private var generatedLevel: VolleyGeneratedLevel? = null
    private var showCollisionBounds = false
    private val drawerContent = Rectangle(35f, 125f, 830f, 365f)
    private var scrollingDrawer = false
    private var draggingScrollThumb = false
    private var drawerTouchStartY = 0f
    private var drawerLastY = 0f
    private var drawerMoved = false
    private var pendingDrawerAction: Pair<Rectangle, () -> Unit>? = null
    private var libraryOpen = false
    private var libraryPage = 0
    private var savedLevels = emptyList<VolleyEditorDraft>()
    private var selectedSavedId: String? = null
    private var confirmClear = false
    private var confirmDelete = false
    private var status = "DRAG ON THE GRID TO PAINT"
    private var drawing = false
    private val actions = mutableListOf<Pair<Rectangle, () -> Unit>>()
    private val visualCache = mutableMapOf<BrickType, MutableMap<VolleyPieceShape, VolleyPieceVisual>>()
    private val previewBrick = Brick(1, Rectangle(0f, 0f, 80f, 66f), BrickType.NORMAL_ONE_HIT, hue = 0f)
    private val editorLabelLayout = GlyphLayout()

    private fun paletteFor(world: Int): List<Swatch> {
        val types = (
            VolleyStageRepository.editorMaterials(world) + listOf(
                BrickType.NORMAL_ONE_HIT, BrickType.ARMORED_TWO_HIT, BrickType.ARMORED_THREE_HIT,
                BrickType.GLASS, BrickType.EXPLOSIVE, BrickType.POWERUP_CARRIER,
                BrickType.REGENERATING, BrickType.GHOST, BrickType.MOVING_HORIZONTAL,
                BrickType.MOVING_VERTICAL, BrickType.KEY_BRICK, BrickType.LOCKED,
                BrickType.SWITCH, BrickType.CHAIN_BRICK, BrickType.INDESTRUCTIBLE,
                BrickType.SPIKED_HAZARD, BrickType.BOSS_CORE
            )
            ).distinct().map { Swatch(it, it.name.replace('_', ' ')) }
        val material = VolleyStageRepository.editorMaterials(world).first()
        val worldTiles = game.assets.volleyWorldArt(world).editorBrickTiles().mapIndexed { index, tile ->
            val kind = when {
                tile.shape != VolleyPieceShape.RECTANGLE -> "TRIANGLE"
                tile.region.regionWidth > tile.region.regionHeight * 1.45f -> "WIDE"
                else -> "SQUARE"
            }
            Swatch(material, "TILE ${index + 1}  $kind", tile.name, tile.shape)
        }
        return worldTiles + types
    }
    private var paletteTypes = paletteFor(draft.world)
    init {
        selectedSpriteName = paletteTypes.firstOrNull()?.spriteName
        selectedShape = paletteTypes.firstOrNull()?.shape ?: VolleyPieceShape.RECTANGLE
    }

    private fun remember() {
        if (undo.size == 60) undo.removeFirst()
        undo.addLast(draft.duplicate())
        redo.clear()
        generatedLevel = null
    }

    private fun action(
        label: String,
        x: Float,
        y: Float,
        w: Float,
        h: Float = 70f,
        style: ForgeUiRenderer.GradientStyle = ForgeUiRenderer.GradientStyle.NEUTRAL,
        onTap: () -> Unit
    ) {
        val rect = Rectangle(x, y, w, h)
        val iconName = when {
            label.startsWith("PAINT") -> "paint"
            label.startsWith("ERASE") -> "erase"
            label.startsWith("FILL") -> "fill"
            label.startsWith("BALL +") -> "ball"
            label.startsWith("UNDO") -> "undo"
            label.startsWith("REDO") -> "redo"
            label.startsWith("CLEAR") || label.startsWith("DELETE") -> "clear"
            label.startsWith("SAVE AS") || label.startsWith("COPY") || label.startsWith("NEW") -> "duplicate"
            label.startsWith("SAVE") -> "save"
            label.startsWith("TEST") -> "test"
            label.startsWith("LIBRARY") || label.startsWith("LOAD") -> "load"
            label.startsWith("BG") || label.startsWith("WORLD") -> "world"
            else -> null
        }
        val icon = when {
            label.startsWith("BALLS") -> game.assets.atlasUiIcon("ball")
            iconName != null -> game.assets.volleyEditorAtlas.requireRegion(iconName)
            else -> null
        }
        val hasIconDock = icon != null && h >= 75f && w >= 270f
        val buttonName = when {
            w >= 360f && h >= 90f && !hasIconDock -> "tool_pill_plain"

            w >= 360f && h >= 90f -> when (style) {
                ForgeUiRenderer.GradientStyle.PRIMARY -> "tool_pill_selected"
                ForgeUiRenderer.GradientStyle.CRIMSON -> "tool_pill_danger"
                else -> "tool_pill"
            }

            h >= 75f -> when (style) {
                ForgeUiRenderer.GradientStyle.CRIMSON -> "footer_pill_danger"
                ForgeUiRenderer.GradientStyle.SUCCESS -> "footer_pill_save"
                ForgeUiRenderer.GradientStyle.PRIMARY -> "footer_pill_primary"
                else -> "footer_pill"
            }

            w >= 245f -> if (style == ForgeUiRenderer.GradientStyle.PRIMARY) "tab_pill_selected" else "tab_pill"

            w <= 90f -> "arrow_pill"

            else -> if (style == ForgeUiRenderer.GradientStyle.PRIMARY) "small_pill_selected" else "small_pill"
        }
        val pressed = Gdx.input.isTouched && rect.contains(touchPoint().x, touchPoint().y)
        batch.color = if (pressed) Color(.72f, .88f, 1f, 1f) else Color.WHITE
        batch.draw(game.assets.volleyEditorControls.requireRegion(buttonName), x, y, w, h)
        batch.color = Color.WHITE
        val iconWidth = if (icon != null && hasIconDock) minOf(h * .60f, w * .20f) else 0f
        if (icon != null && iconWidth > 0f) {
            batch.draw(
                icon,
                x + 14f,
                y + (h - iconWidth) / 2f,
                iconWidth,
                iconWidth
            )
        }
        val textLeft = x + if (hasIconDock) (if (w >= 360f) 96f else 78f) else 15f
        editorText(
            game.assets.hudLabelFont,
            label,
            textLeft,
            y + h * .64f,
            w - (textLeft - x) - 20f,
            if (w < 160f) .52f else .68f,
            center = !hasIconDock
        )
        actions += rect to onTap
    }

    /** Keep every label inside its artwork at the fixed orientation text scale. */
    private fun editorText(
        font: BitmapFont,
        text: String,
        x: Float,
        y: Float,
        width: Float,
        maxScale: Float,
        center: Boolean = false
    ) {
        val oldX = font.data.scaleX
        val oldY = font.data.scaleY
        val desired = maxScale * uiTextScale
        font.data.setScale(desired)
        editorLabelLayout.setText(font, text)
        if (editorLabelLayout.width > width) {
            font.data.setScale((desired * width / editorLabelLayout.width).coerceAtLeast(desired * .60f))
        }
        var shown = text
        editorLabelLayout.setText(font, shown)
        while (editorLabelLayout.width > width && shown.isNotEmpty()) {
            shown = shown.dropLast(1).trimEnd()
            editorLabelLayout.setText(font, "$shown...")
        }
        font.draw(
            batch,
            if (shown == text) text else "$shown...",
            if (center) x + (width - editorLabelLayout.width) / 2f else x,
            y
        )
        font.data.setScale(oldX, oldY)
    }

    private fun changeWorld(delta: Int) {
        remember()
        draft.world = Math.floorMod(draft.world - 1 + delta, VolleyStageRepository.WORLDS_COUNT) + 1
        draft.backgroundStage = 1
        selectedType = VolleyStageRepository.editorMaterials(draft.world).first()
        paletteTypes = paletteFor(draft.world)
        selectedSpriteName = paletteTypes.firstOrNull()?.spriteName
        selectedShape = paletteTypes.firstOrNull()?.shape ?: VolleyPieceShape.RECTANGLE
        bricksScroll = 0f
        visualCache.clear()
        status = "WORLD ${draft.world} | ${game.assets.volleyWorldArt(draft.world).editorBrickTiles().size} TILES | DRAG BLUE BAR"
    }

    private fun showDraft(next: VolleyEditorDraft) {
        generatedLevel = null
        draft = next.duplicate()
        selectedType = VolleyStageRepository.editorMaterials(draft.world).first()
        paletteTypes = paletteFor(draft.world)
        selectedSpriteName = paletteTypes.firstOrNull()?.spriteName
        selectedShape = paletteTypes.firstOrNull()?.shape ?: VolleyPieceShape.RECTANGLE
        bricksScroll = 0f
        visualCache.clear()
    }

    override fun render(delta: Float) {
        val preview = VolleyStageRepository.forLevel(
            VolleyStageRepository.firstLevel(draft.world) + draft.backgroundStage - 1
        )
        val background = game.assets.volleyStageBackground(preview)
        beginStaticBackground(background, .64f)
        actions.clear()
        batch.color = Color.WHITE
        batch.draw(game.assets.boards2.requireRegion("scroll_board"), 14f, 4f, 872f, 1592f)
        batch.setColor(1f, 1f, 1f, .82f)
        batch.draw(background, 137f, 650f, 626f, 500f)
        batch.color = Color.WHITE
        batch.draw(game.assets.volleyEditorAtlas.requireRegion("editor_canvas"), 24f, 600f, 852f, 600f)
        fittedText(game.assets.titleFont, "VOLLEY EDITOR", 88f, 1415f, 724f, .68f)
        editorText(game.assets.hudLabelFont, draft.name, 110f, 1360f, 680f, .70f, center = true)
        if (VolleyGeneratorFeature.available && draft.generatorPattern != null) {
            val count = draft.cells.flatten().count { it != null }
            val avg = draft.cells.flatten().filterNotNull().map { draft.baseHp * it.hpScale }.average()
            editorText(
                game.assets.smallFont,
                "BRICKS $count  AVG HP ${"%.1f".format(java.util.Locale.US, avg)}  SEED ${draft.generatorSeed}",
                105f,
                1208f,
                690f,
                .51f,
                center = true
            )
        }
        action("NAME", 20f, 1250f, 145f) { rename() }
        action("<", 175f, 1250f, 80f) { changeWorld(-1) }
        batch.draw(
            game.assets.volleyEditorControls.requireRegion("tab_pill_selected"),
            267f,
            1250f,
            373f,
            70f
        )
        editorText(
            game.assets.hudLabelFont,
            VolleyStageRepository.worlds[draft.world - 1].name,
            291f,
            1297f,
            325f,
            .72f,
            center = true
        )
        action(">", 645f, 1250f, 80f) { changeWorld(1) }
        action("BG ${draft.backgroundStage}", 730f, 1250f, 150f) {
            remember()
            draft.backgroundStage = draft.backgroundStage % VolleyStageRepository.worldLevelCount(draft.world) + 1
        }

        drawGrid()
        drawDrawer()
        action("SAVE", 20f, 10f, 276f, 76f, ForgeUiRenderer.GradientStyle.SUCCESS) { save() }
        action("TEST", 312f, 10f, 276f, 76f, ForgeUiRenderer.GradientStyle.PRIMARY) { testStage() }
        action("BACK", 604f, 10f, 276f, 76f) { game.openMenu() }

        if (libraryOpen) {
            actions.clear()
            drawLibrary()
        }
        if (confirmClear || confirmDelete) {
            actions.clear()
            drawConfirmation()
        }
        end()
        handleInput()
    }

    private fun drawGrid() {
        val cellW = (GRID_RIGHT - GRID_LEFT - 10f * GRID_GAP) / 11f
        val pitchX = cellW + GRID_GAP
        val top = GRID_TOP
        val art = game.assets.volleyWorldArt(draft.world)
        repeat(9) { row ->
            repeat(11) { col ->
                val x = GRID_LEFT + col * pitchX
                val y = top - (row + 1) * GRID_PITCH_Y
                batch.setColor(1f, 1f, 1f, .22f)
                batch.draw(game.assets.boards2.requireRegion("stone_button_style_02"), x, y, cellW, GRID_BRICK_HEIGHT)
                batch.color = Color.WHITE
                draft.cells[row][col]?.let { seed ->
                    val named = seed.atlasSpriteName?.let(art::editorBrickSprite)
                    if (named != null) {
                        drawContained(named, x, y, cellW, GRID_BRICK_HEIGHT)
                    } else {
                        val visual = visualFor(art, seed.type, seed.shape)
                        batch.draw(
                            visual.region, x, y, cellW / 2f, GRID_BRICK_HEIGHT / 2f,
                            cellW, GRID_BRICK_HEIGHT, 1f, 1f, visual.rotationDegrees
                        )
                    }
                    if (showCollisionBounds && VolleyGeneratorFeature.available) {
                        drawCollisionOutline(seed.shape, x, y, cellW, GRID_BRICK_HEIGHT)
                    }
                }
                if (draft.pickups.any { it.x == col + .5f && it.y == row + .5f }) {
                    batch.draw(
                        game.assets.volleyEditorAtlas.requireRegion("ball"),
                        x + cellW * .20f,
                        y + 9f,
                        cellW * .60f,
                        GRID_BRICK_HEIGHT - 18f
                    )
                    batch.color = Color.WHITE
                }
            }
        }
    }

    private fun drawCollisionOutline(shape: VolleyPieceShape, x: Float, y: Float, width: Float, height: Float) {
        val left = x + 1f
        val right = x + width - 1f
        val bottom = y + 1f
        val top = y + height - 1f
        if (shape == VolleyPieceShape.RECTANGLE || shape == VolleyPieceShape.TRIANGLE_NW ||
            shape == VolleyPieceShape.TRIANGLE_NE
        ) {
            drawCollisionEdge(left, top, right, top)
        }
        if (shape == VolleyPieceShape.RECTANGLE || shape == VolleyPieceShape.TRIANGLE_SW ||
            shape == VolleyPieceShape.TRIANGLE_SE
        ) {
            drawCollisionEdge(left, bottom, right, bottom)
        }
        if (shape == VolleyPieceShape.RECTANGLE || shape == VolleyPieceShape.TRIANGLE_NW ||
            shape == VolleyPieceShape.TRIANGLE_SW
        ) {
            drawCollisionEdge(left, bottom, left, top)
        }
        if (shape == VolleyPieceShape.RECTANGLE || shape == VolleyPieceShape.TRIANGLE_NE ||
            shape == VolleyPieceShape.TRIANGLE_SE
        ) {
            drawCollisionEdge(right, bottom, right, top)
        }
        when (shape) {
            VolleyPieceShape.TRIANGLE_NW, VolleyPieceShape.TRIANGLE_SE -> drawCollisionEdge(left, bottom, right, top)
            VolleyPieceShape.TRIANGLE_NE, VolleyPieceShape.TRIANGLE_SW -> drawCollisionEdge(left, top, right, bottom)
            else -> Unit
        }
    }

    private fun drawCollisionEdge(x1: Float, y1: Float, x2: Float, y2: Float) {
        val dx = x2 - x1
        val dy = y2 - y1
        val length = kotlin.math.sqrt(dx * dx + dy * dy)
        val angle = kotlin.math.atan2(dy, dx) * 180f / kotlin.math.PI.toFloat()
        batch.setColor(.15f, .95f, 1f, .95f)
        batch.draw(
            game.assets.solidUiRegion, x1, y1, 0f, 1.5f,
            length, 3f, 1f, 1f, angle
        )
        batch.color = Color.WHITE
    }

    private fun drawDrawer() {
        batch.draw(game.assets.boards2.requireRegion("large_stone_item_panel"), 20f, 96f, 860f, 405f)
        action(
            "TOOLS",
            28f,
            510f,
            if (VolleyGeneratorFeature.available) 198f else 267f,
            70f,
            if (drawerTab == DrawerTab.TOOLS) ForgeUiRenderer.GradientStyle.PRIMARY else ForgeUiRenderer.GradientStyle.NEUTRAL
        ) {
            drawerTab = DrawerTab.TOOLS
        }
        action(
            "BRICKS",
            if (VolleyGeneratorFeature.available) 239f else 317f,
            510f,
            if (VolleyGeneratorFeature.available) 198f else 267f,
            70f,
            if (drawerTab == DrawerTab.BRICKS) ForgeUiRenderer.GradientStyle.PRIMARY else ForgeUiRenderer.GradientStyle.NEUTRAL
        ) {
            drawerTab = DrawerTab.BRICKS
        }
        if (VolleyGeneratorFeature.available) {
            action(
                "GENERATOR",
                450f,
                510f,
                198f,
                70f,
                if (drawerTab == DrawerTab.GENERATOR) ForgeUiRenderer.GradientStyle.PRIMARY else ForgeUiRenderer.GradientStyle.NEUTRAL
            ) {
                drawerTab = DrawerTab.GENERATOR
            }
        }
        action(
            "LIBRARY",
            if (VolleyGeneratorFeature.available) 662f else 606f,
            510f,
            if (VolleyGeneratorFeature.available) 211f else 267f,
            70f
        ) {
            savedLevels = repository.list()
            selectedSavedId = draft.id.takeIf { id -> savedLevels.any { it.id == id } }
            libraryOpen = true
            libraryPage = 0
        }
        batch.flush()
        val scissors = Rectangle()
        ScissorStack.calculateScissors(camera, batch.transformMatrix, drawerContent, scissors)
        if (ScissorStack.pushScissors(scissors)) {
            try {
                when (drawerTab) {
                    DrawerTab.TOOLS -> drawTools()
                    DrawerTab.BRICKS -> drawPalette()
                    DrawerTab.GENERATOR -> drawGenerator()
                }
            } finally {
                batch.flush()
                ScissorStack.popScissors()
            }
        }
        editorText(game.assets.smallFont, status, 50f, 111f, 800f, .59f, center = true)
        val maxScroll = drawerMaxScroll()
        if (maxScroll > 0f) {
            val offset = when (drawerTab) {
                DrawerTab.TOOLS -> toolsScroll
                DrawerTab.BRICKS -> bricksScroll
                DrawerTab.GENERATOR -> generatorScroll
            }
            batch.setColor(.10f, .23f, .32f, .75f)
            batch.draw(game.assets.solidUiRegion, 852f, 130f, 7f, 350f)
            batch.setColor(.29f, .78f, 1f, 1f)
            val thumbHeight = (350f * 365f / (365f + maxScroll)).coerceAtLeast(45f)
            val thumbY = 130f + (350f - thumbHeight) * (1f - offset / maxScroll)
            batch.draw(game.assets.solidUiRegion, 851f, thumbY, 9f, thumbHeight)
            batch.color = Color.WHITE
        }
    }

    private fun drawerMaxScroll(): Float {
        val rows = when (drawerTab) {
            DrawerTab.TOOLS -> 8
            DrawerTab.BRICKS -> (paletteTypes.size + 1) / 2
            DrawerTab.GENERATOR -> 10
        }
        val step = if (drawerTab == DrawerTab.BRICKS) 192f else 104f
        val firstRowBottom = if (drawerTab == DrawerTab.BRICKS) 300f else 382f
        return (drawerContent.y - (firstRowBottom - (rows - 1) * step)).coerceAtLeast(0f)
    }

    private fun toolAction(
        index: Int,
        label: String,
        style: ForgeUiRenderer.GradientStyle = ForgeUiRenderer.GradientStyle.NEUTRAL,
        onTap: () -> Unit
    ) {
        val x = 43f + index % 2 * 405f
        val y = 382f - index / 2 * 104f + toolsScroll
        if (y + 96f < drawerContent.y || y > drawerContent.y + drawerContent.height) return
        action(label, x, y, 394f, 96f, style, onTap)
    }

    private fun drawTools() {
        val selected = ForgeUiRenderer.GradientStyle.PRIMARY
        toolAction(0, "PAINT", if (tool == Tool.PAINT) selected else ForgeUiRenderer.GradientStyle.NEUTRAL) { tool = Tool.PAINT }
        toolAction(1, "ERASE", if (tool == Tool.ERASE) selected else ForgeUiRenderer.GradientStyle.NEUTRAL) { tool = Tool.ERASE }
        toolAction(2, "FILL", if (tool == Tool.FILL) selected else ForgeUiRenderer.GradientStyle.NEUTRAL) { tool = Tool.FILL }
        toolAction(3, "BALL +", if (tool == Tool.BALL) selected else ForgeUiRenderer.GradientStyle.NEUTRAL) { tool = Tool.BALL }
        toolAction(4, "UNDO") {
            if (undo.isNotEmpty()) {
                redo.addLast(draft.duplicate())
                showDraft(undo.removeLast())
            }
        }
        toolAction(5, "REDO") {
            if (redo.isNotEmpty()) {
                undo.addLast(draft.duplicate())
                showDraft(redo.removeLast())
            }
        }
        toolAction(6, "SHAPE") {
            selectedShape = VolleyPieceShape.entries[(selectedShape.ordinal + 1) % VolleyPieceShape.entries.size]
            selectedSpriteName = null
            status = "SHAPE: ${selectedShape.name.replace('_', ' ')}"
        }
        toolAction(7, "CLEAR", ForgeUiRenderer.GradientStyle.CRIMSON) { confirmClear = true }
        toolAction(8, "WORLD -") { changeWorld(-1) }
        toolAction(9, "WORLD +") { changeWorld(1) }
        toolAction(10, "BG ${draft.backgroundStage}") {
            remember()
            draft.backgroundStage = draft.backgroundStage % VolleyStageRepository.worldLevelCount(draft.world) + 1
        }
        toolAction(11, "HP ${draft.baseHp}") {
            remember()
            draft.baseHp = if (draft.baseHp >= 20) 1 else draft.baseHp + 1
        }
        toolAction(12, "BALLS ${draft.startingBalls}") {
            remember()
            draft.startingBalls = if (draft.startingBalls >= 99) 1 else draft.startingBalls + 1
        }
        toolAction(13, "GROUP $selectedGroup") { selectedGroup = if (selectedGroup >= 9) 1 else selectedGroup + 1 }
        toolAction(14, "NAME") { rename() }
        toolAction(15, "NEW") {
            remember()
            showDraft(VolleyEditorDraft(world = draft.world))
            status = "NEW EMPTY STAGE - UNDO CAN RESTORE"
        }
    }

    private fun generatorAction(index: Int, label: String, onTap: () -> Unit) {
        val x = 43f + index % 2 * 405f
        val y = 382f - index / 2 * 104f + generatorScroll
        if (y + 96f < drawerContent.y || y > drawerContent.y + drawerContent.height) return
        action(label, x, y, 394f, 96f, onTap = onTap)
    }

    /** QA controls reuse the editor grid and its isolated TEST route. */
    private fun drawGenerator() {
        if (!VolleyGeneratorFeature.available) return
        generatorAction(0, "PATTERN ${generatorSpec.pattern.name.replace('_', ' ')}") {
            generatorSpec = generatorSpec.copy(
                pattern = VolleyPattern.entries[
                    (generatorSpec.pattern.ordinal + 1) % VolleyPattern.entries.size
                ]
            )
        }
        generatorAction(1, "SEED ${generatorSpec.seed}") {
            Gdx.input.getTextInput(
                object : Input.TextInputListener {
                    override fun input(text: String) {
                        text.trim().toLongOrNull()?.let { generatorSpec = generatorSpec.copy(seed = it) }
                            ?: run { status = "SEED MUST BE A WHOLE NUMBER" }
                    }
                    override fun canceled() = Unit
                },
                "GENERATOR SEED",
                generatorSpec.seed.toString(),
                "Whole number"
            )
        }
        generatorAction(2, "DIFFICULTY ${generatorSpec.difficulty}") {
            generatorSpec = generatorSpec.copy(difficulty = generatorSpec.difficulty % 9 + 1)
        }
        generatorAction(3, "DENSITY ${(generatorSpec.density * 100).toInt()}%") {
            generatorSpec = generatorSpec.copy(density = if (generatorSpec.density >= .98f) .4f else generatorSpec.density + .1f)
        }
        generatorAction(4, "ROTATION ${generatorSpec.rotation * 90}") {
            generatorSpec = generatorSpec.copy(rotation = (generatorSpec.rotation + 1) % 4)
        }
        generatorAction(5, "MIRROR ${if (generatorSpec.reflectX) "ON" else "OFF"}") {
            generatorSpec = generatorSpec.copy(reflectX = !generatorSpec.reflectX)
        }
        generatorAction(6, "SCALE ${"%.2f".format(java.util.Locale.US, generatorSpec.scale)}") {
            generatorSpec = generatorSpec.copy(scale = if (generatorSpec.scale >= 1.24f) .7f else generatorSpec.scale + .1f)
        }
        generatorAction(7, "SHIFT X ${generatorSpec.translateX}") {
            generatorSpec = generatorSpec.copy(translateX = if (generatorSpec.translateX >= .19f) -.2f else generatorSpec.translateX + .1f)
        }
        generatorAction(8, "SHIFT Y ${generatorSpec.translateY}") {
            generatorSpec = generatorSpec.copy(translateY = if (generatorSpec.translateY >= .19f) -.2f else generatorSpec.translateY + .1f)
        }
        generatorAction(9, "SPACING ${generatorSpec.spacing}") {
            generatorSpec = generatorSpec.copy(spacing = generatorSpec.spacing % 3 + 1)
        }
        generatorAction(10, "COMBINE ${generatorSpec.combineWith?.name ?: "OFF"}") {
            generatorSpec = generatorSpec.copy(
                combineWith = when (generatorSpec.combineWith) {
                    null -> VolleyPattern.CROSS
                    VolleyPattern.CROSS -> VolleyPattern.RING
                    else -> null
                }
            )
        }
        generatorAction(11, "GENERATE") { generateLevel() }
        generatorAction(12, "COLLISION ${if (showCollisionBounds) "ON" else "OFF"}") {
            showCollisionBounds = !showCollisionBounds
        }
        generatorAction(13, "CAMPAIGN ${if (VolleyGeneratorFeature.enabled()) "GENERATED" else "ORIGINAL"}") {
            VolleyGeneratorFeature.setEnabled(!VolleyGeneratorFeature.enabled())
            status = "NEW CAMPAIGN SESSIONS: ${if (VolleyGeneratorFeature.enabled()) "GENERATED" else "ORIGINAL"}"
        }
        generatorAction(14, "EXPORT JSON") {
            generatedLevel?.let { level ->
                val json = VolleyGeneratedLevelJson.export(level)
                Gdx.app.clipboard.contents = json
                val file = Gdx.files.local("volley-editor/generated-${level.stage.levelId}-${level.spec.seed}.json")
                file.parent().mkdirs()
                file.writeString(json, false, "UTF-8")
                status = "JSON COPIED AND SAVED LOCALLY"
            } ?: run { status = "GENERATE A STAGE BEFORE EXPORT" }
        }
        generatorAction(15, "IMPORT JSON") {
            val source = Gdx.app.clipboard.contents ?: ""
            VolleyGeneratedLevelJson.decode(source).fold(
                onSuccess = { level ->
                    remember()
                    generatorSpec = level.spec
                    showDraft(
                        VolleyEditorDraft(
                            name = level.stage.name.take(40), world = level.stage.world,
                            backgroundStage = level.stage.stage, startingBalls = level.stage.startingBalls,
                            baseHp = level.stage.baseHp, generatorSeed = level.spec.seed,
                            generatorPattern = level.spec.pattern,
                            generatorDifficulty = level.spec.difficulty,
                            bonusStage = level.stage.bonusStage,
                            bossStage = level.stage.bossStage,
                            cells = level.stage.cells.map { it.toMutableList() }.toMutableList(),
                            pickups = level.stage.pickups.toMutableList()
                        )
                    )
                    generatedLevel = level
                    status = "IMPORTED ${level.stats.brickCount} BRICKS"
                },
                onFailure = { status = "INVALID JSON: ${it.message?.take(45) ?: "PARSE ERROR"}" }
            )
        }
        generatorAction(16, "VALIDATE") {
            val errors = runCatching { VolleyGeneratedLevelValidator.validate(draft.toStage()) }
                .getOrElse { listOf(it.message ?: "INVALID STAGE") }
            status = if (errors.isEmpty()) "STRUCTURE OK; WIN NOT PROVEN" else errors.first().uppercase()
        }
        generatorAction(17, "NEXT SEED +1") {
            generatorSpec = generatorSpec.copy(seed = generatorSpec.seed + 1)
            generateLevel()
        }
        generatorAction(18, "BALL PROBE") {
            val level = generatedLevel
            if (level == null) {
                status = "GENERATE A STAGE FIRST"
            } else {
                val result = VolleyLevelProbe.measure(level.stage, trials = 1, maxTurns = 2)
                status = "PROBE: ${result.damage} DAMAGE, ${result.wins} WINS, ${result.timeouts} TIMEOUTS"
            }
        }
        generatorAction(19, "SAMPLE ${sampleIndex + 1}/3") {
            val sample = VolleyGeneratorSamples.levels[sampleIndex]
            sampleIndex = (sampleIndex + 1) % VolleyGeneratorSamples.levels.size
            val base = VolleyStageRepository.forLevel(sample.levelId)
            remember()
            draft.world = base.world
            draft.backgroundStage = base.stage
            generatorSpec = sample.spec
            generateLevel()
            status = "${sample.name}: ${draft.cells.flatten().count { it != null }} BRICKS"
        }
    }

    private fun generateLevel() {
        val base = VolleyStageRepository.forLevel(VolleyStageRepository.firstLevel(draft.world) + draft.backgroundStage - 1)
        runCatching { VolleyLevelGenerator.generate(base, generatorSpec) }.fold(
            onSuccess = { result ->
                remember()
                generatorSpec = result.spec
                showDraft(
                    VolleyEditorDraft(
                        name = result.stage.name.take(40), world = result.stage.world,
                        backgroundStage = result.stage.stage,
                        startingBalls = result.stage.startingBalls, baseHp = result.stage.baseHp,
                        generatorSeed = result.spec.seed, generatorPattern = result.spec.pattern,
                        generatorDifficulty = result.spec.difficulty, bonusStage = result.stage.bonusStage,
                        bossStage = result.stage.bossStage,
                        cells = result.stage.cells.map { it.toMutableList() }.toMutableList(),
                        pickups = result.stage.pickups.toMutableList()
                    )
                )
                generatedLevel = result
                status = "${result.stats.brickCount} BRICKS | AVG HP ${"%.1f".format(java.util.Locale.US, result.stats.averageHp)}"
            },
            onFailure = { status = "GENERATION FAILED: ${it.message?.take(38) ?: "ERROR"}" }
        )
    }

    private fun drawPalette() {
        val art = game.assets.volleyWorldArt(draft.world)
        paletteTypes.forEachIndexed { index, swatch ->
            val y = 300f - index / 2 * 192f + bricksScroll
            if (y + 182f < drawerContent.y || y > drawerContent.y + drawerContent.height) {
                return@forEachIndexed
            }
            val rect = Rectangle(43f + index % 2 * 405f, y, 394f, 182f)
            val visual = swatch.spriteName?.let(art::editorBrickSprite)
                ?: visualFor(art, swatch.type, VolleyPieceShape.RECTANGLE).region
            val wide = swatch.shape == VolleyPieceShape.RECTANGLE &&
                visual.regionWidth > visual.regionHeight * 1.45f
            val selected = swatch.type == selectedType && swatch.spriteName == selectedSpriteName
            val cardName = when {
                wide && selected -> "brick_wide_card_selected"
                wide -> "brick_wide_card"
                selected -> "brick_tile_card_selected"
                else -> "brick_tile_card"
            }
            batch.draw(
                game.assets.volleyEditorControls.requireRegion(cardName),
                rect.x,
                rect.y,
                rect.width,
                rect.height
            )
            if (wide) {
                drawContained(visual, rect.x + 58f, rect.y + 52f, 278f, 104f)
            } else {
                drawContained(visual, rect.x + 140f, rect.y + 51f, 114f, 106f)
            }
            editorText(
                game.assets.hudLabelFont,
                swatch.label,
                rect.x + 24f,
                rect.y + 13f,
                rect.width - 48f,
                .58f,
                center = true
            )
            actions += rect to {
                selectedType = swatch.type
                selectedSpriteName = swatch.spriteName
                selectedShape = swatch.shape
                tool = Tool.PAINT
                status = swatch.label
            }
        }
    }

    private fun drawContained(region: TextureRegion, x: Float, y: Float, width: Float, height: Float) {
        val scale = minOf(width / region.regionWidth, height / region.regionHeight)
        val drawnWidth = region.regionWidth * scale
        val drawnHeight = region.regionHeight * scale
        batch.draw(
            region,
            x + (width - drawnWidth) / 2f,
            y + (height - drawnHeight) / 2f,
            drawnWidth,
            drawnHeight
        )
    }

    private fun drawLibrary() {
        val levels = savedLevels
        batch.setColor(0f, 0f, 0f, .78f)
        batch.draw(game.assets.solidUiRegion, visibleLeft, visibleBottom, visibleWidth, visibleHeight)
        batch.color = Color.WHITE
        batch.draw(game.assets.boards2.requireRegion("large_stone_item_panel"), 48f, 330f, 804f, 935f)
        fittedText(game.assets.titleFont, "SAVED VOLLEY STAGES", 80f, 1205f, 740f, .62f)
        action("SAVE AS CURRENT", 260f, 1115f, 380f, 70f, ForgeUiRenderer.GradientStyle.SUCCESS) {
            val next = draft.duplicate(newId = true).apply { name = "${name.take(32)} COPY" }
            remember()
            showDraft(next)
            save()
            libraryOpen = false
        }
        val shown = levels.drop(libraryPage * 6).take(6)
        shown.forEachIndexed { index, level ->
            val y = 1010f - index * 95f
            val rect = Rectangle(78f, y, 744f, 90f)
            batch.color = if (level.id == selectedSavedId) Color(.68f, .91f, 1f, 1f) else Color.WHITE
            batch.draw(
                game.assets.volleyEditorControls.requireRegion("library_row"),
                rect.x,
                rect.y,
                rect.width,
                rect.height
            )
            batch.color = Color.WHITE
            editorText(
                game.assets.hudLabelFont,
                "${level.name}  |  WORLD ${level.world}",
                rect.x + 28f,
                y + 59f,
                rect.width - 56f,
                .67f
            )
            actions += rect to { selectedSavedId = level.id }
        }
        if (levels.isEmpty()) fittedText(game.assets.hudLabelFont, "NO SAVED STAGES YET", 90f, 850f, 720f, .75f)
        action("PREV", 72f, 424f, 172f) { libraryPage = (libraryPage - 1).coerceAtLeast(0) }
        action("NEXT", 258f, 424f, 172f) { libraryPage = (libraryPage + 1).coerceAtMost((levels.size - 1).coerceAtLeast(0) / 6) }
        action("LOAD", 446f, 424f, 172f) {
            levels.firstOrNull { it.id == selectedSavedId }?.let {
                remember()
                showDraft(it)
                libraryOpen = false
                status = "STAGE LOADED"
            }
        }
        action("COPY", 632f, 424f, 172f) {
            levels.firstOrNull { it.id == selectedSavedId }?.let {
                remember()
                showDraft(it.duplicate(newId = true).apply { name = "${name.take(32)} COPY" })
                libraryOpen = false
                status = "COPY READY TO SAVE"
            }
        }
        action("DELETE", 72f, 342f, 350f, style = ForgeUiRenderer.GradientStyle.CRIMSON) { if (selectedSavedId != null) confirmDelete = true }
        action("CLOSE", 470f, 342f, 334f) { libraryOpen = false }
    }

    private fun drawConfirmation() {
        batch.setColor(0f, 0f, 0f, .78f)
        batch.draw(game.assets.solidUiRegion, visibleLeft, visibleBottom, visibleWidth, visibleHeight)
        batch.color = Color.WHITE
        val dialog = game.assets.boards.contain("confirmation_dialog", Rectangle(92f, 625f, 716f, 340f))
        batch.draw(game.assets.boards.requireRegion("confirmation_dialog"), dialog.x, dialog.y, dialog.width, dialog.height)
        fittedText(
            game.assets.hudLabelFont,
            if (confirmDelete) "DELETE SAVED STAGE?" else "CLEAR ALL BRICKS?",
            dialog.x + 35f,
            dialog.y + dialog.height * .49f,
            dialog.width - 70f,
            .70f
        )
        val no = game.assets.boards.placement("confirmation_dialog", "confirmation_no_button", dialog)
        val yes = game.assets.boards.placement("confirmation_dialog", "confirmation_yes_button", dialog)
        actions += no to {
            confirmClear = false
            confirmDelete = false
        }
        actions += yes to {
            if (confirmClear) {
                remember()
                draft.cells.forEach { row -> row.fill(null) }
                draft.pickups.clear()
                status = "BOARD CLEARED"
            }
            if (confirmDelete) {
                selectedSavedId?.let(repository::delete)
                savedLevels = repository.list()
                selectedSavedId = null
                status = "STAGE DELETED"
            }
            confirmClear = false
            confirmDelete = false
        }
    }

    private fun handleInput() {
        if (Gdx.input.isKeyJustPressed(Input.Keys.BACK) || Gdx.input.isKeyJustPressed(Input.Keys.ESCAPE)) {
            when {
                confirmClear || confirmDelete -> {
                    confirmClear = false
                    confirmDelete = false
                }

                libraryOpen -> libraryOpen = false

                else -> game.openMenu()
            }
            return
        }
        if (scrollingDrawer) {
            val point = touchPoint()
            if (Gdx.input.isTouched) {
                if (draggingScrollThumb) {
                    scrollDrawerToThumb(point.y)
                } else {
                    val distance = point.y - drawerTouchStartY
                    if (kotlin.math.abs(distance) > 14f) drawerMoved = true
                    if (drawerMoved) {
                        val currentScroll = when (drawerTab) {
                            DrawerTab.TOOLS -> toolsScroll
                            DrawerTab.BRICKS -> bricksScroll
                            DrawerTab.GENERATOR -> generatorScroll
                        }
                        val next = (
                            currentScroll +
                                point.y - drawerLastY
                            ).coerceIn(0f, drawerMaxScroll())
                        when (drawerTab) {
                            DrawerTab.TOOLS -> toolsScroll = next
                            DrawerTab.BRICKS -> bricksScroll = next
                            DrawerTab.GENERATOR -> generatorScroll = next
                        }
                    }
                }
                drawerLastY = point.y
            } else {
                if (!drawerMoved && drawerContent.contains(point.x, point.y)) {
                    pendingDrawerAction?.takeIf { it.first.contains(point.x, point.y) }?.second?.invoke()
                }
                scrollingDrawer = false
                draggingScrollThumb = false
                pendingDrawerAction = null
            }
            return
        }
        if (Gdx.input.justTouched()) {
            val point = touchPoint()
            if (!libraryOpen && !confirmClear && !confirmDelete && drawerContent.contains(point.x, point.y)) {
                scrollingDrawer = true
                draggingScrollThumb = point.x >= 844f && drawerMaxScroll() > 0f
                drawerTouchStartY = point.y
                drawerLastY = point.y
                drawerMoved = draggingScrollThumb
                pendingDrawerAction = if (draggingScrollThumb) {
                    null
                } else {
                    actions.asReversed().firstOrNull { it.first.contains(point.x, point.y) }
                }
                if (draggingScrollThumb) scrollDrawerToThumb(point.y)
                drawing = false
                return
            }
            val hit = actions.asReversed().firstOrNull { it.first.contains(point.x, point.y) }
            if (hit != null) {
                hit.second()
                drawing = false
                return
            }
            if (libraryOpen || confirmClear || confirmDelete) return
            drawing = gridCell(point.x, point.y) != null
            if (drawing) {
                remember()
                paintAt(point.x, point.y)
            }
        } else if (drawing && Gdx.input.isTouched) {
            val point = touchPoint()
            paintAt(point.x, point.y)
        } else if (!Gdx.input.isTouched) {
            drawing = false
        }
    }

    private fun scrollDrawerToThumb(y: Float) {
        val maxScroll = drawerMaxScroll()
        if (maxScroll <= 0f) return
        val trackHeight = 350f
        val thumbHeight = (
            trackHeight * drawerContent.height /
                (drawerContent.height + maxScroll)
            ).coerceAtLeast(45f)
        val position = (
            (y - 130f - thumbHeight / 2f) /
                (trackHeight - thumbHeight)
            ).coerceIn(0f, 1f)
        val offset = maxScroll * (1f - position)
        when (drawerTab) {
            DrawerTab.TOOLS -> toolsScroll = offset
            DrawerTab.BRICKS -> bricksScroll = offset
            DrawerTab.GENERATOR -> generatorScroll = offset
        }
    }

    private fun gridCell(x: Float, y: Float): Pair<Int, Int>? {
        val cellW = (GRID_RIGHT - GRID_LEFT - 10f * GRID_GAP) / 11f
        val col = ((x - GRID_LEFT) / (cellW + GRID_GAP)).toInt()
        val row = ((GRID_TOP - y) / GRID_PITCH_Y).toInt()
        return if (x in GRID_LEFT..GRID_RIGHT && y in (GRID_TOP - 9f * GRID_PITCH_Y)..GRID_TOP &&
            col in 0..10 && row in 0..8
        ) {
            row to col
        } else {
            null
        }
    }

    private fun paintAt(x: Float, y: Float) {
        val (row, col) = gridCell(x, y) ?: return
        when (tool) {
            Tool.PAINT -> {
                draft.pickups.removeAll { it.x == col + .5f && it.y == row + .5f }
                draft.cells[row][col] = VolleyBrickSeed(
                    selectedType,
                    selectedShape,
                    groupId = if (selectedType in setOf(BrickType.KEY_BRICK, BrickType.LOCKED, BrickType.SWITCH)) selectedGroup else 0,
                    atlasSpriteName = selectedSpriteName
                )
            }

            Tool.ERASE -> {
                draft.cells[row][col] = null
                draft.pickups.removeAll { it.x == col + .5f && it.y == row + .5f }
            }

            Tool.BALL -> if (draft.cells[row][col] == null && draft.pickups.none { it.x == col + .5f && it.y == row + .5f }) {
                draft.pickups += VolleyPickupSeed(col + .5f, row + .5f)
            }

            Tool.FILL -> {
                draft.cells.forEachIndexed { rowIndex, line ->
                    line.indices.forEach { index ->
                        if (line[index] == null && draft.pickups.none { it.x == index + .5f && it.y == rowIndex + .5f }) {
                            line[index] = VolleyBrickSeed(
                                selectedType,
                                selectedShape,
                                atlasSpriteName = selectedSpriteName
                            )
                        }
                    }
                }
                drawing = false
            }
        }
    }

    private fun rename() {
        Gdx.input.getTextInput(
            object : Input.TextInputListener {
                override fun input(text: String) {
                    remember()
                    draft.name = text.trim().take(40).ifBlank { "MY VOLLEY STAGE" }
                }
                override fun canceled() = Unit
            },
            "STAGE NAME",
            draft.name,
            "Name"
        )
    }

    private fun save() {
        draft.validationError()?.let {
            status = it
            return
        }
        try {
            repository.save(draft)
            status = "STAGE SAVED"
        } catch (error: Exception) {
            status = "SAVE FAILED: ${error.message ?: "STORAGE ERROR"}"
        }
    }

    private fun testStage() {
        draft.validationError()?.let {
            status = it
            return
        }
        game.playVolleyEditorTest(draft.duplicate())
    }

    private fun visualFor(art: VolleyWorldArt, type: BrickType, shape: VolleyPieceShape): VolleyPieceVisual = visualCache.getOrPut(type) { mutableMapOf() }.getOrPut(shape) {
        previewBrick.type = type
        previewBrick.health = type.maxHealth
        previewBrick.initialHealth = type.maxHealth
        previewBrick.volleyPieceShape = shape
        art.brickVisual(previewBrick)
    }
}
