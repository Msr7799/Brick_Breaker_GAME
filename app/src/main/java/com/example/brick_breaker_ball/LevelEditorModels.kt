package com.example.brick_breaker_ball

import com.badlogic.gdx.Gdx
import com.badlogic.gdx.files.FileHandle
import com.badlogic.gdx.utils.Json
import com.badlogic.gdx.utils.JsonWriter
import java.util.ArrayDeque

object BrickCodec {
    private val symbols = linkedMapOf(
        BrickType.NORMAL_ONE_HIT to 'N', BrickType.ARMORED_TWO_HIT to '2',
        BrickType.ARMORED_THREE_HIT to '3', BrickType.INDESTRUCTIBLE to 'I',
        BrickType.GLASS to 'G', BrickType.EXPLOSIVE to 'E', BrickType.POWERUP_CARRIER to 'P',
        BrickType.MOVING_HORIZONTAL to 'H', BrickType.MOVING_VERTICAL to 'V',
        BrickType.REGENERATING to 'R', BrickType.GHOST to 'O', BrickType.SWITCH to 'S',
        BrickType.LOCKED to 'L', BrickType.KEY_BRICK to 'K', BrickType.CHAIN_BRICK to 'C',
        BrickType.BOSS_CORE to 'B',
        BrickType.ROUGH_STONE to 'A', BrickType.LIGHTNING_SPEED_PASS_THROUGH to 'D',
        BrickType.ELECTRIC_WHITE to 'F', BrickType.CRYSTAL_BLUE to 'J', BrickType.CRYSTAL_RED to 'M',
        BrickType.CRYSTAL_PURPLE to 'Q', BrickType.STONE_GRAY to 'T', BrickType.BLACK_HOLE_TELEPORTER to 'U',
        BrickType.RANDOM_INVENTORY_POWERUP to 'W', BrickType.CRYSTAL_CYAN to 'X', BrickType.ARMORED_DARK to 'Y',
        BrickType.CRYSTAL_PINK to 'Z', BrickType.CRYSTAL_ORANGE to '4', BrickType.CRYSTAL_GREEN to '5',
        BrickType.SPIKED_HAZARD to '6', BrickType.TRANSPARENT_SLOW_PASS_THROUGH to '7',
    )
    fun symbol(type: BrickType) = symbols.getValue(type)
    fun type(symbol: Char) = if (symbol == '1') BrickType.NORMAL_ONE_HIT else symbols.entries.firstOrNull { it.value == symbol }?.key
    fun key(row: Int, column: Int) = "$row:$column"
}

enum class EditorTool { PAINT, ERASE, FILL }
enum class PaletteSection { BASIC, SPECIAL, MOVEMENT, PUZZLE, DANGER_BOSS }

data class BrickInfo(val displayName: String, val description: String, val section: PaletteSection)

