package com.example.brick_breaker_ball

import com.badlogic.gdx.Gdx
import com.badlogic.gdx.Preferences

data class WorldDefinition(val id: Int, val name: String, val subtitle: String, val backgroundRegion: String, val tintHex: String)
data class LevelDefinition(
    val id: Int = -1,
    val world: Int = 1,
    val name: String = "UNTITLED",
    val rows: Int = 8,
    val columns: Int = 9,
    val layout: List<String> = List(8) { "........." },
    val ballSpeed: Float = 600f,
    val lives: Int = 3,
    val threeStarScore: Int = 5000,
    val modifiers: Set<String> = emptySet(),
    val customId: String? = null,
    val dropRate: Float = .12f,
    val deathRailEnabled: Boolean = true,
    val bossHealth: Int = 5,
    val brickGroups: Map<String, Int> = emptyMap(),
    /** Canonical BrickType IDs for custom/new levels; layout remains as a legacy migration fallback. */
    val brickIds: Map<String, String> = emptyMap(),
)

object LevelRepository {
    const val REGULAR_WORLD_LEVELS = 7
    const val FINAL_WORLD_LEVELS = 6
    const val TOTAL_LEVELS = 90

    val worlds = listOf(
        WorldDefinition(1, "CRYSTAL LEVIATHAN", "Crystal water dragon beneath the deep", "world_2_crystal", "19E6FF"),
        WorldDefinition(2, "CELESTIAL GROVE", "Mushroom kingdom beneath planetary skies", "world_2_crystal", "56FFD2"),
        WorldDefinition(3, "VOID WHALE", "A cosmic whale sailing the stars", "world_4_zerog", "7DDCFF"),
        WorldDefinition(4, "AZURE PYRAMID", "Ancient desert city of crystal pyramids", "world_1_foundry", "FFD15C"),
        WorldDefinition(5, "SKY KINGDOM", "Floating city and islands above the clouds", "world_2_crystal", "48A7FF"),
        WorldDefinition(6, "STORM SPIRE", "Storm towers charged with lightning", "world_4_zerog", "B56CFF"),
        WorldDefinition(7, "CRYSTAL COAST", "Crystal shore and the water pyramid", "world_2_crystal", "56FFD2"),
        WorldDefinition(8, "FROZEN CITADEL", "Frozen fortress beneath the aurora", "world_3_magma", "7DDCFF"),
        WorldDefinition(9, "DARK MATTER", "Enter the dark vortex", "world_4_zerog", "9E83FF"),
        WorldDefinition(10, "CONJUNCTION", "Survive the planetary alignment", "world_4_zerog", "F4BA63"),
        WorldDefinition(11, "CHAOS", "Face the unraveling cosmos", "world_3_magma", "FF5878"),
        WorldDefinition(12, "THE END OF TIME", "Reach the edge of all time", "world_2_crystal", "74FFB5"),
        WorldDefinition(13, "DARK VOLCANO", "The final world", "world_3_magma", "FF6B2C"),
    )
    val levels: List<LevelDefinition> = (1..TOTAL_LEVELS).map(::createLevel)
    fun level(id: Int) = levels[(id - 1).coerceIn(0, levels.lastIndex)]
    fun worldLevelCount(world: Int): Int = if (world == worlds.last().id) FINAL_WORLD_LEVELS else REGULAR_WORLD_LEVELS
    fun firstLevel(world: Int): Int = worlds.takeWhile { it.id < world }.sumOf { worldLevelCount(it.id) } + 1
    fun lastLevel(world: Int): Int = firstLevel(world) + worldLevelCount(world) - 1
    fun worldForLevel(levelId: Int): Int = worlds.first { levelId in firstLevel(it.id)..lastLevel(it.id) }.id
    private fun isBonusStage(world: Int, stage: Int): Boolean = world in setOf(3, 6, 9, 12) && stage == 6

