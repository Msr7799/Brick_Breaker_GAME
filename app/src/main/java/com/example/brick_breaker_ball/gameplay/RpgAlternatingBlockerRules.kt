package com.example.brick_breaker_ball.gameplay

/**
 * Rules ported from the recovered Godot wooden/rust Alt_Blocker scripts.
 * Shared by the live Volley engine and pure JVM regression tests.
 *
 * Original source:
 * - /Enemies/Blocker/Alt_Blocker/enemy_wooden_alt_blocker.gd
 * - /Enemies/Blocker/Alt_Blocker/enemy_rust_alt_blocker_2x2.gd
 * - /Enemies/Enemies.gd (open_close_animation on each descent)
 *
 * No state fields are added: the existing saved turn is the source of truth.
 */
internal object RpgAlternatingBlockerRules {
    private const val SOURCE_FOLDER = "/Blocker/Alt_Blocker/"

    fun isSourceGate(sourceScene: String?): Boolean =
        sourceScene?.contains(SOURCE_FOLDER) == true

    fun openAtTurn(turn: Int): Boolean = turn % 2 == 1
}