object BrickInfoRepository {
    val all = linkedMapOf(
        BrickType.NORMAL_ONE_HIT to BrickInfo("NORMAL", "Breaks in one hit.", PaletteSection.BASIC),
        BrickType.ARMORED_TWO_HIT to BrickInfo("ARMORED II", "Requires two hits.", PaletteSection.BASIC),
        BrickType.ARMORED_THREE_HIT to BrickInfo("ARMORED III", "Requires three hits.", PaletteSection.BASIC),
        BrickType.GLASS to BrickInfo("GLASS", "Fragile one-hit glass.", PaletteSection.BASIC),
        BrickType.INDESTRUCTIBLE to BrickInfo("STEEL", "Reflects the ball and never breaks.", PaletteSection.SPECIAL),
        BrickType.EXPLOSIVE to BrickInfo("EXPLOSIVE", "Damages nearby breakable bricks.", PaletteSection.SPECIAL),
        BrickType.POWERUP_CARRIER to BrickInfo("CARRIER", "Always releases a power-up.", PaletteSection.SPECIAL),
        BrickType.REGENERATING to BrickInfo("REGENERATING", "Restores lost HP after a delay.", PaletteSection.SPECIAL),
        BrickType.GHOST to BrickInfo("GHOST", "A linked Switch toggles visibility and collision.", PaletteSection.SPECIAL),
        BrickType.MOVING_HORIZONTAL to BrickInfo("MOVE H", "Oscillates horizontally from its origin.", PaletteSection.MOVEMENT),
        BrickType.MOVING_VERTICAL to BrickInfo("MOVE V", "Oscillates vertically from its origin.", PaletteSection.MOVEMENT),
        BrickType.KEY_BRICK to BrickInfo("KEY", "Unlocks Locked bricks in its group.", PaletteSection.PUZZLE),
        BrickType.LOCKED to BrickInfo("LOCKED", "Cannot be damaged until its group unlocks.", PaletteSection.PUZZLE),
        BrickType.SWITCH to BrickInfo("SWITCH", "Unlocks and toggles its linked group.", PaletteSection.PUZZLE),
        BrickType.CHAIN_BRICK to BrickInfo("CHAIN", "Damage propagates through its group once.", PaletteSection.PUZZLE),
        BrickType.BOSS_CORE to BrickInfo("BOSS CORE", "Configurable high-HP completion target.", PaletteSection.DANGER_BOSS),
        BrickType.ROUGH_STONE to BrickInfo("ROUGH STONE", "Breaks and deflects the ball at an irregular angle.", PaletteSection.BASIC),
        BrickType.LIGHTNING_SPEED_PASS_THROUGH to BrickInfo("LIGHTNING SPEED", "The ball passes through, destroys it, and accelerates.", PaletteSection.SPECIAL),
        BrickType.ELECTRIC_WHITE to BrickInfo("ELECTRIC WHITE", "Two-state electric brick.", PaletteSection.BASIC),
        BrickType.CRYSTAL_BLUE to BrickInfo("BLUE CRYSTAL", "Two-state blue crystal.", PaletteSection.BASIC),
        BrickType.CRYSTAL_RED to BrickInfo("RED CRYSTAL", "Two-state red crystal.", PaletteSection.BASIC),
        BrickType.CRYSTAL_PURPLE to BrickInfo("PURPLE CRYSTAL", "Two-state purple crystal.", PaletteSection.BASIC),
        BrickType.STONE_GRAY to BrickInfo("GRAY STONE", "Two-state gray stone.", PaletteSection.BASIC),
        BrickType.BLACK_HOLE_TELEPORTER to BrickInfo("BLACK HOLE", "Teleports the ball to a safe location and new angle.", PaletteSection.SPECIAL),
        BrickType.RANDOM_INVENTORY_POWERUP to BrickInfo("ITEM CACHE", "Adds one random positive reward to Items.", PaletteSection.SPECIAL),
        BrickType.CRYSTAL_CYAN to BrickInfo("CYAN CRYSTAL", "Two-state cyan crystal.", PaletteSection.BASIC),
        BrickType.ARMORED_DARK to BrickInfo("DARK ARMOR", "Two-state dark armored brick.", PaletteSection.BASIC),
        BrickType.CRYSTAL_PINK to BrickInfo("PINK CRYSTAL", "Two-state pink crystal.", PaletteSection.BASIC),
        BrickType.CRYSTAL_ORANGE to BrickInfo("ORANGE CRYSTAL", "Two-state orange crystal.", PaletteSection.BASIC),
        BrickType.CRYSTAL_GREEN to BrickInfo("GREEN CRYSTAL", "Two-state green crystal.", PaletteSection.BASIC),
        BrickType.SPIKED_HAZARD to BrickInfo("SPIKED HAZARD", "Hazard response is controlled by gameplay tuning.", PaletteSection.DANGER_BOSS),
        BrickType.TRANSPARENT_SLOW_PASS_THROUGH to BrickInfo("SLOW FIELD", "The ball passes through, destroys it, and slows down.", PaletteSection.SPECIAL),
    )
}

data class EditorCell(var type: BrickType? = null, var groupId: Int = 0)

data class LevelProperties(
    var customId: String = "custom-001",
    var name: String = "NEW CUSTOM LEVEL",
    var world: Int = 1,
    var ballSpeed: Float = 600f,
    var lives: Int = 3,
    var dropRate: Float = .12f,
    var deathRailEnabled: Boolean = true,
    var threeStarScore: Int = 5000,
    var modifiers: MutableSet<String> = mutableSetOf(),
    var bossHealth: Int = 5,
)