    private fun createLevel(id: Int): LevelDefinition {
        val world = worldForLevel(id)
        val stage = id - firstLevel(world) + 1
        val stageCount = worldLevelCount(world)
        val bonusStage = isBonusStage(world, stage)
        val rows = 8; val columns = 9
        val cells = MutableList(rows) { MutableList<BrickType?>(columns) { null } }
        val groups = mutableMapOf<String, Int>()
        fun key(row: Int, column: Int) = BrickCodec.key(row, column)
        fun place(row: Int, column: Int, type: BrickType, group: Int = 0) {
            if (row !in 0 until rows || column !in 0 until columns) return
            cells[row][column] = type
            if (group > 0) groups[key(row, column)] = group else groups.remove(key(row, column))
        }

        val targetCount = (19 + stage * 3 + world * 2).coerceAtMost(64)
        val positions = (0 until rows).flatMap { row -> (0 until columns).map { column -> row to column } }
            .sortedBy { (row, column) -> boardHash(id, row, column) }.take(targetCount)
        val materials = worldMaterials(world)
        positions.forEachIndexed { index, (row, column) -> place(row, column, materials[(index + stage + row * 2) % materials.size]) }

        when (stage) {
            1 -> (0..8).forEach { c -> place(0, c, materials[c % materials.size]) }
            2 -> (0..5).forEach { r -> place(r, (r + world) % columns, BrickType.GLASS); place(r, columns - 1 - (r + world) % columns, BrickType.NORMAL_ONE_HIT) }
            3 -> (1..7).forEach { c -> place(2, c, materials[(c + 1) % materials.size]); place(4, c, materials[(c + 3) % materials.size]) }
            4 -> (0..6).forEach { r -> place(r, 0, materials[r % materials.size]); place(r, 8, materials[(r + 2) % materials.size]) }
            5 -> (0..6).forEach { r -> (r..(8 - r.coerceAtMost(4))).forEach { c -> place(r, c, materials[(r + c) % materials.size]) } }
            6 -> (0..8).forEach { c -> place(3, c, materials[(c + stage) % materials.size]) }
            7 -> (0..6).forEach { r -> place(r, (r * 2 + world) % columns, materials[r % materials.size]) }
        }

        if (!bonusStage) {
            if (world >= 2 || stage >= 6) place(1, 2, BrickType.POWERUP_CARRIER)
            if (world >= 3) { place(1, 1, BrickType.MOVING_HORIZONTAL); place(2, 7, BrickType.MOVING_VERTICAL); place(4, 2, BrickType.LIGHTNING_SPEED_PASS_THROUGH); place(4, 6, BrickType.TRANSPARENT_SLOW_PASS_THROUGH) }
            if (world >= 4) place(1, 4, BrickType.REGENERATING)
            if (world >= 5) { place(1, 3, BrickType.SWITCH, 2); place(2, 5, BrickType.GHOST, 2); place(4, 4, BrickType.BLACK_HOLE_TELEPORTER) }
            if (world >= 6) { place(1, 1, BrickType.KEY_BRICK, 1); place(1, 7, BrickType.LOCKED, 1); place(2, 7, BrickType.LOCKED, 1) }
            if (world >= 7) { place(3, 2, BrickType.CHAIN_BRICK, 3); place(3, 4, BrickType.CHAIN_BRICK, 3); place(3, 6, BrickType.CHAIN_BRICK, 3) }
        }
        if (bonusStage) {
            // Item-rewarding bricks are deliberately limited to four bonus boards.
            listOf(0 to 1, 0 to 3, 0 to 5, 0 to 7, 2 to 2, 2 to 4, 2 to 6).forEach { (row, column) ->
                place(row, column, BrickType.RANDOM_INVENTORY_POWERUP)
            }
            (1..7).filter { it % 2 == 1 }.forEach { column -> place(1, column, BrickType.CRYSTAL_CYAN) }
            place(3, 4, BrickType.POWERUP_CARRIER)
        }
        if (stage == stageCount) { place(3, 4, BrickType.BOSS_CORE); place(2, 3, BrickType.ARMORED_DARK); place(2, 5, BrickType.ARMORED_DARK); place(4, 3, BrickType.ARMORED_DARK); place(4, 5, BrickType.ARMORED_DARK) }

        val brickIds = buildMap { cells.forEachIndexed { row, line -> line.forEachIndexed { column, type -> type?.let { put(key(row, column), it.name) } } } }
        val layout = cells.map { row -> row.joinToString("") { type -> type?.let(BrickCodec::symbol)?.toString() ?: "." } }
        val breakableCount = cells.flatten().count { it?.breakable == true }
        return LevelDefinition(
            id = id, world = world, name = if (bonusStage) "WORLD $world • BONUS CACHE" else "WORLD $world • STAGE ${stage.toString().padStart(2, '0')}", rows = rows, columns = columns, layout = layout,
            ballSpeed = (600f + (world - 1) * 26f + (stage - 1) * 3.5f).coerceAtMost(GameSession.MAX_SPEED),
            lives = if (world <= 2 || stage == stageCount) 4 else 3,
            threeStarScore = maxOf(1200, breakableCount * 85 + world * 120 + stage * 35),
            modifiers = buildSet { if (stage == stageCount) { add("BOSS"); add("CHALLENGE") }; if (bonusStage) add("BONUS"); if (world >= 4) add("VOLATILE") },
            dropRate = (.11f + world * .018f + stage * .004f).coerceAtMost(.34f), deathRailEnabled = true,
            bossHealth = 4 + world * 2 + stage / 2, brickGroups = groups, brickIds = brickIds,
        )
    }

