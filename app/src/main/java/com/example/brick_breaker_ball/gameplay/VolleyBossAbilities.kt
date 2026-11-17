package com.example.brick_breaker_ball.gameplay

import java.io.Serializable

data class VolleyBossAbilityState(
    val forecastTurn: Int = 0,
    val forecastTargets: List<Int> = emptyList(),
    val activeTurn: Int = 0,
    val activeTargets: Set<Int> = emptySet(),
    val consumedTargets: Set<Int> = emptySet(),
    val rootRenewals: Int = 0,
    val vulnerabilityThroughTurn: Int = 0
) : Serializable {
    companion object { private const val serialVersionUID = 1L }
}

/** Existing pieces only. Core, guards, passage gates and impact counters are excluded by caller. */
internal data class VolleyBossCandidate(val id: Int, val kind: String, val damaged: Boolean)

internal object VolleyBossAbilities {
    fun name(world: Int): String = when (world) {
        1 -> "ROOT RENEWAL"
        2 -> "SPORE VEIL"
        3 -> "TIDAL DEFLECTION"
        4 -> "SAND SHELTER"
        5 -> "FROST LOCK"
        6 -> "ECHO WARD"
        7 -> "MOON PHASE"
        8 -> "EMBER SURGE"
        9 -> "LIGHT / DARK CYCLE"
        else -> ""
    }

    fun interval(world: Int): Int = if (world in setOf(3, 6, 7)) 3 else 4

    fun forecast(world: Int, turn: Int, candidates: List<VolleyBossCandidate>,
                 previous: VolleyBossAbilityState): VolleyBossAbilityState {
        if (world !in 1..9 || turn < 2 || (turn + 1) % interval(world) != 0) return previous
        if (world == 1 && previous.rootRenewals >= 2) return previous // finite healing budget
        val eligible = candidates.filter { candidate -> when (world) {
            1 -> candidate.damaged
            3 -> candidate.kind == "VORTEX"
            4 -> candidate.kind == "SAND"
            5 -> candidate.kind == "ICE"
            7 -> candidate.kind == "ROTATING"
            8 -> candidate.kind == "BURN"
            9 -> candidate.kind == "PROTECTOR"
            else -> true
        } }.sortedBy { Math.floorMod(it.id.toLong() * 31L + turn.toLong() * 17L, 997L) }
        // Always leave an ordinary alternative; never freeze/shield the sole objective.
        val limit = if (world in setOf(3, 7, 8, 9)) 1 else (candidates.size - 1).coerceIn(0, 2)
        val targets = eligible.take(if (world in setOf(1, 2)) limit.coerceAtMost(1) else limit).map { it.id }
        return previous.copy(forecastTurn = if (targets.isEmpty()) 0 else turn + 1, forecastTargets = targets)
    }
}
