/*
 * الوظيفة: ثلاث مراحل تجريبية أصلية لاختبار شكل سهل وممرات وزعيم في أداة QA.
 */
package com.example.brick_breaker_ball.gameplay

data class VolleyGeneratorSample(val name: String, val levelId: Int, val spec: VolleyGeneratorSpec)

object VolleyGeneratorSamples {
    val levels = listOf(
        VolleyGeneratorSample(
            "OPEN DIAMOND",
            2,
            VolleyGeneratorSpec(VolleyPattern.DIAMOND, 101L, 1, density = 1f)
        ),
        VolleyGeneratorSample(
            "TWIN CHANNELS",
            20,
            VolleyGeneratorSpec(VolleyPattern.TWIN_CHANNELS, 202L, 6, density = .9f)
        ),
        VolleyGeneratorSample(
            "CORE ARENA",
            30,
            VolleyGeneratorSpec(VolleyPattern.BOSS_ARENA, 303L, 8, density = .9f)
        )
    )
}
