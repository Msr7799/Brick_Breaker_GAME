/*
 * ملاحظات صيانة الملف:
 * المسار: app/src/main/java/com/example/brick_breaker_ball/LevelEditorScreen.kt
 * المؤلف: mohamed alromaihi
 * الدوال الموجودة: `render`، `drawGrid`، `handleGridInput`، `gridAtTouch`، `drawPalette`، `handlePaletteInput`، `drawProperties`، `handlePropertiesInput`، `hit`، `drawFiles`، `handleFilesInput`، `save`، `play`، `showValidation`، `drawValidation`، `drawConfirmation`، `handleClearConfirmation`، `handleDeleteConfirmation`، `loadInto`، `refreshFiles`، `requestText`، `input`، `canceled`، `hpLabel`، `panel`، `editorButton`
 */

package com.example.brick_breaker_ball

import com.badlogic.gdx.Gdx
import com.badlogic.gdx.Input
import com.badlogic.gdx.graphics.Color
import com.badlogic.gdx.math.Rectangle
import com.badlogic.gdx.utils.Align

private enum class EditorTab { GRID, PALETTE, PROPERTIES, FILES }

class LevelEditorScreen(game: BrickBreakerGame, internal val editor: LevelEditorState = LevelEditorState()) : ForgeScreen(game) {
    private var tab = EditorTab.GRID
    private var paletteSection = PaletteSection.BASIC
    private var clearConfirmation = false
    private var deleteConfirmation = false
    private var pointerWasDown = false
    private var validationLines = listOf<String>()
    private var savedLevels = game.customLevels.list()
    private var selectedFile = savedLevels.firstOrNull()?.customId

    /** ملاحظة صيانة: الدالة `render` تنفّذ العقد الموروث وتربط دورة حياة المكوّن بسلوك هذا الملف؛ راجع استدعاءاتها واختباراتها قبل تعديلها. */
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

        if (clearConfirmation) {
            handleClearConfirmation()
            return
        }
        if (deleteConfirmation) {
            handleDeleteConfirmation()
            return
        }
        tabs.firstOrNull { tapped(it.second) }?.let {
            tab = it.first
            return
        }
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

