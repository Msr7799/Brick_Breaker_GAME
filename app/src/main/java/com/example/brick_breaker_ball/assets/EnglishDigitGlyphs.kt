/*
 * المسار: app/src/main/java/com/example/brick_breaker_ball/assets/EnglishDigitGlyphs.kt
 * الوظيفة: توحيد شكل كل الأرقام العشرية المعروضة في خطوط اللعبة إلى الأرقام الإنجليزية.
 * الحسابات: ربط كل محرف عشري محلي بمحرف ASCII المناظر له دون توليد صور خط جديدة.
 */

package com.example.brick_breaker_ball.assets

import com.badlogic.gdx.graphics.g2d.BitmapFont

private val localizedDecimalDigits: IntArray = (0..Char.MAX_VALUE.code)
    .filter { code ->
        code !in '0'.code..'9'.code &&
            Character.getType(code) == Character.DECIMAL_DIGIT_NUMBER.toInt()
    }
    .toIntArray()

/** ينطبق على كل استدعاء BitmapFont.draw، بما فيه النصوص التي ترجع من خدمات خارجية. */
fun installEnglishDigitGlyphs(data: BitmapFont.BitmapFontData) {
    localizedDecimalDigits.forEach { code ->
        val digit = Character.digit(code, 10)
        if (digit in 0..9) {
            data.getGlyph('0' + digit)?.let { data.setGlyph(code, it) }
        }
    }
    // قد ترجع بعض الخدمات الفاصلة العشرية وفاصلة الآلاف العربية مع الأرقام المحلية.
    data.getGlyph('.')?.let { data.setGlyph('٫'.code, it) }
    data.getGlyph(',')?.let { data.setGlyph('٬'.code, it) }
}
