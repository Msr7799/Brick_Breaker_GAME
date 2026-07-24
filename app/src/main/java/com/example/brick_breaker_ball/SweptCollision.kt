package com.example.brick_breaker_ball

import com.badlogic.gdx.math.Rectangle
import com.badlogic.gdx.math.Vector2
import kotlin.math.abs

data class CollisionResult(val time: Float, val normalX: Float, val normalY: Float, val targetId: Int = -1)

object SweptCollision {
    private const val EPSILON = 0.00001f

    fun circleVsAabb(origin: Vector2, delta: Vector2, radius: Float, box: Rectangle, targetId: Int = -1): CollisionResult? {
        val minX = box.x - radius; val maxX = box.x + box.width + radius
        val minY = box.y - radius; val maxY = box.y + box.height + radius
        var nearX = Float.NEGATIVE_INFINITY; var farX = Float.POSITIVE_INFINITY
        var nearY = Float.NEGATIVE_INFINITY; var farY = Float.POSITIVE_INFINITY
        if (abs(delta.x) < EPSILON) { if (origin.x !in minX..maxX) return null }
        else {
            nearX = (minX - origin.x) / delta.x; farX = (maxX - origin.x) / delta.x
            if (nearX > farX) { val t = nearX; nearX = farX; farX = t }
        }
        if (abs(delta.y) < EPSILON) { if (origin.y !in minY..maxY) return null }
        else {
            nearY = (minY - origin.y) / delta.y; farY = (maxY - origin.y) / delta.y
            if (nearY > farY) { val t = nearY; nearY = farY; farY = t }
        }
        val near = maxOf(nearX, nearY); val far = minOf(farX, farY)
        if (near > far || far < 0f || near !in 0f..1f) return null
        return if (nearX > nearY) CollisionResult(near, if (delta.x > 0f) -1f else 1f, 0f, targetId)
        else CollisionResult(near, 0f, if (delta.y > 0f) -1f else 1f, targetId)
    }
}
