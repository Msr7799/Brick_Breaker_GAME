/*
 * يحمّل تشكيلات مراحل Volley المحفوظة ويحوّل مواضع البركس إلى إحداثيات ساحة اللعب.
 */
package com.example.brick_breaker_ball.campaign

import com.badlogic.gdx.Gdx
import com.badlogic.gdx.files.FileHandle
import com.badlogic.gdx.utils.JsonReader
import com.badlogic.gdx.utils.JsonValue
import com.example.brick_breaker_ball.gameplay.BrickType
import com.example.brick_breaker_ball.gameplay.VolleyPieceShape
import com.example.brick_breaker_ball.gameplay.VolleySession
import java.io.File
import java.security.MessageDigest

internal object VolleySourceStageData {
    private const val ASSET = "volley-source/stages.json"
    private const val GATES_ASSET = "volley-source/solvability_gates.json"
    private const val SPECIALS_ASSET = "volley-source/special-brick-behaviors.json"
    private const val SOURCE_PITCH = VolleySession.SOURCE_BRICK_PITCH
    // Preserve each tile's fraction of the visible board width (64/720 in RPG).
    private const val TARGET_PITCH = VolleySession.TARGET_SOURCE_BRICK_PITCH
    private const val SCALE = TARGET_PITCH / SOURCE_PITCH
    // Keep the first wave high enough to reach the portrait viewport ceiling while leaving
    // the existing authored rows above it for later descents.
    private const val INITIAL_WAVE_BOTTOM = VolleySession.SOURCE_WAVE_BOTTOM

    private data class BossPlacement(val x: Float, val y: Float, val baseHealth: Float)

    // Each world uses its own boss portrait on one physical core. Keep the surrounding
    // imported formation, but never attach a alternate boss sprite to that core.
    private val worldBosses = listOf(
        BossPlacement(330f, 1650f, 35f),
        BossPlacement(330f, 1650f, 45f),
        BossPlacement(330f, 1630f, 60f),
        BossPlacement(330f, 1710f, 75f),
        BossPlacement(330f, 1650f, 90f),
        BossPlacement(250f, 1550f, 110f),
        BossPlacement(330f, 1650f, 130f),
        BossPlacement(170f, 1290f, 150f)
    )

    private fun placeWorldBoss(world: Int, bricks: MutableList<VolleyAuthoredBrickSeed>) {
        val placement = worldBosses[world - 1]
        val width = 3f * TARGET_PITCH
        val height = 3f * TARGET_PITCH
        // Keep every original index stable for saved games and orb receipts. Intersecting
        // bricks become inactive, leaving only the actual boss collision in this space.
        bricks.indices.forEach { index ->
            val brick = bricks[index]
            if (brick.x < placement.x + width && brick.x + brick.width > placement.x &&
                brick.y < placement.y + height && brick.y + brick.height > placement.y) {
                bricks[index] = brick.copy(suppressedByWorldBoss = true)
            }
        }
        bricks += VolleyAuthoredBrickSeed(
            sourceRow = -1, sourceColumn = 5,
            x = placement.x, y = placement.y, width = width, height = height,
            type = BrickType.BOSS_CORE, health = 1, shape = VolleyPieceShape.RECTANGLE,
            countsForClear = true, sourceKind = "BOSS",
            sourceScene = null,
            sourceBaseHealth = placement.baseHealth
        )
    }
    private fun asset(name: String): FileHandle = if (Gdx.files != null) Gdx.files.internal(name) else {
        val candidates = listOf(File("app/src/main/assets/$name"), File("src/main/assets/$name"))
        FileHandle(candidates.firstOrNull(File::isFile) ?: error("Missing $name"))
    }
    private val sourceBytes: ByteArray by lazy { asset(ASSET).readBytes() }
    private val root: JsonValue by lazy { JsonReader().parse(String(sourceBytes, Charsets.UTF_8)) }
    private val specialPlan: JsonValue by lazy {
        JsonReader().parse(asset(SPECIALS_ASSET)).also { plan ->
            val digest = MessageDigest.getInstance("SHA-256").digest(sourceBytes)
                .joinToString("") { "%02x".format(it.toInt() and 0xff) }
            require(plan.getString("sourceSha256") == digest) { "Stale Volley special-brick metadata" }
        }
    }
    private val gatePlan: JsonValue by lazy {
        val plan = JsonReader().parse(asset(GATES_ASSET))
        val digest = MessageDigest.getInstance("SHA-256").digest(sourceBytes)
            .joinToString("") { "%02x".format(it.toInt() and 0xff) }
        require(plan.getString("sourceSha256") == digest) {
            "Volley reachability plan is stale; regenerate its stage data."
        }
        plan
    }
    private val stages: List<JsonValue> by lazy {
        buildList {
            var node = root.get("stages").child
            while (node != null) {
                add(node)
                node = node.next
            }
        }.also { require(it.size == 160) { "Volley campaign must contain exactly 160 stages" } }
    }

