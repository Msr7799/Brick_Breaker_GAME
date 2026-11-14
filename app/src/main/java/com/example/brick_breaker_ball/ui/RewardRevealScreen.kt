/*
 * المسار: app/src/main/java/com/example/brick_breaker_ball/ui/RewardRevealScreen.kt
 * الوظيفة: واجهة الكشف عن المكافآت.
 * الحسابات: توقيت الظهور وحجم بطاقات الجوائز.
 */

package com.example.brick_breaker_ball.ui

import com.badlogic.gdx.Gdx
import com.badlogic.gdx.Input
import com.badlogic.gdx.Preferences
import com.badlogic.gdx.graphics.Color
import com.badlogic.gdx.utils.Align
import com.example.brick_breaker_ball.BrickBreakerGame
import com.example.brick_breaker_ball.assets.VolleyAssetCatalog
import com.example.brick_breaker_ball.economy.SHOP_ELIGIBLE_TYPES
import com.example.brick_breaker_ball.gameplay.GameplayMode
import com.example.brick_breaker_ball.gameplay.PowerUpType
import com.example.brick_breaker_ball.gameplay.VolleyItemDescriptions
import kotlin.math.pow

data class PendingReward(
    val token: String,
    val type: PowerUpType,
    val owned: Int,
    val mode: GameplayMode = GameplayMode.VOLLEY
)
class PendingRewardRevealStore(private val prefs: Preferences = Gdx.app.getPreferences("brickbreakerball-pending-reward-v1")) {
    /** ملاحظة صيانة: الدالة `save` تحفظ البيانات أو تضيفها إلى الحالة المعتمدة في النظام؛ راجع استدعاءاتها واختباراتها قبل تعديلها. */
    fun save(r: PendingReward) {
        prefs.putString("token", r.token)
            .putString("type", r.type.name)
            .putInteger("owned", r.owned)
            .putString("mode", r.mode.name)
            .flush()
    }

    /** ملاحظة صيانة: الدالة `load` تقرأ البيانات المطلوبة أو تسترجعها بصيغة مناسبة للاستخدام؛ راجع استدعاءاتها واختباراتها قبل تعديلها. */
    fun load(): PendingReward? {
        val token = prefs.getString("token", "")
        if (token.isEmpty()) return null
        return runCatching {
            PendingReward(
                token,
                PowerUpType.valueOf(prefs.getString("type")),
                prefs.getInteger("owned"),
                GameplayMode.VOLLEY
            )
        }.getOrNull()
    }

    /** ملاحظة صيانة: الدالة `clear` تنظّف الحالة أو الموارد المرتبطة بهذه المسؤولية بأمان؛ راجع استدعاءاتها واختباراتها قبل تعديلها. */
    fun clear() {
        prefs.clear()
        prefs.flush()
    }
}
class RewardRevealScreen(game: BrickBreakerGame, private val reward: PendingReward, private val destination: ShopReturnDestination) : ForgeScreen(game) {
    private var elapsed = 0f
    private var finalFxTriggered = false
    private val rewardFx = RewardFxSystem(game)
    private val volleyAssets by lazy { VolleyAssetCatalog(game.assets.volley) }

    private fun rewardIcon(type: PowerUpType) = volleyAssets.itemIcon(type)

    /** ملاحظة صيانة: الدالة `render` تنفّذ العقد الموروث وتربط دورة حياة المكوّن بسلوك هذا الملف؛ راجع استدعاءاتها واختباراتها قبل تعديلها. */
    override fun render(delta: Float) {
        elapsed += delta.coerceAtMost(.06f)
        rewardFx.update(delta)
        beginStaticBackground(game.assets.volleyWorldBackground(1), .24f)
        title("YOUR REWARD")
        val revealDuration = if (game.progress.settings.reduceMotion) .05f else 1.75f
        val final = elapsed >= revealDuration
        val shown = if (final) {
            reward.type
        } else {
            val progress = (elapsed / revealDuration).coerceIn(0f, 1f)
            val ease =
                1f - (1f - progress).pow(3)
            SHOP_ELIGIBLE_TYPES[((ease * 45).toInt()).coerceAtMost(44) % SHOP_ELIGIBLE_TYPES.size]
        }
        game.assets.uiRenderer.drawGradientBorderPanel(
            batch,
            com.badlogic.gdx.math.Rectangle(285f, 745f, 330f, 330f),
            if (final) ForgeUiRenderer.GradientStyle.SUCCESS else ForgeUiRenderer.GradientStyle.PRIMARY,
            7f
        )
        batch.draw(rewardIcon(shown), 335f, 795f, 230f, 230f)
        if (final) {
            // مكافآت المتجر الحالية تستخدم وصف عناصر Volley.
            val info = VolleyItemDescriptions.info(reward.type)
            if (!finalFxTriggered) {
                finalFxTriggered = true
                rewardFx.trigger(
                    kind = RewardFxSystem.Kind.TALISMAN,
                    icons = listOf(rewardIcon(reward.type)),
                    centerX = 450f,
                    centerY = 895f,
                    targetX = 760f,
                    targetY = 1435f,
                    accent = ForgeUiPalette.purple
                )
                game.uiFeedback.emit(UiFeedbackKind.UNLOCK)
            }
            game.assets.pauseTitleFont.draw(batch, "AMAZING!", 0f, 1270f, 900f, Align.center, false)
            game.assets.bodyFont.draw(batch, "YOU GOT", 0f, 690f, 900f, Align.center, false)
            game.assets.titleFont.draw(batch, info.fullName, 35f, 620f, 830f, Align.center, false)
            game.assets.bodyFont.draw(batch, "+1     OWNED: ${reward.owned}", 0f, 535f, 900f, Align.center, false)
            game.assets.smallFont.draw(batch, info.description, 70f, 470f, 760f, Align.center, true)

            // Small visual inventory target: the celebration copies visibly fly here so the
            // player sees that the talisman was actually stored, not merely announced by text.
            game.assets.uiRenderer.drawRoundedGradientBorderPanel(
                batch,
                com.badlogic.gdx.math.Rectangle(690f, 1380f, 140f, 105f),
                ForgeUiRenderer.GradientStyle.SUCCESS,
                4f
            )
            batch.draw(rewardIcon(reward.type), 708f, 1398f, 64f, 64f)
            game.assets.hudValueFont.draw(batch, "x${reward.owned}", 770f, 1446f, 48f, Align.center, false)

            val ok = button("AWESOME", 175f, 270f, 550f, 100f)
            rewardFx.draw(batch)
            end()
            if (tapped(ok) ||
                Gdx.input.isKeyJustPressed(Input.Keys.BACK)
            ) {
                game.pendingRewards.clear()
                game.navigate(ShopScreen(game, destination, reward.mode))
            }
            return
        }
        game.assets.smallFont.draw(batch, "SHUFFLING...", 0f, 660f, 900f, Align.center, false)
        rewardFx.draw(batch)
        end()
    }
}
