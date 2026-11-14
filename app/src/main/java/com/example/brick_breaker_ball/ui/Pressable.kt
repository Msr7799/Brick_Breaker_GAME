/*
 * المسار: app/src/main/java/com/example/brick_breaker_ball/ui/Pressable.kt
 * الوظيفة: حالة لمس عناصر الواجهة.
 * الحسابات: تحديد وقوع النقطة داخل المستطيل.
 */

package com.example.brick_breaker_ball.ui

import com.badlogic.gdx.math.Rectangle

/** Shared geometry for tactile buttons. Rendering stays with each screen's visual language. */
object Pressable {
    fun visualRect(rect: Rectangle, pressed: Boolean, reduceMotion: Boolean): Rectangle {
        val scale = if (pressed && !reduceMotion) .965f else 1f
        val w = rect.width * scale
        val h = rect.height * scale
        return Rectangle(rect.x + (rect.width - w) / 2f, rect.y + (rect.height - h) / 2f, w, h)
    }
}
