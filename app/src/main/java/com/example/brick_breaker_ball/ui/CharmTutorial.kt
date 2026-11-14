/*
 * المسار: app/src/main/java/com/example/brick_breaker_ball/ui/CharmTutorial.kt
 * الوظيفة: شرح تأثيرات عناصر Volley برسوم متحركة.
 * الحسابات: مواضع الرسم والزمن الدوري للحركة.
 */

package com.example.brick_breaker_ball.ui

import com.badlogic.gdx.graphics.Color
import com.badlogic.gdx.graphics.g2d.BitmapFont
import com.badlogic.gdx.graphics.g2d.GlyphLayout
import com.badlogic.gdx.graphics.g2d.SpriteBatch
import com.badlogic.gdx.graphics.g2d.TextureRegion
import com.badlogic.gdx.math.MathUtils
import com.badlogic.gdx.math.Matrix4
import com.badlogic.gdx.math.Rectangle
import com.badlogic.gdx.utils.Align
import com.example.brick_breaker_ball.assets.GameAssets
import com.example.brick_breaker_ball.assets.VolleyAssetCatalog
import com.example.brick_breaker_ball.cosmetics.VolleyBallSelection
import com.example.brick_breaker_ball.gameplay.PowerUpCatalog
import com.example.brick_breaker_ball.gameplay.PowerUpCategory
import com.example.brick_breaker_ball.gameplay.PowerUpType
import com.example.brick_breaker_ball.gameplay.VolleyItemDescriptions

enum class CharmDemoKind {
    BALLS,
    DUAL_LAUNCH,
    LASER,
    WARD,
    LAUNCH_PATTERN,
    BRICKS,
    SPEED,
    DANGER,
    PASS_THROUGH,
    BALL_SIZE,
    MAGNET,
    SCORE,
    WARP,
    RANDOM,
    FALLING,
    CONTROL,
    JAM
}
enum class CharmLessonStatus { ACTIVATED, STORED, UNAVAILABLE, PREVIEW }

/** A lesson blocks simulation until the player dismisses it. Multiple same-frame rewards queue. */
class CharmTutorial {
    private data class Lesson(val type: PowerUpType, val status: CharmLessonStatus)
    private val pending = ArrayDeque<Lesson>()
    var elapsed = 0f
        private set
    val type: PowerUpType? get() = pending.firstOrNull()?.type
    val status: CharmLessonStatus? get() = pending.firstOrNull()?.status
    val active: Boolean get() = pending.isNotEmpty()

    fun show(type: PowerUpType, status: CharmLessonStatus = CharmLessonStatus.ACTIVATED) {
        pending.addLast(Lesson(type, status))
    }
    fun animate(delta: Float) {
        if (active) elapsed += delta.coerceIn(0f, .05f)
    }

    /** Returns true only when the final lesson was dismissed. */
    fun skip(): Boolean {
        if (pending.isEmpty()) return false
        pending.removeFirst()
        elapsed = 0f
        return pending.isEmpty()
    }

    companion object {
        fun demo(type: PowerUpType): CharmDemoKind = when (type) {
            PowerUpType.DUAL_PADDLE -> CharmDemoKind.DUAL_LAUNCH

            PowerUpType.MULTIBALL_PLUS_4, PowerUpType.MULTIBALL_15, PowerUpType.MULTI_BALL,
            PowerUpType.TRIPLE_BALL, PowerUpType.EIGHT_BALL -> CharmDemoKind.BALLS

            PowerUpType.LASER_AUTO_CHARGE, PowerUpType.LASER_PADDLE -> CharmDemoKind.LASER

            PowerUpType.EXTRA_LIFE, PowerUpType.BOTTOM_SHIELD, PowerUpType.PADDLE_SHIELD -> CharmDemoKind.WARD

            PowerUpType.EXPAND_PADDLE, PowerUpType.SHRINK_PADDLE, PowerUpType.SUPER_SHRINK,
            PowerUpType.STICKY_PADDLE -> CharmDemoKind.LAUNCH_PATTERN

            PowerUpType.MAGNETIC_PADDLE, PowerUpType.POWERUP_MAGNET -> CharmDemoKind.MAGNET

            PowerUpType.FAST_BALL, PowerUpType.SLOW_BALL -> CharmDemoKind.SPEED

            PowerUpType.PIERCING_BALL, PowerUpType.GHOST_BALL -> CharmDemoKind.PASS_THROUGH

            PowerUpType.SHRINK_BALL, PowerUpType.MEGA_BALL -> CharmDemoKind.BALL_SIZE

            PowerUpType.SCORE_X2, PowerUpType.SCORE_X3 -> CharmDemoKind.SCORE

            PowerUpType.LEVEL_WARP -> CharmDemoKind.WARP

            PowerUpType.RANDOM_GOOD -> CharmDemoKind.RANDOM

            PowerUpType.FALLING_BRICKS -> CharmDemoKind.FALLING

            PowerUpType.POWERUP_JAM -> CharmDemoKind.JAM

            PowerUpType.INVERT_CONTROLS, PowerUpType.SLIPPERY_PADDLE -> CharmDemoKind.CONTROL

            PowerUpType.KILL_PADDLE, PowerUpType.INSTANT_KILL_BALL,
            PowerUpType.RANDOM_BAD -> CharmDemoKind.DANGER

            else -> CharmDemoKind.BRICKS
        }
    }
}

