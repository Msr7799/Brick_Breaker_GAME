package com.example.brick_breaker_ball.ui

import com.badlogic.gdx.graphics.Color
import com.badlogic.gdx.graphics.g2d.SpriteBatch
import com.badlogic.gdx.graphics.g2d.TextureRegion
import com.badlogic.gdx.math.Rectangle
import com.example.brick_breaker_ball.assets.GameAssets

/** Replayable pictorial lessons. Opening help never changes onboarding checkpoints. */
internal class VisualHelpOverlay {
    enum class Topic { GAME, ORBS, ITEMS, BALLS }
    var page = -1
        private set
    val active: Boolean get() = page >= 0
    private var topic = Topic.GAME
    private val animation = GuideAnimation()
    private var closing = false
    private var lessonLines = emptyList<String>()
    val focus = Rectangle()

    fun open(topic: Topic) {
        this.topic = topic
        page = 0
        closing = false
        animation.reset()
    }

    fun update(delta: Float, reduceMotion: Boolean) {
        if (!active) return
        animation.update(delta, reduceMotion)
        if (animation.exitFinished) {
            page = if (closing || page >= 3) -1 else page + 1
            animation.reset()
        }
    }

    fun tap(close: Boolean = false) {
        if (!active || animation.exiting || animation.elapsed < .15f) return
        if (close || animation.revealOrContinue(lessonLines)) {
            closing = close
            animation.beginExit()
        }
    }

    fun draw(batch: SpriteBatch, assets: GameAssets, screen: Rectangle, reduceMotion: Boolean,
             selectedIcon: TextureRegion? = null, effect: VisualGameUi.Icon = VisualGameUi.Icon.HIT,
             balls: Int = 0, damage: Int = 0, spreadAvailable: Boolean = true,
             emptyCollection: Boolean = false) {
        if (active) {
            val key = if (emptyCollection && topic == Topic.ORBS && page <= 1) "empty_$page"
                else "${topic.name.lowercase()}_$page"
            lessonLines = assets.guideHelpText.lines(key)
            drawLesson(batch, assets, screen, topic, page, 4, animation,
                focus.takeIf { it.width > 0f }, reduceMotion, selectedIcon, effect, balls, damage,
                spreadAvailable, emptyCollection, key)
        }
    }

