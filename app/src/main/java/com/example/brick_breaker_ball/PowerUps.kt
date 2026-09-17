/*
 * ملاحظات صيانة الملف:
 * المسار: app/src/main/java/com/example/brick_breaker_ball/PowerUps.kt
 * المؤلف: mohamed alromaihi
 * الدوال الموجودة: `update`، `choose`، `activate`، `deactivate`، `clearLifeScoped`، `clear`، `persistentSnapshot`، `restorePersistent`، `activeEffects`، `contains`
 */

package com.example.brick_breaker_ball

import java.util.Random

enum class PowerUpCategory { GOOD, BAD, SPECIAL }
enum class EffectScope { INSTANT, TIMED, LIFE, LEVEL }
enum class StackPolicy { IGNORE_IF_ACTIVE, REFRESH_DURATION, ADD_DURATION, INCREASE_STRENGTH, REPLACE, CANCEL_OPPOSITE }
enum class CapsuleStyle { ROUND_GOOD, HEX_BAD }
enum class PowerUpType {
    EXPAND_PADDLE,
    STICKY_PADDLE,
    LASER_PADDLE,
    MAGNETIC_PADDLE,
    MULTI_BALL,
    TRIPLE_BALL,
    EXTRA_LIFE,
    SLOW_BALL,
    FIRE_BALL,
    PIERCING_BALL,
    EXPLOSIVE_BALL,
    BOTTOM_SHIELD,
    SCORE_X2,
    SCORE_X3,
    POWERUP_MAGNET,
    PADDLE_SHIELD,
    RANDOM_GOOD,
    SHRINK_PADDLE,
    FAST_BALL,
    INVERT_CONTROLS,
    SLIPPERY_PADDLE,
    POWERUP_JAM,
    RANDOM_BAD,
    KILL_PADDLE,
    SET_OFF_EXPLODING,
    LEVEL_WARP,
    SHRINK_BALL,
    ZAP_BRICKS,
    MEGA_BALL,
    SUPER_SHRINK,
    EXPAND_EXPLODING,
    FALLING_BRICKS,
    EIGHT_BALL,
    LASER_AUTO_CHARGE,
    TIMED_BOMB_BRICKS,
    MULTIBALL_PLUS_4,
    DUAL_PADDLE,
    INSTANT_KILL_BALL,
    ONE_HIT_ANY_BRICK,
    GHOST_BALL,
    MULTIBALL_15
}

data class PowerUpDefinition(
    val id: String,
    val type: PowerUpType,
    val category: PowerUpCategory,
    val scope: EffectScope,
    val durationSeconds: Float?,
    val stackPolicy: StackPolicy,
    val maxStacks: Int,
    val dropWeight: Float,
    val conflictsWith: Set<PowerUpType>,
    val icon: String,
    val capsuleStyle: CapsuleStyle
)

object PowerUpCatalog {
    private val bad = setOf(
        PowerUpType.SHRINK_PADDLE, PowerUpType.FAST_BALL, PowerUpType.INVERT_CONTROLS,
        PowerUpType.SLIPPERY_PADDLE, PowerUpType.POWERUP_JAM, PowerUpType.RANDOM_BAD,
        PowerUpType.KILL_PADDLE, PowerUpType.SHRINK_BALL, PowerUpType.SUPER_SHRINK, PowerUpType.FALLING_BRICKS,
        PowerUpType.INSTANT_KILL_BALL
    )
    private val special = setOf(PowerUpType.GHOST_BALL)
    private val instant = setOf(
        PowerUpType.MULTI_BALL, PowerUpType.TRIPLE_BALL, PowerUpType.EXTRA_LIFE,
        PowerUpType.RANDOM_GOOD, PowerUpType.RANDOM_BAD,
        PowerUpType.KILL_PADDLE, PowerUpType.SET_OFF_EXPLODING, PowerUpType.LEVEL_WARP,
        PowerUpType.ZAP_BRICKS, PowerUpType.EIGHT_BALL,
        PowerUpType.MULTIBALL_PLUS_4, PowerUpType.MULTIBALL_15
    )
    private val levelScoped = setOf(
        PowerUpType.EXPAND_PADDLE,
        PowerUpType.SHRINK_PADDLE,
        PowerUpType.SUPER_SHRINK,
        PowerUpType.SHRINK_BALL,
        PowerUpType.EXPAND_EXPLODING,
        PowerUpType.FALLING_BRICKS
    )
    private val lifeScoped = setOf(PowerUpType.BOTTOM_SHIELD, PowerUpType.PADDLE_SHIELD)

