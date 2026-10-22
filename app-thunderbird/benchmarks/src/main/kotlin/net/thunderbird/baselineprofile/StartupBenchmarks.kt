package net.thunderbird.baselineprofile

import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.LargeTest
import net.thunderbird.benchmarks.common.BaseStartupBenchmarks
import org.junit.runner.RunWith

/**
 * Measures Thunderbird cold startup to the message list, see [BaseStartupBenchmarks].
 *
 * Without an account in `local.properties`, the demo account is used. It's only available in the `daily` build type,
 * so run the `benchmarkDaily` variant:
 * ```
 * ./gradlew :app-thunderbird:benchmarks:connectedFossBenchmarkDailyAndroidTest
 * ```
 *
 * Use `-Pci=true` to enable R8 for the daily build, so it matches release. With a real account configured, the
 * `benchmarkRelease` variant works too.
 */
@RunWith(AndroidJUnit4::class)
@LargeTest
class StartupBenchmarks : BaseStartupBenchmarks()