/** Draws a short looping explanation with the game's existing art. No extra texture is loaded. */
object CharmTutorialRenderer {
    private const val BOARD_ASPECT = 560f / 977f
    private val textLayout = GlyphLayout()

    private fun boardRect(visibleLeft: Float, visibleBottom: Float, visibleWidth: Float, visibleHeight: Float): Rectangle {
        val height = minOf(visibleHeight * .84f, visibleWidth * .84f / BOARD_ASPECT)
        val width = height * BOARD_ASPECT
        return Rectangle(
            visibleLeft + (visibleWidth - width) * .5f,
            visibleBottom + (visibleHeight - height) * .5f,
            width,
            height
        )
    }

    fun skipButton(visibleLeft: Float, visibleBottom: Float, visibleWidth: Float, visibleHeight: Float): Rectangle {
        val board = boardRect(visibleLeft, visibleBottom, visibleWidth, visibleHeight)
        return Rectangle(
            board.x + board.width * .33f,
            board.y + board.height * .07f,
            board.width * .34f,
            board.height * .075f
        )
    }

    fun draw(
        batch: SpriteBatch,
        assets: GameAssets,
        type: PowerUpType,
        status: CharmLessonStatus,
        elapsed: Float,
        reduceMotion: Boolean,
        visibleLeft: Float,
        visibleBottom: Float,
        visibleWidth: Float,
        visibleHeight: Float
    ): Rectangle {
        val solid = assets.solidUiRegion
        // الشاشة الحالية تعرض عناصر Volley فقط؛ لا نعتمد على قاموس Classic.
        val info = VolleyItemDescriptions.info(type)
        // شرح العناصر يستخدم أيقونات Volley حتى لا يحمّل أطلس Classic المخفي.
        val icon = VolleyAssetCatalog(assets.volley).itemIcon(type)
        val centerX = visibleLeft + visibleWidth / 2f
        val board = boardRect(visibleLeft, visibleBottom, visibleWidth, visibleHeight)
        val titleArea = assets.boards2.placement("scroll_board", "scroll_board_title_area", board)
        val content = assets.boards2.placement("scroll_board", "scroll_board_content_area", board)
        val skip = skipButton(visibleLeft, visibleBottom, visibleWidth, visibleHeight)

        batch.color = Color(0f, 0f, 0f, .78f)
        batch.draw(solid, visibleLeft, visibleBottom, visibleWidth, visibleHeight)
        batch.color = Color.WHITE
        batch.draw(assets.boards2.requireRegion("scroll_board"), board.x, board.y, board.width, board.height)
        drawFittedLine(
            batch,
            assets.pauseTitleFont,
            info.fullName,
            titleArea.x + 10f,
            titleArea.y + titleArea.height * .69f,
            titleArea.width - 20f,
            1f
        )
        val statusText = when (status) {
            CharmLessonStatus.ACTIVATED -> "ITEM USED"
            CharmLessonStatus.STORED -> "ADDED TO ITEMS - ARM BETWEEN VOLLEYS"
            CharmLessonStatus.UNAVAILABLE -> "NO EFFECT THIS TIME"
            CharmLessonStatus.PREVIEW -> "HOW THIS ITEM WORKS"
        }
        drawFittedLine(
            batch,
            assets.buttonFont,
            statusText,
            content.x + 25f,
            content.y + content.height * .95f,
            content.width - 50f,
            .78f
        )
        val pulse = if (reduceMotion) 1f else 1f + MathUtils.sin(elapsed * 5f) * .06f
        val iconSize = minOf(content.width * .25f, content.height * .19f) * pulse
        batch.draw(
            icon,
            centerX - iconSize / 2f,
            content.y + content.height * .65f - (iconSize - minOf(content.width * .25f, content.height * .19f)) / 2f,
            iconSize,
            iconSize
        )
        drawWrappedDescription(
            batch,
            assets.smallFont,
            info.description,
            content.x + 35f,
            content.y + content.height * .55f,
            content.width - 70f,
            content.height * .16f
        )

        val demoCenterY = content.y + content.height * .17f
        val demoScale = minOf(1f, (content.width - 24f) / 570f, content.height * .32f / 220f)
        val originalTransform = Matrix4(batch.transformMatrix)
        try {
            if (demoScale < 1f) {
                batch.transformMatrix = Matrix4(originalTransform)
                    .translate(centerX, demoCenterY, 0f)
                    .scale(demoScale, demoScale, 1f)
                    .translate(-centerX, -demoCenterY, 0f)
            }
            drawDemo(batch, assets, solid, type, elapsed, reduceMotion, centerX, demoCenterY)
        } finally {
            if (demoScale < 1f) batch.transformMatrix = originalTransform
        }
        batch.draw(assets.boards2.requireRegion("ui_button_red_blank"), skip.x, skip.y, skip.width, skip.height)
        drawFittedLine(
            batch,
            assets.buttonFont,
            "SKIP",
            skip.x + 8f,
            skip.y + skip.height * .68f,
            skip.width - 16f,
            .82f
        )
        batch.color = Color.WHITE
        return skip
    }

