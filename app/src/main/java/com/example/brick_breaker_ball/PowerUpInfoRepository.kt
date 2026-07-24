package com.example.brick_breaker_ball

data class PowerUpInfo(val fullName:String,val shortName:String,val description:String)
object PowerUpInfoRepository{
    private val short=mapOf(
        PowerUpType.EXPAND_PADDLE to "WIDE PADDLE",PowerUpType.PIERCING_BALL to "PHASE BALL",PowerUpType.EXTRA_LIFE to "EXTRA LIFE",
        PowerUpType.SET_OFF_EXPLODING to "DETONATOR",PowerUpType.LEVEL_WARP to "LEVEL WARP",PowerUpType.FIRE_BALL to "FIRE BALL",
        PowerUpType.ZAP_BRICKS to "ZAP BRICKS",PowerUpType.MULTI_BALL to "MULTIBALL",PowerUpType.LASER_PADDLE to "LASER PADDLE",
        PowerUpType.MEGA_BALL to "MEGA BALL",PowerUpType.SLOW_BALL to "SLOW BALL",PowerUpType.STICKY_PADDLE to "STICKY",
        PowerUpType.EXPAND_EXPLODING to "BLAST RADIUS",PowerUpType.EIGHT_BALL to "EIGHT BALL",
        PowerUpType.KILL_PADDLE to "KILL PADDLE",PowerUpType.SHRINK_PADDLE to "PADDLE SHRUNK",
        PowerUpType.SUPER_SHRINK to "SUPER SHRINK",PowerUpType.FALLING_BRICKS to "FALLING BRICKS",
        PowerUpType.FAST_BALL to "FAST BALL",PowerUpType.SHRINK_BALL to "SHRINK BALL",
        PowerUpType.LASER_AUTO_CHARGE to "AUTO LASER",PowerUpType.TIMED_BOMB_BRICKS to "TIMED BOMBS",
        PowerUpType.MULTIBALL_PLUS_4 to "+4 BALLS",PowerUpType.DUAL_PADDLE to "DUAL PADDLE",
        PowerUpType.INSTANT_KILL_BALL to "BALL KILL",PowerUpType.ONE_HIT_ANY_BRICK to "ONE HIT",
        PowerUpType.GHOST_BALL to "GHOST BALL",PowerUpType.MULTIBALL_15 to "15 BALLS")
    fun info(type:PowerUpType)=PowerUpInfo(type.name.replace('_',' '),short[type]?:type.name.replace('_',' '),when(type){
        PowerUpType.EXPAND_PADDLE->"Widens the paddle for the level and stacks up to four times."
        PowerUpType.SHRINK_PADDLE->"Decreases the paddle by one bounded size level."
        PowerUpType.SUPER_SHRINK->"Sets the paddle to its minimum width for this level."
        PowerUpType.PIERCING_BALL->"Passes through breakable bricks; steel still reflects it."
        PowerUpType.FIRE_BALL->"Destroys the struck brick and burns nearby breakable bricks."
        PowerUpType.EXTRA_LIFE->"Adds one extra life."
        PowerUpType.KILL_PADDLE->"Destroys the paddle and removes exactly one life."
        PowerUpType.SET_OFF_EXPLODING->"Detonates every explosive brick with safe chain reactions."
        PowerUpType.LEVEL_WARP->"Completes a non-boss level instantly with the current score."
        PowerUpType.SHRINK_BALL->"Reduces every ball by one size level until the level ends."
        PowerUpType.ZAP_BRICKS->"Turns steel, armored, regenerating, and ghost bricks into one-hit bricks."
        PowerUpType.MEGA_BALL->"Temporarily increases every active ball by one size level."
        PowerUpType.FAST_BALL->"Temporarily speeds up every ball within the safe maximum."
        PowerUpType.SLOW_BALL->"Temporarily slows every ball within the safe minimum."
        PowerUpType.MULTI_BALL->"Doubles active balls up to eight with unique trajectories."
        PowerUpType.EIGHT_BALL->"Creates up to eight balls with distributed launch angles."
        PowerUpType.LASER_PADDLE->"Temporarily adds twin swept-collision laser cannons."
        PowerUpType.STICKY_PADDLE->"Temporarily catches balls for aimed relaunch."
        PowerUpType.EXPAND_EXPLODING->"Expands every explosion radius for this level."
        PowerUpType.FALLING_BRICKS->"Moves breakable bricks downward after paddle rebounds."
        PowerUpType.LASER_AUTO_CHARGE->"Automatically fires twin swept-collision laser shots with a fast recharge."
        PowerUpType.TIMED_BOMB_BRICKS->"Temporarily turns each brick struck by the ball into a timed bomb."
        PowerUpType.MULTIBALL_PLUS_4->"Temporarily adds four distinct balls without exceeding fifteen total."
        PowerUpType.DUAL_PADDLE->"Adds a second paddle alongside the main paddle for the effect duration."
        PowerUpType.INSTANT_KILL_BALL->"Temporarily turns four random eligible bricks into ball-killing spike hazards."
        PowerUpType.ONE_HIT_ANY_BRICK->"Breaks any breakable brick in one hit; unbreakable steel remains protected."
        PowerUpType.GHOST_BALL->"Passes through bricks without damaging or reflecting from them."
        PowerUpType.MULTIBALL_15->"Temporarily creates distinct trajectories until there are fifteen active balls."
        else->"Activates ${type.name.replace('_',' ').lowercase()} according to its gameplay rule."
    })
}