    private fun worldMaterials(world: Int): List<BrickType> = when (world) {
        1 -> listOf(BrickType.NORMAL_ONE_HIT, BrickType.GLASS, BrickType.EXPLOSIVE, BrickType.ROUGH_STONE, BrickType.CRYSTAL_BLUE)
        2 -> listOf(BrickType.NORMAL_ONE_HIT, BrickType.ARMORED_TWO_HIT, BrickType.POWERUP_CARRIER, BrickType.ELECTRIC_WHITE, BrickType.CRYSTAL_GREEN)
        3 -> listOf(BrickType.ARMORED_TWO_HIT, BrickType.ARMORED_THREE_HIT, BrickType.MOVING_HORIZONTAL, BrickType.MOVING_VERTICAL, BrickType.LIGHTNING_SPEED_PASS_THROUGH, BrickType.TRANSPARENT_SLOW_PASS_THROUGH, BrickType.STONE_GRAY, BrickType.CRYSTAL_CYAN)
        4 -> listOf(BrickType.ARMORED_THREE_HIT, BrickType.REGENERATING, BrickType.CRYSTAL_RED, BrickType.CRYSTAL_PURPLE, BrickType.CRYSTAL_PINK, BrickType.CRYSTAL_ORANGE)
        5 -> listOf(BrickType.ARMORED_THREE_HIT, BrickType.REGENERATING, BrickType.GHOST, BrickType.SWITCH, BrickType.BLACK_HOLE_TELEPORTER, BrickType.CRYSTAL_PURPLE, BrickType.ARMORED_DARK)
        6 -> listOf(BrickType.ARMORED_THREE_HIT, BrickType.KEY_BRICK, BrickType.LOCKED, BrickType.CRYSTAL_CYAN, BrickType.ARMORED_DARK, BrickType.CRYSTAL_GREEN)
        7 -> listOf(BrickType.ARMORED_THREE_HIT, BrickType.CHAIN_BRICK, BrickType.KEY_BRICK, BrickType.LOCKED, BrickType.GHOST, BrickType.SWITCH, BrickType.CRYSTAL_PINK)
        else -> listOf(BrickType.ARMORED_THREE_HIT, BrickType.INDESTRUCTIBLE, BrickType.ARMORED_DARK, BrickType.REGENERATING, BrickType.CHAIN_BRICK, BrickType.MOVING_HORIZONTAL, BrickType.MOVING_VERTICAL, BrickType.CRYSTAL_RED, BrickType.CRYSTAL_ORANGE, BrickType.CRYSTAL_GREEN)
    }

    private fun boardHash(levelId: Int, row: Int, column: Int): Int = ((levelId * 1103515245L + row * 92821L + column * 68917L) and Int.MAX_VALUE.toLong()).toInt()
}