    private fun drawFittedLine(
        batch: SpriteBatch,
        font: BitmapFont,
        text: String,
        x: Float,
        baseline: Float,
        width: Float,
        maxScale: Float
    ) {
        val oldX = font.data.scaleX
        val oldY = font.data.scaleY
        val oldColor = Color(font.color)
        font.color = Color.WHITE
        font.data.setScale(maxScale)
        textLayout.setText(font, text)
        if (textLayout.width > width && textLayout.width > 0f) {
            font.data.setScale(maxScale * width / textLayout.width)
        }
        font.draw(batch, text, x, baseline, width, Align.center, false)
        font.data.setScale(oldX, oldY)
        font.color = oldColor
    }

    private fun drawWrappedDescription(
        batch: SpriteBatch,
        font: BitmapFont,
        text: String,
        x: Float,
        top: Float,
        width: Float,
        maxHeight: Float
    ) {
        val oldX = font.data.scaleX
        val oldY = font.data.scaleY
        val oldColor = Color(font.color)
        font.color = Color.WHITE
        var scale = .78f
        font.data.setScale(scale)
        textLayout.setText(font, text, Color.WHITE, width, Align.center, true)
        if (textLayout.height > maxHeight && textLayout.height > 0f) {
            scale *= maxHeight / textLayout.height
            font.data.setScale(scale)
        }
        font.draw(batch, text, x, top, width, Align.center, true)
        font.data.setScale(oldX, oldY)
        font.color = oldColor
    }