    /** Stable order matching the annotated 20-talisman source sheet. */
    val classicOrderedTypes = listOf(
        PowerUpType.LASER_AUTO_CHARGE, PowerUpType.EXTRA_LIFE, PowerUpType.KILL_PADDLE,
        PowerUpType.EXPAND_PADDLE, PowerUpType.TIMED_BOMB_BRICKS, PowerUpType.RANDOM_GOOD, PowerUpType.SHRINK_BALL,
        PowerUpType.SHRINK_PADDLE, PowerUpType.FIRE_BALL, PowerUpType.MAGNETIC_PADDLE, PowerUpType.SLOW_BALL,
        PowerUpType.MULTIBALL_PLUS_4, PowerUpType.DUAL_PADDLE, PowerUpType.FAST_BALL, PowerUpType.INSTANT_KILL_BALL,
        PowerUpType.MEGA_BALL, PowerUpType.STICKY_PADDLE, PowerUpType.ONE_HIT_ANY_BRICK,
        PowerUpType.GHOST_BALL, PowerUpType.MULTIBALL_15
    )
    val classicTypes = classicOrderedTypes.toSet()

    /** Every world exposes the complete 20-talisman set; rotation only changes deterministic ordering. */
    fun worldDropTypes(world: Int): List<PowerUpType> {
        if (classicOrderedTypes.isEmpty()) return emptyList()
        val offset = ((world - 1).coerceAtLeast(0) * 3) % classicOrderedTypes.size
        return classicOrderedTypes.drop(offset) + classicOrderedTypes.take(offset)
    }

    private val icons = mapOf(
        PowerUpType.LASER_AUTO_CHARGE to "powerup_laser_auto_charge", PowerUpType.LASER_PADDLE to "powerup_laser_auto_charge",
        PowerUpType.EXTRA_LIFE to "powerup_extra_life", PowerUpType.KILL_PADDLE to "powerup_kill_player",
        PowerUpType.EXPAND_PADDLE to "powerup_expand_paddle", PowerUpType.TIMED_BOMB_BRICKS to "powerup_timed_bomb_bricks",
        PowerUpType.SET_OFF_EXPLODING to "powerup_timed_bomb_bricks", PowerUpType.EXPLOSIVE_BALL to "powerup_timed_bomb_bricks",
        PowerUpType.RANDOM_GOOD to "powerup_random_positive", PowerUpType.SHRINK_BALL to "powerup_shrink_ball",
        PowerUpType.SHRINK_PADDLE to "powerup_shrink_paddle", PowerUpType.SUPER_SHRINK to "powerup_shrink_paddle",
        PowerUpType.FIRE_BALL to "powerup_fireball", PowerUpType.MAGNETIC_PADDLE to "powerup_magnet_paddle",
        PowerUpType.POWERUP_MAGNET to "powerup_magnet_paddle", PowerUpType.SLOW_BALL to "powerup_slow_ball",
        PowerUpType.MULTIBALL_PLUS_4 to "powerup_multiball_plus_4", PowerUpType.MULTI_BALL to "powerup_multiball_plus_4",
        PowerUpType.TRIPLE_BALL to "powerup_multiball_plus_4", PowerUpType.DUAL_PADDLE to "powerup_dual_paddle",
        PowerUpType.FAST_BALL to "powerup_speed_ball", PowerUpType.INSTANT_KILL_BALL to "powerup_instant_kill_ball",
        PowerUpType.MEGA_BALL to "powerup_big_ball", PowerUpType.STICKY_PADDLE to "powerup_sticky_paddle",
        PowerUpType.ONE_HIT_ANY_BRICK to "powerup_one_hit_any_brick", PowerUpType.PIERCING_BALL to "powerup_one_hit_any_brick",
        PowerUpType.ZAP_BRICKS to "powerup_one_hit_any_brick", PowerUpType.GHOST_BALL to "powerup_ghost_ball",
        PowerUpType.MULTIBALL_15 to "powerup_multiball_15", PowerUpType.EIGHT_BALL to "powerup_multiball_15",
        PowerUpType.RANDOM_BAD to "powerup_kill_player", PowerUpType.POWERUP_JAM to "powerup_kill_player",
        PowerUpType.FALLING_BRICKS to "powerup_instant_kill_ball", PowerUpType.LEVEL_WARP to "powerup_random_positive",
        PowerUpType.BOTTOM_SHIELD to "powerup_dual_paddle", PowerUpType.PADDLE_SHIELD to "powerup_dual_paddle",
        PowerUpType.SCORE_X2 to "powerup_random_positive", PowerUpType.SCORE_X3 to "powerup_random_positive",
        PowerUpType.INVERT_CONTROLS to "powerup_shrink_paddle", PowerUpType.SLIPPERY_PADDLE to "powerup_slow_ball",
        PowerUpType.EXPAND_EXPLODING to "powerup_timed_bomb_bricks"
    )
    val definitions: Map<PowerUpType, PowerUpDefinition> = PowerUpType.entries.associateWith { type ->
        val category = when (type) {
            in bad -> PowerUpCategory.BAD
            in special -> PowerUpCategory.SPECIAL
            else -> PowerUpCategory.GOOD
        }
        val conflicts = when (type) {
            PowerUpType.EXPAND_PADDLE -> setOf(PowerUpType.SHRINK_PADDLE, PowerUpType.SUPER_SHRINK)
            PowerUpType.SHRINK_PADDLE -> setOf(PowerUpType.EXPAND_PADDLE, PowerUpType.SUPER_SHRINK)
            PowerUpType.SUPER_SHRINK -> setOf(PowerUpType.EXPAND_PADDLE, PowerUpType.SHRINK_PADDLE)
            PowerUpType.SLOW_BALL -> setOf(PowerUpType.FAST_BALL)
            PowerUpType.FAST_BALL -> setOf(PowerUpType.SLOW_BALL)
            PowerUpType.SHRINK_BALL -> setOf(PowerUpType.MEGA_BALL)
            PowerUpType.MEGA_BALL -> setOf(PowerUpType.SHRINK_BALL)
            PowerUpType.MULTIBALL_PLUS_4 -> setOf(PowerUpType.MULTIBALL_15)
            PowerUpType.MULTIBALL_15 -> setOf(PowerUpType.MULTIBALL_PLUS_4)
            PowerUpType.FIRE_BALL -> setOf(PowerUpType.EXPLOSIVE_BALL)
            PowerUpType.EXPLOSIVE_BALL -> setOf(PowerUpType.FIRE_BALL)
            PowerUpType.SCORE_X2 -> setOf(PowerUpType.SCORE_X3)
            PowerUpType.SCORE_X3 -> setOf(PowerUpType.SCORE_X2)
            else -> emptySet()
        }
        val scope = when (type) {
            in instant -> EffectScope.INSTANT
            in levelScoped -> EffectScope.LEVEL
            in lifeScoped -> EffectScope.LIFE
            else -> EffectScope.TIMED
        }
        PowerUpDefinition(
            type.name.lowercase(), type, category, scope, if (scope == EffectScope.TIMED) GameplayTuning.TIMED_POWERUP_DURATION else null,
            if (conflicts.isEmpty()) StackPolicy.REFRESH_DURATION else StackPolicy.CANCEL_OPPOSITE,
            1, if (category == PowerUpCategory.GOOD) 1f else .32f, conflicts,
            icons[type] ?: "powerup_random_positive",
            if (category ==
                PowerUpCategory.BAD
            ) {
                CapsuleStyle.HEX_BAD
            } else {
                CapsuleStyle.ROUND_GOOD
            }
        )
    }
}

