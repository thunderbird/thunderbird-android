package app.k9mail.feature.funding

import app.k9mail.feature.funding.api.FundingManager
import app.k9mail.feature.funding.api.FundingNavigation
import app.k9mail.feature.funding.noop.NoOpFundingManager
import app.k9mail.feature.funding.noop.NoOpFundingNavigation
import org.koin.dsl.module

val featureFundingModule = module {
    single<FundingManager> { NoOpFundingManager() }
    single<FundingNavigation> { NoOpFundingNavigation() }
}