    private fun drawDemo(
        batch: SpriteBatch,
        assets: GameAssets,
        panel: TextureRegion,
        type: PowerUpType,
        elapsed: Float,
        reduceMotion: Boolean,
        cx: Float,
        cy: Float
    ) {
        val kind = CharmTutorial.demo(type)
        val t = if (reduceMotion) .65f else (elapsed * .7f) % 1f
        val ball = assets.cosmetics.requireBallRegion(VolleyBallSelection.STARTER_GROUP, VolleyBallSelection.STARTER_SPRITE)
        val hazardous = PowerUpCatalog.definitions[type]?.category == PowerUpCategory.BAD
        val accent = if (hazardous) Color(.95f, .2f, .24f, .95f) else Color(.18f, .86f, 1f, .95f)
        batch.color = Color(ForgeUiPalette.neutralBlack).also { it.a = .62f }
        batch.draw(panel, cx - 285f, cy - 90f, 570f, 220f)
        batch.color = accent
        when (kind) {
            CharmDemoKind.BALLS -> {
                val count = if (t < .42f) 1 else 4
                repeat(count) { i -> batch.draw(ball, cx - 21f + (i - (count - 1) / 2f) * 76f, cy + 10f + (i % 2) * 22f, 42f, 42f) }
            }

            CharmDemoKind.DUAL_LAUNCH -> {
                repeat(2) { i ->
                    val x = cx + if (i == 0) -125f else 125f
                    batch.draw(panel, x - 45f, cy - 57f, 90f, 12f)
                    batch.draw(ball, x - 20f, cy - 25f + t * 120f, 40f, 40f)
                }
            }

            CharmDemoKind.LASER -> {
                repeat(2) { i -> batch.draw(panel, cx + if (i == 0) -88f else 76f, cy - 48f, 12f, 70f + t * 85f) }
                batch.draw(panel, cx - 120f, cy - 60f, 240f, 16f)
            }

            CharmDemoKind.WARD -> {
                batch.draw(panel, cx - 135f, cy - 32f, 270f, 16f)
                batch.draw(ball, cx - 24f, cy + 8f + t * 35f, 48f, 48f)
            }

            CharmDemoKind.LAUNCH_PATTERN -> {
                // شرح عناصر Volley من نقاط الإطلاق ومسار الكرة، من دون رسم بادل Classic.
                when (type) {
                    PowerUpType.EXPAND_PADDLE -> repeat(3) { lane ->
                        val laneX = cx + (lane - 1) * 115f
                        batch.draw(ball, laneX - 21f, cy - 30f + t * 115f, 42f, 42f)
                    }

                    PowerUpType.STICKY_PADDLE -> {
                        batch.draw(panel, cx - 6f, cy - 52f, 12f, 28f)
                        batch.draw(ball, cx - 21f, cy - 22f + (1f - kotlin.math.abs(t * 2f - 1f)) * 120f, 42f, 42f)
                    }

                    else -> {
                        val guideWidth = 180f - t * 115f
                        batch.draw(panel, cx - guideWidth / 2f, cy - 48f, guideWidth, 5f)
                        batch.draw(ball, cx - 21f, cy - 28f + t * 105f, 42f, 42f)
                    }
                }
            }

            CharmDemoKind.SPEED -> batch.draw(ball, cx - 220f + t * 400f, cy + 5f, 45f, 45f)

            CharmDemoKind.PASS_THROUGH -> {
                batch.draw(panel, cx - 36f, cy + 45f, 72f, 48f)
                batch.draw(ball, cx - 200f + t * 360f, cy + 46f, 42f, 42f)
            }

            CharmDemoKind.BALL_SIZE -> {
                val shrinking = type == PowerUpType.SHRINK_BALL
                val size = if (shrinking) 80f - t * 45f else 40f + t * 55f
                batch.draw(ball, cx - size / 2f, cy + 12f - size / 2f, size, size)
            }

            CharmDemoKind.MAGNET -> {
                batch.draw(panel, cx - 100f, cy - 48f, 200f, 20f)
                val pulled = if (type == PowerUpType.POWERUP_MAGNET) {
                    VolleyAssetCatalog(assets.volley).itemIcon(PowerUpType.FIRE_BALL)
                } else {
                    ball
                }
                batch.draw(pulled, cx - 205f + t * 180f, cy + 58f - t * 70f, 42f, 42f)
            }

            CharmDemoKind.SCORE -> {
                batch.draw(ball, cx - 145f + t * 120f, cy + 5f, 42f, 42f)
                assets.hudValueFont.draw(batch, if (type == PowerUpType.SCORE_X3) "x3" else "x2", cx + 60f, cy + 63f)
            }

            CharmDemoKind.WARP -> {
                batch.draw(ball, cx - 190f + t * 380f, cy + 8f, 44f, 44f)
                assets.hudValueFont.draw(batch, "NEXT", cx - 82f, cy + 112f)
            }

            CharmDemoKind.RANDOM -> {
                val icons = listOf(PowerUpType.FIRE_BALL, PowerUpType.EXTRA_LIFE, PowerUpType.EXPAND_PADDLE)
                val shown = icons[((elapsed * 2f).toInt() % icons.size).coerceAtLeast(0)]
                val art = VolleyAssetCatalog(assets.volley).itemIcon(shown)
                batch.draw(art, cx - 42f, cy - 10f, 84f, 84f)
            }

            CharmDemoKind.FALLING -> repeat(3) { i ->
                batch.draw(panel, cx - 120f + i * 82f, cy + 75f - t * 90f, 70f, 44f)
            }

            CharmDemoKind.CONTROL -> {
                val launchX = cx + MathUtils.sin(t * MathUtils.PI2) * 115f
                batch.draw(ball, launchX - 21f, cy - 28f, 42f, 42f)
                assets.smallFont.draw(batch, if (type == PowerUpType.INVERT_CONTROLS) "REVERSED" else "SLIPPERY", cx - 230f, cy + 74f, 460f, Align.center, false)
            }

            CharmDemoKind.JAM -> {
                assets.hudValueFont.draw(batch, "BLOCKED", cx - 210f, cy + 80f, 420f, Align.center, false)
            }

            CharmDemoKind.DANGER -> {
                batch.draw(ball, cx - 21f, cy + 4f - t * 44f, 42f, 42f)
            }

            CharmDemoKind.BRICKS -> {
                repeat(3) { i ->
                    if (i != 2 || t < .68f) batch.draw(panel, cx - 112f + i * 78f, cy + 56f, 66f, 45f)
                }
                batch.draw(ball, cx - 20f + t * 85f, cy - 28f + t * 120f, 40f, 40f)
            }
        }
        batch.color = Color.WHITE
    }
}
