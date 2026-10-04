package com.example.brick_breaker_ball

import kotlin.math.ceil
import kotlin.math.max
import kotlin.math.min

enum class VolleyBrickType {
    NORMAL_HP,
    ROUGH_STONE,
    REGENERATING,
    POWERUP_CARRIER,
    DEATH_HAZARD_BRICK,
    ARMORED_THREE_HIT,
    TRANSPARENT_SLOW_PASS_THROUGH,
    EXPLOSIVE,
    SWITCH,
    GHOST,
    INDESTRUCTIBLE,
    LOCKED,
    KEY_BRICK,
    CHAIN_BRICK,
    BLACK_HOLE_TELEPORTER,
    SPIKED_HAZARD,
    MOVING_HORIZONTAL,
    MOVING_VERTICAL,
    BOSS_CORE,
}

enum class VolleyBrickSkin {
    GRAY, BROWN, ORANGE, RED, GOLD, GREEN, CYAN, BLUE, PURPLE, PINK
}

enum class VolleyBonusType {
    PLUS_BALL,
    HEART,
    SHIELD,
    MYSTERY,
    STAR,
    SPEED,
}

data class VolleyWorldDefinition(
    val id: Int,
    val name: String,
    val subtitle: String,
    val bossName: String,
    val preferredSkin: VolleyBrickSkin,
)

data class VolleyBrickSeed(
    val id: Int,
    val col: Int,
    val row: Int,
    val type: VolleyBrickType,
    val hp: Int,
    val maxHp: Int = hp,
    val skin: VolleyBrickSkin,
    val objective: Boolean = true,
    val groupId: Int = 0,
    val moveMin: Int = 0,
    val moveMax: Int = 10,
    val moveDirection: Int = 1,
)

data class VolleyRailSeed(
    val id: Int,
    val x1: Float,
    val y1: Float,
    val x2: Float,
    val y2: Float,
    val fixed: Boolean = false,
)

data class VolleyBonusSeed(
    val id: Int,
    val col: Float,
    val row: Float,
    val type: VolleyBonusType,
)

data class VolleyStageDefinition(
    val id: Int,
    val world: Int,
    val stage: Int,
    val name: String,
    val patternName: String,
    val startingBalls: Int,
    val ballSpeed: Float,
    val difficulty: Int,
    val bricks: List<VolleyBrickSeed>,
    val rails: List<VolleyRailSeed>,
    val bonuses: List<VolleyBonusSeed>,
    val bossName: String? = null,
) {
    val isBoss: Boolean get() = stage == VolleyCampaignRepository.STAGES_PER_WORLD
}

object VolleyCampaignRepository {
    const val WORLD_COUNT = 10
    const val STAGES_PER_WORLD = 20
    const val TOTAL_STAGES = WORLD_COUNT * STAGES_PER_WORLD
    const val COLUMNS = 11
    const val BOARD_ROWS = 18

    val worlds: List<VolleyWorldDefinition> = listOf(
        VolleyWorldDefinition(1, "VERDANT RUINS", "Living stone and ricochet gardens", "THE MOSS TITAN", VolleyBrickSkin.GREEN),
        VolleyWorldDefinition(2, "FROST CRYSTAL PASS", "Ice vaults, armor and narrow crystal lanes", "THE FROST WARDEN", VolleyBrickSkin.CYAN),
        VolleyWorldDefinition(3, "SANDSTONE CANYON", "Ancient locks buried beneath the dunes", "THE DUNE SENTINEL", VolleyBrickSkin.ORANGE),
        VolleyWorldDefinition(4, "INFERNO FORGE", "Explosive chains inside a volcanic foundry", "THE MAGMA ENGINE", VolleyBrickSkin.RED),
        VolleyWorldDefinition(5, "MOON SHRINE", "Ghost gates beneath a midnight moon", "THE LUNAR SHOGUN", VolleyBrickSkin.PURPLE),
        VolleyWorldDefinition(6, "CORAL DEPTHS", "Sunken passages and portal currents", "THE ABYSS LEVIATHAN", VolleyBrickSkin.BLUE),
        VolleyWorldDefinition(7, "BIOLUME CAVERN", "Glowing caverns with shifting arcane routes", "THE MYCELIUM ORACLE", VolleyBrickSkin.PINK),
        VolleyWorldDefinition(8, "IRON FOUNDRY", "Moving armor inside a machine fortress", "THE IRON COLOSSUS", VolleyBrickSkin.GRAY),
        VolleyWorldDefinition(9, "ASTRAL PALACE", "Celestial locks and energy corridors", "THE ASTRAL ARCHON", VolleyBrickSkin.GOLD),
        VolleyWorldDefinition(10, "PHANTOM MARSH", "Every lesson returns in the haunted endgame", "THE GRAVE WARDEN", VolleyBrickSkin.PURPLE),
    )

