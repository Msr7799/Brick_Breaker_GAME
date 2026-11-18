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

    private val worldBossHealth = listOf(35f, 45f, 60f, 75f, 90f, 110f, 130f, 150f)

    private fun placeWorldBoss(world: Int, bricks: MutableList<VolleyAuthoredBrickSeed>) {
        val width = 3f * TARGET_PITCH
        val height = 3f * TARGET_PITCH
        val originalCore = bricks.indexOfFirst { it.type == BrickType.BOSS_CORE && !it.suppressedByWorldBoss }
        // Y grows toward later waves. Keep the entire existing formation, with a clear
        // one-row gap before the finale instead of erasing tiles around an opening boss.
        val top = bricks.filter { !it.suppressedByWorldBoss && it.type != BrickType.BOSS_CORE }
            .maxOf { it.y + it.height }
        val core = VolleyAuthoredBrickSeed(
            sourceRow = -1, sourceColumn = 5,
            x = VolleySession.SOURCE_LANE_LEFT + (11f * TARGET_PITCH - width) / 2f,
            y = top + TARGET_PITCH, width = width, height = height,
            type = BrickType.BOSS_CORE, health = 1, shape = VolleyPieceShape.RECTANGLE,
            countsForClear = true, sourceKind = "BOSS",
            sourceScene = null,
            sourceBaseHealth = if (originalCore >= 0) bricks[originalCore].sourceBaseHealth
                else worldBossHealth[world - 1]
        )
        if (originalCore >= 0) bricks[originalCore] = core else bricks += core

        // A world boss must have more than one attack lane. The RPG source can place
        // indestructible BLOCKER tiles across the entire board directly below the core.
        // Keep their stable IDs and silhouettes, but turn selected blockers into
        // two-hit doors so the player can open *two* routes through the fortress.
        // Existing save restoration recognizes these as authored breakable gates.
        val laneCenters = listOf(3f, 8f).map { column ->
            VolleySession.SOURCE_LANE_LEFT + column * TARGET_PITCH
        }
        val playableBlockers = bricks.indices.filter { index ->
            val brick = bricks[index]
            !brick.suppressedByWorldBoss &&
                brick.sourceKind == "BLOCKER" &&
                brick.type == BrickType.INDESTRUCTIBLE &&
                brick.groupId == 0 &&
                brick.y + brick.height <= core.y &&
                brick.y + brick.height >= core.y - 9f * TARGET_PITCH
        }
        for (laneCenter in laneCenters) {
            // Convert every wall tile intersecting this lane, not just one brick
            // beside the boss: a second solid row must not seal the passage.
            for (index in playableBlockers) {
                val brick = bricks[index]
                if (brick.x < laneCenter + TARGET_PITCH * .3f &&
                    brick.x + brick.width > laneCenter - TARGET_PITCH * .3f) {
                    bricks[index] = brick.copy(
                        type = BrickType.ARMORED_DARK,
                        health = 2,
                        countsForClear = true,
                        shieldLock = false,
                        sourceKind = "BLOCKER",
                        sourceBaseHealth = null,
                        sourceBaseHealthExact = null
                    )
                }
            }
        }

        val count = when (world) { in 1..3 -> 2; in 4..6 -> 3; else -> 4 }
        // Replace ordinary square enemies only: preserve collision, IDs, passages,
        // keys, bombs, movers and every other authored puzzle relationship.
        val candidates = bricks.indices.filter { index ->
            val b = bricks[index]
            b.type.breakable && b.countsForClear && !b.suppressedByWorldBoss &&
                b.shape == VolleyPieceShape.RECTANGLE && b.groupId == 0 &&
                b.sourceKind in setOf("GRASS", "STONE", "SAND", "BLACK", "ICE", "BURN", "ENEMY") &&
                b.sourceBaseHealth != null
        }
        require(candidates.size >= count) { "World $world needs $count reachable boss guards" }
        val bottom = candidates.minOf { bricks[it].y }
        val ceiling = candidates.maxOf { bricks[it].y }
        val remaining = candidates.toMutableSet()
        repeat(count) { guard ->
            val targetY = bottom + (ceiling - bottom) * (.3f + .6f * (guard + .5f) / count)
            val column = listOf(2, 8, 5, 1)[(guard + world) % 4]
            val targetX = VolleySession.SOURCE_LANE_LEFT + column * TARGET_PITCH
            val index = remaining.minBy { index ->
                val b = bricks[index]
                kotlin.math.abs(b.y - targetY) * 2f + kotlin.math.abs(b.x - targetX)
            }
            remaining.remove(index)
            val brick = bricks[index]
            bricks[index] = brick.copy(type = BrickType.ARMORED_DARK, shieldLock = true,
                sourceKind = "BOSS_GUARD", sourceScene = null,
                sourceBaseHealth = brick.sourceBaseHealth!! * .5f,
                sourceBaseHealthExact = (brick.sourceBaseHealthExact ?: brick.sourceBaseHealth!!.toDouble()) * .5)
        }
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
                sourceBaseHealthExact = if (playableGate || node.has("sourceHits") || !node.has("sourceHealth")) null
                    else node.getDouble("sourceHealth"),
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
        if (local == VolleyStageRepository.worldLevelCount(world)) {
            placeWorldBoss(world, bricks)
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
