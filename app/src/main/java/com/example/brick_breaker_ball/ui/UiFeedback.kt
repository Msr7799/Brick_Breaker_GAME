/*
 * المسار: app/src/main/java/com/example/brick_breaker_ball/ui/UiFeedback.kt
 * الوظيفة: ردود الواجهة الصوتية واللمسية.
 * الحسابات: اختيار مدة وشدة الاستجابة.
 */

package com.example.brick_breaker_ball.ui

import com.badlogic.gdx.Gdx
import com.example.brick_breaker_ball.BrickBreakerGame

enum class UiFeedbackKind { TAP, CONFIRM, BACK, ERROR, PURCHASE, UNLOCK }

class UiFeedback(private val game: BrickBreakerGame) {
    fun emit(kind: UiFeedbackKind = UiFeedbackKind.TAP) {
        val settings = try {
            game.progress.settings
        } catch (_: Throwable) {
            return
        }
        val volume = settings.masterVolume * settings.sfxVolume
        when (kind) {
            UiFeedbackKind.TAP, UiFeedbackKind.BACK -> game.assets.play("button", volume * .72f)

            UiFeedbackKind.CONFIRM -> game.assets.play("button", volume * .78f)

            UiFeedbackKind.PURCHASE -> {
                game.assets.play("ui_click", volume * .48f)
                game.assets.play("powerup", volume * .78f)
            }

            UiFeedbackKind.UNLOCK -> {
                game.assets.play("powerup", volume * .82f)
                game.assets.play("explosion", volume * .18f)
            }

            UiFeedbackKind.ERROR -> game.assets.play("wall_hit", volume * .55f)
        }
        if (settings.haptics) {
            val ms = when (kind) {
                UiFeedbackKind.TAP -> 10
                UiFeedbackKind.BACK -> 8
                UiFeedbackKind.CONFIRM -> 16
                UiFeedbackKind.ERROR -> 22
                UiFeedbackKind.PURCHASE, UiFeedbackKind.UNLOCK -> 28
            }
            Gdx.input.vibrate(ms)
        }
    }
}
