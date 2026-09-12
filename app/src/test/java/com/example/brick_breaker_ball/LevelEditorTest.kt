/*
 * ملاحظات صيانة الملف:
 * المسار: app/src/test/java/com/example/brick_breaker_ball/LevelEditorTest.kt
 * المؤلف: mohamed alromaihi
 * الدوال الموجودة: `clean`، `gridIsExactlyNineByEightAndEveryCanonicalBrickRoundTrips`، `dragStrokeIsOneUndoAndRedoOperation`، `historyKeepsAtLeastFiftyOperations`، `fillEraserAndConfirmedClearCommandsAreReversible`، `levelPropertiesAndGroupsBuildCanonicalDefinition`، `validationRejectsDimensionsEmptyIndestructibleLocksAndUnreachableBoss`، `level`، `customRepositorySupportsVersionedRoundTripSaveAsDuplicateAndDelete`، `paletteMetadataCoversEveryBrickAndPreviewKeyIsDeterministic`، `pausePersistencePreservesPuzzleRuntimeState`
 */

package com.example.brick_breaker_ball

import com.badlogic.gdx.files.FileHandle
import com.badlogic.gdx.math.Rectangle
import java.nio.file.Files
import org.junit.After
import org.junit.Assert.*
import org.junit.Test

class LevelEditorTest {
    private val temp = Files.createTempDirectory("brick-editor-test").toFile()

    /** ملاحظة صيانة: الدالة `clean` توثّق حالة اختبار أو تهيئة آلية وتحمي السلوك المتوقع من التراجع؛ راجع استدعاءاتها واختباراتها قبل تعديلها. */
    @After fun clean() {
        temp.deleteRecursively()
    }

    /** ملاحظة صيانة: الدالة `gridIsExactlyNineByEightAndEveryCanonicalBrickRoundTrips` توثّق حالة اختبار أو تهيئة آلية وتحمي السلوك المتوقع من التراجع؛ راجع استدعاءاتها واختباراتها قبل تعديلها. */
    @Test fun gridIsExactlyNineByEightAndEveryCanonicalBrickRoundTrips() {
        val state = LevelEditorState()
        assertEquals(8, state.cells.size)
        assertTrue(state.cells.all { it.size == 9 })
        BrickType.entries.forEachIndexed { index, type ->
            assertEquals(type, BrickCodec.type(BrickCodec.symbol(type)))
            state.brush = type
            state.groupId = index % 4
            state.beginStroke()
            state.paint(index / 9, index % 9)
            state.endStroke()
        }
        val level = state.toLevelDefinition()
        assertEquals(8, level.rows)
        assertEquals(9, level.columns)
        assertEquals(BrickType.entries.toSet(), level.layout.joinToString("").mapNotNull(BrickCodec::type).toSet())
    }

    /** ملاحظة صيانة: الدالة `dragStrokeIsOneUndoAndRedoOperation` توثّق حالة اختبار أو تهيئة آلية وتحمي السلوك المتوقع من التراجع؛ راجع استدعاءاتها واختباراتها قبل تعديلها. */
    @Test fun dragStrokeIsOneUndoAndRedoOperation() {
        val state = LevelEditorState()
        state.brush = BrickType.GLASS
        state.beginStroke()
        repeat(9) { state.paint(0, it) }
        assertTrue(state.endStroke())
        assertEquals(1, state.undoCount)
        assertTrue(state.undo())
        assertTrue(state.cells[0].all { it.type == null })
        assertTrue(state.redo())
        assertTrue(state.cells[0].all { it.type == BrickType.GLASS })
    }

    /** ملاحظة صيانة: الدالة `historyKeepsAtLeastFiftyOperations` توثّق حالة اختبار أو تهيئة آلية وتحمي السلوك المتوقع من التراجع؛ راجع استدعاءاتها واختباراتها قبل تعديلها. */
    @Test fun historyKeepsAtLeastFiftyOperations() {
        val state = LevelEditorState()
        repeat(60) { index ->
            state.brush = BrickType.entries[index % BrickType.entries.size]
            state.beginStroke()
            state.paint(index % 8, index % 9)
            state.endStroke()
        }
        assertEquals(50, state.undoCount)
        repeat(50) { assertTrue(state.undo()) }
        assertFalse(state.undo())
    }

    /** ملاحظة صيانة: الدالة `fillEraserAndConfirmedClearCommandsAreReversible` توثّق حالة اختبار أو تهيئة آلية وتحمي السلوك المتوقع من التراجع؛ راجع استدعاءاتها واختباراتها قبل تعديلها. */
    @Test fun fillEraserAndConfirmedClearCommandsAreReversible() {
        val state = LevelEditorState()
        state.brush = BrickType.NORMAL_ONE_HIT
        state.tool = EditorTool.FILL
        assertTrue(state.fill(0, 0))
        assertTrue(state.cells.all { row -> row.all { it.type == BrickType.NORMAL_ONE_HIT } })
        state.tool = EditorTool.ERASE
        assertTrue(state.fill(0, 0))
        assertTrue(state.cells.all { row -> row.all { it.type == null } })
        assertTrue(state.undo())
        assertTrue(state.clear())
        assertTrue(state.undo())
        assertTrue(state.cells.all { row -> row.all { it.type == BrickType.NORMAL_ONE_HIT } })
    }

