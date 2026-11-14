/*
 * المسار: app/src/main/java/com/example/brick_breaker_ball/ui/RewardFxSystem.kt
 * الوظيفة: رسوم كشف المكافآت والمؤثرات.
 * الحسابات: استيفاء الموضع والشفافية عبر الزمن.
 */

package com.example.brick_breaker_ball.ui

import com.badlogic.gdx.graphics.Color
import com.badlogic.gdx.graphics.g2d.SpriteBatch
import com.badlogic.gdx.graphics.g2d.TextureRegion
import com.badlogic.gdx.math.MathUtils
import com.example.brick_breaker_ball.BrickBreakerGame

/**
 * Shared, asset-light celebration layer used by Shop, rewards and unlock screens.
 * It deliberately reuses the project's particle atlas so reward moments feel visual without
 * requiring another large sprite sheet in memory.
 */
class RewardFxSystem(private val game: BrickBreakerGame) {
    enum class Kind { PURCHASE, TALISMAN, COSMETIC, DAILY, UNLOCK }

    private var active = false
    private var elapsed = 0f
    private var duration = 1.35f
    private var kind = Kind.PURCHASE
    private var icons: List<TextureRegion> = emptyList()
    private var centerX = 450f
    private var centerY = 820f
    private var targetX: Float? = null
    private var targetY: Float? = null
    private val accent = Color(ForgeUiPalette.primary)

    val isActive: Boolean get() = active

    fun trigger(
        kind: Kind,
        icons: List<TextureRegion>,
        centerX: Float = 450f,
        centerY: Float = 820f,
        targetX: Float? = null,
        targetY: Float? = null,
        accent: Color? = null,
        duration: Float = when (kind) {
            Kind.PURCHASE -> 1.18f
            Kind.TALISMAN -> 1.65f
            Kind.COSMETIC -> 1.45f
            Kind.DAILY -> 1.55f
            Kind.UNLOCK -> 1.35f
        }
    ) {
        this.kind = kind
        this.icons = icons.take(5)
        this.centerX = centerX
        this.centerY = centerY
        this.targetX = targetX
        this.targetY = targetY
        this.duration = duration.coerceAtLeast(.45f)
        val c = accent ?: defaultAccent(kind)
        this.accent.set(c)
        elapsed = 0f
        active = true
    }

    fun update(delta: Float) {
        if (!active) return
        elapsed += delta.coerceAtMost(.06f)
        if (elapsed >= duration) active = false
    }

