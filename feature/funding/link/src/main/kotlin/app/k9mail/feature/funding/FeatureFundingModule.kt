package app.k9mail.feature.funding

import app.k9mail.feature.funding.api.FundingManager
import app.k9mail.feature.funding.link.LinkFundingManager
import org.koin.dsl.module

val featureFundingModule = module {
    single<FundingManager> { LinkFundingManager() }
}
