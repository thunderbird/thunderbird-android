package net.thunderbird.baselineprofile

import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.LargeTest
import net.thunderbird.benchmarks.common.BaseStartupBenchmarks
import org.junit.runner.RunWith

/**
 * Measures K-9 Mail cold startup to the message list, see [BaseStartupBenchmarks].
 *
 * Needs an account in `local.properties`:
 * ```
 * ./gradlew :app-k9mail:benchmarks:connectedFullBenchmarkReleaseAndroidTest
 * ```
 */
@RunWith(AndroidJUnit4::class)
@LargeTest
class StartupBenchmarks : BaseStartupBenchmarks()