    val stages: List<VolleyStageDefinition> =
        (1..WORLD_COUNT).flatMap { world -> (1..STAGES_PER_WORLD).map { stage -> buildStage(world, stage) } }

    init {
        require(stages.size == TOTAL_STAGES)
        require(stages.count { it.isBoss } == WORLD_COUNT)
        require(stages.groupBy { it.world }.values.all { it.size == STAGES_PER_WORLD })
        require(stages.all { it.bricks.isNotEmpty() })
        require(stages.all { stageHasPlayableOpening(it) })
    }

    fun stage(globalId: Int): VolleyStageDefinition = stages[(globalId - 1).coerceIn(0, stages.lastIndex)]

    fun stage(world: Int, stage: Int): VolleyStageDefinition =
        stage((world.coerceIn(1, WORLD_COUNT) - 1) * STAGES_PER_WORLD + stage.coerceIn(1, STAGES_PER_WORLD))

    fun firstStage(world: Int): Int = (world.coerceIn(1, WORLD_COUNT) - 1) * STAGES_PER_WORLD + 1

    fun lastStage(world: Int): Int = firstStage(world) + STAGES_PER_WORLD - 1

    fun worldForStage(globalId: Int): Int = ((globalId.coerceIn(1, TOTAL_STAGES) - 1) / STAGES_PER_WORLD) + 1

    private fun buildStage(world: Int, stage: Int): VolleyStageDefinition {
        val theme = worlds[world - 1]
        val builder = StageBuilder(world, stage, theme)
        builder.buildGeometry()
        builder.applyWorldMechanics()
        if (stage == STAGES_PER_WORLD) builder.addBossFortress()
        builder.addRewards()

        return VolleyStageDefinition(
            id = (world - 1) * STAGES_PER_WORLD + stage,
            world = world,
            stage = stage,
            name = if (stage == STAGES_PER_WORLD) "${theme.bossName} • BOSS" else "STAGE ${stage.toString().padStart(2, '0')}",
            patternName = PATTERN_NAMES[stage - 1],
            startingBalls = min(44, 10 + (world - 1) * 2 + (stage - 1) / 3),
            ballSpeed = min(930f, 610f + (world - 1) * 22f + (stage - 1) * 4.5f),
            difficulty = (world - 1) * 20 + stage,
            bricks = builder.bricks.toList(),
            rails = builder.rails.toList(),
            bonuses = builder.bonuses.toList(),
            bossName = theme.bossName.takeIf { stage == STAGES_PER_WORLD },
        )
    }

    private val PATTERN_NAMES = listOf(
        "CENTER FUNNEL", "LEFT CHUTE", "RIGHT CHUTE", "TWIN LANES", "ARROWHEAD",
        "DIAMOND POCKET", "W GATES", "SAWTOOTH", "SPLIT VAULT", "REBOUND BOX",
        "SNAKE RUN", "TWIN CHAMBERS", "NEEDLE EYE", "CROSS FIRE", "PORTAL SPIRAL",
        "SWITCHBACK", "KEYHOLE", "RICOCHET GAUNTLET", "BOSS APPROACH", "BOSS FORTRESS",
    )

