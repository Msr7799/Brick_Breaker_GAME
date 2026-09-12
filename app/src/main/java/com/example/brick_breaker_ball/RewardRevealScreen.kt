/*
 * ملاحظات صيانة الملف:
 * المسار: app/src/main/java/com/example/brick_breaker_ball/RewardRevealScreen.kt
 * المؤلف: mohamed alromaihi
 * الدوال الموجودة: `save`، `load`، `clear`، `render`
 */

package com.example.brick_breaker_ball

import com.badlogic.gdx.Gdx
import com.badlogic.gdx.Input
import com.badlogic.gdx.Preferences
import com.badlogic.gdx.graphics.Color
import com.badlogic.gdx.utils.Align
import kotlin.math.pow

data class PendingReward(val token: String, val type: PowerUpType, val owned: Int)
class PendingRewardRevealStore(private val prefs: Preferences = Gdx.app.getPreferences("brickbreakerball-pending-reward-v1")) {
    /** ملاحظة صيانة: الدالة `save` تحفظ البيانات أو تضيفها إلى الحالة المعتمدة في النظام؛ راجع استدعاءاتها واختباراتها قبل تعديلها. */
    fun save(r: PendingReward) {
        prefs.putString("token", r.token).putString("type", r.type.name).putInteger("owned", r.owned).flush()
    }

    /** ملاحظة صيانة: الدالة `load` تقرأ البيانات المطلوبة أو تسترجعها بصيغة مناسبة للاستخدام؛ راجع استدعاءاتها واختباراتها قبل تعديلها. */
    fun load(): PendingReward? {
        val token = prefs.getString("token", "")
        if (token.isEmpty()) return null
        return runCatching { PendingReward(token, PowerUpType.valueOf(prefs.getString("type")), prefs.getInteger("owned")) }.getOrNull()
    }

    /** ملاحظة صيانة: الدالة `clear` تنظّف الحالة أو الموارد المرتبطة بهذه المسؤولية بأمان؛ راجع استدعاءاتها واختباراتها قبل تعديلها. */
    fun clear() {
        prefs.clear()
        prefs.flush()
    }
}
class RewardRevealScreen(game: BrickBreakerGame, private val reward: PendingReward, private val destination: ShopReturnDestination) : ForgeScreen(game) {
    private var elapsed = 0f

    /** ملاحظة صيانة: الدالة `render` تنفّذ العقد الموروث وتربط دورة حياة المكوّن بسلوك هذا الملف؛ راجع استدعاءاتها واختباراتها قبل تعديلها. */
    override fun render(delta: Float) {
        elapsed += delta
        begin(2)
        title("YOUR REWARD")
        val final = elapsed >= 2.45f || game.progress.settings.reduceMotion
        val shown = if (final) {
            reward.type
        } else {
            val progress = (elapsed / 2.45f).coerceIn(0f, 1f)
            val ease =
                1f - (1f - progress).pow(3)
            SHOP_ELIGIBLE_TYPES[((ease * 45).toInt()).coerceAtMost(44) % SHOP_ELIGIBLE_TYPES.size]
        }
        game.assets.uiRenderer.drawGradientBorderPanel(
            batch,
            com.badlogic.gdx.math.Rectangle(285f, 745f, 330f, 330f),
            if (final) ForgeUiRenderer.GradientStyle.SUCCESS else ForgeUiRenderer.GradientStyle.PRIMARY,
            7f,
        )
        batch.draw(game.assets.gameplayAtlas.powerUpIcon(shown), 335f, 795f, 230f, 230f)
        if (final) {
            val info = PowerUpInfoRepository.info(reward.type)
            game.assets.pauseTitleFont.draw(batch, "AMAZING!", 0f, 1270f, 900f, Align.center, false)
            game.assets.bodyFont.draw(batch, "YOU GOT", 0f, 690f, 900f, Align.center, false)
            game.assets.titleFont.draw(batch, info.fullName, 35f, 620f, 830f, Align.center, false)
            game.assets.bodyFont.draw(batch, "+1     OWNED: ${reward.owned}", 0f, 535f, 900f, Align.center, false)
            game.assets.smallFont.draw(batch, info.description, 70f, 470f, 760f, Align.center, true)
            val ok = button("AWESOME", 175f, 270f, 550f, 100f)
            end()
            if (tapped(ok) ||
                Gdx.input.isKeyJustPressed(Input.Keys.BACK)
            ) {
                game.pendingRewards.clear()
                game.setScreen(ShopScreen(game, destination))
            }
            return
        }
        game.assets.smallFont.draw(batch, "SHUFFLING...", 0f, 660f, 900f, Align.center, false)
        end()
    }
}