private data class EditorSnapshot(val cells: List<List<EditorCell>>, val properties: LevelProperties)

class LevelEditorState(
    val cells: MutableList<MutableList<EditorCell>> = MutableList(ROWS) { MutableList(COLUMNS) { EditorCell() } },
    var properties: LevelProperties = LevelProperties(),
) {
    companion object {
        const val COLUMNS = 9; const val ROWS = 8; const val HISTORY_LIMIT = 50
        fun from(level: LevelDefinition): LevelEditorState {
            val state = LevelEditorState(properties = LevelProperties(
                customId = level.customId ?: "custom-001", name = level.name, world = level.world,
                ballSpeed = level.ballSpeed, lives = level.lives, dropRate = level.dropRate,
                deathRailEnabled = level.deathRailEnabled, threeStarScore = level.threeStarScore,
                modifiers = level.modifiers.toMutableSet(), bossHealth = level.bossHealth,
            ))
            level.layout.take(ROWS).forEachIndexed { r, line -> line.take(COLUMNS).forEachIndexed { c, symbol ->
                val key = BrickCodec.key(r, c)
                val canonical = level.brickIds[key]?.let { runCatching { BrickType.valueOf(it) }.getOrNull() }
                state.cells[r][c] = EditorCell(canonical ?: BrickCodec.type(symbol), level.brickGroups[key] ?: 0)
            } }
            state.markSaved(); return state
        }
    }
    var tool = EditorTool.PAINT
    var brush = BrickType.NORMAL_ONE_HIT
    var groupId = 0
    var dirty = false; private set
    private val undo = ArrayDeque<EditorSnapshot>()
    private val redo = ArrayDeque<EditorSnapshot>()
    private var strokeSnapshot: EditorSnapshot? = null

    val undoCount get() = undo.size
    val redoCount get() = redo.size

    fun beginStroke() { if (strokeSnapshot == null) strokeSnapshot = snapshot() }
    fun paint(row: Int, column: Int): Boolean {
        if (row !in 0 until ROWS || column !in 0 until COLUMNS) return false
        if (strokeSnapshot == null) beginStroke()
        val replacement = if (tool == EditorTool.ERASE) EditorCell() else EditorCell(brush, groupId.coerceAtLeast(0))
        if (cells[row][column] == replacement) return false
        cells[row][column] = replacement; dirty = true; return true
    }
    fun endStroke(): Boolean {
        val before = strokeSnapshot ?: return false
        strokeSnapshot = null
        if (before == snapshot()) return false
        pushUndo(before); redo.clear(); return true
    }
    fun fill(row: Int, column: Int): Boolean {
        if (row !in 0 until ROWS || column !in 0 until COLUMNS) return false
        val before = snapshot(); val target = cells[row][column].copy()
        val replacement = if (tool == EditorTool.ERASE) EditorCell() else EditorCell(brush, groupId.coerceAtLeast(0))
        if (target == replacement) return false
        val pending = ArrayDeque<Pair<Int, Int>>(); pending.add(row to column)
        while (pending.isNotEmpty()) {
            val (r, c) = pending.removeFirst()
            if (r !in 0 until ROWS || c !in 0 until COLUMNS || cells[r][c] != target) continue
            cells[r][c] = replacement.copy()
            pending.add(r - 1 to c); pending.add(r + 1 to c); pending.add(r to c - 1); pending.add(r to c + 1)
        }
        pushUndo(before); redo.clear(); dirty = true; return true
    }
    fun clear(): Boolean {
        if (cells.all { row -> row.all { it.type == null } }) return false
        pushUndo(snapshot()); redo.clear(); cells.forEach { row -> row.indices.forEach { row[it] = EditorCell() } }; dirty = true; return true
    }
    fun mutateProperties(change: (LevelProperties) -> Unit) {
        val before = snapshot(); change(properties)
        if (before != snapshot()) { pushUndo(before); redo.clear(); dirty = true }
    }
    fun undo(): Boolean {
        val previous = undo.pollLast() ?: return false
        redo.addLast(snapshot()); restore(previous); dirty = true; return true
    }
    fun redo(): Boolean {
        val next = redo.pollLast() ?: return false
        pushUndo(snapshot()); restore(next); dirty = true; return true
    }
    fun markSaved() { dirty = false }
    fun toLevelDefinition(): LevelDefinition {
        val layout = cells.map { row -> String(CharArray(COLUMNS) { c -> row[c].type?.let(BrickCodec::symbol) ?: '.' }) }
        val groups = buildMap { cells.forEachIndexed { r, row -> row.forEachIndexed { c, cell -> if (cell.type != null && cell.groupId > 0) put(BrickCodec.key(r, c), cell.groupId) } } }
        val brickIds = buildMap { cells.forEachIndexed { r, row -> row.forEachIndexed { c, cell -> cell.type?.let { put(BrickCodec.key(r, c), it.name) } } } }
        return LevelDefinition(
            id = -1, world = properties.world, name = properties.name.trim().ifBlank { "UNTITLED CUSTOM LEVEL" },
            rows = ROWS, columns = COLUMNS, layout = layout, ballSpeed = properties.ballSpeed,
            lives = properties.lives, threeStarScore = properties.threeStarScore,
            modifiers = properties.modifiers.toSet(), customId = properties.customId.trim(),
            dropRate = properties.dropRate, deathRailEnabled = properties.deathRailEnabled,
            bossHealth = properties.bossHealth, brickGroups = groups, brickIds = brickIds,
        )
    }
    private fun snapshot() = EditorSnapshot(cells.map { row -> row.map(EditorCell::copy) }, properties.copy(modifiers = properties.modifiers.toMutableSet()))
    private fun restore(snapshot: EditorSnapshot) {
        cells.indices.forEach { r -> cells[r].indices.forEach { c -> cells[r][c] = snapshot.cells[r][c].copy() } }
        properties = snapshot.properties.copy(modifiers = snapshot.properties.modifiers.toMutableSet())
    }
    private fun pushUndo(snapshot: EditorSnapshot) { undo.addLast(snapshot); while (undo.size > HISTORY_LIMIT) undo.removeFirst() }
}

