/*
 * ملاحظات صيانة الملف:
 * المسار: app/src/main/java/com/example/brick_breaker_ball/BoosterStores.kt
 * المؤلف: mohamed alromaihi
 * الدوال الموجودة: `count`، `snapshot`، `add`، `addAll`، `consume`، `refund`، `clearForTests`، `total`، `hasProcessed`، `markProcessed`، `currentState`، `canWatch`، `recordReward`، `remainingCooldownMillis`
 */

package com.example.brick_breaker_ball

import com.badlogic.gdx.Gdx
import com.badlogic.gdx.Preferences

class BoosterInventoryStore(private val prefs: Preferences = Gdx.app.getPreferences("brickbreakerball-booster-inventory-v1")) {
    /** ملاحظة صيانة: الدالة `count` تنفّذ مسؤولية محلية يعتمد عليها هذا الجزء من اللعبة؛ راجع استدعاءاتها واختباراتها قبل تعديلها. */
    fun count(type: PowerUpType) = if (type in SHOP_ELIGIBLE_TYPES) prefs.getInteger(type.name, 0) else 0

    /** ملاحظة صيانة: الدالة `snapshot` تقرأ البيانات المطلوبة أو تسترجعها بصيغة مناسبة للاستخدام؛ راجع استدعاءاتها واختباراتها قبل تعديلها. */
    fun snapshot() = SHOP_ELIGIBLE_TYPES.associateWith(::count)

    /** ملاحظة صيانة: الدالة `add` تحفظ البيانات أو تضيفها إلى الحالة المعتمدة في النظام؛ راجع استدعاءاتها واختباراتها قبل تعديلها. */
    fun add(type: PowerUpType, amount: Int) {
        require(type in SHOP_ELIGIBLE_TYPES && amount >= 0)
        prefs.putInteger(
            type.name,
            (
                count(type).toLong() +
                    amount
                ).coerceAtMost(Int.MAX_VALUE.toLong()).toInt()
        ).flush()
    }

    /** ملاحظة صيانة: الدالة `addAll` تحفظ البيانات أو تضيفها إلى الحالة المعتمدة في النظام؛ راجع استدعاءاتها واختباراتها قبل تعديلها. */
    fun addAll(grants: Map<PowerUpType, Int>) {
        grants.forEach { (t, n) ->
            require(t in SHOP_ELIGIBLE_TYPES && n >= 0)
            prefs.putInteger(
                t.name,
                (
                    count(t).toLong() +
                        n
                    ).coerceAtMost(Int.MAX_VALUE.toLong()).toInt()
            )
        }
        prefs.flush()
    }

    /** ملاحظة صيانة: الدالة `consume` تنفّذ مسؤولية محلية يعتمد عليها هذا الجزء من اللعبة؛ راجع استدعاءاتها واختباراتها قبل تعديلها. */
    fun consume(type: PowerUpType, amount: Int = 1): Boolean {
        if (amount <= 0 || type !in SHOP_ELIGIBLE_TYPES ||
            count(type) < amount
        ) {
            return false
        }
        prefs.putInteger(type.name, count(type) - amount).flush()
        return true
    }

    /** ملاحظة صيانة: الدالة `refund` تنفّذ مسؤولية محلية يعتمد عليها هذا الجزء من اللعبة؛ راجع استدعاءاتها واختباراتها قبل تعديلها. */
    fun refund(type: PowerUpType, amount: Int = 1) = add(type, amount)

    /** ملاحظة صيانة: الدالة `clearForTests` تنظّف الحالة أو الموارد المرتبطة بهذه المسؤولية بأمان؛ راجع استدعاءاتها واختباراتها قبل تعديلها. */
    fun clearForTests() {
        prefs.clear()
        prefs.flush()
    }

    /** ملاحظة صيانة: الدالة `total` تحوّل البيانات أو تبني المعرّف المتوافق مع بقية النظام؛ راجع استدعاءاتها واختباراتها قبل تعديلها. */
    fun total() = snapshot().values.sum()
}
class TransactionLedger {
    private val prefs = Gdx.app.getPreferences("brickbreakerball-transactions-v1")

    /** ملاحظة صيانة: الدالة `hasProcessed` تتحقق من الشرط المطلوب وتعيد نتيجة يمكن لبقية النظام الاعتماد عليها؛ راجع استدعاءاتها واختباراتها قبل تعديلها. */
    fun hasProcessed(id: String) = prefs.getBoolean(id, false)

    /** ملاحظة صيانة: الدالة `markProcessed` تحفظ البيانات أو تضيفها إلى الحالة المعتمدة في النظام؛ راجع استدعاءاتها واختباراتها قبل تعديلها. */
    fun markProcessed(id: String) {
        prefs.putBoolean(id, true).flush()
    }
}
class DailyRewardStore {
    private val prefs = Gdx.app.getPreferences("brickbreakerball-daily-reward-v1")

    /** ملاحظة صيانة: الدالة `currentState` تنفّذ مسؤولية محلية يعتمد عليها هذا الجزء من اللعبة؛ راجع استدعاءاتها واختباراتها قبل تعديلها. */
    fun currentState(now: Long): DailyRewardState {
        val key = dayKey(now)
        if (prefs.getString("day", "") !=
            key
        ) {
            prefs.putString("day", key).putInteger("count", 0).putLong("next", 0).flush()
        }
        return DailyRewardState(prefs.getInteger("count", 0), 5, prefs.getLong("next", 0))
    }

    /** ملاحظة صيانة: الدالة `canWatch` تتحقق من الشرط المطلوب وتعيد نتيجة يمكن لبقية النظام الاعتماد عليها؛ راجع استدعاءاتها واختباراتها قبل تعديلها. */
    fun canWatch(now: Long) = currentState(now).let { it.claimedToday < it.dailyLimit && now >= it.nextEligibleAtMillis }

    /** ملاحظة صيانة: الدالة `recordReward` تحفظ البيانات أو تضيفها إلى الحالة المعتمدة في النظام؛ راجع استدعاءاتها واختباراتها قبل تعديلها. */
    fun recordReward(now: Long) {
        val s = currentState(now)
        prefs.putInteger("count", (s.claimedToday + 1).coerceAtMost(5)).putLong(
            "next",
            now + 60_000
        ).flush()
    }

    /** ملاحظة صيانة: الدالة `remainingCooldownMillis` تنفّذ مسؤولية محلية يعتمد عليها هذا الجزء من اللعبة؛ راجع استدعاءاتها واختباراتها قبل تعديلها. */
    fun remainingCooldownMillis(now: Long) = (currentState(now).nextEligibleAtMillis - now).coerceAtLeast(0)
}
