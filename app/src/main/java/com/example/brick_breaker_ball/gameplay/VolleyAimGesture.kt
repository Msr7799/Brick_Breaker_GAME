/*
 * المسار: app/src/main/java/com/example/brick_breaker_ball/gameplay/VolleyAimGesture.kt
 * الوظيفة: تفسير لمس اللاعب واتجاه التصويب.
 * الحسابات: تحويل السحب إلى زاوية واتجاه إطلاق.
 */

package com.example.brick_breaker_ball.gameplay

/** Result of lifting the active aiming finger. */
enum class VolleyAimRelease { LAUNCH, CANCEL }

/**
 * Small, rendering-independent state machine for Volley touch aiming.
 *
 * The gesture starts anywhere in the playable area. Pulling the same finger back into the
 * cancellation strip arms cancellation; lifting outside that strip launches. Requiring a real
 * downward pull prevents an aim that starts near the bottom from being cancelled accidentally.
 */
class VolleyAimGesture(
    cancelZoneTop: Float,
    private val minimumPullBack: Float = 64f
) {
    private var cancelZoneTop = cancelZoneTop
    var active: Boolean = false
        private set
    var cancelArmed: Boolean = false
        private set
    var startX: Float = 0f
        private set
    var startY: Float = 0f
        private set
    var currentX: Float = 0f
        private set
    var currentY: Float = 0f
        private set

    fun begin(x: Float, y: Float) {
        active = true
        cancelArmed = false
        startX = x
        startY = y
        currentX = x
        currentY = y
    }

    fun updateCancelZoneTop(top: Float) {
        cancelZoneTop = top
        if (active) update(currentX, currentY)
    }

    fun update(x: Float, y: Float) {
        if (!active) return
        currentX = x
        currentY = y
        cancelArmed = y <= cancelZoneTop && startY - y >= minimumPullBack
    }

    fun finish(x: Float = currentX, y: Float = currentY): VolleyAimRelease {
        update(x, y)
        val result = if (cancelArmed) VolleyAimRelease.CANCEL else VolleyAimRelease.LAUNCH
        active = false
        cancelArmed = false
        return result
    }

    fun cancel() {
        active = false
        cancelArmed = false
    }
}
