package net.thunderbird.baselineprofile

import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.LargeTest
import net.thunderbird.benchmarks.common.BaseBaselineProfileGenerator
import org.junit.runner.RunWith

/**
 * Generates the K-9 Mail startup Baseline Profile, see [BaseBaselineProfileGenerator].
 *
 * The profile is generated from the release builds, which need an account in `local.properties`, and merged into
 * `app-k9mail/src/main/generated/baselineProfiles/`:
 * ```
 * ./gradlew :app-k9mail:generateBaselineProfile
 * ```
 */
@RunWith(AndroidJUnit4::class)
@LargeTest
class BaselineProfileGenerator : BaseBaselineProfileGenerator()
