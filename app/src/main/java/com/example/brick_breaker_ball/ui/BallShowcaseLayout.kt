package com.example.brick_breaker_ball.ui

import com.badlogic.gdx.math.Rectangle

/** One set of render and touch bounds, recalculated only when the usable viewport changes. */
internal class BallShowcaseLayout {
    val page = Rectangle()
    val header = Rectangle()
    val back = Rectangle()
    val help = Rectangle()
    val coins = Rectangle()
    val gems = Rectangle()
    val hero = Rectangle()
    val previous = Rectangle()
    val next = Rectangle()
    val status = Rectangle()
    val ball = Rectangle()
    val pedestal = Rectangle()
    val name = Rectangle()
    val identity = Rectangle()
    val position = Rectangle()
    val ability = Rectangle()
    val progression = Rectangle()
    val primary = Rectangle()
    val straight = Rectangle()
    val spread = Rectangle()
    val tabs = Array(4) { Rectangle() }
    val stats = Array(4) { Rectangle() }
    val statsRegion = Rectangle()
    val modesRegion = Rectangle()
    var scale = 1f
        private set
    var wide = false
        private set
    private var lastLeft = Float.NaN
    private var lastBottom = Float.NaN
    private var lastWidth = Float.NaN
    private var lastHeight = Float.NaN

    fun update(left: Float, bottom: Float, width: Float, height: Float) {
        if (left == lastLeft && bottom == lastBottom && width == lastWidth && height == lastHeight) return
        lastLeft = left; lastBottom = bottom; lastWidth = width; lastHeight = height
        wide = width / height.coerceAtLeast(1f) >= 1.12f
        val designWidth = if (wide) 1440f else 860f
        val designHeight = if (wide) 900f else 1552f
        scale = minOf(width / designWidth, height / designHeight).coerceAtLeast(.01f)
        val x = left + (width - designWidth * scale) / 2f
        val y = bottom + (height - designHeight * scale) / 2f
        fun place(rect: Rectangle, px: Float, py: Float, w: Float, h: Float) {
            rect.set(x + px * scale, y + py * scale, w * scale, h * scale)
        }
        place(page, 0f, 0f, designWidth, designHeight)
        if (wide) {
            place(header, 112f, 806f, 550f, 66f)
            place(back, 24f, 806f, 72f, 66f)
            place(help, 1344f, 806f, 72f, 66f)
            place(coins, 868f, 806f, 214f, 66f)
            place(gems, 1098f, 806f, 214f, 66f)
            tabs.forEachIndexed { i, rect -> place(rect, 24f + i * 352f, 734f, 336f, 60f) }
            place(hero, 24f, 24f, 664f, 690f)
            place(ability, 712f, 334f, 704f, 118f)
            place(progression, 712f, 238f, 704f, 80f)
            place(primary, 712f, 126f, 704f, 96f)
            place(straight, 712f, 24f, 344f, 86f)
            place(spread, 1072f, 24f, 344f, 86f)
            stats.forEachIndexed { i, rect ->
                place(rect, 712f + (i % 2) * 360f, 596f - (i / 2) * 134f, 344f, 118f)
            }
        } else {
            place(header, 112f, 1468f, 632f, 64f)
            place(back, 20f, 1468f, 76f, 64f)
            place(help, 764f, 1468f, 76f, 64f)
            place(coins, 20f, 1398f, 402f, 56f)
            place(gems, 438f, 1398f, 402f, 56f)
            tabs.forEachIndexed { i, rect -> place(rect, 20f + i * 209f, 1316f, 193f, 64f) }
            place(hero, 20f, 742f, 820f, 554f)
            place(ability, 20f, 370f, 820f, 132f)
            place(progression, 20f, 248f, 820f, 106f)
            place(primary, 20f, 128f, 820f, 104f)
            place(straight, 20f, 20f, 402f, 92f)
            place(spread, 438f, 20f, 402f, 92f)
            stats.forEachIndexed { i, rect -> place(rect, 20f + i * 209f, 518f, 193f, 208f) }
        }
        // The stage remains large on both layouts; all controls use the exact same bounds for input.
        val s = scale
        previous.set(hero.x + 12f * s, hero.y + hero.height * .42f, 76f * s, 112f * s)
        next.set(hero.x + hero.width - 88f * s, previous.y, previous.width, previous.height)
        status.set(hero.x + hero.width / 2f - 142f * s, hero.y + hero.height - 70f * s, 284f * s, 46f * s)
        val diameter = if (wide) 326f else 310f
        ball.set(hero.x + (hero.width - diameter * s) / 2f,
            hero.y + hero.height * .49f - diameter * s / 2f + 40f * s, diameter * s, diameter * s)
        pedestal.set(hero.x + hero.width / 2f - 224f * s, ball.y - 42f * s, 448f * s, 112f * s)
        name.set(hero.x + 38f * s, hero.y + 76f * s, hero.width - 76f * s, 70f * s)
        identity.set(name.x, hero.y + 45f * s, name.width, 30f * s)
        position.set(name.x, hero.y + 12f * s, name.width, 28f * s)
        val statLeft = stats.minOf { it.x }
        val statBottom = stats.minOf { it.y }
        statsRegion.set(statLeft, statBottom, stats.maxOf { it.x + it.width } - statLeft,
            stats.maxOf { it.y + it.height } - statBottom)
        modesRegion.set(straight.x, straight.y, spread.x + spread.width - straight.x, straight.height)
    }

    fun contains(rect: Rectangle, x: Float, y: Float): Boolean {
        // Wide layouts already have large physical controls. Portrait arrows need extra width too.
        val minimum = (if (wide) 60f else 112f) * scale
        val extraX = ((minimum - rect.width) / 2f).coerceAtLeast(0f)
        val extraY = ((minimum - rect.height) / 2f).coerceAtLeast(0f)
        return x >= rect.x - extraX && x <= rect.x + rect.width + extraX &&
            y >= rect.y - extraY && y <= rect.y + rect.height + extraY
    }
}
