package net.thunderbird.feature.funding

import android.content.Context
import kotlin.test.Test
import net.thunderbird.core.android.common.activity.ActivityProvider
import net.thunderbird.core.logging.Logger
import net.thunderbird.feature.funding.api.FundingSettings
import org.koin.core.annotation.KoinExperimentalAPI
import org.koin.test.KoinTest
import org.koin.test.verify.verify

class FeatureFundingModuleKtTest : KoinTest {

    @OptIn(KoinExperimentalAPI::class)
    @Test
    fun `should have a valid di module`() {
        featureFundingModule.verify(
            extraTypes = listOf(
                Logger::class,
                ActivityProvider::class,
                FundingSettings::class,
                Context::class,
            ),
        )
    }
}
