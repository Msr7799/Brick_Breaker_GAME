package com.example.brick_breaker_ball

import com.badlogic.gdx.Gdx
import com.badlogic.gdx.Input
import com.badlogic.gdx.InputAdapter
import com.badlogic.gdx.InputMultiplexer
import com.badlogic.gdx.InputProcessor
import com.badlogic.gdx.graphics.GL20
import com.badlogic.gdx.graphics.Pixmap
import com.badlogic.gdx.graphics.Color
import com.badlogic.gdx.graphics.Texture
import com.badlogic.gdx.graphics.g2d.GlyphLayout
import com.badlogic.gdx.graphics.g2d.TextureRegion
import com.badlogic.gdx.math.Rectangle
import com.badlogic.gdx.math.Vector3
import com.badlogic.gdx.utils.Align
import kotlin.math.abs
import kotlin.math.exp
import kotlin.math.max
import kotlin.math.min

private sealed interface RuleBlock {

    data class Heading(
        val text: String,
        val level: Int
    ) : RuleBlock

    data class Paragraph(
        val text: String
    ) : RuleBlock

    data class Image(
        val path: String,
        val alt: String
    ) : RuleBlock

    data object Spacer : RuleBlock
}

class RulesScreen(
    game: BrickBreakerGame
) : ForgeScreen(game) {

    private val blocks: List<RuleBlock> = loadBlocks()

    private val images =
        mutableMapOf<String, Texture>()

    /*
     * 1x1 texture used to draw the orange background for ==highlighted== text.
     * It is created lazily the first time a highlight is actually needed.
     */
    private var highlightPixel: Texture? = null

    /*
     * Scroll:
     *
     * 0 = أعلى الصفحة
     * maxScroll = أسفل الصفحة
     */
    /** Current rendered scroll position. */
    private var scrollOffset = 0f

    /** Destination used by mouse-wheel, drag and fling scrolling. */
    private var targetScrollOffset = 0f

    private var maxScroll = 0f

    private var touchActive = false
    private var touchMoved = false
    private var lastTouchY = 0f
    private var touchVelocityY = 0f

    private var previousInputProcessor: InputProcessor? = null

    /**
     * Mouse-wheel input is event based in LibGDX, so keep it separate from the
     * touch polling used by this screen. Positive amountY means wheel-down and
     * therefore moves farther down the rules page.
     */
    private val mouseWheelProcessor = object : InputAdapter() {
        override fun scrolled(amountX: Float, amountY: Float): Boolean {
            if (maxScroll <= 0f || amountY == 0f) return false

            targetScrollOffset =
                (targetScrollOffset + amountY * MOUSE_WHEEL_STEP)
                    .coerceIn(0f, maxScroll)

            return true
        }
    }

    override fun render(delta: Float) {
        updateSmoothScroll(delta)

        begin("world_4_zerog")

        title(
            "GAME RULES",
            1515f
        )

        val back =
            button(
                "BACK",
                650f,
                1430f,
                185f,
                72f
            )

        drawRules()

        end()

        handleScroll(delta)

        if (
            Gdx.input.isKeyJustPressed(Input.Keys.BACK) ||
            tapped(back)
        ) {
            game.openMenu()
        }
    }

    override fun show() {
        super.show()

        previousInputProcessor = Gdx.input.inputProcessor

        Gdx.input.inputProcessor =
            previousInputProcessor?.let { existing ->
                InputMultiplexer(mouseWheelProcessor, existing)
            } ?: mouseWheelProcessor
    }

    override fun hide() {
        /*
         * Restore the processor that existed before this screen added wheel
         * support, so other screens keep their own input behaviour unchanged.
         */
        Gdx.input.inputProcessor = previousInputProcessor
        previousInputProcessor = null

        super.hide()
    }

    private fun updateSmoothScroll(delta: Float) {
        targetScrollOffset =
            targetScrollOffset.coerceIn(
                0f,
                maxScroll
            )

        val safeDelta = delta.coerceIn(0f, 1f / 15f)

        /*
         * Frame-rate independent easing:
         * a quick response at the start, then a soft arrival at the target.
         */
        val blend =
            1f -
                exp(
                    -SCROLL_SMOOTHING * safeDelta
                )

        scrollOffset +=
            (targetScrollOffset - scrollOffset) * blend

        if (abs(targetScrollOffset - scrollOffset) < 0.15f) {
            scrollOffset = targetScrollOffset
        }

        scrollOffset =
            scrollOffset.coerceIn(
                0f,
                maxScroll
            )
    }

    private fun drawRules() {
        val panel =
            Rectangle(
                34f,
                95f,
                832f,
                1300f
            )

        game.assets.uiRenderer.drawGradientBorderPanel(
            batch,
            panel,
            ForgeUiRenderer.GradientStyle.PURPLE
        )

        /*
         * مساحة المحتوى داخل البانل.
         */
        val contentTop =
            panel.y + panel.height - 28f

        val contentBottom =
            panel.y + 28f

        val visibleHeight =
            contentTop - contentBottom

        /*
         * الموقع الأصلي لأول عنصر بدون Scroll.
         */
        val baseCursorY =
            panel.y + panel.height - 56f

        /*
         * كلما زاد scrollOffset
         * يتحرك المحتوى إلى الأعلى.
         */
        var cursorY =
            baseCursorY + scrollOffset

        // لا نسمح للنص أو الصور بالخروج من حدود المربع البرتقالي.
        withPanelClip(panel) {
            blocks.forEach { block: RuleBlock ->
    
                when (block) {
    
                    is RuleBlock.Heading -> {
                        drawHeading(
                            block,
                            cursorY
                        )
    
                        cursorY -=
                            if (block.level == 1) {
                                78f
                            } else {
                                58f
                            }
                    }
    
                    is RuleBlock.Paragraph -> {
                        cursorY =
                            drawParagraph(
                                block,
                                cursorY
                            )
                    }
    
                    is RuleBlock.Image -> {
                        cursorY =
                            drawImageBlock(
                                block,
                                cursorY
                            )
                    }
    
                    RuleBlock.Spacer -> {
                        cursorY -= 22f
                    }
                }
            }
        }

        /*
         * cursorY الحالي يحتوي scrollOffset.
         *
         * نشيله حتى نحصل على الحجم الحقيقي
         * للمحتوى بصرف النظر عن مكان السكرول.
         */
        val unscrolledCursorY =
            cursorY - scrollOffset

        val contentHeight =
            baseCursorY - unscrolledCursorY

        maxScroll =
            (
                contentHeight -
                    visibleHeight +
                    50f
            ).coerceAtLeast(0f)

        scrollOffset =
            scrollOffset.coerceIn(
                0f,
                maxScroll
            )

        targetScrollOffset =
            targetScrollOffset.coerceIn(
                0f,
                maxScroll
            )

        drawScrollBar(
            panel = panel,
            visibleHeight = visibleHeight
        )
    }

    /**
     * يقص كل محتوى القواعد داخل الحدود الداخلية للمربع البرتقالي.
     *
     * الشاشة مصممة على مساحة افتراضية 900x1600 مع FitViewport، لذلك نحول
     * إحداثيات العالم إلى إحداثيات الشاشة الفعلية قبل glScissor.
     *
     * الـ scrollbar يُرسم بعد انتهاء القص، لذلك يظل ظاهرًا بالكامل.
     */
    private fun withPanelClip(
        panel: Rectangle,
        drawContent: () -> Unit
    ) {
        val clipLeft = panel.x + 10f
        val clipBottom = panel.y + 10f
        val clipRight = panel.x + panel.width - 28f
        val clipTop = panel.y + panel.height - 10f

        val bottomLeft = Vector3(clipLeft, clipBottom, 0f)
        val topRight = Vector3(clipRight, clipTop, 0f)
        viewport.project(bottomLeft)
        viewport.project(topRight)

        batch.flush()
        Gdx.gl.glEnable(GL20.GL_SCISSOR_TEST)
        Gdx.gl.glScissor(
            bottomLeft.x.toInt(),
            bottomLeft.y.toInt(),
            (topRight.x - bottomLeft.x).toInt().coerceAtLeast(1),
            (topRight.y - bottomLeft.y).toInt().coerceAtLeast(1),
        )

        try {
            drawContent()
        } finally {
            batch.flush()
            Gdx.gl.glDisable(GL20.GL_SCISSOR_TEST)
        }
    }

    private fun drawHeading(
        block: RuleBlock.Heading,
        y: Float
    ) {
        val font =
            if (block.level == 1) {
                game.assets.pauseTitleFont
            } else {
                game.assets.titleFont
            }

        val size =
            if (block.level == 1) {
                0.54f
            } else {
                0.40f
            }

        font.color =
            if (block.level == 1) {
                ForgeUiPalette.purpleLight
            } else {
                ForgeUiPalette.goldLight
            }

        fittedText(
            font,
            block.text,
            72f,
            y,
            756f,
            size
        )

        font.color = Color.WHITE
    }

    private data class InlineToken(
        val text: String,
        val highlighted: Boolean = false,
        val emphasized: Boolean = false,
        val newline: Boolean = false
    )

    /**
     * Inline formatting supported inside normal Markdown paragraphs:
     *
     * ==Negative==  -> orange highlighted background.
     * **Large**     -> removes the ** markers and draws brighter text.
     *
     * The renderer also performs the same word wrapping as the old paragraph
     * renderer, so highlighted words stay inside the rules panel.
     */
    private fun drawParagraph(
        block: RuleBlock.Paragraph,
        cursorY: Float
    ): Float {
        val font = game.assets.smallFont

        val startX = 78f
        val maxWidth = 744f
        val endX = startX + maxWidth

        val tokens = parseInlineTokens(block.text)

        var x = startX
        var y = cursorY
        var lineCount = 1

        val lineHeight =
            max(
                1f,
                font.lineHeight
            )

        tokens.forEach { token ->
            if (token.newline) {
                x = startX
                y -= lineHeight
                lineCount++
                return@forEach
            }

            if (token.text.isEmpty()) {
                return@forEach
            }

            /*
             * Whitespace is measured separately. If it would start a wrapped
             * line, we simply drop it so the next line does not begin with a gap.
             */
            if (token.text.all { it == ' ' || it == '\t' }) {
                val spaceWidth =
                    GlyphLayout(
                        font,
                        token.text
                    ).width

                if (x > startX && x + spaceWidth <= endX) {
                    x += spaceWidth
                }

                return@forEach
            }

            val tokenLayout =
                GlyphLayout(
                    font,
                    token.text
                )

            val tokenWidth =
                tokenLayout.width

            if (
                x > startX &&
                x + tokenWidth > endX
            ) {
                x = startX
                y -= lineHeight
                lineCount++
            }

            if (token.highlighted) {
                drawOrangeHighlight(
                    x = x,
                    topY = y,
                    width = tokenWidth,
                    textHeight = tokenLayout.height
                )

                font.color = Color.WHITE
            } else if (token.emphasized) {
                /*
                 * We do not have a separate bold font here, so Markdown bold
                 * is represented by a brighter text color without showing **.
                 */
                font.color = Color.WHITE
            } else {
                font.color =
                    ForgeUiPalette.muted
            }

            font.draw(
                batch,
                token.text,
                x,
                y
            )

            x += tokenWidth
        }

        font.color = Color.WHITE
        batch.color = Color.WHITE

        val paragraphHeight =
            lineCount * lineHeight

        return cursorY -
            max(
                48f,
                paragraphHeight + 24f
            )
    }

    /**
     * Parses only the small amount of inline Markdown needed by the rules UI.
     *
     * ==text== is treated as a highlighted token.
     * **text** is treated as emphasized text.
     *
     * Newlines are preserved because list items are collected into a paragraph
     * by loadBlocks().
     */
    private fun parseInlineTokens(
        text: String
    ): List<InlineToken> {
        val result =
            mutableListOf<InlineToken>()

        val tokenRegex =
            Regex(
                "==.*?==|\\*\\*.*?\\*\\*|\\S+|[ \\t]+"
            )

        val lines =
            text.split('\n')

        lines.forEachIndexed { lineIndex, line ->
            tokenRegex
                .findAll(line)
                .forEach { match ->
                    val raw =
                        match.value

                    when {
                        raw.startsWith("==") &&
                            raw.endsWith("==") &&
                            raw.length >= 4 -> {
                            val value =
                                raw
                                    .removePrefix("==")
                                    .removeSuffix("==")

                            if (value.isNotEmpty()) {
                                result +=
                                    InlineToken(
                                        text = value,
                                        highlighted = true
                                    )
                            }
                        }

                        raw.startsWith("**") &&
                            raw.endsWith("**") &&
                            raw.length >= 4 -> {
                            val value =
                                raw
                                    .removePrefix("**")
                                    .removeSuffix("**")

                            if (value.isNotEmpty()) {
                                result +=
                                    InlineToken(
                                        text = value,
                                        emphasized = true
                                    )
                            }
                        }

                        else -> {
                            result +=
                                InlineToken(
                                    text = raw
                                )
                        }
                    }
                }

            if (lineIndex < lines.lastIndex) {
                result +=
                    InlineToken(
                        text = "",
                        newline = true
                    )
            }
        }

        return result
    }

    /**
     * Draws a compact orange marker behind ==highlighted== text.
     */
    private fun drawOrangeHighlight(
        x: Float,
        topY: Float,
        width: Float,
        textHeight: Float
    ) {
        val pixel =
            getHighlightPixel()

        val horizontalPadding = 7f
        val verticalPadding = 4f

        val backgroundX =
            x - horizontalPadding

        val backgroundY =
            topY -
                textHeight -
                verticalPadding

        val backgroundWidth =
            width +
                horizontalPadding * 2f

        val backgroundHeight =
            textHeight +
                verticalPadding * 2f

        /*
         * Warm orange highlight with a little transparency so it belongs to
         * the existing dark/orange rules-screen theme.
         */
        batch.color =
            Color(
                0.94f,
                0.26f,
                0.05f,
                0.88f
            )

        batch.draw(
            pixel,
            backgroundX,
            backgroundY,
            backgroundWidth,
            backgroundHeight
        )

        batch.color = Color.WHITE
    }

    private fun getHighlightPixel(): Texture {
        highlightPixel?.let {
            return it
        }

        val pixmap =
            Pixmap(
                1,
                1,
                Pixmap.Format.RGBA8888
            )

        pixmap.setColor(
            Color.WHITE
        )
        pixmap.fill()

        val texture =
            Texture(pixmap)

        pixmap.dispose()

        highlightPixel =
            texture

        return texture
    }

    private fun drawImageBlock(
        block: RuleBlock.Image,
        cursorY: Float
    ): Float {

        val texture =
            imageFor(block.path)
                ?: return cursorY - 24f

        val maxWidth = 300f
        val maxHeight = 190f

        val scale =
            min(
                maxWidth / texture.width.toFloat(),
                maxHeight / texture.height.toFloat()
            )

        val width =
            texture.width * scale

        val height =
            texture.height * scale

        val x =
            (900f - width) / 2f

        val y =
            cursorY - height

        batch.color =
            Color.WHITE

        batch.draw(
            TextureRegion(texture),
            x,
            y,
            width,
            height
        )

        game.assets.smallFont.color =
            ForgeUiPalette.muted

        fittedText(
            game.assets.smallFont,
            block.alt,
            100f,
            y - 24f,
            700f,
            0.44f
        )

        game.assets.smallFont.color =
            Color.WHITE

        return y - 58f
    }

    private fun drawScrollBar(
        panel: Rectangle,
        visibleHeight: Float
    ) {
        if (maxScroll <= 0f) {
            return
        }

        val track =
            Rectangle(
                panel.x + panel.width - 24f,
                panel.y + 55f,
                7f,
                panel.height - 120f
            )

        game.assets.uiRenderer.drawGradientPanel(
            batch,
            track,
            ForgeUiRenderer.GradientStyle.NEUTRAL
        )

        val totalHeight =
            visibleHeight + maxScroll

        val thumbHeight =
            (
                track.height *
                    visibleHeight /
                    totalHeight
            ).coerceIn(
                80f,
                track.height
            )

        val progress =
            (
                scrollOffset /
                    maxScroll
            ).coerceIn(
                0f,
                1f
            )

        /*
         * LibGDX:
         * y الأكبر = أعلى الشاشة.
         *
         * البداية:
         * المؤشر فوق.
         *
         * نهاية الصفحة:
         * المؤشر تحت.
         */
        val thumbY =
            track.y +
                (
                    track.height -
                        thumbHeight
                    ) *
                (1f - progress)

        val thumb =
            Rectangle(
                track.x,
                thumbY,
                track.width,
                thumbHeight
            )

        game.assets.uiRenderer.drawGradientPanel(
            batch,
            thumb,
            ForgeUiRenderer.GradientStyle.GOLD
        )
    }

    private fun handleScroll(delta: Float) {
        /*
         * Touch / mouse-drag start.
         */
        if (Gdx.input.justTouched()) {
            touchActive = true
            touchMoved = false
            touchVelocityY = 0f

            lastTouchY =
                touchPoint().y

            /*
             * Start the drag from the position currently visible on screen.
             * This prevents a jump when a wheel animation is still settling.
             */
            targetScrollOffset = scrollOffset

            return
        }

        /*
         * Finger or mouse button is still held.
         */
        if (
            touchActive &&
            Gdx.input.isTouched
        ) {
            val currentY =
                touchPoint().y

            val deltaY =
                currentY - lastTouchY

            if (abs(deltaY) > 0.25f) {
                targetScrollOffset += deltaY * DRAG_SCROLL_MULTIPLIER

                targetScrollOffset =
                    targetScrollOffset.coerceIn(
                        0f,
                        maxScroll
                    )

                val safeDelta =
                    delta.coerceAtLeast(1f / 240f)

                /*
                 * Low-pass the drag speed so release momentum is stable and
                 * does not depend on one noisy touch sample.
                 */
                val instantVelocity =
                    deltaY / safeDelta

                touchVelocityY =
                    touchVelocityY * 0.72f +
                        instantVelocity * 0.28f

                if (abs(deltaY) > 4f) {
                    touchMoved = true
                }
            }

            lastTouchY =
                currentY

            return
        }

        /*
         * Release: project a small amount of the final drag velocity forward.
         * The normal easing then glides to that destination smoothly.
         */
        if (touchActive) {
            touchActive = false

            if (
                touchMoved &&
                abs(touchVelocityY) > MIN_FLING_VELOCITY
            ) {
                val flingDistance =
                    (touchVelocityY * FLING_PROJECTION_SECONDS)
                        .coerceIn(
                            -MAX_FLING_DISTANCE,
                            MAX_FLING_DISTANCE
                        )

                targetScrollOffset =
                    (targetScrollOffset + flingDistance)
                        .coerceIn(
                            0f,
                            maxScroll
                        )
            }

            touchVelocityY = 0f
        }
    }

    private fun loadBlocks(): List<RuleBlock> {
        val file =
            Gdx.files.internal(
                "rules/BrickBreakerBall_Game_Rules_EN.md"
            )

        if (!file.exists()) {
            return listOf(
                RuleBlock.Heading(
                    "RULES FILE UNAVAILABLE",
                    1
                )
            )
        }

        val result =
            mutableListOf<RuleBlock>()

        val paragraph =
            StringBuilder()

        fun flushParagraph() {
            val text =
                paragraph
                    .toString()
                    .trim()

            if (text.isNotEmpty()) {
                result +=
                    RuleBlock.Paragraph(
                        text
                    )
            }

            paragraph.clear()
        }

        file
            .readString()
            .lineSequence()
            .forEach { raw ->

                val line =
                    raw.trim()

                when {

                    line.isEmpty() -> {
                        flushParagraph()

                        /*
                         * نتجنب إضافة عشرات الفراغات
                         * المتتالية.
                         */
                        if (
                            result.lastOrNull()
                                !is RuleBlock.Spacer
                        ) {
                            result +=
                                RuleBlock.Spacer
                        }
                    }

                    line.startsWith("![") -> {
                        flushParagraph()

                        val match =
                            Regex(
                                "!\\[([^]]*)]\\(([^)]+)\\)"
                            ).find(line)

                        if (match != null) {
                            result +=
                                RuleBlock.Image(
                                    path =
                                        match.groupValues[2],
                                    alt =
                                        match.groupValues[1]
                                )
                        }
                    }

                    line.startsWith("#") -> {
                        flushParagraph()

                        val level =
                            line
                                .takeWhile {
                                    it == '#'
                                }
                                .length
                                .coerceIn(
                                    1,
                                    3
                                )

                        val heading =
                            line
                                .drop(level)
                                .trim()

                        if (heading.isNotEmpty()) {
                            result +=
                                RuleBlock.Heading(
                                    text = heading,
                                    level = level
                                )
                        }
                    }

                    line.startsWith("- ") -> {
                        paragraph
                            .append("• ")
                            .append(
                                line
                                    .drop(2)
                                    .trim()
                            )
                            .append('\n')
                    }

                    line.startsWith("* ") -> {
                        paragraph
                            .append("• ")
                            .append(
                                line
                                    .drop(2)
                                    .trim()
                            )
                            .append('\n')
                    }

                    else -> {
                        paragraph
                            .append(line)
                            .append('\n')
                    }
                }
            }

        flushParagraph()

        return result
    }

    private fun imageFor(
        markdownPath: String
    ): Texture? {

        val assetPath =
            markdownPath
                .removePrefix("./")
                .removePrefix("assets/")

        images[assetPath]?.let {
            return it
        }

        val file =
            Gdx.files.internal(
                assetPath
            )

        if (!file.exists()) {
            return null
        }

        return Texture(file).also { texture ->

            texture.setFilter(
                Texture.TextureFilter.Linear,
                Texture.TextureFilter.Linear
            )

            images[assetPath] =
                texture
        }
    }

    private companion object {
        /** Extra responsiveness for touch/mouse drag without making it jumpy. */
        const val DRAG_SCROLL_MULTIPLIER = 1.18f

        /** World units moved for one mouse-wheel notch. */
        const val MOUSE_WHEEL_STEP = 230f

        /** Higher values settle faster while keeping the ease-out motion smooth. */
        const val SCROLL_SMOOTHING = 18f

        /** Small release momentum for touch / mouse dragging. */
        const val FLING_PROJECTION_SECONDS = 0.16f
        const val MIN_FLING_VELOCITY = 70f
        const val MAX_FLING_DISTANCE = 520f
    }

    override fun dispose() {
        images
            .values
            .forEach { texture ->
                texture.dispose()
            }

        images.clear()

        highlightPixel?.dispose()
        highlightPixel = null

        super.dispose()
    }
}