class PowerUpDropDirector(seed: Long, world: Int = 1) {
    private val random = Random(seed)
    private var elapsedSinceDrop = 0f
    private var budget = GameplayTuning.POWERUP_DROP_BUDGET
    private var last: PowerUpType? = null
    private val worldPool = PowerUpCatalog.worldDropTypes(world)

    /** ملاحظة صيانة: الدالة `update` تحدّث الحالة المتغيرة خلال دورة التشغيل أو المحاكاة؛ راجع استدعاءاتها واختباراتها قبل تعديلها. */
    fun update(dt: Float) {
        elapsedSinceDrop += dt
    }

    /** ملاحظة صيانة: الدالة `choose` تنفّذ مسؤولية محلية يعتمد عليها هذا الجزء من اللعبة؛ راجع استدعاءاتها واختباراتها قبل تعديلها. */
    fun choose(lives: Int, activeFalling: Int, forced: Boolean = false, baseChance: Float = .16f): PowerUpType? {
        if (!forced && (budget <= 0 || activeFalling >= 3 || elapsedSinceDrop < GameplayTuning.POWERUP_DROP_MIN_INTERVAL)) return null
        if (!forced &&
            random.nextFloat() > if (elapsedSinceDrop > GameplayTuning.POWERUP_DROP_PITY_SECONDS) {
                maxOf(GameplayTuning.POWERUP_DROP_PITY_CHANCE, baseChance)
            } else {
                baseChance.coerceIn(0f, 1f)
            }
        ) {
            return null
        }
        val pool = worldPool.map(PowerUpCatalog.definitions::getValue).filter {
            it.type != last && (lives > 1 || it.category == PowerUpCategory.GOOD)
        }
        val total = pool.sumOf { it.dropWeight.toDouble() }.toFloat()
        var roll = random.nextFloat() * total
        val selected = pool.firstOrNull {
            roll -= it.dropWeight
            roll <= 0f
        }?.type ?: pool.last().type
        last = selected
        elapsedSinceDrop = 0f
        if (!forced) budget--
        return selected
    }
}

