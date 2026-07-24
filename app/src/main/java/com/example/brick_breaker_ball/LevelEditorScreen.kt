package com.example.brick_breaker_ball

import com.badlogic.gdx.Gdx
import com.badlogic.gdx.Input
import com.badlogic.gdx.graphics.Color
import com.badlogic.gdx.math.Rectangle
import com.badlogic.gdx.utils.Align

private enum class EditorTab { GRID, PALETTE, PROPERTIES, FILES }

class LevelEditorScreen(
    game: BrickBreakerGame,
    internal val editor: LevelEditorState = LevelEditorState(),
) : ForgeScreen(game) {
    private var tab = EditorTab.GRID
    private var paletteSection = PaletteSection.BASIC
    private var clearConfirmation = false
    private var deleteConfirmation = false
    private var pointerWasDown = false
    private var validationLines = listOf<String>()
    private var savedLevels = game.customLevels.list()
    private var selectedFile = savedLevels.firstOrNull()?.customId

    override fun render(delta: Float) {
        begin(editor.properties.world)
        title("LEVEL EDITOR", 1540f)
        game.assets.smallFont.draw(batch, "9 x 8  •  ${if (editor.dirty) "UNSAVED" else "SAVED"}", 0f, 1472f, 900f, Align.center, false)
        val tabs = EditorTab.entries.mapIndexed { index, value ->
            value to editorButton(value.name, 22f + index * 218f, 1375f, 202f, 62f, value == tab)
        }
        when (tab) {
            EditorTab.GRID -> drawGrid()
            EditorTab.PALETTE -> drawPalette()
            EditorTab.PROPERTIES -> drawProperties()
            EditorTab.FILES -> drawFiles()
        }
        drawValidation()
        val play = editorButton("PLAY", 30f, 105f, 260f, 82f, primary = true)
        val save = editorButton("SAVE", 320f, 105f, 260f, 82f)
        val back = editorButton("BACK", 610f, 105f, 260f, 82f)
        if (clearConfirmation) drawConfirmation("CLEAR ALL 72 CELLS?", "CLEAR", "CANCEL")
        if (deleteConfirmation) drawConfirmation("DELETE SELECTED LEVEL?", "DELETE", "CANCEL")
        end()

        if (clearConfirmation) { handleClearConfirmation(); return }
        if (deleteConfirmation) { handleDeleteConfirmation(); return }
        tabs.firstOrNull { tapped(it.second) }?.let { tab = it.first; return }
        when (tab) {
            EditorTab.GRID -> handleGridInput()
            EditorTab.PALETTE -> handlePaletteInput()
            EditorTab.PROPERTIES -> handlePropertiesInput()
            EditorTab.FILES -> handleFilesInput()
        }
        when {
            tapped(play) -> play()
            tapped(save) -> save(false)
            tapped(back) || Gdx.input.isKeyJustPressed(Input.Keys.BACK) -> game.openMenu()
        }
    }

    private val gridCells = mutableListOf<Triple<Rectangle, Int, Int>>()
    private val gridControls = mutableMapOf<String, Rectangle>()
    private fun drawGrid() {
        gridCells.clear(); gridControls.clear()
        val cellW = 88f; val cellH = 61f; val gap = 5f; val left = 34f; val top = 1300f
        for (row in 0 until LevelEditorState.ROWS) for (column in 0 until LevelEditorState.COLUMNS) {
            val x = left + column * (cellW + gap); val y = top - (row + 1) * (cellH + gap)
            val rect = Rectangle(x, y, cellW, cellH); val cell = editor.cells[row][column]
            batch.color = if (cell.type == null) Color(.03f, .1f, .17f, .82f) else Color.WHITE
            val region = cell.type?.let { game.assets.gameplayAtlas.preview(it, editor.properties.world) } ?: game.assets.ui.findRegion("panel")
            batch.draw(region, x, y, cellW, cellH); batch.color = Color.WHITE
            if (cell.groupId > 0) game.assets.smallFont.draw(batch, cell.groupId.toString(), x + 3f, y + 25f, 25f, Align.center, false)
            gridCells += Triple(rect, row, column)
        }
        val info = BrickInfoRepository.all.getValue(editor.brush)
        panel(32f, 690f, 836f, 82f)
        batch.draw(game.assets.gameplayAtlas.preview(editor.brush, editor.properties.world), 44f, 701f, 92f, 60f)
        game.assets.hudLabelFont.draw(batch, "${info.displayName}  HP ${hpLabel(editor.brush)}  GROUP ${editor.groupId}", 150f, 748f, 690f, Align.left, false)
        game.assets.smallFont.draw(batch, info.description, 150f, 712f, 690f, Align.left, false)
        listOf("PAINT", "ERASE", "FILL", "UNDO", "REDO").forEachIndexed { i, label ->
            val selected = (label == editor.tool.name)
            gridControls[label] = editorButton(label, 25f + i * 174f, 590f, 154f, 70f, selected)
        }
        gridControls["GROUP"] = editorButton("GROUP ${editor.groupId}", 25f, 495f, 200f, 70f)
        gridControls["PALETTE"] = editorButton("BRICKS", 245f, 495f, 200f, 70f)
        gridControls["CLEAR"] = editorButton("CLEAR", 465f, 495f, 200f, 70f, destructive = true)
        gridControls["PROPS"] = editorButton("PROPERTIES", 685f, 495f, 190f, 70f)
        game.assets.smallFont.draw(batch, "UNDO ${editor.undoCount}/50    REDO ${editor.redoCount}", 0f, 455f, 900f, Align.center, false)
    }

    private fun handleGridInput() {
        if (Gdx.input.justTouched()) {
            when {
                tapped(gridControls.getValue("PAINT")) -> editor.tool = EditorTool.PAINT
                tapped(gridControls.getValue("ERASE")) -> editor.tool = EditorTool.ERASE
                tapped(gridControls.getValue("FILL")) -> editor.tool = EditorTool.FILL
                tapped(gridControls.getValue("UNDO")) -> editor.undo()
                tapped(gridControls.getValue("REDO")) -> editor.redo()
                tapped(gridControls.getValue("GROUP")) -> editor.groupId = (editor.groupId + 1) % 5
                tapped(gridControls.getValue("PALETTE")) -> tab = EditorTab.PALETTE
                tapped(gridControls.getValue("CLEAR")) -> clearConfirmation = true
                tapped(gridControls.getValue("PROPS")) -> tab = EditorTab.PROPERTIES
                else -> if (editor.tool == EditorTool.FILL) gridAtTouch()?.let { editor.fill(it.second, it.third) } else editor.beginStroke()
            }
        }
        if (Gdx.input.isTouched && editor.tool != EditorTool.FILL) gridAtTouch()?.let { editor.paint(it.second, it.third) }
        if (pointerWasDown && !Gdx.input.isTouched) editor.endStroke()
        pointerWasDown = Gdx.input.isTouched
    }

    private fun gridAtTouch(): Triple<Rectangle, Int, Int>? {
        val point = touchPoint(); return gridCells.firstOrNull { it.first.contains(point.x, point.y) }
    }

    private val paletteRects = mutableListOf<Pair<Rectangle, BrickType>>()
    private val sectionRects = mutableListOf<Pair<Rectangle, PaletteSection>>()
    private fun drawPalette() {
        paletteRects.clear(); sectionRects.clear()
        PaletteSection.entries.forEachIndexed { index, section ->
            sectionRects += editorButton(section.name.replace('_', '/'), 15f + index * 176f, 1290f, 166f, 62f, section == paletteSection) to section
        }
        BrickInfoRepository.all.filterValues { it.section == paletteSection }.entries.forEachIndexed { index, entry ->
            val column = index % 2; val row = index / 2; val x = 35f + column * 420f; val y = 1120f - row * 170f
            panel(x, y, 390f, 145f, if (editor.brush == entry.key) Color(.05f, .35f, .48f, .96f) else Color(.02f, .08f, .14f, .94f))
            batch.draw(game.assets.gameplayAtlas.preview(entry.key, editor.properties.world), x + 12f, y + 72f, 110f, 62f)
            game.assets.hudLabelFont.draw(batch, entry.value.displayName, x + 135f, y + 122f, 240f, Align.left, false)
            game.assets.smallFont.draw(batch, "HP ${hpLabel(entry.key)}", x + 135f, y + 82f, 240f, Align.left, false)
            game.assets.smallFont.draw(batch, entry.value.description, x + 12f, y + 48f, 365f, Align.left, true)
            paletteRects += Rectangle(x, y, 390f, 145f) to entry.key
        }
        editorButton("RETURN TO GRID", 260f, 300f, 380f, 78f, primary = true).also { gridControls["RETURN"] = it }
    }

    private fun handlePaletteInput() {
        sectionRects.firstOrNull { tapped(it.first) }?.let { paletteSection = it.second; return }
        paletteRects.firstOrNull { tapped(it.first) }?.let { editor.brush = it.second; editor.tool = EditorTool.PAINT; tab = EditorTab.GRID; return }
        if (gridControls["RETURN"]?.let(::tapped) == true) tab = EditorTab.GRID
    }

    private val propertyRects = mutableMapOf<String, Rectangle>()
    private fun drawProperties() {
        propertyRects.clear(); val p = editor.properties
        val entries = listOf(
            "NAME" to p.name, "ID" to p.customId, "WORLD" to p.world.toString(),
            "SPEED" to p.ballSpeed.toInt().toString(), "LIVES" to p.lives.toString(),
            "DROP RATE" to "${(p.dropRate * 100).toInt()}%", "DEATH RAIL" to if (p.deathRailEnabled) "ON" else "OFF",
            "3-STAR" to p.threeStarScore.toString(), "BOSS HP" to p.bossHealth.toString(),
            "BOSS MOD" to if ("BOSS" in p.modifiers) "ON" else "OFF",
        )
        entries.forEachIndexed { index, (label, value) ->
            val column = index % 2; val row = index / 2; val x = 45f + column * 415f; val y = 1190f - row * 145f
            game.assets.smallFont.draw(batch, label, x, y + 90f, 380f, Align.left, false)
            propertyRects[label] = editorButton(value.take(24), x, y, 380f, 72f)
        }
        game.assets.smallFont.draw(batch, "Tap NAME or ID for keyboard. Other values cycle through safe limits.", 35f, 430f, 830f, Align.center, true)
    }

    private fun handlePropertiesInput() {
        fun hit(name: String) = propertyRects[name]?.let(::tapped) == true
        when {
            hit("NAME") -> requestText("Level name", editor.properties.name) { text -> editor.mutateProperties { it.name = text.take(32) } }
            hit("ID") -> requestText("Custom identifier", editor.properties.customId) { text -> editor.mutateProperties { it.customId = text.take(48) } }
            hit("WORLD") -> editor.mutateProperties { it.world = it.world % 8 + 1 }
            hit("SPEED") -> editor.mutateProperties { it.ballSpeed = if (it.ballSpeed >= 900f) 520f else it.ballSpeed + 80f }
            hit("LIVES") -> editor.mutateProperties { it.lives = it.lives % 9 + 1 }
            hit("DROP RATE") -> editor.mutateProperties { it.dropRate = if (it.dropRate >= .5f) 0f else it.dropRate + .1f }
            hit("DEATH RAIL") -> editor.mutateProperties { it.deathRailEnabled = !it.deathRailEnabled }
            hit("3-STAR") -> editor.mutateProperties { it.threeStarScore = if (it.threeStarScore >= 20000) 1000 else it.threeStarScore + 1000 }
            hit("BOSS HP") -> editor.mutateProperties { it.bossHealth = if (it.bossHealth >= 20) 1 else it.bossHealth + 1 }
            hit("BOSS MOD") -> editor.mutateProperties { if (!it.modifiers.add("BOSS")) it.modifiers.remove("BOSS") }
        }
    }

    private val fileRects = mutableMapOf<String, Rectangle>()
    private val fileRows = mutableListOf<Pair<Rectangle, String>>()
    private fun drawFiles() {
        fileRects.clear(); fileRows.clear()
        game.assets.smallFont.draw(batch, "CUSTOM LEVEL FILES  •  VERSIONED JSON", 0f, 1295f, 900f, Align.center, false)
        savedLevels.take(6).forEachIndexed { index, level ->
            val id = level.customId ?: return@forEachIndexed; val y = 1165f - index * 112f
            val rect = editorButton("${level.name.take(18)}  [$id]", 65f, y, 770f, 82f, selectedFile == id)
            fileRows += rect to id
        }
        fileRects["SAVE AS"] = editorButton("SAVE AS", 35f, 380f, 190f, 72f)
        fileRects["LOAD"] = editorButton("LOAD", 245f, 380f, 190f, 72f)
        fileRects["DUPLICATE"] = editorButton("DUPLICATE", 455f, 380f, 200f, 72f)
        fileRects["DELETE"] = editorButton("DELETE", 675f, 380f, 190f, 72f, destructive = true)
    }

    private fun handleFilesInput() {
        fileRows.firstOrNull { tapped(it.first) }?.let { selectedFile = it.second; return }
        when {
            fileRects["SAVE AS"]?.let(::tapped) == true -> save(true)
            fileRects["LOAD"]?.let(::tapped) == true -> selectedFile?.let { id -> game.customLevels.load(id)?.let { loadInto(LevelEditorState.from(it)) } }
            fileRects["DUPLICATE"]?.let(::tapped) == true -> selectedFile?.let { id -> game.customLevels.duplicate(id)?.let { duplicated -> refreshFiles(duplicated.customId) } }
            fileRects["DELETE"]?.let(::tapped) == true && selectedFile != null -> deleteConfirmation = true
        }
    }

    private fun save(saveAs: Boolean) {
        val level = editor.toLevelDefinition(); val result = LevelValidator.validate(level); showValidation(result)
        if (!result.valid) return
        val saved = if (saveAs) game.customLevels.saveAs(level, level.customId ?: level.name) else game.customLevels.save(level)
        editor.mutateProperties { it.customId = saved.customId ?: it.customId }; editor.markSaved(); refreshFiles(saved.customId)
    }

    private fun play() {
        val result = LevelValidator.validate(editor.toLevelDefinition()); showValidation(result)
        if (result.valid) game.playCustom(editor)
    }

    private fun showValidation(result: ValidationResult) {
        validationLines = if (result.valid) listOf("READY TO PLAY") + result.warnings else result.errors
    }

    private fun drawValidation() {
        if (validationLines.isEmpty()) return
        panel(35f, 215f, 830f, 95f, if (validationLines.first() == "READY TO PLAY") Color(.02f, .24f, .2f, .94f) else Color(.34f, .045f, .07f, .96f))
        game.assets.smallFont.draw(batch, validationLines.take(2).joinToString(" • "), 52f, 275f, 796f, Align.center, true)
    }

    private fun drawConfirmation(message: String, confirm: String, cancel: String) {
        panel(100f, 520f, 700f, 430f, Color(.01f, .03f, .08f, .99f))
        game.assets.bodyFont.draw(batch, message, 130f, 855f, 640f, Align.center, true)
        fileRects["CONFIRM"] = editorButton(confirm, 155f, 630f, 270f, 86f, destructive = true)
        fileRects["CANCEL"] = editorButton(cancel, 475f, 630f, 270f, 86f)
    }
    private fun handleClearConfirmation() { when { fileRects["CONFIRM"]?.let(::tapped) == true -> { editor.clear(); clearConfirmation = false }; fileRects["CANCEL"]?.let(::tapped) == true -> clearConfirmation = false } }
    private fun handleDeleteConfirmation() { when { fileRects["CONFIRM"]?.let(::tapped) == true -> { selectedFile?.let(game.customLevels::delete); deleteConfirmation = false; refreshFiles(null) }; fileRects["CANCEL"]?.let(::tapped) == true -> deleteConfirmation = false } }

    private fun loadInto(loaded: LevelEditorState) { game.setScreen(LevelEditorScreen(game, loaded)) }
    private fun refreshFiles(select: String?) { savedLevels = game.customLevels.list(); selectedFile = select ?: savedLevels.firstOrNull()?.customId }
    private fun requestText(title: String, current: String, accept: (String) -> Unit) {
        Gdx.input.getTextInput(object : Input.TextInputListener {
            override fun input(text: String) { if (text.isNotBlank()) accept(text.trim()) }
            override fun canceled() = Unit
        }, title, current, "")
    }
    private fun hpLabel(type: BrickType) = if (!type.breakable) "∞" else if (type == BrickType.BOSS_CORE) editor.properties.bossHealth.toString() else type.maxHealth.toString()
    private fun panel(x: Float, y: Float, w: Float, h: Float, tint: Color = Color(.02f, .08f, .14f, .94f)) { batch.color = tint; batch.draw(game.assets.ui.findRegion("panel"), x, y, w, h); batch.color = Color.WHITE }
    private fun editorButton(text: String, x: Float, y: Float, w: Float, h: Float, selected: Boolean = false, primary: Boolean = false, destructive: Boolean = false): Rectangle {
        batch.color = when { destructive -> Color(.95f, .22f, .28f, .96f); selected || primary -> Color(.14f, .95f, 1f, 1f); else -> Color(.34f, .7f, .88f, .9f) }
        batch.draw(game.assets.ui.findRegion("button_primary"), x, y, w, h); batch.color = Color.WHITE
        game.assets.hudLabelFont.draw(batch, text, x + 4f, y + h * .64f, w - 8f, Align.center, false)
        return Rectangle(x, y, w, h)
    }
}
