package net.thunderbird.feature.funding

import net.thunderbird.feature.funding.api.FundingManager
import net.thunderbird.feature.funding.api.FundingNavigation
import net.thunderbird.feature.funding.noop.NoOpFundingManager
import net.thunderbird.feature.funding.noop.NoOpFundingNavigation
import org.koin.dsl.module

val featureFundingModule = module {
    single<FundingManager> { NoOpFundingManager() }
    single<FundingNavigation> { NoOpFundingNavigation() }
}