    private fun stageHasPlayableOpening(stage: VolleyStageDefinition): Boolean {
        val dangerRows = stage.bricks.filter { it.row <= 8 }.groupBy { it.row }
        return dangerRows.values.all { row -> row.map { it.col }.distinct().size < COLUMNS }
    }

    private class StageBuilder(
        private val world: Int,
        private val stage: Int,
        private val theme: VolleyWorldDefinition,
    ) {
        val bricks = mutableListOf<VolleyBrickSeed>()
        val rails = mutableListOf<VolleyRailSeed>()
        val bonuses = mutableListOf<VolleyBonusSeed>()
        private var brickId = 1
        private var railId = 1
        private var bonusId = 1

        private val mirror = (world + stage) % 2 == 0
        private val laneBias = ((world * 2 + stage / 4) % 3) - 1

        fun buildGeometry() {
            when (stage) {
                1 -> centerFunnel()
                2 -> leftChute()
                3 -> rightChute()
                4 -> twinLanes()
                5 -> arrowHead()
                6 -> diamondPocket()
                7 -> wGates()
                8 -> sawTooth()
                9 -> splitVault()
                10 -> reboundBox()
                11 -> snakeRun()
                12 -> twinChambers()
                13 -> needleEye()
                14 -> crossFire()
                15 -> portalSpiral()
                16 -> switchBack()
                17 -> keyHole()
                18 -> ricochetGauntlet()
                19 -> bossApproach()
                20 -> bossShell()
            }
        }

        private fun hp(row: Int, col: Int): Int {
            val baseBalls = 10 + (world - 1) * 2 + (stage - 1) / 3
            val stageFactor = .34f + world * .025f + stage * .013f
            val variation = ((row * 3 + col * 5 + world + stage) % 4) - 1
            return max(1, ceil(baseBalls * stageFactor).toInt() + variation)
        }

        private fun add(
            col: Int,
            row: Int,
            type: VolleyBrickType = VolleyBrickType.NORMAL_HP,
            hpOverride: Int? = null,
            objective: Boolean = type !in NON_OBJECTIVE_TYPES,
            groupId: Int = 0,
        ) {
            if (col !in 0 until COLUMNS || row !in 0..11) return
            val transformed = transformedCol(col)
            if (bricks.any { it.col == transformed && it.row == row }) return
            val logicalHp = hpOverride ?: when (type) {
                VolleyBrickType.ARMORED_THREE_HIT -> max(3, hp(row, col) + 2)
                VolleyBrickType.INDESTRUCTIBLE,
                VolleyBrickType.BLACK_HOLE_TELEPORTER,
                VolleyBrickType.SPIKED_HAZARD -> Int.MAX_VALUE
                VolleyBrickType.BOSS_CORE -> 5 + world * 2
                else -> hp(row, col)
            }
            bricks += VolleyBrickSeed(
                id = brickId++,
                col = transformed,
                row = row,
                type = type,
                hp = logicalHp,
                maxHp = logicalHp,
                skin = skinFor(type),
                objective = objective,
                groupId = groupId,
                moveMin = 0,
                moveMax = COLUMNS - 1,
                moveDirection = if ((row + col + world) % 2 == 0) 1 else -1,
            )
        }

        private fun transformedCol(col: Int): Int {
            val biased = when {
                col in 1..9 -> (col + laneBias).coerceIn(0, COLUMNS - 1)
                else -> col
            }
            return if (mirror) COLUMNS - 1 - biased else biased
        }

        private fun rail(x1: Float, y1: Float, x2: Float, y2: Float, fixed: Boolean = false) {
            rails += VolleyRailSeed(railId++, transformedRailX(x1), y1, transformedRailX(x2), y2, fixed)
        }

        private fun transformedRailX(x: Float): Float {
            val biased = (x + laneBias).coerceIn(0f, COLUMNS.toFloat())
            return if (mirror) COLUMNS - biased else biased
        }

        private fun bonus(col: Float, row: Float, type: VolleyBonusType) {
            val x = if (mirror) COLUMNS - 1f - col else col
            bonuses += VolleyBonusSeed(bonusId++, x.coerceIn(.2f, COLUMNS - 1.2f), row, type)
        }

        private fun fillRow(row: Int, gaps: Set<Int>, type: VolleyBrickType = VolleyBrickType.NORMAL_HP) {
            for (col in 0 until COLUMNS) if (col !in gaps) add(col, row, type)
        }

        private fun centerFunnel() {
            fillRow(0, setOf(5))
            fillRow(1, setOf(4, 5, 6))
            fillRow(2, setOf(3, 4, 5, 6, 7))
            fillRow(3, setOf(5))
            rail(2f, 4.2f, 4.6f, 6.4f)
            rail(9f, 4.2f, 6.4f, 6.4f)
            bonus(5f, 5.7f, VolleyBonusType.PLUS_BALL)
        }

        private fun leftChute() {
            for (row in 0..5) {
                val gap = min(8, 1 + row)
                fillRow(row, setOf(gap, (gap + 1).coerceAtMost(10)))
            }
            rail(1f, 6.5f, 5f, 3.0f)
            rail(5f, 3.0f, 8.5f, 3.0f)
            bonus(2f, 5.5f, VolleyBonusType.PLUS_BALL)
        }

        private fun rightChute() {
            for (row in 0..5) {
                val gap = max(2, 9 - row)
                fillRow(row, setOf(gap, (gap - 1).coerceAtLeast(0)))
            }
            rail(10f, 6.5f, 6f, 3.0f)
            rail(6f, 3.0f, 2.5f, 3.0f)
            bonus(8f, 5.5f, VolleyBonusType.PLUS_BALL)
        }

        private fun twinLanes() {
            for (row in 0..5) fillRow(row, setOf(2, 3, 7, 8))
            rail(.5f, 6.2f, 3.4f, 3.5f)
            rail(10.5f, 6.2f, 7.6f, 3.5f)
            bonus(2.5f, 5.4f, VolleyBonusType.PLUS_BALL)
            bonus(7.5f, 5.4f, VolleyBonusType.PLUS_BALL)
        }

        private fun arrowHead() {
            for (row in 0..6) {
                val half = min(4, row)
                fillRow(row, ((5 - half)..(5 + half)).toSet())
            }
            rail(1f, 6.8f, 5f, 3.4f)
            rail(10f, 6.8f, 6f, 3.4f)
            bonus(5f, 4.6f, VolleyBonusType.MYSTERY)
        }

        private fun diamondPocket() {
            for (row in 0..6) {
                val radius = when (row) { 0, 6 -> 1; 1, 5 -> 2; 2, 4 -> 3; else -> 4 }
                fillRow(row, ((5 - radius)..(5 + radius)).toSet())
            }
            rail(3f, 5.8f, 5f, 7.2f)
            rail(8f, 5.8f, 6f, 7.2f)
            rail(3f, 3.0f, 5f, 1.6f)
            rail(8f, 3.0f, 6f, 1.6f)
            bonus(5f, 5.0f, VolleyBonusType.PLUS_BALL)
        }

        private fun wGates() {
            for (row in 0..6) fillRow(row, setOf(1, 5, 9))
            rail(.5f, 6.8f, 2.5f, 4.7f)
            rail(2.5f, 4.7f, 5f, 6.8f)
            rail(5f, 6.8f, 7.5f, 4.7f)
            rail(7.5f, 4.7f, 10.5f, 6.8f)
            bonus(5f, 5.8f, VolleyBonusType.PLUS_BALL)
        }

        private fun sawTooth() {
            for (row in 0..7) {
                val gap = if (row % 2 == 0) setOf(1, 2, 6, 7) else setOf(3, 4, 8, 9)
                fillRow(row, gap)
            }
            rail(.5f, 7.5f, 3.5f, 5.4f)
            rail(3.5f, 5.4f, 6.5f, 7.5f)
            rail(6.5f, 7.5f, 10.5f, 5.1f)
            bonus(8.2f, 6.3f, VolleyBonusType.SPEED)
        }

        private fun splitVault() {
            for (row in 0..6) fillRow(row, if (row < 3) setOf(4, 5, 6) else setOf(1, 5, 9))
            rail(1f, 6.8f, 4.4f, 4.0f)
            rail(10f, 6.8f, 6.6f, 4.0f)
            rail(4.4f, 4.0f, 5.5f, 5.0f)
            rail(6.6f, 4.0f, 5.5f, 5.0f)
            bonus(5f, 5.5f, VolleyBonusType.SHIELD)
        }

        private fun reboundBox() {
            fillRow(0, setOf(5))
            fillRow(1, setOf(5))
            for (row in 2..6) fillRow(row, setOf(2, 5, 8))
            rail(2f, 6.8f, 2f, 3.0f)
            rail(9f, 6.8f, 9f, 3.0f)
            rail(2f, 3.0f, 4.7f, 1.8f)
            rail(9f, 3.0f, 6.3f, 1.8f)
            bonus(5f, 4.5f, VolleyBonusType.PLUS_BALL)
        }

        private fun snakeRun() {
            for (row in 0..7) {
                val center = when (row % 4) { 0 -> 2; 1 -> 4; 2 -> 7; else -> 5 }
                fillRow(row, setOf(center, (center + 1).coerceAtMost(10)))
            }
            rail(1f, 7.8f, 4f, 6.0f)
            rail(4f, 6.0f, 7.5f, 4.2f)
            rail(7.5f, 4.2f, 4.5f, 2.4f)
            bonus(6f, 5.0f, VolleyBonusType.MYSTERY)
        }

        private fun twinChambers() {
            for (row in 0..7) fillRow(row, setOf(2, 3, 5, 7, 8))
            rail(.7f, 7.5f, 3.6f, 5.5f)
            rail(4.3f, 4.0f, 1.2f, 2.2f)
            rail(10.3f, 7.5f, 7.4f, 5.5f)
            rail(6.7f, 4.0f, 9.8f, 2.2f)
            bonus(2.5f, 5.0f, VolleyBonusType.PLUS_BALL)
            bonus(7.5f, 5.0f, VolleyBonusType.PLUS_BALL)
        }

        private fun needleEye() {
            for (row in 0..8) {
                val gap = if (row < 4) 5 else if (row % 2 == 0) 4 else 6
                fillRow(row, setOf(gap))
            }
            rail(1f, 8.7f, 4.65f, 5.1f)
            rail(10f, 8.7f, 6.35f, 5.1f)
            bonus(5f, 6.0f, VolleyBonusType.STAR)
        }

        private fun crossFire() {
            for (row in 0..8) {
                val gaps = if (row % 3 == 0) setOf(1, 5, 9) else setOf(3, 7)
                fillRow(row, gaps)
            }
            rail(.5f, 8.5f, 5f, 4.2f)
            rail(10.5f, 8.5f, 6f, 4.2f)
            rail(2f, 2.8f, 9f, 2.8f)
            bonus(5f, 5.1f, VolleyBonusType.HEART)
        }

        private fun portalSpiral() {
            for (row in 0..8) {
                val gaps = when (row % 4) { 0 -> setOf(1, 2); 1 -> setOf(8, 9); 2 -> setOf(4, 5); else -> setOf(6, 7) }
                fillRow(row, gaps)
            }
            rail(1f, 8.5f, 4f, 6.4f)
            rail(9.5f, 6.1f, 6.5f, 4.1f)
            rail(2f, 3.8f, 5f, 1.8f)
            bonus(5f, 5.2f, VolleyBonusType.MYSTERY)
        }

        private fun switchBack() {
            for (row in 0..8) {
                val gaps = if (row % 2 == 0) setOf(2, 3, 8) else setOf(2, 7, 8)
                fillRow(row, gaps)
            }
            rail(.5f, 8.6f, 4f, 6.1f)
            rail(10.5f, 6.0f, 7f, 3.7f)
            rail(.5f, 3.4f, 4f, 1.3f)
            bonus(8f, 5.0f, VolleyBonusType.PLUS_BALL)
        }

        private fun keyHole() {
            for (row in 0..8) {
                val gaps = when {
                    row <= 2 -> setOf(5)
                    row <= 5 -> setOf(4, 5, 6)
                    else -> setOf(2, 5, 8)
                }
                fillRow(row, gaps)
            }
            rail(1f, 8.5f, 4.5f, 5.4f)
            rail(10f, 8.5f, 6.5f, 5.4f)
            bonus(5f, 4.7f, VolleyBonusType.PLUS_BALL)
        }

        private fun ricochetGauntlet() {
            for (row in 0..9) {
                val gaps = when (row % 4) {
                    0 -> setOf(1, 5, 9)
                    1 -> setOf(2, 6)
                    2 -> setOf(4, 8)
                    else -> setOf(3, 7)
                }
                fillRow(row, gaps)
            }
            rail(.5f, 9.3f, 3.2f, 7.1f)
            rail(10.5f, 9.3f, 7.8f, 7.1f)
            rail(2f, 5.5f, 5f, 3.5f)
            rail(9f, 5.5f, 6f, 3.5f)
            bonus(5f, 6.8f, VolleyBonusType.SHIELD)
        }

        private fun bossApproach() {
            for (row in 0..9) {
                val width = min(3, row / 3)
                val gaps = ((5 - width)..(5 + width)).toSet() + setOf(if (row % 2 == 0) 1 else 9)
                fillRow(row, gaps)
            }
            rail(.5f, 9.3f, 4.5f, 6.0f)
            rail(10.5f, 9.3f, 6.5f, 6.0f)
            rail(3f, 4.0f, 5f, 2.7f)
            rail(8f, 4.0f, 6f, 2.7f)
            bonus(5f, 7.0f, VolleyBonusType.PLUS_BALL)
            bonus(5f, 5.2f, VolleyBonusType.HEART)
        }

        private fun bossShell() {
            for (row in 0..9) {
                val gaps = when {
                    row <= 2 -> setOf(4, 5, 6)
                    row <= 5 -> setOf(2, 5, 8)
                    else -> setOf(1, 5, 9)
                }
                fillRow(row, gaps)
            }
            rail(.5f, 9.4f, 4.3f, 6.6f)
            rail(10.5f, 9.4f, 6.7f, 6.6f)
            rail(2.2f, 5.0f, 4.8f, 3.0f)
            rail(8.8f, 5.0f, 6.2f, 3.0f)
        }

        fun applyWorldMechanics() {
            if (stage <= 3) return
            val candidates = bricks.filter { it.type == VolleyBrickType.NORMAL_HP && it.row <= 8 }
            val replacements = mutableMapOf<Int, VolleyBrickType>()

            fun assignEvery(step: Int, type: VolleyBrickType, offset: Int = 0) {
                candidates.forEachIndexed { index, brick ->
                    if ((index + offset + stage + world) % step == 0) replacements.putIfAbsent(brick.id, type)
                }
            }

            when (world) {
                1 -> { if (stage >= 5) assignEvery(11, VolleyBrickType.ROUGH_STONE); if (stage >= 9) assignEvery(17, VolleyBrickType.REGENERATING, 3) }
                2 -> { assignEvery(10, VolleyBrickType.ARMORED_THREE_HIT); if (stage >= 8) assignEvery(18, VolleyBrickType.TRANSPARENT_SLOW_PASS_THROUGH, 2) }
                3 -> { assignEvery(14, VolleyBrickType.ARMORED_THREE_HIT); if (stage >= 10) installKeyLockPair() }
                4 -> { assignEvery(max(8, 14 - stage / 4), VolleyBrickType.EXPLOSIVE); if (stage >= 12) assignEvery(17, VolleyBrickType.CHAIN_BRICK, 4) }
                5 -> { if (stage >= 6) installSwitchGhostGroup(); assignEvery(15, VolleyBrickType.ROUGH_STONE, 2) }
                6 -> { if (stage >= 7) installPortalPair(); assignEvery(15, VolleyBrickType.TRANSPARENT_SLOW_PASS_THROUGH, 4) }
                7 -> { if (stage >= 5) installPortalPair(); if (stage >= 9) installSwitchGhostGroup(); if (stage >= 13) assignEvery(19, VolleyBrickType.MOVING_HORIZONTAL) }
                8 -> { assignEvery(9, VolleyBrickType.ARMORED_THREE_HIT); if (stage >= 7) assignEvery(16, VolleyBrickType.MOVING_HORIZONTAL, 2); if (stage >= 12) assignEvery(21, VolleyBrickType.MOVING_VERTICAL, 5) }
                9 -> { assignEvery(8, VolleyBrickType.ARMORED_THREE_HIT); if (stage >= 8) installKeyLockPair(); if (stage >= 14) installSwitchGhostGroup() }
                10 -> { assignEvery(12, VolleyBrickType.EXPLOSIVE); assignEvery(17, VolleyBrickType.REGENERATING, 5); if (stage >= 8) installPortalPair(); if (stage >= 11) installSwitchGhostGroup(); if (stage >= 13) installKeyLockPair(); if (stage >= 16) assignEvery(23, VolleyBrickType.SPIKED_HAZARD, 6) }
            }
            if (stage >= 6) assignEvery(19, VolleyBrickType.POWERUP_CARRIER, 7)
            if (stage >= 12 && world >= 4) assignEvery(29, VolleyBrickType.DEATH_HAZARD_BRICK, 11)

            if (replacements.isNotEmpty()) {
                for (i in bricks.indices) {
                    val type = replacements[bricks[i].id] ?: continue
                    val current = bricks[i]
                    val newHp = when (type) {
                        VolleyBrickType.ARMORED_THREE_HIT -> max(3, current.hp + 2)
                        VolleyBrickType.SPIKED_HAZARD -> Int.MAX_VALUE
                        else -> current.hp
                    }
                    bricks[i] = current.copy(type = type, hp = newHp, maxHp = newHp, skin = skinFor(type), objective = type !in NON_OBJECTIVE_TYPES)
                }
            }
        }

        private fun installKeyLockPair() {
            val normals = bricks.filter { it.type == VolleyBrickType.NORMAL_HP }.sortedBy { it.row * COLUMNS + it.col }
            if (normals.size < 4) return
            val group = world * 100 + stage
            replaceBrick(normals[normals.size / 4].id, VolleyBrickType.KEY_BRICK, group)
            replaceBrick(normals[(normals.size * 3) / 4].id, VolleyBrickType.LOCKED, group)
        }

        private fun installSwitchGhostGroup() {
            val normals = bricks.filter { it.type == VolleyBrickType.NORMAL_HP }.sortedBy { it.row * COLUMNS + it.col }
            if (normals.size < 5) return
            val group = world * 1000 + stage
            replaceBrick(normals[normals.size / 5].id, VolleyBrickType.SWITCH, group)
            replaceBrick(normals[normals.size / 2].id, VolleyBrickType.GHOST, group)
            replaceBrick(normals[(normals.size * 4) / 5].id, VolleyBrickType.GHOST, group)
        }

        private fun installPortalPair() {
            val normals = bricks.filter { it.type == VolleyBrickType.NORMAL_HP }.sortedBy { it.row * COLUMNS + it.col }
            if (normals.size < 4) return
            val group = world * 10000 + stage
            replaceBrick(normals[normals.size / 3].id, VolleyBrickType.BLACK_HOLE_TELEPORTER, group)
            replaceBrick(normals[(normals.size * 2) / 3].id, VolleyBrickType.BLACK_HOLE_TELEPORTER, group)
        }

        private fun replaceBrick(id: Int, type: VolleyBrickType, group: Int) {
            val index = bricks.indexOfFirst { it.id == id }
            if (index < 0) return
            val old = bricks[index]
            val newHp = when (type) {
                VolleyBrickType.BLACK_HOLE_TELEPORTER,
                VolleyBrickType.INDESTRUCTIBLE,
                VolleyBrickType.SPIKED_HAZARD -> Int.MAX_VALUE
                else -> old.hp
            }
            bricks[index] = old.copy(type = type, hp = newHp, maxHp = newHp, objective = type !in NON_OBJECTIVE_TYPES, groupId = group, skin = skinFor(type))
        }

        fun addBossFortress() {
            val coreCol = transformedCol(5)
            bricks.removeAll { it.row <= 2 && it.col in (coreCol - 1)..(coreCol + 1) }
            bricks += VolleyBrickSeed(brickId++, coreCol, 1, VolleyBrickType.BOSS_CORE, 5 + world * 2, 5 + world * 2, theme.preferredSkin, true, 90_000 + world)
            when (world) {
                1 -> { add(3, 2, VolleyBrickType.REGENERATING, 9); add(7, 2, VolleyBrickType.REGENERATING, 9) }
                2 -> { add(3, 2, VolleyBrickType.ARMORED_THREE_HIT, 11); add(7, 2, VolleyBrickType.ARMORED_THREE_HIT, 11) }
                3 -> installKeyLockPair()
                4 -> { add(3, 2, VolleyBrickType.EXPLOSIVE, 7); add(7, 2, VolleyBrickType.EXPLOSIVE, 7) }
                5 -> installSwitchGhostGroup()
                6, 7 -> installPortalPair()
                8 -> { add(2, 3, VolleyBrickType.MOVING_HORIZONTAL, 12); add(8, 3, VolleyBrickType.MOVING_HORIZONTAL, 12) }
                9 -> installKeyLockPair()
                10 -> { installPortalPair(); installSwitchGhostGroup(); add(1, 4, VolleyBrickType.SPIKED_HAZARD, objective = false); add(9, 4, VolleyBrickType.SPIKED_HAZARD, objective = false) }
            }
        }

        fun addRewards() {
            if (stage % 2 == 0) bonus(5f, 8.6f, VolleyBonusType.PLUS_BALL)
            if (stage % 5 == 0) bonus(if (stage % 10 == 0) 3.2f else 7.8f, 7.3f, VolleyBonusType.MYSTERY)
            if (stage in setOf(10, 15, 19)) bonus(5f, 9.4f, VolleyBonusType.SHIELD)
            if (stage == 20) bonus(5f, 9.0f, VolleyBonusType.HEART)
        }

        private fun skinFor(type: VolleyBrickType): VolleyBrickSkin = when (type) {
            VolleyBrickType.EXPLOSIVE -> VolleyBrickSkin.RED
            VolleyBrickType.REGENERATING -> VolleyBrickSkin.GREEN
            VolleyBrickType.TRANSPARENT_SLOW_PASS_THROUGH -> VolleyBrickSkin.CYAN
            VolleyBrickType.SWITCH, VolleyBrickType.GHOST -> VolleyBrickSkin.PURPLE
            VolleyBrickType.KEY_BRICK, VolleyBrickType.POWERUP_CARRIER, VolleyBrickType.BOSS_CORE -> VolleyBrickSkin.GOLD
            VolleyBrickType.ARMORED_THREE_HIT, VolleyBrickType.INDESTRUCTIBLE, VolleyBrickType.LOCKED -> VolleyBrickSkin.GRAY
            else -> theme.preferredSkin
        }
    }

    private val NON_OBJECTIVE_TYPES = setOf(
        VolleyBrickType.INDESTRUCTIBLE,
        VolleyBrickType.BLACK_HOLE_TELEPORTER,
        VolleyBrickType.SPIKED_HAZARD,
        VolleyBrickType.DEATH_HAZARD_BRICK,
    )
}
