/*
 * ملاحظات صيانة الملف:
 * المسار: app/src/main/java/com/example/brick_breaker_ball/Replay.kt
 * المؤلف: mohamed alromaihi
 * الدوال الموجودة: `record`، `snapshot`
 */

package com.example.brick_breaker_ball

data class ReplayInput(val time: Float, val paddleX: Float, val command: String? = null)
data class ReplayData(val seed: Long, val levelId: Int, val inputs: List<ReplayInput>)

class ReplayRecorder(val seed: Long, val levelId: Int) {
    private val events = mutableListOf<ReplayInput>()

    /** ملاحظة صيانة: الدالة `record` تحفظ البيانات أو تضيفها إلى الحالة المعتمدة في النظام؛ راجع استدعاءاتها واختباراتها قبل تعديلها. */
    fun record(time: Float, paddleX: Float, command: String? = null) {
        if (events.lastOrNull()?.let { command == null && kotlin.math.abs(it.paddleX - paddleX) < 2f } == true) return
        events += ReplayInput(time, paddleX, command)
    }

    /** ملاحظة صيانة: الدالة `snapshot` تقرأ البيانات المطلوبة أو تسترجعها بصيغة مناسبة للاستخدام؛ راجع استدعاءاتها واختباراتها قبل تعديلها. */
    fun snapshot() = ReplayData(seed, levelId, events.toList())
}
