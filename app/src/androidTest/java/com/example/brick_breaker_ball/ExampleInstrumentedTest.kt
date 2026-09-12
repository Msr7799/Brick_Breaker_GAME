/*
 * ملاحظات صيانة الملف:
 * المسار: app/src/androidTest/java/com/example/brick_breaker_ball/ExampleInstrumentedTest.kt
 * المؤلف: mohamed alromaihi
 * الدوال الموجودة: `useAppContext`
 */

package com.example.brick_breaker_ball

import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Instrumented test, which will execute on an Android device.
 *
 * See [testing documentation](http://d.android.com/tools/testing).
 */
@RunWith(AndroidJUnit4::class)
class ExampleInstrumentedTest {
    /** ملاحظة صيانة: الدالة `useAppContext` توثّق حالة اختبار أو تهيئة آلية وتحمي السلوك المتوقع من التراجع؛ راجع استدعاءاتها واختباراتها قبل تعديلها. */
    @Test
    fun useAppContext() {
        // Context of the app under test.
        val appContext = InstrumentationRegistry.getInstrumentation().targetContext
        assertEquals("com.example.brick_breaker_ball", appContext.packageName)
    }
}
