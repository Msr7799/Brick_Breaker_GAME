/*
 * ملاحظات صيانة الملف:
 * المسار: app/src/test/java/com/example/brick_breaker_ball/TestPreferences.kt
 * المؤلف: mohamed alromaihi
 * الدوال الموجودة: `putBoolean`، `putInteger`، `putLong`، `putFloat`، `putString`، `put`، `getBoolean`، `getInteger`، `getLong`، `getFloat`، `getString`، `get`، `contains`، `clear`، `remove`، `flush`
 */

package com.example.brick_breaker_ball

import com.badlogic.gdx.Preferences

class TestPreferences : Preferences {
    private val values = linkedMapOf<String, Any>()

    /** ملاحظة صيانة: الدالة `putBoolean` توثّق حالة اختبار أو تهيئة آلية وتحمي السلوك المتوقع من التراجع؛ راجع استدعاءاتها واختباراتها قبل تعديلها. */
    override fun putBoolean(key: String, value: Boolean) = apply { values[key] = value }

    /** ملاحظة صيانة: الدالة `putInteger` توثّق حالة اختبار أو تهيئة آلية وتحمي السلوك المتوقع من التراجع؛ راجع استدعاءاتها واختباراتها قبل تعديلها. */
    override fun putInteger(key: String, value: Int) = apply { values[key] = value }

    /** ملاحظة صيانة: الدالة `putLong` توثّق حالة اختبار أو تهيئة آلية وتحمي السلوك المتوقع من التراجع؛ راجع استدعاءاتها واختباراتها قبل تعديلها. */
    override fun putLong(key: String, value: Long) = apply { values[key] = value }

    /** ملاحظة صيانة: الدالة `putFloat` توثّق حالة اختبار أو تهيئة آلية وتحمي السلوك المتوقع من التراجع؛ راجع استدعاءاتها واختباراتها قبل تعديلها. */
    override fun putFloat(key: String, value: Float) = apply { values[key] = value }

    /** ملاحظة صيانة: الدالة `putString` توثّق حالة اختبار أو تهيئة آلية وتحمي السلوك المتوقع من التراجع؛ راجع استدعاءاتها واختباراتها قبل تعديلها. */
    override fun putString(key: String, value: String) = apply { values[key] = value }

    /** ملاحظة صيانة: الدالة `put` توثّق حالة اختبار أو تهيئة آلية وتحمي السلوك المتوقع من التراجع؛ راجع استدعاءاتها واختباراتها قبل تعديلها. */
    override fun put(vals: MutableMap<String, *>?) = apply { vals?.forEach { (key, value) -> if (value != null) values[key] = value } }

    /** ملاحظة صيانة: الدالة `getBoolean` توثّق حالة اختبار أو تهيئة آلية وتحمي السلوك المتوقع من التراجع؛ راجع استدعاءاتها واختباراتها قبل تعديلها. */
    override fun getBoolean(key: String) = getBoolean(key, false)

    /** ملاحظة صيانة: الدالة `getInteger` توثّق حالة اختبار أو تهيئة آلية وتحمي السلوك المتوقع من التراجع؛ راجع استدعاءاتها واختباراتها قبل تعديلها. */
    override fun getInteger(key: String) = getInteger(key, 0)

    /** ملاحظة صيانة: الدالة `getLong` توثّق حالة اختبار أو تهيئة آلية وتحمي السلوك المتوقع من التراجع؛ راجع استدعاءاتها واختباراتها قبل تعديلها. */
    override fun getLong(key: String) = getLong(key, 0L)

    /** ملاحظة صيانة: الدالة `getFloat` توثّق حالة اختبار أو تهيئة آلية وتحمي السلوك المتوقع من التراجع؛ راجع استدعاءاتها واختباراتها قبل تعديلها. */
    override fun getFloat(key: String) = getFloat(key, 0f)

    /** ملاحظة صيانة: الدالة `getString` توثّق حالة اختبار أو تهيئة آلية وتحمي السلوك المتوقع من التراجع؛ راجع استدعاءاتها واختباراتها قبل تعديلها. */
    override fun getString(key: String) = getString(key, "")

    /** ملاحظة صيانة: الدالة `getBoolean` توثّق حالة اختبار أو تهيئة آلية وتحمي السلوك المتوقع من التراجع؛ راجع استدعاءاتها واختباراتها قبل تعديلها. */
    override fun getBoolean(key: String, defValue: Boolean) = values[key] as? Boolean ?: defValue

    /** ملاحظة صيانة: الدالة `getInteger` توثّق حالة اختبار أو تهيئة آلية وتحمي السلوك المتوقع من التراجع؛ راجع استدعاءاتها واختباراتها قبل تعديلها. */
    override fun getInteger(key: String, defValue: Int) = values[key] as? Int ?: defValue

    /** ملاحظة صيانة: الدالة `getLong` توثّق حالة اختبار أو تهيئة آلية وتحمي السلوك المتوقع من التراجع؛ راجع استدعاءاتها واختباراتها قبل تعديلها. */
    override fun getLong(key: String, defValue: Long) = values[key] as? Long ?: defValue

    /** ملاحظة صيانة: الدالة `getFloat` توثّق حالة اختبار أو تهيئة آلية وتحمي السلوك المتوقع من التراجع؛ راجع استدعاءاتها واختباراتها قبل تعديلها. */
    override fun getFloat(key: String, defValue: Float) = values[key] as? Float ?: defValue

    /** ملاحظة صيانة: الدالة `getString` توثّق حالة اختبار أو تهيئة آلية وتحمي السلوك المتوقع من التراجع؛ راجع استدعاءاتها واختباراتها قبل تعديلها. */
    override fun getString(key: String, defValue: String) = values[key] as? String ?: defValue

    /** ملاحظة صيانة: الدالة `get` توثّق حالة اختبار أو تهيئة آلية وتحمي السلوك المتوقع من التراجع؛ راجع استدعاءاتها واختباراتها قبل تعديلها. */
    override fun get(): MutableMap<String, *> = LinkedHashMap(values)

    /** ملاحظة صيانة: الدالة `contains` توثّق حالة اختبار أو تهيئة آلية وتحمي السلوك المتوقع من التراجع؛ راجع استدعاءاتها واختباراتها قبل تعديلها. */
    override fun contains(key: String) = key in values

    /** ملاحظة صيانة: الدالة `clear` توثّق حالة اختبار أو تهيئة آلية وتحمي السلوك المتوقع من التراجع؛ راجع استدعاءاتها واختباراتها قبل تعديلها. */
    override fun clear() = values.clear()

    /** ملاحظة صيانة: الدالة `remove` توثّق حالة اختبار أو تهيئة آلية وتحمي السلوك المتوقع من التراجع؛ راجع استدعاءاتها واختباراتها قبل تعديلها. */
    override fun remove(key: String) {
        values.remove(key)
    }

    /** ملاحظة صيانة: الدالة `flush` توثّق حالة اختبار أو تهيئة آلية وتحمي السلوك المتوقع من التراجع؛ راجع استدعاءاتها واختباراتها قبل تعديلها. */
    override fun flush() = Unit
}
