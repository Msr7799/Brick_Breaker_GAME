/*
 * الوظيفة: علم QA لإرجاع مراحل Volley الأصلية فور تعطيل المولد.
 * الحسابات: اختيار تعريف المرحلة قبل إنشاء جلسة اللعب؛ لا يغيّر التقدم أو الفيزياء.
 */
package com.example.brick_breaker_ball.gameplay

import com.badlogic.gdx.Gdx
import com.example.brick_breaker_ball.BuildConfig
import com.example.brick_breaker_ball.campaign.VolleyStageDefinition
import com.example.brick_breaker_ball.campaign.VolleyStageRepository

object VolleyGeneratorFeature {
    private const val PREFERENCES = "brickbreakerball-volley-generator-qa-v1"
    private const val ENABLED = "enabled"

    val available: Boolean get() = BuildConfig.DEVELOPER_ACCESS_ALLOWED

    fun enabled(): Boolean = available && Gdx.app.getPreferences(PREFERENCES).getBoolean(ENABLED, false)

    fun setEnabled(value: Boolean) {
        if (available) Gdx.app.getPreferences(PREFERENCES).putBoolean(ENABLED, value).flush()
    }

    fun stageForLevel(levelId: Int, useGenerator: Boolean = enabled()): VolleyStageDefinition {
        val original = VolleyStageRepository.forLevel(levelId)
        // Opening Salvo is authored in world space and remains protected.
        return if (useGenerator && available && levelId != 1) {
            VolleyLevelGenerator.forCampaignLevel(levelId).stage
        } else {
            original
        }
    }
}
