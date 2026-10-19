package net.thunderbird.baselineprofile

import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.LargeTest
import net.thunderbird.benchmarks.common.BaseBaselineProfileGenerator
import org.junit.runner.RunWith

/**
 * Generates the Thunderbird startup Baseline Profile, see [BaseBaselineProfileGenerator].
 *
 * The profile is generated from the release, beta and daily builds, and merged into
 * `app-thunderbird/src/main/generated/baselineProfiles/`. It needs an account in `local.properties`, otherwise the
 * daily build uses the demo account and the release and beta builds fail:
 * ```
 * ./gradlew :app-thunderbird:generateBaselineProfile
 * ```
 */
@RunWith(AndroidJUnit4::class)
@LargeTest
class BaselineProfileGenerator : BaseBaselineProfileGenerator()