data class StoredCustomLevel(var version: Int = CustomLevelRepository.CURRENT_VERSION, var level: LevelDefinition? = null)

class CustomLevelRepository(private val directory: FileHandle = Gdx.files.local("custom-levels")) {
    companion object { const val CURRENT_VERSION = 2 }
    private val json = Json().apply { setOutputType(JsonWriter.OutputType.json); setIgnoreUnknownFields(true) }
    fun list(): List<LevelDefinition> {
        ensureDirectory()
        return directory.list("json").mapNotNull { runCatching { read(it) }.getOrNull() }.sortedBy { it.name }
    }
    fun save(level: LevelDefinition): LevelDefinition {
        val id = safeId(level.customId ?: level.name)
        val saved = level.copy(customId = id)
        ensureDirectory(); directory.child("$id.json").writeString(json.prettyPrint(StoredCustomLevel(level = saved)), false, "UTF-8")
        return saved
    }
    fun saveAs(level: LevelDefinition, requestedId: String): LevelDefinition = save(level.copy(customId = uniqueId(requestedId)))
    fun load(id: String): LevelDefinition? = directory.child("${safeId(id)}.json").takeIf(FileHandle::exists)?.let(::read)
    fun delete(id: String): Boolean = directory.child("${safeId(id)}.json").let { it.exists() && it.delete() }
    fun duplicate(id: String): LevelDefinition? = load(id)?.let { source ->
        save(source.copy(customId = uniqueId("${source.customId}-copy"), name = "${source.name} COPY"))
    }
    private fun read(file: FileHandle): LevelDefinition? {
        val stored = json.fromJson(StoredCustomLevel::class.java, file)
        require(stored.version in 1..CURRENT_VERSION) { "Unsupported custom level version ${stored.version}" }
        return stored.level
    }
    private fun uniqueId(raw: String): String {
        val base = safeId(raw); var candidate = base; var suffix = 2
        while (directory.child("$candidate.json").exists()) candidate = "$base-$suffix".also { suffix++ }
        return candidate
    }
    private fun safeId(raw: String) = raw.trim().lowercase().replace(Regex("[^a-z0-9_-]+"), "-").trim('-').ifBlank { "custom-level" }.take(48)
    private fun ensureDirectory() { if (!directory.exists()) directory.mkdirs() }
}
