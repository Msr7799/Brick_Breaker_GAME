/*
 * المسار: app/src/main/java/com/example/brick_breaker_ball/ui/VolleyControlTutorial.kt
 * الوظيفة: خطوات تعليم تحكم Volley.
 * الحسابات: تحويل السحب واللمس إلى تعليمات مصورة.
 */

package com.example.brick_breaker_ball.ui

/** Single, one-time control lesson, driven by foreground REAL seconds, not accelerated simulation. */
class VolleyControlTutorial(alreadyCompleted: Boolean, private val thresholdSeconds: Float = 20f) {
    enum class Step { NONE, SPEED, RECALL }
    var step = Step.NONE
        private set
    private var elapsed = 0.0
    private var completed = alreadyCompleted
    var typingSeconds: Float = 0f
        private set

    val active: Boolean get() = step != Step.NONE
    val done: Boolean get() = completed

    /** Pauses the underlying simulation only after an actual live volley has remained in flight. */
    fun observe(realSeconds: Float, ballsInFlight: Boolean) {
        if (completed || active) return
        if (!ballsInFlight) {
            elapsed = 0.0
            return
        }
        elapsed += realSeconds.coerceIn(0f, .05f).toDouble()
        if (elapsed >= thresholdSeconds.toDouble() - 0.0001) {
            step = Step.SPEED
            typingSeconds = 0f
        }
    }

    fun animate(realSeconds: Float) {
        if (active) typingSeconds += realSeconds.coerceIn(0f, .05f)
    }

    /** Any new screen tap skips to the next highlight; the second one resumes play. */
    fun skip(): Boolean {
        when (step) {
            Step.NONE -> return false

            Step.SPEED -> {
                step = Step.RECALL
                typingSeconds = 0f
            }

            Step.RECALL -> {
                step = Step.NONE
                completed = true
                elapsed = 0.0
            }
        }
        return completed
    }

    fun skipAll() {
        step = Step.NONE
        completed = true
        elapsed = 0.0
        typingSeconds = 0f
    }

    fun reveal(text: String, charsPerSecond: Float = 28f): String {
        if (!active) return text
        val count = (typingSeconds * charsPerSecond).toInt().coerceIn(0, text.length)
        return text.take(count)
    }
}
