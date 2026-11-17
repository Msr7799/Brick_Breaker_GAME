package com.example.brick_breaker_ball.gameplay

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class RpgAlternatingBlockerRulesTest {
    @Test
    fun `only recovered wooden and rust alternating blockers are gates`() {
        assertTrue(RpgAlternatingBlockerRules.isSourceGate(
            "res://Enemies/Blocker/Alt_Blocker/enemy_wooden_alt_blocker_2x2.tscn"))
        assertTrue(RpgAlternatingBlockerRules.isSourceGate(
            "res://Enemies/Blocker/Alt_Blocker/enemy_rust_alt_blocker_2x2.tscn"))
        assertFalse(RpgAlternatingBlockerRules.isSourceGate(
            "res://Enemies/Blocker/Stone1/enemy_stone1_2x2.tscn"))
        assertFalse(RpgAlternatingBlockerRules.isSourceGate(
            "res://Enemies/Spike_Traps/enemy_spike_trap_2x2.tscn"))
        assertFalse(RpgAlternatingBlockerRules.isSourceGate(null))
    }

    @Test
    fun `godot starts open and toggles on each descent`() {
        assertTrue(RpgAlternatingBlockerRules.openAtTurn(1))
        assertFalse(RpgAlternatingBlockerRules.openAtTurn(2))
        assertTrue(RpgAlternatingBlockerRules.openAtTurn(3))
        assertFalse(RpgAlternatingBlockerRules.openAtTurn(4))
    }

    @Test
    fun `saved turn fully restores gate state without migration`() {
        val savedTurns = listOf(1, 2, 3, 8, 9, 120, 121)
        for (turn in savedTurns) {
            val before = RpgAlternatingBlockerRules.openAtTurn(turn)
            val restored = RpgAlternatingBlockerRules.openAtTurn(turn)
            assertTrue(before == restored)
        }
    }
}
