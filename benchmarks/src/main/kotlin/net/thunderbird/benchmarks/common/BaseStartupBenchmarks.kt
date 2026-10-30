package net.thunderbird.benchmarks.common

import androidx.benchmark.macro.BaselineProfileMode
import androidx.benchmark.macro.CompilationMode
import androidx.benchmark.macro.StartupMode
import androidx.benchmark.macro.StartupTimingMetric
import androidx.benchmark.macro.junit4.MacrobenchmarkRule
import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.uiautomator.By
import androidx.test.uiautomator.UiDevice
import androidx.test.uiautomator.Until
import net.thunderbird.benchmarks.common.extension.ensureAccountConfigured
import net.thunderbird.benchmarks.common.extension.waitForMessageListItems
import org.junit.Before
import org.junit.Rule
import org.junit.Test

/**
 * Measures cold startup to the message list, with an account configured (see [ensureAccountConfigured]).
 *
 * Extend it in the app's benchmark module, annotated with `@RunWith(AndroidJUnit4::class)` and `@LargeTest`.
 *
 * Reports `timeToInitialDisplayMs` (first frame) and `timeToFullDisplayMs` (message list items shown).
 *
 * To compare two code changes, run the same test before and after the change, on the same device,
 * and compare the medians. Run on a physical device, not an emulator.
 *
 * See: https://d.android.com/topic/performance/benchmarking/macrobenchmark-overview
 */
abstract class BaseStartupBenchmarks {

    @get:Rule
    val rule = MacrobenchmarkRule()

    @Before
    fun setUp() {
        UiDevice.getInstance(InstrumentationRegistry.getInstrumentation()).ensureAccountConfigured(targetAppId)
    }

    /**
     * Worst case: no AOT compilation at all, everything is interpreted or JIT compiled.
     */
    @Test
    fun startupCompilationNone() = benchmark(CompilationMode.None())

    /**
     * Baseline Profile ignored, but the app is warmed up once first, so ART compiles the code it saw at runtime.
     *
     * Compare with [startupCompilationBaselineProfiles] to see what the Baseline Profile adds over JIT warm-up.
     */
    @Test
    fun startupCompilationBaselineProfilesDisabled() = benchmark(
        CompilationMode.Partial(baselineProfileMode = BaselineProfileMode.Disable, warmupIterations = 1),
    )

    /**
     * Closest to what users get on install from the Play Store: AOT compilation driven by the Baseline Profile.
     *
     * Fails if the APK doesn't contain a Baseline Profile; generate one first.
     */
    @Test
    fun startupCompilationBaselineProfiles() = benchmark(CompilationMode.Partial(BaselineProfileMode.Require))

    /**
     * Best case: the whole app is AOT compiled. Users don't get this, it shows how far the Baseline Profile could go.
     */
    @Test
    fun startupCompilationFull() = benchmark(CompilationMode.Full())

    private fun benchmark(compilationMode: CompilationMode) {
        rule.measureRepeated(
            packageName = targetAppId,
            metrics = listOf(StartupTimingMetric()),
            compilationMode = compilationMode,
            startupMode = StartupMode.COLD,
            iterations = ITERATIONS,
            setupBlock = {
                pressHome()
            },
            measureBlock = {
                startActivityAndWait()
                check(
                    device.wait(
                        Until.hasObject(By.res(packageName, MESSAGE_LIST_RESOURCE_ID)),
                        MESSAGE_LIST_TIMEOUT_MS,
                    ),
                ) { "Startup did not reach the message list" }
                device.waitForMessageListItems(packageName, MESSAGE_LIST_TIMEOUT_MS)
            },
        )
    }

    private companion object {
        const val ITERATIONS = 15
    }
}