data class ValidationResult(val valid: Boolean, val errors: List<String>, val warnings: List<String> = emptyList())
object LevelValidator {
    fun validate(level: LevelDefinition): ValidationResult {
        val errors = mutableListOf<String>()
        val warnings = mutableListOf<String>()
        if (level.rows != 8 || level.columns != 9 || level.layout.size != 8 || level.layout.any { it.length != 9 })
            errors += "Grid must be exactly 9 columns by 8 rows"
        val text = level.layout.joinToString("")
        fun typeAt(row: Int, column: Int, symbol: Char): BrickType? =
            level.brickIds[BrickCodec.key(row, column)]?.let { runCatching { BrickType.valueOf(it) }.getOrNull() }
                ?: BrickCodec.type(symbol)
        val placed = level.layout.flatMapIndexed { row, line -> line.mapIndexedNotNull { column, symbol -> typeAt(row, column, symbol) } }
        val targets = placed.filter { it.breakable && it != BrickType.SWITCH && it != BrickType.KEY_BRICK }
        if (targets.isEmpty()) errors += "Add at least one completable target"
        if (placed.isNotEmpty() && placed.all { it == BrickType.INDESTRUCTIBLE }) errors += "A level cannot contain only indestructible bricks"
        val groupsByType = mutableMapOf<BrickType, MutableSet<Int>>()
        level.layout.forEachIndexed { row, line -> line.forEachIndexed { column, symbol ->
            typeAt(row, column, symbol)?.let { type -> groupsByType.getOrPut(type) { mutableSetOf() } += level.brickGroups[BrickCodec.key(row, column)] ?: 0 }
        } }
        val unlockers = (groupsByType[BrickType.KEY_BRICK].orEmpty() + groupsByType[BrickType.SWITCH].orEmpty())
        groupsByType[BrickType.LOCKED].orEmpty().forEach { group ->
            if (group !in unlockers && !(group == 0 && unlockers.isNotEmpty())) errors += "Locked group $group requires a Key or Switch"
        }
        groupsByType[BrickType.KEY_BRICK].orEmpty().forEach { group ->
            if (group !in groupsByType[BrickType.LOCKED].orEmpty() && !(group == 0 && groupsByType[BrickType.LOCKED].orEmpty().isNotEmpty()))
                warnings += "Key group $group has no matching Locked brick"
        }
        level.layout.forEachIndexed { row, line -> line.forEachIndexed { column, symbol -> if (symbol == 'B') {
            val reachableNeighbor = listOf(row - 1 to column, row + 1 to column, row to column - 1, row to column + 1).any { (r, c) ->
                r !in level.layout.indices || c !in 0 until 9 || level.layout[r][c] !in "IL"
            }
            if (!reachableNeighbor) errors += "Boss Core at row ${row + 1}, column ${column + 1} is unreachable"
        } } }
        if (text.count { it == 'P' } > 16) errors += "Too many carriers"
        if (level.ballSpeed !in GameSession.MIN_SPEED..GameSession.MAX_SPEED) errors += "Ball speed must be ${GameSession.MIN_SPEED.toInt()}-${GameSession.MAX_SPEED.toInt()}"
        if (level.lives !in 1..9) errors += "Lives must be 1-9"
        if (level.dropRate !in 0f..1f) errors += "Drop rate must be 0-100%"
        if (level.bossHealth !in 1..99) errors += "Boss HP must be 1-99"
        if (level.threeStarScore <= 0) errors += "Invalid score threshold"
        return ValidationResult(errors.isEmpty(), errors.distinct(), warnings.distinct())
    }
}

enum class GraphicsQuality { LOW, MEDIUM, HIGH }
data class GameSettings(
    var masterVolume: Float = .8f,
    var musicVolume: Float = .55f,
    var sfxVolume: Float = .8f,
    var haptics: Boolean = true,
    var reduceMotion: Boolean = false,
    var highContrastBall: Boolean = false,
    var colorBlind: Boolean = false,
    var leftHanded: Boolean = false,
    var relativeControl: Boolean = false,
    var sensitivity: Float = 1f,
    var textScale: Float = 1f,
    var quality: GraphicsQuality = GraphicsQuality.HIGH,
    var selectedBallSpriteName: String = CosmeticDefaults.BALL_SPRITE,
    var selectedBallGroupName: String = CosmeticDefaults.BALL_GROUP,
    var selectedBallBaseSize: BallSize = BallSize.DEFAULT,
    var selectedPaddleId: String = CosmeticDefaults.PADDLE_ID,
    var selectedWeaponPaddleId: String = CosmeticDefaults.WEAPON_PADDLE_ID,
    var selectedStickyPaddleId: String = CosmeticDefaults.STICKY_PADDLE_ID,
)

