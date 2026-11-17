package com.example.brick_breaker_ball.ui

/** Shared by contextual feedback and voluntary Help; matches the current source rules. */
internal object VolleyMechanicHints {
    val all = linkedMapOf(
        "SPIKE_TRAP" to "Spikes re-arm. Watch the reveal before your next shot.",
        "VORTEX" to "Vortices pulse on a cycle. Aim through the quiet window.",
        "BLOCKER" to "Wood and rust gates alternate each turn. Watch their open state.",
        "DYNAMITE" to "Dynamite opens nearby paths after its hit counter is cleared.",
        "SUICIDE" to "This explosive sends four beams when its hit counter is cleared.",
        "PROTECTOR" to "Break the protector to remove its nearby protection.",
        "ICE_FREEZE" to "Ice protects nearby targets. Fire can thaw frozen bricks.",
        "TORCH" to "Torches ignite balls. Fire can thaw ice and clear protection.",
        "BURN" to "Hot tiles consume balls while charges remain. Find a safe route.",
        "TELEPORT" to "A portal changes the ball's position. Follow the exit.",
        "ROTATING" to "Reflectors rotate between turns. Plan your next bank shot."
    )
}
