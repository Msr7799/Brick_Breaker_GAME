/*
 * ملاحظات صيانة الملف:
 * المسار: app/src/test/java/com/example/brick_breaker_ball/PuzzleBrickBehaviorTest.kt
 * المؤلف: mohamed alromaihi
 * الدوال الموجودة: `keyUnlocksMatchingLockedGroupAndLockedRejectsDamageBeforeKey`، `switchTogglesLinkedGhostToNonCollidableAndDoesNotBlockCompletion`، `chainGroupPropagatesDamageOnceWithoutTouchingOtherGroup`، `bossCoreUsesConfiguredHealthAndShowsRealDamageState`، `movingOriginsAndRegenerationRemainSimulationOwned`، `session`، `definition`، `hit`
 */

package com.example.brick_breaker_ball

import com.badlogic.gdx.math.Vector2
import org.junit.Assert.*
import org.junit.Test

class PuzzleBrickBehaviorTest {
    /** ملاحظة صيانة: الدالة `keyUnlocksMatchingLockedGroupAndLockedRejectsDamageBeforeKey` توثّق حالة اختبار أو تهيئة آلية وتحمي السلوك المتوقع من التراجع؛ راجع استدعاءاتها واختباراتها قبل تعديلها. */
    @Test fun keyUnlocksMatchingLockedGroupAndLockedRejectsDamageBeforeKey() {
        val game = session("KL.......", mapOf("0:0" to 2, "0:1" to 2))
        val locked = game.bricks.single { it.type == BrickType.LOCKED }
        val original = locked.health
        hit(game, locked)
        assertEquals(original, locked.health)
        assertTrue(locked.locked)
        hit(game, game.bricks.single { it.type == BrickType.KEY_BRICK })
        assertFalse(locked.locked)
        hit(game, locked)
        assertEquals(1, locked.health)
        hit(game, locked)
        assertTrue(game.bricks.none { it.id == locked.id })
    }

    /** ملاحظة صيانة: الدالة `switchTogglesLinkedGhostToNonCollidableAndDoesNotBlockCompletion` توثّق حالة اختبار أو تهيئة آلية وتحمي السلوك المتوقع من التراجع؛ راجع استدعاءاتها واختباراتها قبل تعديلها. */
    @Test fun switchTogglesLinkedGhostToNonCollidableAndDoesNotBlockCompletion() {
        val game = session("SO.......", mapOf("0:0" to 4, "0:1" to 4))
        val ghost = game.bricks.single { it.type == BrickType.GHOST }
        assertTrue(ghost.ghostVisible)
        hit(game, game.bricks.single { it.type == BrickType.SWITCH })
        assertFalse(ghost.ghostVisible)
        game.update(1f / 120f)
        assertEquals(GamePhase.LEVEL_COMPLETE, game.phase)
    }

    /** ملاحظة صيانة: الدالة `chainGroupPropagatesDamageOnceWithoutTouchingOtherGroup` توثّق حالة اختبار أو تهيئة آلية وتحمي السلوك المتوقع من التراجع؛ راجع استدعاءاتها واختباراتها قبل تعديلها. */
    @Test fun chainGroupPropagatesDamageOnceWithoutTouchingOtherGroup() {
        val game = session("CCC......", mapOf("0:0" to 1, "0:1" to 1, "0:2" to 2))
        hit(game, game.bricks.first { it.type == BrickType.CHAIN_BRICK && it.groupId == 1 })
        assertEquals(1, game.bricks.count { it.type == BrickType.CHAIN_BRICK })
        assertEquals(2, game.bricks.single().groupId)
    }

    /** ملاحظة صيانة: الدالة `bossCoreUsesConfiguredHealthAndShowsRealDamageState` توثّق حالة اختبار أو تهيئة آلية وتحمي السلوك المتوقع من التراجع؛ راجع استدعاءاتها واختباراتها قبل تعديلها. */
    @Test fun bossCoreUsesConfiguredHealthAndShowsRealDamageState() {
        val level = definition("B........").copy(bossHealth = 9, modifiers = setOf("BOSS"))
        val game = GameSession(level = level)
        val boss = game.bricks.single()
        assertEquals(9, boss.health)
        hit(game, boss)
        assertEquals(8, boss.health)
        assertNotEquals(DamageStage.INTACT, boss.damageStage)
    }

    /** ملاحظة صيانة: الدالة `movingOriginsAndRegenerationRemainSimulationOwned` توثّق حالة اختبار أو تهيئة آلية وتحمي السلوك المتوقع من التراجع؛ راجع استدعاءاتها واختباراتها قبل تعديلها. */
    @Test fun movingOriginsAndRegenerationRemainSimulationOwned() {
        val game = session("HR.......")
        val moving = game.bricks.single { it.type == BrickType.MOVING_HORIZONTAL }
        val origin = moving.originX
        val regen = game.bricks.single { it.type == BrickType.REGENERATING }
        regen.health = 1
        regen.age = 7.9f
        game.update(.2f)
        assertEquals(origin, moving.originX, .001f)
        assertNotEquals(origin, moving.bounds.x, .001f)
        assertEquals(2, regen.health)
    }

    /** ملاحظة صيانة: الدالة `session` توثّق حالة اختبار أو تهيئة آلية وتحمي السلوك المتوقع من التراجع؛ راجع استدعاءاتها واختباراتها قبل تعديلها. */
    private fun session(firstRow: String, groups: Map<String, Int> = emptyMap()): GameSession = GameSession(level = definition(firstRow, groups)).apply { action() }

    /** ملاحظة صيانة: الدالة `definition` توثّق حالة اختبار أو تهيئة آلية وتحمي السلوك المتوقع من التراجع؛ راجع استدعاءاتها واختباراتها قبل تعديلها. */
    private fun definition(firstRow: String, groups: Map<String, Int> = emptyMap()) = LevelDefinition(-1, 1, "CUSTOM", 8, 9, listOf(firstRow) + List(7) { "........." }, 600f, 3, 5000, customId = "behavior", brickGroups = groups)

    /** ملاحظة صيانة: الدالة `hit` توثّق حالة اختبار أو تهيئة آلية وتحمي السلوك المتوقع من التراجع؛ راجع استدعاءاتها واختباراتها قبل تعديلها. */
    private fun hit(game: GameSession, target: Brick) {
        game.phase = GamePhase.PLAYING
        game.ball.position.set(target.bounds.x + target.bounds.width / 2f, target.bounds.y - 70f)
        game.ball.previousPosition.set(game.ball.position)
        game.ball.velocity.set(0f, 800f)
        game.ball.baseSpeed = 800f
        game.update(.13f)
    }
}
