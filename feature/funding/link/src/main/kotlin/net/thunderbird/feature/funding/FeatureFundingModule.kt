package net.thunderbird.feature.funding

import net.thunderbird.feature.funding.api.FundingManager
import net.thunderbird.feature.funding.api.FundingNavigation
import net.thunderbird.feature.funding.link.LinkFundingManager
import net.thunderbird.feature.funding.link.LinkFundingNavigation
import org.koin.dsl.module

val featureFundingModule = module {
    single<FundingManager> { LinkFundingManager() }
    single<FundingNavigation> { LinkFundingNavigation() }
}