    fun draw(batch: SpriteBatch) {
        if (!active) return
        val reduceMotion = game.progress.settings.reduceMotion
        val p = (elapsed / duration).coerceIn(0f, 1f)
        val glow = game.assets.softGlowRegion
        val spark = game.assets.particles.findRegion("spark")
        val star = game.assets.particles.findRegion("star")
        val shockwave = game.assets.particles.findRegion("shockwave")

        // A very short luminance flash gives the action weight without hiding the screen.
        val flash = when {
            p < .11f -> 1f - p / .11f
            else -> 0f
        }
        if (flash > 0f) {
            batch.color = Color(accent.r, accent.g, accent.b, flash * if (reduceMotion) .05f else .12f)
            batch.draw(game.assets.solidUiRegion, 0f, 0f, 900f, 1600f)
        }

        val burstP = (p / .48f).coerceIn(0f, 1f)
        val shockSize = MathUtils.lerp(120f, if (kind == Kind.TALISMAN) 690f else 520f, easeOutCubic(burstP))
        val shockAlpha = (1f - burstP) * if (reduceMotion) .18f else .48f
        batch.color = Color(accent.r, accent.g, accent.b, shockAlpha)
        batch.draw(shockwave, centerX - shockSize / 2f, centerY - shockSize / 2f, shockSize, shockSize)

        if (!reduceMotion) {
            repeat(if (kind == Kind.TALISMAN) 18 else 12) { index ->
                val count = if (kind == Kind.TALISMAN) 18 else 12
                val angle = index * (MathUtils.PI2 / count.toFloat()) + elapsed * (1.7f + index % 3 * .12f)
                val radius = 55f + 255f * easeOutCubic(burstP) + (index % 3) * 16f
                val sx = centerX + MathUtils.cos(angle) * radius
                val sy = centerY + MathUtils.sin(angle) * radius
                val size = 18f + (index % 4) * 7f
                val alpha = (1f - p).coerceAtLeast(0f) * .88f
                batch.color = Color(accent.r, accent.g, accent.b, alpha)
                batch.draw(if (index % 3 == 0) star else spark, sx - size / 2f, sy - size / 2f, size, size)
            }
        }

        // Main reward art. Multiple bundle items fan out instead of becoming a line of text.
        if (icons.isNotEmpty()) {
            val enterP = (p / .30f).coerceIn(0f, 1f)
            val leaveP = ((p - .74f) / .26f).coerceIn(0f, 1f)
            val enter = if (reduceMotion) 1f else easeOutBack(enterP)
            val alpha = 1f - leaveP
            val baseSize = if (kind == Kind.TALISMAN || icons.size == 1) 210f else 132f
            val spread = if (icons.size <= 1) 0f else 122f
            icons.forEachIndexed { index, icon ->
                val centeredIndex = index - (icons.size - 1) / 2f
                val x = centerX + centeredIndex * spread
                val y = centerY + if (icons.size > 1) kotlin.math.abs(centeredIndex) * -18f else 0f
                val scale = (.58f + .42f * enter) * (1f + .025f * MathUtils.sin(elapsed * 10f + index))
                drawAspect(batch, icon, x, y, baseSize * scale, alpha)
            }
        }

        // A handful of copies fly toward the wallet/inventory after the burst, visually explaining
        // that the reward was actually stored rather than only acknowledged by text.
        val tx = targetX
        val ty = targetY
        if (tx != null && ty != null && icons.isNotEmpty()) {
            val flyP = ((p - .48f) / .42f).coerceIn(0f, 1f)
            if (flyP > 0f && flyP < 1f) {
                val e = easeInOutCubic(flyP)
                repeat(minOf(5, icons.size * 2)) { i ->
                    val icon = icons[i % icons.size]
                    val stagger = i * .055f
                    val local = ((flyP - stagger) / (1f - stagger).coerceAtLeast(.15f)).coerceIn(0f, 1f)
                    val le = easeInOutCubic(local)
                    val arc = MathUtils.sin(local * MathUtils.PI) * (95f + i * 10f)
                    val x = MathUtils.lerp(centerX + (i - 2) * 18f, tx, le)
                    val y = MathUtils.lerp(centerY, ty, le) + arc
                    drawAspect(batch, icon, x, y, MathUtils.lerp(64f, 26f, le), 1f - local * .35f)
                }
                batch.color = Color(accent.r, accent.g, accent.b, (1f - e) * .22f)
                val s = 110f + 80f * e
                batch.draw(glow, tx - s / 2f, ty - s / 2f, s, s)
            }
        }

        batch.color = Color.WHITE
    }

    private fun drawAspect(batch: SpriteBatch, region: TextureRegion, cx: Float, cy: Float, maxSize: Float, alpha: Float) {
        val rw = region.regionWidth.coerceAtLeast(1).toFloat()
        val rh = region.regionHeight.coerceAtLeast(1).toFloat()
        val scale = maxSize / maxOf(rw, rh)
        val w = rw * scale
        val h = rh * scale
        batch.color = Color(1f, 1f, 1f, alpha.coerceIn(0f, 1f))
        batch.draw(region, cx - w / 2f, cy - h / 2f, w, h)
    }

    private fun defaultAccent(kind: Kind): Color = when (kind) {
        Kind.PURCHASE -> ForgeUiPalette.primaryLight
        Kind.TALISMAN -> ForgeUiPalette.purple
        Kind.COSMETIC -> ForgeUiPalette.primaryLight
        Kind.DAILY -> ForgeUiPalette.gold
        Kind.UNLOCK -> ForgeUiPalette.successLight
    }

    private fun easeOutCubic(t: Float): Float = 1f - (1f - t) * (1f - t) * (1f - t)
    private fun easeInOutCubic(t: Float): Float {
        if (t < .5f) return 4f * t * t * t
        val x = -2f * t + 2f
        return 1f - (x * x * x) / 2f
    }
    private fun easeOutBack(t: Float): Float {
        val c1 = 1.70158f
        val c3 = c1 + 1f
        val x = t - 1f
        return 1f + c3 * x * x * x + c1 * x * x
    }
}
