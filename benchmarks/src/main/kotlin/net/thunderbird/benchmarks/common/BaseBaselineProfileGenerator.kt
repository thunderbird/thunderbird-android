package net.thunderbird.benchmarks.common

import android.os.Build
import androidx.annotation.RequiresApi
import androidx.benchmark.macro.junit4.BaselineProfileRule
import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.uiautomator.By
import androidx.test.uiautomator.UiDevice
import androidx.test.uiautomator.Until
import net.thunderbird.benchmarks.common.extension.ensureAccountConfigured
import org.junit.Before
import org.junit.Rule
import org.junit.Test

/**
 * Generates a startup Baseline Profile, for startup to the message list with an account configured
 * (see [ensureAccountConfigured]).
 *
 * Extend it in the app's benchmark module, annotated with `@RunWith(AndroidJUnit4::class)` and `@LargeTest`.
 *
 * Requires a device running API 33+, or a rooted device running API 28+.
 *
 * See: https://d.android.com/topic/performance/baselineprofiles
 */
@RequiresApi(Build.VERSION_CODES.P)
abstract class BaseBaselineProfileGenerator {

    @get:Rule
    val rule = BaselineProfileRule()

    @Before
    fun setUp() {
        UiDevice.getInstance(InstrumentationRegistry.getInstrumentation())
            .ensureAccountConfigured(targetAppId)
    }

    @Test
    fun generate() {
        rule.collect(
            packageName = targetAppId,
            // See: https://d.android.com/topic/performance/baselineprofiles/dex-layout-optimizations
            includeInStartupProfile = true,
        ) {
            pressHome()
            startActivityAndWait()
            check(
                device.wait(Until.hasObject(By.res(packageName, MESSAGE_LIST_RESOURCE_ID)), MESSAGE_LIST_TIMEOUT_MS),
            ) { "Startup did not reach the message list" }
        }
    }
}
