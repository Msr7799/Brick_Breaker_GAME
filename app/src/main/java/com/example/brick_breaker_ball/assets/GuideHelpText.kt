package com.example.brick_breaker_ball.assets

import com.badlogic.gdx.Gdx
import com.badlogic.gdx.graphics.Color
import com.badlogic.gdx.graphics.g2d.BitmapFont
import com.badlogic.gdx.graphics.g2d.GlyphLayout
import com.badlogic.gdx.graphics.g2d.SpriteBatch
import com.badlogic.gdx.math.Rectangle
import com.badlogic.gdx.utils.JsonReader

/** Short English lessons drawn with the game's existing font and left-to-right typing. */
class GuideHelpText(private val font: BitmapFont) {
    private val entries = JsonReader().parse(Gdx.files.internal("ui/hero-guide/help-en.json"))
    private val measure = GlyphLayout()

    fun keys(lesson: String, orbFamily: String? = null): List<String> = (0..2).map {
        val effect = "effect_${orbFamily?.lowercase()}_0"
        if (lesson == "orbs_3" && it == 1 && entries.get(effect) != null) effect else "${lesson}_$it"
    }

    fun lines(lesson: String, orbFamily: String? = null): List<String> = keys(lesson, orbFamily).map(::text)

    fun text(key: String): String = requireNotNull(entries.get(key)) { "Missing guide text: $key" }.asString()

    fun draw(batch: SpriteBatch, key: String, box: Rectangle, visibleCharacters: Int = Int.MAX_VALUE,
             alpha: Float = 1f) {
        if (visibleCharacters <= 0) return
        val fullText = text(key)
        val oldX = font.data.scaleX
        val oldY = font.data.scaleY
        val oldColor = Color(font.color)
        try {
            font.data.setScale(1f)
            measure.setText(font, fullText)
            val scale = minOf(box.width / measure.width.coerceAtLeast(1f),
                box.height / measure.height.coerceAtLeast(1f)) * .90f
            font.data.setScale(scale)
            measure.setText(font, fullText)
            val x = box.x + (box.width - measure.width) / 2f
            val y = box.y + (box.height + measure.height) / 2f
            font.setColor(1f, .965f, .87f, alpha)
            // Position from the full sentence so typing never shifts the already visible letters.
            font.draw(batch, fullText.take(visibleCharacters.coerceAtMost(fullText.length)), x, y)
        } finally {
            font.data.setScale(oldX, oldY)
            font.color = oldColor
        }
    }
}
