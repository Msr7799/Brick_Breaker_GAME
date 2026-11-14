/*
 * المسار: app/src/main/java/com/example/brick_breaker_ball/gameplay/SweptCollision.kt
 * الوظيفة: كشف اصطدام الكرة أثناء الحركة.
 * الحسابات: تقاطع المسار المستمر مع المستطيل والحواف.
 */

package com.example.brick_breaker_ball.gameplay

import com.badlogic.gdx.math.Rectangle
import com.badlogic.gdx.math.Vector2
import kotlin.math.abs
import kotlin.math.sqrt

data class CollisionResult(val time: Float, val normalX: Float, val normalY: Float, val targetId: Int = -1)

object SweptCollision {
    private const val EPSILON = 0.00001f

    /** ملاحظة صيانة: الدالة `circleVsAabb` تنفّذ مسؤولية محلية يعتمد عليها هذا الجزء من اللعبة؛ راجع استدعاءاتها واختباراتها قبل تعديلها. */
    fun circleVsAabb(origin: Vector2, delta: Vector2, radius: Float, box: Rectangle, targetId: Int = -1): CollisionResult? {
        val minX = box.x - radius
        val maxX = box.x + box.width + radius
        val minY = box.y - radius
        val maxY = box.y + box.height + radius
        var nearX = Float.NEGATIVE_INFINITY
        var farX = Float.POSITIVE_INFINITY
        var nearY = Float.NEGATIVE_INFINITY
        var farY = Float.POSITIVE_INFINITY
        if (abs(delta.x) < EPSILON) {
            if (origin.x !in minX..maxX) return null
        } else {
            nearX = (minX - origin.x) / delta.x
            farX = (maxX - origin.x) / delta.x
            if (nearX > farX) {
                val t = nearX
                nearX = farX
                farX = t
            }
        }
        if (abs(delta.y) < EPSILON) {
            if (origin.y !in minY..maxY) return null
        } else {
            nearY = (minY - origin.y) / delta.y
            farY = (maxY - origin.y) / delta.y
            if (nearY > farY) {
                val t = nearY
                nearY = farY
                farY = t
            }
        }
        val near = maxOf(nearX, nearY)
        val far = minOf(farX, farY)
        if (near > far || far < 0f || near !in 0f..1f) return null
        return if (nearX > nearY) {
            CollisionResult(near, if (delta.x > 0f) -1f else 1f, 0f, targetId)
        } else {
            CollisionResult(near, 0f, if (delta.y > 0f) -1f else 1f, targetId)
        }
    }

    /** Sweeps a circle against a finite thick segment (a capsule) and returns the earliest hit. */
    fun circleVsSegment(
        origin: Vector2,
        delta: Vector2,
        radius: Float,
        x1: Float,
        y1: Float,
        x2: Float,
        y2: Float,
        thickness: Float,
        targetId: Int = -1
    ): CollisionResult? {
        val capsuleRadius = radius + thickness * .5f
        val sx = x2 - x1
        val sy = y2 - y1
        val segmentLength2 = sx * sx + sy * sy
        if (segmentLength2 <= EPSILON) {
            return movingPointVsCircle(origin, delta, x1, y1, capsuleRadius, targetId)
        }

        val segmentLength = sqrt(segmentLength2)
        val nx = -sy / segmentLength
        val ny = sx / segmentLength
        val startDistance = (origin.x - x1) * nx + (origin.y - y1) * ny
        val distanceVelocity = delta.x * nx + delta.y * ny
        var best: CollisionResult? = null

        fun consider(candidate: CollisionResult?) {
            if (candidate != null && (best == null || candidate.time < best!!.time)) best = candidate
        }

        if (abs(distanceVelocity) > EPSILON) {
            for (side in floatArrayOf(-capsuleRadius, capsuleRadius)) {
                val time = (side - startDistance) / distanceVelocity
                if (time !in 0f..1f) continue
                val hitX = origin.x + delta.x * time
                val hitY = origin.y + delta.y * time
                val projection = ((hitX - x1) * sx + (hitY - y1) * sy) / segmentLength2
                if (projection in 0f..1f) {
                    val normalSign = if (side < 0f) -1f else 1f
                    consider(CollisionResult(time, nx * normalSign, ny * normalSign, targetId))
                }
            }
        } else if (abs(startDistance) <= capsuleRadius) {
            val projection = ((origin.x - x1) * sx + (origin.y - y1) * sy) / segmentLength2
            if (projection in 0f..1f) {
                val sign = if (startDistance < 0f) -1f else 1f
                consider(CollisionResult(0f, nx * sign, ny * sign, targetId))
            }
        }

        consider(movingPointVsCircle(origin, delta, x1, y1, capsuleRadius, targetId))
        consider(movingPointVsCircle(origin, delta, x2, y2, capsuleRadius, targetId))
        return best
    }

    private fun movingPointVsCircle(
        origin: Vector2,
        delta: Vector2,
        centerX: Float,
        centerY: Float,
        radius: Float,
        targetId: Int
    ): CollisionResult? {
        val ox = origin.x - centerX
        val oy = origin.y - centerY
        val a = delta.x * delta.x + delta.y * delta.y
        val c = ox * ox + oy * oy - radius * radius
        if (c <= 0f) {
            val length = sqrt(ox * ox + oy * oy).coerceAtLeast(EPSILON)
            return CollisionResult(0f, ox / length, oy / length, targetId)
        }
        if (a <= EPSILON) return null
        val b = 2f * (ox * delta.x + oy * delta.y)
        val discriminant = b * b - 4f * a * c
        if (discriminant < 0f) return null
        val time = (-b - sqrt(discriminant)) / (2f * a)
        if (time !in 0f..1f) return null
        val hitX = origin.x + delta.x * time - centerX
        val hitY = origin.y + delta.y * time - centerY
        val length = sqrt(hitX * hitX + hitY * hitY).coerceAtLeast(EPSILON)
        return CollisionResult(time, hitX / length, hitY / length, targetId)
    }
}
