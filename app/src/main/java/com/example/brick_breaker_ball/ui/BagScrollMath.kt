/*
 * المسار: app/src/main/java/com/example/brick_breaker_ball/ui/BagScrollMath.kt
 * الوظيفة: حساب تمرير عناصر حقيبة التعويذات.
 * الحسابات: حدود التمرير وعدد الصفوف الظاهرة.
 */

package com.example.brick_breaker_ball.ui

/** The unprojected touch Y increases when the finger swipes up. */
internal fun bagScrollAfterDrag(current: Float, dragDeltaY: Float, maxScroll: Float): Float = (current + dragDeltaY).coerceIn(0f, maxScroll.coerceAtLeast(0f))

/** Stop only after the bottom of the final row clears the clipped inventory window. */
internal fun bagMaxScroll(lastRowBottom: Float, viewportBottom: Float, bottomPadding: Float): Float = (viewportBottom + bottomPadding - lastRowBottom).coerceAtLeast(0f)