    companion object {
        fun closeBounds(screen: Rectangle) = Rectangle(screen.x + screen.width - 104f,
            screen.y + screen.height - 170f, 76f, 76f)

        fun drawLesson(batch: SpriteBatch, assets: GameAssets, screen: Rectangle, topic: Topic,
                       page: Int, pages: Int, animation: GuideAnimation, focus: Rectangle?,
                       reduceMotion: Boolean, selectedIcon: TextureRegion? = null,
                       effect: VisualGameUi.Icon = VisualGameUi.Icon.HIT,
                       balls: Int = 0, damage: Int = 0, spreadAvailable: Boolean = true,
                       emptyCollection: Boolean = false, lessonKey: String? = null,
                       orbFamily: String? = null) {
            val alpha = animation.presence
            val target = focus?.let(animation::focusBounds)
            HeroGuideRenderer.drawSpotlight(batch, assets, screen, target, .75f * alpha)
            target?.let { HeroGuideRenderer.drawFocus(batch, assets, it, animation.elapsed, reduceMotion, alpha) }
            val width = minOf(screen.width - 40f, 980f)
            val height = minOf(390f, screen.height * .22f)
            val heroHeight = minOf(490f, screen.height * .27f)
            val heroWidth = minOf(width * .56f, heroHeight * 1.15f)
            val left = screen.x + (screen.width - width) / 2f
            val overlap = minOf(32f, heroHeight * .09f)
            val groupHeight = height + heroHeight - overlap
            val bottom = screen.y + 40f
            val top = screen.y + screen.height - 180f
            val below = target?.let { it.y - 24f - bottom } ?: Float.MAX_VALUE
            val above = target?.let { top - it.y - it.height - 24f } ?: 0f
            val desiredY = if (target == null) bottom else if (below >= above)
                target.y - 24f - groupHeight else target.y + target.height + 24f
            val y = desiredY.coerceIn(bottom, maxOf(bottom, top - groupHeight))
            val bubble = Rectangle(left, y, width, height)
            HeroGuideRenderer.drawCompanion(batch, assets,
                Rectangle(left + width * .03f - (1f - alpha) * 110f,
                    y + height - overlap + animation.bob, heroWidth, heroHeight),
                if (topic == Topic.ORBS || topic == Topic.ITEMS)
                    GuideSpeaker.ORB_SAGE else GuideSpeaker.DRAGON, alpha)
            VisualGameUi.panel(batch, assets, bubble, true, alpha)
            val key = lessonKey ?: "${topic.name.lowercase()}_$page"
            val textKeys = assets.guideHelpText.keys(key, orbFamily)
            val lines = textKeys.map(assets.guideHelpText::text)
            val visibleLines = animation.visibleLines(lines)
            listOf(.79f, .64f, .51f).forEachIndexed { index, row ->
                assets.guideHelpText.draw(batch, textKeys[index],
                    Rectangle(bubble.x + width * .07f, bubble.y + height * row,
                        width * .86f, height * if (index == 0) .15f else .12f),
                    visibleLines[index].length, alpha)
            }
            val cx = bubble.x + bubble.width * .10f
            val cy = bubble.y + bubble.height * .17f
            val side = minOf(bubble.width * .20f, bubble.height * .29f)
            fun icon(i: VisualGameUi.Icon, column: Int) {
                VisualGameUi.icon(batch, assets, i,
                    Rectangle(cx + column * bubble.width * .31f, cy, side, side), alpha)
            }
            fun arrow() {
                VisualGameUi.icon(batch, assets, VisualGameUi.Icon.NEXT,
                    Rectangle(cx + bubble.width * .245f, cy + side * .25f,
                        side * .30f, side * .50f), alpha)
            }
            when (topic) {
                Topic.GAME -> when (page) {
                    0 -> {
                        icon(VisualGameUi.Icon.STRAIGHT, 0); arrow(); icon(VisualGameUi.Icon.HIT, 1)
                        val t = if (reduceMotion) .6f else (animation.elapsed * .55f) % 1f
                        VisualGameUi.icon(batch, assets, VisualGameUi.Icon.BALLS,
                            Rectangle(cx + t * side * .26f, cy + t * side * .26f, side * .25f, side * .25f))
                    }
                    1 -> { icon(VisualGameUi.Icon.BALLS, 0); arrow(); icon(VisualGameUi.Icon.HIT, 1) }
                    2 -> { VisualGameUi.stats(batch, assets,
                        Rectangle(cx, cy + side * .20f, bubble.width * .79f, side * .60f), balls, damage) }
                    else -> { icon(VisualGameUi.Icon.SPEED, 0); icon(VisualGameUi.Icon.RECALL, 2) }
                }
                Topic.BALLS -> when (page) {
                    0 -> VisualGameUi.stats(batch, assets,
                        Rectangle(cx, cy + side * .20f, bubble.width * .79f, side * .60f), balls, damage)
                    1 -> { icon(VisualGameUi.Icon.BALLS, 0); arrow(); icon(VisualGameUi.Icon.HIT, 1)
                        icon(VisualGameUi.Icon.PLUS, 2) }
                    2 -> { icon(VisualGameUi.Icon.STRAIGHT, 0); icon(VisualGameUi.Icon.SPREAD, 2)
                        if (!spreadAvailable) VisualGameUi.icon(batch, assets, VisualGameUi.Icon.LOCK,
                            Rectangle(cx + bubble.width * .76f, cy, side * .42f, side * .42f)) }
                    else -> { icon(VisualGameUi.Icon.EQUIP, 0); arrow(); icon(VisualGameUi.Icon.CHECK, 1) }
                }
                Topic.ORBS, Topic.ITEMS -> {
                    val first = when (page) { 0 -> VisualGameUi.Icon.GRID
                        1 -> if (emptyCollection) VisualGameUi.Icon.COIN else VisualGameUi.Icon.EQUIP
                        2 -> VisualGameUi.Icon.BALLS; else -> VisualGameUi.Icon.DETAILS }
                    if (selectedIcon != null && page <= 2) {
                        batch.setColor(1f, 1f, 1f, alpha)
                        batch.draw(selectedIcon, cx, cy, side, side)
                        batch.color = Color.WHITE
                    } else icon(first, 0)
                    arrow()
                    icon(when (page) { 0 -> VisualGameUi.Icon.EQUIP
                        1 -> if (emptyCollection) VisualGameUi.Icon.GRID else VisualGameUi.Icon.CHECK
                        2 -> effect; else -> VisualGameUi.Icon.PLUS }, 1)
                    if (page == 2) icon(VisualGameUi.Icon.PLUS, 2)
                }
            }
            assets.guideHelpText.draw(batch, if (animation.textComplete(lines)) "next_0" else "reveal_0",
                Rectangle(bubble.x + width * .25f, bubble.y + height * .035f,
                    width * .50f, height * .09f), alpha = alpha)
            VisualGameUi.number(batch, assets, "${page + 1}/$pages",
                Rectangle(bubble.x + width * .06f, bubble.y + height * .035f,
                    width * .13f, height * .09f), .46f, Color(1f, .94f, .77f, alpha))
            batch.color = Color.WHITE
            VisualGameUi.icon(batch, assets, VisualGameUi.Icon.NEXT,
                Rectangle(bubble.x + bubble.width - 63f, bubble.y + 12f, 46f, 46f))
            val close = closeBounds(screen)
            VisualGameUi.panel(batch, assets, close)
            VisualGameUi.icon(batch, assets, VisualGameUi.Icon.CLOSE, close)
            batch.color = Color.WHITE
        }
    }
}