class ProgressStore(private val prefs: Preferences = Gdx.app.getPreferences("brickbreakerball-progress-v1")) {
    val settings = GameSettings()
    var unlockedLevel: Int = prefs.getInteger("unlocked", 1).coerceIn(1, LevelRepository.TOTAL_LEVELS); private set
    var hasStartedGame: Boolean = prefs.getBoolean("has_started_game", false); private set
    init { loadSettings() }
    fun stars(level: Int) = prefs.getInteger("stars_$level", 0)
    fun bestScore(level: Int) = prefs.getInteger("score_$level", 0)
    fun markGameStarted() {
        if (!hasStartedGame) { hasStartedGame = true; prefs.putBoolean("has_started_game", true).flush() }
    }
    fun complete(level: Int, score: Int, stars: Int) {
        unlockedLevel = maxOf(unlockedLevel, (level + 1).coerceAtMost(LevelRepository.TOTAL_LEVELS))
        prefs.putInteger("unlocked", unlockedLevel).putInteger("stars_$level", maxOf(stars(level), stars))
            .putInteger("score_$level", maxOf(bestScore(level), score)).flush()
    }
    fun saveSettings() { with(settings) { prefs.putFloat("master",masterVolume).putFloat("music",musicVolume).putFloat("sfx",sfxVolume).putBoolean("haptics",haptics).putBoolean("motion",reduceMotion).putBoolean("contrast",highContrastBall).putBoolean("colorblind",colorBlind).putBoolean("left",leftHanded).putBoolean("relative",relativeControl).putFloat("sensitivity",sensitivity).putFloat("text",textScale).putString("quality",quality.name)
        .putString("selected_ball_sprite",selectedBallSpriteName).putString("selected_ball_group",selectedBallGroupName)
        .putString("selected_ball_size",BallSize.DEFAULT.name).putString("selected_paddle_id",selectedPaddleId)
        .putString("selected_weapon_paddle_id",selectedWeaponPaddleId)
        .putString("selected_sticky_paddle_id",selectedStickyPaddleId).flush() } }
    private fun loadSettings() { with(settings) { masterVolume=prefs.getFloat("master",.8f);musicVolume=prefs.getFloat("music",.55f);sfxVolume=prefs.getFloat("sfx",.8f);haptics=prefs.getBoolean("haptics",true);reduceMotion=prefs.getBoolean("motion",false);highContrastBall=prefs.getBoolean("contrast",false);colorBlind=prefs.getBoolean("colorblind",false);leftHanded=prefs.getBoolean("left",false);relativeControl=prefs.getBoolean("relative",false);sensitivity=prefs.getFloat("sensitivity",1f);textScale=prefs.getFloat("text",1f);quality=runCatching{GraphicsQuality.valueOf(prefs.getString("quality","HIGH"))}.getOrDefault(GraphicsQuality.HIGH)
        selectedBallSpriteName=prefs.getString("selected_ball_sprite",CosmeticDefaults.BALL_SPRITE)
        selectedBallGroupName=prefs.getString("selected_ball_group",CosmeticDefaults.BALL_GROUP)
        // Ball size is gameplay-owned: every run starts Normal and size talismans change it temporarily.
        selectedBallBaseSize=BallSize.DEFAULT
        val legacyPaddleId=prefs.getString("selected_paddle_id",CosmeticDefaults.PADDLE_ID)
        selectedPaddleId=legacyPaddleId.takeIf { "_normal_" in it } ?: CosmeticDefaults.PADDLE_ID
        selectedWeaponPaddleId=prefs.getString("selected_weapon_paddle_id",
            legacyPaddleId.takeIf { "_weapon_" in it } ?: CosmeticDefaults.WEAPON_PADDLE_ID)
        selectedStickyPaddleId=prefs.getString("selected_sticky_paddle_id",
            legacyPaddleId.takeIf { "_sticky_" in it } ?: CosmeticDefaults.STICKY_PADDLE_ID) } }
}