    /** ملاحظة صيانة: الدالة `levelPropertiesAndGroupsBuildCanonicalDefinition` توثّق حالة اختبار أو تهيئة آلية وتحمي السلوك المتوقع من التراجع؛ راجع استدعاءاتها واختباراتها قبل تعديلها. */
    @Test fun levelPropertiesAndGroupsBuildCanonicalDefinition() {
        val state = LevelEditorState()
        state.brush = BrickType.LOCKED
        state.groupId = 3
        state.beginStroke()
        state.paint(2, 4)
        state.endStroke()
        state.mutateProperties {
            it.name = "PUZZLE"
            it.customId = "puzzle-7"
            it.world = 7
            it.ballSpeed = 760f
            it.lives = 5
            it.dropRate = .3f
            it.deathRailEnabled = false
            it.threeStarScore = 9000
            it.bossHealth = 12
            it.modifiers += "VOLATILE"
        }
        val level = state.toLevelDefinition()
        assertEquals("L", level.layout[2][4].toString())
        assertEquals(3, level.brickGroups["2:4"])
        assertEquals(7, level.world)
        assertEquals(760f, level.ballSpeed)
        assertEquals(5, level.lives)
        assertEquals(.3f, level.dropRate)
        assertFalse(level.deathRailEnabled)
        assertEquals(12, level.bossHealth)
    }

    /** ملاحظة صيانة: الدالة `validationRejectsDimensionsEmptyIndestructibleLocksAndUnreachableBoss` توثّق حالة اختبار أو تهيئة آلية وتحمي السلوك المتوقع من التراجع؛ راجع استدعاءاتها واختباراتها قبل تعديلها. */
    @Test fun validationRejectsDimensionsEmptyIndestructibleLocksAndUnreachableBoss() {
        /** ملاحظة صيانة: الدالة `level` توثّق حالة اختبار أو تهيئة آلية وتحمي السلوك المتوقع من التراجع؛ راجع استدعاءاتها واختباراتها قبل تعديلها. */
        fun level(layout: List<String>, groups: Map<String, Int> = emptyMap()) = LevelDefinition(-1, 1, "TEST", layout.size, layout.firstOrNull()?.length ?: 0, layout, 600f, 3, 5000, customId = "test", brickGroups = groups)
        assertFalse(LevelValidator.validate(level(List(8) { "........." })).valid)
        assertTrue(LevelValidator.validate(level(List(8) { if (it == 0) "N........" else "........." })).valid)
        assertTrue(LevelValidator.validate(level(List(8) { if (it == 0) "IIIIIIIII" else "........." })).errors.any { "indestructible" in it })
        assertTrue(LevelValidator.validate(level(listOf("L........") + List(7) { "........." })).errors.any { "requires" in it })
        val boss = listOf(".........", "...I.....", "..IBI....", "...I.....") + List(4) { "........." }
        assertTrue(LevelValidator.validate(level(boss)).errors.any { "unreachable" in it })
    }

    /** ملاحظة صيانة: الدالة `customRepositorySupportsVersionedRoundTripSaveAsDuplicateAndDelete` توثّق حالة اختبار أو تهيئة آلية وتحمي السلوك المتوقع من التراجع؛ راجع استدعاءاتها واختباراتها قبل تعديلها. */
    @Test fun customRepositorySupportsVersionedRoundTripSaveAsDuplicateAndDelete() {
        val repository = CustomLevelRepository(FileHandle(temp))
        val state = LevelEditorState()
        state.brush = BrickType.NORMAL_ONE_HIT
        state.beginStroke()
        state.paint(0, 0)
        state.endStroke()
        val saved = repository.save(state.toLevelDefinition())
        assertEquals(saved, repository.load(saved.customId!!))
        assertEquals(1, repository.list().size)
        val saveAs = repository.saveAs(saved, "second level")
        assertNotEquals(saved.customId, saveAs.customId)
        val duplicate = repository.duplicate(saved.customId!!)
        assertNotNull(duplicate)
        assertEquals(3, repository.list().size)
        assertTrue(repository.delete(saved.customId!!))
        assertNull(repository.load(saved.customId!!))
    }

    /** ملاحظة صيانة: الدالة `paletteMetadataCoversEveryBrickAndPreviewKeyIsDeterministic` توثّق حالة اختبار أو تهيئة آلية وتحمي السلوك المتوقع من التراجع؛ راجع استدعاءاتها واختباراتها قبل تعديلها. */
    @Test fun paletteMetadataCoversEveryBrickAndPreviewKeyIsDeterministic() {
        assertEquals(BrickType.entries.toSet(), BrickInfoRepository.all.keys)
        assertTrue(BrickInfoRepository.all.values.all { it.displayName.isNotBlank() && it.description.isNotBlank() })
        assertEquals(PaletteSection.entries.toSet(), BrickInfoRepository.all.values.map { it.section }.toSet())
    }

    /** ملاحظة صيانة: الدالة `pausePersistencePreservesPuzzleRuntimeState` توثّق حالة اختبار أو تهيئة آلية وتحمي السلوك المتوقع من التراجع؛ راجع استدعاءاتها واختباراتها قبل تعديلها. */
    @Test fun pausePersistencePreservesPuzzleRuntimeState() {
        val prefs = TestPreferences()
        val store = PausedSessionStore(prefs)
        val level = LevelRepository.level(1)
        val session = GameSession(level = level)
        session.bricks.clear()
        session.bricks += Brick(77, Rectangle(20f, 700f, 92f, 58f), BrickType.LOCKED, 2, 0f, groupId = 3, locked = false)
        session.bricks += Brick(78, Rectangle(120f, 700f, 92f, 58f), BrickType.GHOST, 1, 0f, groupId = 3, ghostVisible = false)
        store.save(level, session)
        val restoredPair = store.restore()
        assertNotNull(restoredPair)
        val restored = restoredPair!!.second
        assertEquals(3, restored.bricks.first().groupId)
        assertFalse(restored.bricks.first().locked)
        assertFalse(restored.bricks.last().ghostVisible)
    }
}