    /** ملاحظة صيانة: الدالة `drawGrid` ترسم العناصر المطلوبة مع الحفاظ على ترتيب طبقات العرض؛ راجع استدعاءاتها واختباراتها قبل تعديلها. */
    private fun drawGrid() {
        gridCells.clear()
        gridControls.clear()
        val cellW = 88f
        val cellH = 61f
        val gap = 5f
        val left = 34f
        val top = 1300f
        for (row in 0 until LevelEditorState.ROWS) {
            for (column in 0 until LevelEditorState.COLUMNS) {
                val x = left + column * (cellW + gap)
                val y = top - (row + 1) * (cellH + gap)
                val rect = Rectangle(x, y, cellW, cellH)
                val cell = editor.cells[row][column]
                batch.color = if (cell.type == null) Color(.03f, .1f, .17f, .82f) else Color.WHITE
                val region =
                    cell.type?.let { game.assets.gameplayAtlas.preview(it, editor.properties.world) } ?: game.assets.ui.findRegion("panel")
                batch.draw(region, x, y, cellW, cellH)
                batch.color = Color.WHITE
                if (cell.groupId > 0) game.assets.smallFont.draw(batch, cell.groupId.toString(), x + 3f, y + 25f, 25f, Align.center, false)
                gridCells += Triple(rect, row, column)
            }
        }
        val info = BrickInfoRepository.all.getValue(editor.brush)
        panel(32f, 690f, 836f, 82f)
        batch.draw(game.assets.gameplayAtlas.preview(editor.brush, editor.properties.world), 44f, 701f, 92f, 60f)
        game.assets.hudLabelFont.draw(
            batch,
            "${info.displayName}  HP ${hpLabel(editor.brush)}  GROUP ${editor.groupId}",
            150f,
            748f,
            690f,
            Align.left,
            false
        )
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

    /** ملاحظة صيانة: الدالة `handleGridInput` تعالج الحدث أو الطلب وتحدّث الحالة المرتبطة به؛ راجع استدعاءاتها واختباراتها قبل تعديلها. */
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

                else -> if (editor.tool ==
                    EditorTool.FILL
                ) {
                    gridAtTouch()?.let { editor.fill(it.second, it.third) }
                } else {
                    editor.beginStroke()
                }
            }
        }
        if (Gdx.input.isTouched && editor.tool != EditorTool.FILL) gridAtTouch()?.let { editor.paint(it.second, it.third) }
        if (pointerWasDown && !Gdx.input.isTouched) editor.endStroke()
        pointerWasDown = Gdx.input.isTouched
    }

    /** ملاحظة صيانة: الدالة `gridAtTouch` تنفّذ مسؤولية محلية يعتمد عليها هذا الجزء من اللعبة؛ راجع استدعاءاتها واختباراتها قبل تعديلها. */
    private fun gridAtTouch(): Triple<Rectangle, Int, Int>? {
        val point = touchPoint()
        return gridCells.firstOrNull { it.first.contains(point.x, point.y) }
    }

    private val paletteRects = mutableListOf<Pair<Rectangle, BrickType>>()
    private val sectionRects = mutableListOf<Pair<Rectangle, PaletteSection>>()

    /**
     * لوحة القراميد مهيأة للموبايل: بطاقات مضغوطة بعمودين، ولا يوجد أي زر عائم فوق المحتوى.
     * زر GRID العلوي هو طريق الرجوع، لذلك لا نكرر RETURN TO GRID داخل القائمة.
     */
    private fun drawPalette() {
        paletteRects.clear()
        sectionRects.clear()

        val sections = PaletteSection.entries
        val sectionGap = 8f
        val sectionX = 16f
        val sectionWidth = (868f - sectionGap * (sections.size - 1)) / sections.size
        sections.forEachIndexed { index, section ->
            val label = section.name.replace('_', '/')
            sectionRects += editorButton(
                label,
                sectionX + index * (sectionWidth + sectionGap),
                1290f,
                sectionWidth,
                62f,
                selected = section == paletteSection,
                fontScale = if (label.length >= 10) .78f else .90f,
            ) to section
        }

        val entries = BrickInfoRepository.all.filterValues { it.section == paletteSection }.entries.toList()
        val cardWidth = 402f
        val cardHeight = 112f
        val columnGap = 20f
        val rowGap = 10f
        val left = 28f
        val top = 1240f

        entries.forEachIndexed { index, entry ->
            val column = index % 2
            val row = index / 2
            val x = left + column * (cardWidth + columnGap)
            val y = top - cardHeight - row * (cardHeight + rowGap)
            val rect = Rectangle(x, y, cardWidth, cardHeight)

            // التحديد صار Border/Glow فقط بدل تعبئة البطاقة كاملة بالبرتقالي.
            if (editor.brush == entry.key) {
                game.assets.uiRenderer.drawGradientBorderPanel(
                    batch,
                    rect,
                    ForgeUiRenderer.GradientStyle.PRIMARY,
                    4f,
                )
            } else {
                panel(x, y, cardWidth, cardHeight, ForgeUiPalette.neutralDark)
            }

            batch.color = Color.WHITE
            batch.draw(
                game.assets.gameplayAtlas.preview(entry.key, editor.properties.world),
                x + 10f,
                y + 51f,
                92f,
                52f,
            )

            game.assets.hudLabelFont.draw(
                batch,
                entry.value.displayName,
                x + 114f,
                y + 94f,
                cardWidth - 126f,
                Align.left,
                false,
            )
            game.assets.smallFont.draw(
                batch,
                "HP ${hpLabel(entry.key)}",
                x + 114f,
                y + 61f,
                cardWidth - 126f,
                Align.left,
                false,
            )

            // في الـPalette نعرض وصفًا سريعًا فقط؛ التفاصيل الكاملة مكانها خصائص/دليل العنصر.
            game.assets.smallFont.color = ForgeUiPalette.textSecondary
            game.assets.smallFont.draw(
                batch,
                compactBrickDescription(entry.value.description),
                x + 10f,
                y + 27f,
                cardWidth - 20f,
                Align.left,
                false,
            )
            game.assets.smallFont.color = Color.WHITE

            paletteRects += rect to entry.key
        }
    }

    /** ملاحظة صيانة: الدالة `handlePaletteInput` تعالج اختيار التصنيف أو القرميدة. */
    private fun handlePaletteInput() {
        sectionRects.firstOrNull { tapped(it.first) }?.let {
            paletteSection = it.second
            return
        }
        paletteRects.firstOrNull { tapped(it.first) }?.let {
            editor.brush = it.second
            editor.tool = EditorTool.PAINT
            tab = EditorTab.GRID
            return
        }
    }

    /** وصف سطر واحد حتى لا تتحول بطاقة الاختيار إلى فقرة طويلة متداخلة. */
    private fun compactBrickDescription(description: String): String {
        val firstSentence = description.substringBefore('.').trim().ifBlank { description.trim() }
        val normalized = firstSentence.replace(Regex("\\s+"), " ")
        return if (normalized.length <= 52) {
            if (description.trim().contains('.') && !normalized.endsWith('.')) "$normalized." else normalized
        } else {
            normalized.take(49).trimEnd() + "..."
        }
    }

    private val propertyRects = mutableMapOf<String, Rectangle>()

    /** ملاحظة صيانة: الدالة `drawProperties` ترسم العناصر المطلوبة مع الحفاظ على ترتيب طبقات العرض؛ راجع استدعاءاتها واختباراتها قبل تعديلها. */
    private fun drawProperties() {
        propertyRects.clear()
        val p = editor.properties
        val entries = listOf(
            "NAME" to p.name, "ID" to p.customId, "WORLD" to p.world.toString(),
            "SPEED" to p.ballSpeed.toInt().toString(), "LIVES" to p.lives.toString(),
            "DROP RATE" to "${(p.dropRate * 100).toInt()}%", "DEATH RAIL" to if (p.deathRailEnabled) "ON" else "OFF",
            "3-STAR" to p.threeStarScore.toString(), "BOSS HP" to p.bossHealth.toString(),
            "BOSS MOD" to if ("BOSS" in p.modifiers) "ON" else "OFF"
        )
        entries.forEachIndexed { index, (label, value) ->
            val column = index % 2
            val row = index / 2
            val x = 45f + column * 415f
            val y = 1190f - row * 145f
            game.assets.smallFont.draw(batch, label, x, y + 90f, 380f, Align.left, false)
            propertyRects[label] = editorButton(value.take(24), x, y, 380f, 72f)
        }
        game.assets.smallFont.draw(
            batch,
            "Tap NAME or ID for keyboard. Other values cycle through safe limits.",
            35f,
            430f,
            830f,
            Align.center,
            true
        )
    }

    /** ملاحظة صيانة: الدالة `handlePropertiesInput` تعالج الحدث أو الطلب وتحدّث الحالة المرتبطة به؛ راجع استدعاءاتها واختباراتها قبل تعديلها. */
    private fun handlePropertiesInput() {
        /** ملاحظة صيانة: الدالة `hit` تعالج الحدث أو الطلب وتحدّث الحالة المرتبطة به؛ راجع استدعاءاتها واختباراتها قبل تعديلها. */
        fun hit(name: String) = propertyRects[name]?.let(::tapped) == true
        when {
            hit("NAME") -> requestText("Level name", editor.properties.name) { text -> editor.mutateProperties { it.name = text.take(32) } }

            hit("ID") -> requestText("Custom identifier", editor.properties.customId) { text ->
                editor.mutateProperties {
                    it.customId =
                        text.take(48)
                }
            }

            hit("WORLD") -> editor.mutateProperties { it.world = it.world % 8 + 1 }

            hit("SPEED") -> editor.mutateProperties { it.ballSpeed = if (it.ballSpeed >= 900f) 520f else it.ballSpeed + 80f }

            hit("LIVES") -> editor.mutateProperties { it.lives = it.lives % 9 + 1 }

            hit("DROP RATE") -> editor.mutateProperties { it.dropRate = if (it.dropRate >= .5f) 0f else it.dropRate + .1f }

            hit("DEATH RAIL") -> editor.mutateProperties { it.deathRailEnabled = !it.deathRailEnabled }

            hit("3-STAR") -> editor.mutateProperties {
                it.threeStarScore =
                    if (it.threeStarScore >= 20000) 1000 else it.threeStarScore + 1000
            }

            hit("BOSS HP") -> editor.mutateProperties { it.bossHealth = if (it.bossHealth >= 20) 1 else it.bossHealth + 1 }

            hit("BOSS MOD") -> editor.mutateProperties { if (!it.modifiers.add("BOSS")) it.modifiers.remove("BOSS") }
        }
    }

    private val fileRects = mutableMapOf<String, Rectangle>()
    private val fileRows = mutableListOf<Pair<Rectangle, String>>()

    /** ملاحظة صيانة: الدالة `drawFiles` ترسم العناصر المطلوبة مع الحفاظ على ترتيب طبقات العرض؛ راجع استدعاءاتها واختباراتها قبل تعديلها. */
    private fun drawFiles() {
        fileRects.clear()
        fileRows.clear()
        game.assets.smallFont.draw(batch, "CUSTOM LEVEL FILES  •  VERSIONED JSON", 0f, 1295f, 900f, Align.center, false)
        savedLevels.take(6).forEachIndexed { index, level ->
            val id = level.customId ?: return@forEachIndexed
            val y = 1165f - index * 112f
            val rect = editorButton("${level.name.take(18)}  [$id]", 65f, y, 770f, 82f, selectedFile == id)
            fileRows += rect to id
        }
        fileRects["SAVE AS"] = editorButton("SAVE AS", 35f, 380f, 190f, 72f)
        fileRects["LOAD"] = editorButton("LOAD", 245f, 380f, 190f, 72f)
        fileRects["DUPLICATE"] = editorButton("DUPLICATE", 455f, 380f, 200f, 72f)
        fileRects["DELETE"] = editorButton("DELETE", 675f, 380f, 190f, 72f, destructive = true)
    }

    /** ملاحظة صيانة: الدالة `handleFilesInput` تعالج الحدث أو الطلب وتحدّث الحالة المرتبطة به؛ راجع استدعاءاتها واختباراتها قبل تعديلها. */
    private fun handleFilesInput() {
        fileRows.firstOrNull { tapped(it.first) }?.let {
            selectedFile = it.second
            return
        }
        when {
            fileRects["SAVE AS"]?.let(::tapped) == true -> save(true)

            fileRects["LOAD"]?.let(::tapped) == true -> selectedFile?.let { id ->
                game.customLevels.load(id)?.let { loadInto(LevelEditorState.from(it)) }
            }

            fileRects["DUPLICATE"]?.let(::tapped) == true -> selectedFile?.let { id ->
                game.customLevels.duplicate(id)?.let { duplicated -> refreshFiles(duplicated.customId) }
            }

            fileRects["DELETE"]?.let(::tapped) == true && selectedFile != null -> deleteConfirmation = true
        }
    }

    /** ملاحظة صيانة: الدالة `save` تحفظ البيانات أو تضيفها إلى الحالة المعتمدة في النظام؛ راجع استدعاءاتها واختباراتها قبل تعديلها. */
    private fun save(saveAs: Boolean) {
        val level = editor.toLevelDefinition()
        val result = LevelValidator.validate(level)
        showValidation(result)
        if (!result.valid) return
        val saved = if (saveAs) game.customLevels.saveAs(level, level.customId ?: level.name) else game.customLevels.save(level)
        editor.mutateProperties { it.customId = saved.customId ?: it.customId }
        editor.markSaved()
        refreshFiles(saved.customId)
    }

    /** ملاحظة صيانة: الدالة `play` تنفّذ انتقالًا أو تعرض التدفق المطلوب للمستخدم؛ راجع استدعاءاتها واختباراتها قبل تعديلها. */
    private fun play() {
        val result = LevelValidator.validate(editor.toLevelDefinition())
        showValidation(result)
        if (result.valid) game.playCustom(editor)
    }

    /** ملاحظة صيانة: الدالة `showValidation` تنفّذ انتقالًا أو تعرض التدفق المطلوب للمستخدم؛ راجع استدعاءاتها واختباراتها قبل تعديلها. */
    private fun showValidation(result: ValidationResult) {
        validationLines = if (result.valid) listOf("READY TO PLAY") + result.warnings else result.errors
    }

    /** ملاحظة صيانة: الدالة `drawValidation` ترسم العناصر المطلوبة مع الحفاظ على ترتيب طبقات العرض؛ راجع استدعاءاتها واختباراتها قبل تعديلها. */
    private fun drawValidation() {
        if (validationLines.isEmpty()) return
        panel(
            35f,
            215f,
            830f,
            95f,
            if (validationLines.first() ==
                "READY TO PLAY"
            ) {
                ForgeUiPalette.success
            } else {
                ForgeUiPalette.crimson
            }
        )
        game.assets.smallFont.draw(batch, validationLines.take(2).joinToString(" • "), 52f, 275f, 796f, Align.center, true)
    }

    /** ملاحظة صيانة: الدالة `drawConfirmation` ترسم العناصر المطلوبة مع الحفاظ على ترتيب طبقات العرض؛ راجع استدعاءاتها واختباراتها قبل تعديلها. */
    private fun drawConfirmation(message: String, confirm: String, cancel: String) {
        panel(100f, 520f, 700f, 430f, ForgeUiPalette.neutralDark)
        game.assets.bodyFont.draw(batch, message, 130f, 855f, 640f, Align.center, true)
        fileRects["CONFIRM"] = editorButton(confirm, 155f, 630f, 270f, 86f, destructive = true)
        fileRects["CANCEL"] = editorButton(cancel, 475f, 630f, 270f, 86f)
    }

    /** ملاحظة صيانة: الدالة `handleClearConfirmation` تعالج الحدث أو الطلب وتحدّث الحالة المرتبطة به؛ راجع استدعاءاتها واختباراتها قبل تعديلها. */
    private fun handleClearConfirmation() {
        when {
            fileRects["CONFIRM"]?.let(::tapped) == true -> {
                editor.clear()
                clearConfirmation =
                    false
            }

            fileRects["CANCEL"]?.let(::tapped) == true -> clearConfirmation = false
        }
    }

    /** ملاحظة صيانة: الدالة `handleDeleteConfirmation` تعالج الحدث أو الطلب وتحدّث الحالة المرتبطة به؛ راجع استدعاءاتها واختباراتها قبل تعديلها. */
    private fun handleDeleteConfirmation() {
        when {
            fileRects["CONFIRM"]?.let(::tapped) == true -> {
                selectedFile?.let(game.customLevels::delete)
                deleteConfirmation =
                    false
                refreshFiles(null)
            }

            fileRects["CANCEL"]?.let(::tapped) == true -> deleteConfirmation = false
        }
    }

    /** ملاحظة صيانة: الدالة `loadInto` تقرأ البيانات المطلوبة أو تسترجعها بصيغة مناسبة للاستخدام؛ راجع استدعاءاتها واختباراتها قبل تعديلها. */
    private fun loadInto(loaded: LevelEditorState) {
        game.setScreen(LevelEditorScreen(game, loaded))
    }

    /** ملاحظة صيانة: الدالة `refreshFiles` تنفّذ مسؤولية محلية يعتمد عليها هذا الجزء من اللعبة؛ راجع استدعاءاتها واختباراتها قبل تعديلها. */
    private fun refreshFiles(select: String?) {
        savedLevels = game.customLevels.list()
        selectedFile =
            select ?: savedLevels.firstOrNull()?.customId
    }

    /** ملاحظة صيانة: الدالة `requestText` تنفّذ مسؤولية محلية يعتمد عليها هذا الجزء من اللعبة؛ راجع استدعاءاتها واختباراتها قبل تعديلها. */
    private fun requestText(title: String, current: String, accept: (String) -> Unit) {
        Gdx.input.getTextInput(
            object : Input.TextInputListener {
                /** ملاحظة صيانة: الدالة `input` تنفّذ العقد الموروث وتربط دورة حياة المكوّن بسلوك هذا الملف؛ راجع استدعاءاتها واختباراتها قبل تعديلها. */
                override fun input(text: String) {
                    if (text.isNotBlank()) accept(text.trim())
                }

                /** ملاحظة صيانة: الدالة `canceled` تنفّذ العقد الموروث وتربط دورة حياة المكوّن بسلوك هذا الملف؛ راجع استدعاءاتها واختباراتها قبل تعديلها. */
                override fun canceled() = Unit
            },
            title,
            current,
            ""
        )
    }

    /** ملاحظة صيانة: الدالة `hpLabel` تنفّذ مسؤولية محلية يعتمد عليها هذا الجزء من اللعبة؛ راجع استدعاءاتها واختباراتها قبل تعديلها. */
    private fun hpLabel(type: BrickType) = if (!type.breakable) {
        "∞"
    } else if (type ==
        BrickType.BOSS_CORE
    ) {
        editor.properties.bossHealth.toString()
    } else {
        type.maxHealth.toString()
    }

    /** ملاحظة صيانة: الدالة `panel` تنفّذ مسؤولية محلية يعتمد عليها هذا الجزء من اللعبة؛ راجع استدعاءاتها واختباراتها قبل تعديلها. */
    private fun panel(x: Float, y: Float, w: Float, h: Float, tint: Color = ForgeUiPalette.neutralDark) {
        val style = when (tint) {
            ForgeUiPalette.primary, ForgeUiPalette.primaryLight, ForgeUiPalette.primaryDark -> ForgeUiRenderer.GradientStyle.PRIMARY
            ForgeUiPalette.success, ForgeUiPalette.successLight, ForgeUiPalette.successDark -> ForgeUiRenderer.GradientStyle.SUCCESS
            ForgeUiPalette.crimson, ForgeUiPalette.crimsonLight, ForgeUiPalette.crimsonDark, ForgeUiPalette.danger -> ForgeUiRenderer.GradientStyle.CRIMSON
            else -> ForgeUiRenderer.GradientStyle.NEUTRAL
        }
        game.assets.uiRenderer.drawGradientPanel(batch, Rectangle(x, y, w, h), style)
    }

    /** ملاحظة صيانة: الدالة `editorButton` تنفّذ مسؤولية محلية يعتمد عليها هذا الجزء من اللعبة؛ راجع استدعاءاتها واختباراتها قبل تعديلها. */
    private fun editorButton(
        text: String,
        x: Float,
        y: Float,
        w: Float,
        h: Float,
        selected: Boolean = false,
        primary: Boolean = false,
        destructive: Boolean = false,
        fontScale: Float = 1f,
    ): Rectangle {
        val rect = Rectangle(x, y, w, h)
        val style = when {
            destructive -> ForgeUiRenderer.GradientStyle.CRIMSON
            selected || primary -> ForgeUiRenderer.GradientStyle.PRIMARY
            else -> ForgeUiRenderer.GradientStyle.NEUTRAL
        }
        game.assets.uiRenderer.drawGradientButton(batch, rect, style)
        game.assets.hudLabelFont.color = ForgeUiPalette.textPrimary
        val oldScaleX = game.assets.hudLabelFont.data.scaleX
        val oldScaleY = game.assets.hudLabelFont.data.scaleY
        game.assets.hudLabelFont.data.setScale(oldScaleX * fontScale, oldScaleY * fontScale)
        game.assets.hudLabelFont.draw(batch, text, x + 4f, y + h * .64f, w - 8f, Align.center, false)
        game.assets.hudLabelFont.data.setScale(oldScaleX, oldScaleY)
        game.assets.hudLabelFont.color = Color.WHITE
        return rect
    }
}