class PowerUpManager {
    val timers = linkedMapOf<PowerUpType, Float>()
    private val lifeScoped = linkedSetOf<PowerUpType>()
    private val levelScoped = linkedSetOf<PowerUpType>()

    /** ملاحظة صيانة: الدالة `activate` تعالج الحدث أو الطلب وتحدّث الحالة المرتبطة به؛ راجع استدعاءاتها واختباراتها قبل تعديلها. */
    fun activate(type: PowerUpType) {
        val definition = PowerUpCatalog.definitions.getValue(type)
        definition.conflictsWith.forEach(::deactivate)
        when (definition.scope) {
            EffectScope.INSTANT -> Unit

            EffectScope.LEVEL -> levelScoped += type

            EffectScope.LIFE -> lifeScoped += type

            EffectScope.TIMED -> {
                val duration = requireNotNull(definition.durationSeconds)
                timers[type] = when (definition.stackPolicy) {
                    StackPolicy.ADD_DURATION -> (timers[type] ?: 0f) + duration
                    StackPolicy.IGNORE_IF_ACTIVE -> timers[type] ?: duration
                    else -> duration
                }
            }
        }
    }

    /** ملاحظة صيانة: الدالة `update` تحدّث الحالة المتغيرة خلال دورة التشغيل أو المحاكاة؛ راجع استدعاءاتها واختباراتها قبل تعديلها. */
    fun update(dt: Float): List<PowerUpType> {
        val expired = mutableListOf<PowerUpType>()
        timers.replaceAll { type, value -> (value - dt).also { if (it <= 0f) expired += type } }
        expired.forEach(timers::remove)
        return expired
    }

    /** ملاحظة صيانة: الدالة `deactivate` تنفّذ مسؤولية محلية يعتمد عليها هذا الجزء من اللعبة؛ راجع استدعاءاتها واختباراتها قبل تعديلها. */
    fun deactivate(type: PowerUpType) {
        timers.remove(type)
        lifeScoped.remove(type)
        levelScoped.remove(type)
    }

    /** ملاحظة صيانة: الدالة `clearLifeScoped` تنظّف الحالة أو الموارد المرتبطة بهذه المسؤولية بأمان؛ راجع استدعاءاتها واختباراتها قبل تعديلها. */
    fun clearLifeScoped() {
        lifeScoped.clear()
    }

    /** ملاحظة صيانة: الدالة `clear` تنظّف الحالة أو الموارد المرتبطة بهذه المسؤولية بأمان؛ راجع استدعاءاتها واختباراتها قبل تعديلها. */
    fun clear() {
        timers.clear()
        lifeScoped.clear()
        levelScoped.clear()
    }

    /** ملاحظة صيانة: الدالة `persistentSnapshot` تنفّذ مسؤولية محلية يعتمد عليها هذا الجزء من اللعبة؛ راجع استدعاءاتها واختباراتها قبل تعديلها. */
    fun persistentSnapshot() = (lifeScoped + levelScoped).toSet()

    /** ملاحظة صيانة: الدالة `restorePersistent` تقرأ البيانات المطلوبة أو تسترجعها بصيغة مناسبة للاستخدام؛ راجع استدعاءاتها واختباراتها قبل تعديلها. */
    fun restorePersistent(types: Collection<PowerUpType>) {
        types.filter { PowerUpCatalog.definitions.getValue(it).scope in setOf(EffectScope.LIFE, EffectScope.LEVEL) }
            .forEach { type ->
                if (PowerUpCatalog.definitions.getValue(type).scope ==
                    EffectScope.LIFE
                ) {
                    lifeScoped += type
                } else {
                    levelScoped += type
                }
            }
    }

    /** ملاحظة صيانة: الدالة `activeEffects` تنفّذ مسؤولية محلية يعتمد عليها هذا الجزء من اللعبة؛ راجع استدعاءاتها واختباراتها قبل تعديلها. */
    fun activeEffects(): List<Pair<PowerUpType, Float?>> = timers.map { it.key to it.value } + lifeScoped.map { it to null } + levelScoped.map { it to null }

    /** ملاحظة صيانة: الدالة `contains` تتحقق من الشرط المطلوب وتعيد نتيجة يمكن لبقية النظام الاعتماد عليها؛ راجع استدعاءاتها واختباراتها قبل تعديلها. */
    operator fun contains(type: PowerUpType) = timers.containsKey(type) || type in lifeScoped || type in levelScoped
}