    fun forLevel(levelId: Int, folder: String): VolleyStageDefinition {
        val entry = stages[levelId - 1]
        require(entry.getInt("id") == levelId)
        val gateIndices = buildSet {
            var gate = gatePlan.get("stages").get(levelId.toString())?.child
            while (gate != null) {
                add(gate.asInt())
                gate = gate.next
            }
        }
        val world = entry.getInt("world")
        val local = entry.getInt("local")
        val image = entry.getInt("image")
        val bricks = mutableListOf<VolleyAuthoredBrickSeed>()
        var node = entry.get("bricks").child
        while (node != null) {
            val index = bricks.size
            val kind = node.getString("kind")
            val overrides = specialPlan.get("stages").get(levelId.toString())?.get(index.toString())
            val playableGate = index in gateIndices
            require(!playableGate || (kind == "BLOCKER" && !node.has("sourceHealth") &&
                node.getString("shape") in setOf("SQ", "NW", "NE", "SW", "SE"))) {
                "Invalid Volley passage in stage $levelId at brick $index"
            }
            val sourceX = node.getFloat("x")
            val sourceY = node.getFloat("y")
            val collisionOffsetY = if (node.has("collisionOffsetY")) node.getFloat("collisionOffsetY") else 0f
            val width = node.getFloat("width") * SCALE
            val height = node.getFloat("height") * SCALE
            // Stage data resolves each collision polygon to its solid corner.
            // Filename suffixes are unreliable, including on the ice blocker variants.
            val shape = when (node.getString("shape")) {
                "NW" -> VolleyPieceShape.TRIANGLE_NW
                "NE" -> VolleyPieceShape.TRIANGLE_NE
                "SW" -> VolleyPieceShape.TRIANGLE_SW
                "SE" -> VolleyPieceShape.TRIANGLE_SE
                "SQ" -> VolleyPieceShape.RECTANGLE
                else -> error("Unknown authored brick shape in level $levelId")
            }
            val type = when (kind) {
                "DYNAMITE", "SUICIDE" -> BrickType.EXPLOSIVE
                "TELEPORT" -> BrickType.BLACK_HOLE_TELEPORTER
                "SPIKE_TRAP" -> BrickType.SPIKED_HAZARD
                "BONUS_BALLS" -> BrickType.POWERUP_CARRIER
                "CHEST" -> BrickType.RANDOM_INVENTORY_POWERUP
                "ICE_FREEZE" -> BrickType.ARMORED_THREE_HIT
                "OSB" -> BrickType.ARMORED_THREE_HIT
                "VORTEX", "TORCH", "ROTATING" -> BrickType.INDESTRUCTIBLE
                "SLIDER" -> if (node.has("sourceHealth")) BrickType.ARMORED_DARK else BrickType.INDESTRUCTIBLE
                "BLOCKER" -> if (node.has("sourceHealth") || playableGate) BrickType.ARMORED_DARK else BrickType.INDESTRUCTIBLE
                "BOSS" -> BrickType.BOSS_CORE
                else -> BrickType.ARMORED_THREE_HIT
            }
            val sourceBaseHealth = if (node.has("sourceHealth")) node.getFloat("sourceHealth") else null
            val sourceHealth = when {
                playableGate -> 1
                node.has("sourceHits") -> node.getInt("sourceHits").coerceAtLeast(1)
                // Each placed enemy is scaled when a stage starts.
                // Retain the source HP instead of treating its unscaled scene default as HP 1.
                sourceBaseHealth != null -> VolleySourceHealth.initialHealth(
                    sourceBaseHealth, levelId, VolleyDifficulty.NORMAL
                )
                else -> 1
            }
            // Scripted impact counters and the new passage gates keep their literal hit counts.
            val group = if (node.has("connected_to_destructible_number")) node.getInt("connected_to_destructible_number") else 0
            bricks += VolleyAuthoredBrickSeed(
                sourceRow = (-sourceY / SOURCE_PITCH).toInt(),
                sourceColumn = ((sourceX - 40f) / SOURCE_PITCH).toInt(),
                x = VolleySession.SOURCE_LANE_LEFT + (sourceX - 8f) * SCALE - width / 2f,
                y = VolleySession.BOARD_TOP - (sourceY + collisionOffsetY + 40f) * SCALE - height / 2f,
                width = width,
                height = height,
                type = type,
                health = sourceHealth,
                shape = shape,
                groupId = group,
                countsForClear = type.breakable && kind !in setOf("BONUS_BALLS", "VORTEX", "TORCH", "ROTATING"),
                sourceKind = kind,
                sourceScene = node.getString("sourceScene"),
                sourceBonusValue = when (kind) {
                    "BONUS_BALLS" -> if (node.has("ball_increase")) node.getInt("ball_increase") else 5
                    "BONUS_DAMAGE" -> if (node.has("attack_increase")) node.getInt("attack_increase") else 1
                    else -> 0
                },
                connectedBombGroup = if (node.has("connected_to_bomb_number"))
                    node.getInt("connected_to_bomb_number") else 0,
                bombDestroyable = node.getString("sourceScene").contains("/DestroyableDF/", ignoreCase = true),
                slideHorizontal = overrides?.getInt("left_right_distance", node.getInt("left_right_distance", 0))
                    ?: node.getInt("left_right_distance", 0),
                slideVertical = overrides?.getInt("up_down_distance", node.getInt("up_down_distance", 0))
                    ?: node.getInt("up_down_distance", 0),
                slideStartsMoved = overrides?.getBoolean("moved", node.getBoolean("moved", true))
                    ?: node.getBoolean("moved", true),
                sourceBaseHealth = if (playableGate || node.has("sourceHits")) null else sourceBaseHealth,
                sourceBurnCharges = (overrides?.getInt("burns_until_depleted", 1)
                    ?: node.getInt("burns_until_depleted", 1)).coerceAtLeast(0),
                sourceProtectionPerRound = (overrides?.getInt("protection_per_round", 2)
                    ?: node.getInt("protection_per_round", 2)).coerceAtLeast(0)
            )
            node = node.next
        }
        require(gateIndices.all { it in bricks.indices }) { "Volley passage index outside stage $levelId" }
        // Source scenes have different lengths. Offset the entire authored formation as one
        // group, preserving every relative position and every preplaced row above the viewport.
        val initialOffset = INITIAL_WAVE_BOTTOM - (bricks.minOfOrNull { it.y } ?: INITIAL_WAVE_BOTTOM)
        for (index in bricks.indices) bricks[index] = bricks[index].copy(y = bricks[index].y + initialOffset)
        // The original bosses at 40/80/120 fall mid-world after the nine-world split.
        // Move those encounters to world finals without changing the remaining board.
        if (levelId in setOf(40, 80, 120)) {
            bricks.indices.forEach { index ->
                if (bricks[index].type == BrickType.BOSS_CORE) {
                    // Keep the stable saved-brick index without retaining the old RPG boss asset.
                    bricks[index] = bricks[index].copy(suppressedByWorldBoss = true, sourceScene = null)
                }
            }
        }
        if (local == VolleyStageRepository.worldLevelCount(world) && levelId != 160) {
            placeWorldBoss(world, bricks)
        }
        if (levelId == 160) {
            // The ninth finale already contains one source core. Preserve its ID, position,
            // collision and HP while replacing only the alternate boss artwork.
            bricks.indices.forEach { index ->
                if (bricks[index].type == BrickType.BOSS_CORE) {
                    bricks[index] = bricks[index].copy(sourceScene = null)
                }
            }
        }
        val bossStage = bricks.any { it.type == BrickType.BOSS_CORE && !it.suppressedByWorldBoss }
        val rows = 32
        return VolleyStageDefinition(
            levelId = levelId, world = world, stage = local,
            name = if (bossStage) "WORLD CORE" else "STAGE $local",
            rows = rows, columns = 11,
            cells = List(rows) { List<VolleyBrickSeed?>(11) { null } },
            authoredBricks = bricks, rails = emptyList(), pickups = emptyList(),
            startingBalls = (12 + levelId / 6).coerceAtMost(44),
            baseHp = 1,
            descentDistance = TARGET_PITCH,
            parTurns = 8, bonusStage = false, bossStage = bossStage,
            backgroundAsset = "volley-world/$folder/$image.webp",
            sourceScene = entry.getString("source")
        )
    }
}
