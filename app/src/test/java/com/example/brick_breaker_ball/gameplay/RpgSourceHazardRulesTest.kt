package com.example.brick_breaker_ball.gameplay

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class RpgSourceHazardRulesTest {
    @Test fun `spikes show after godot startup and hide for ten seconds after a kill`() {
        val rules = RpgSourceHazardRules
        assertFalse(rules.spikeArmed(0f, 0f))
        assertFalse(rules.spikeArmed(0f, .099f))
        assertTrue(rules.spikeArmed(0f, .1f))
        assertFalse(rules.spikeArmed(10f, 5f))
        assertFalse(rules.spikeArmed(.01f, 15f))
        assertTrue(rules.spikeArmed(0f, 15f))
        assertEquals(0f, rules.spikeVisualStrength(0f, .05f), .0001f)
        assertEquals(.5f, rules.spikeVisualStrength(0f, .2f), .0001f)
        assertEquals(1f, rules.spikeVisualStrength(0f, .3f), .0001f)
        assertEquals(1f, rules.spikeVisualStrength(10f, 20f), .0001f)
        assertEquals(0f, rules.spikeVisualStrength(9.8f, 20f), .0001f)
        assertEquals(0f, rules.spikeVisualStrength(.2f, 20f), .0001f)
        assertEquals(.5f, rules.spikeVisualStrength(.1f, 20f), .0001f)
        assertEquals(1f, rules.spikeVisualStrength(0f, 20f), .0001f)
    }

    @Test fun `vortex cycles match three recovered script variants`() {
        val rules = RpgSourceHazardRules
        val rock = "res://Enemies/Vortex/Rock1_Vortex/enemy_rock_vortex_2x2_SQ.tscn"
        val ice = "res://Enemies/Vortex/Ice1_Vortex/enemy_ice_vortex_2x_2_sq.tscn"
        val black = "res://Enemies/Vortex/Black1_vortex/enemy_black_vortex_2x_2_sq.tscn"
        assertEquals(2f, rules.vortexTiming(rock).activeSeconds, 0f)
        assertEquals(4f, rules.vortexTiming(rock).inactiveSeconds, 0f)
        assertTrue(rules.vortexActive(rock, 1.99f))
        assertFalse(rules.vortexActive(rock, 2f))
        assertTrue(rules.vortexActive(rock, 6f))
        assertEquals(3f, rules.vortexTiming(ice).activeSeconds, 0f)
        assertEquals(3f, rules.vortexTiming(ice).inactiveSeconds, 0f)
        assertTrue(rules.vortexActive(ice, 2.99f))
        assertFalse(rules.vortexActive(ice, 3f))
        assertTrue(rules.vortexActive(ice, 6f))
        assertEquals(4f, rules.vortexTiming(black).activeSeconds, 0f)
        assertEquals(2.5f, rules.vortexTiming(black).inactiveSeconds, 0f)
        assertTrue(rules.vortexActive(black, 3.99f))
        assertFalse(rules.vortexActive(black, 4f))
        assertTrue(rules.vortexActive(black, 6.5f))
    }
}
