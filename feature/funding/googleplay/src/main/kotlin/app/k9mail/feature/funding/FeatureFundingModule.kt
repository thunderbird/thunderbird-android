package app.k9mail.feature.funding

import app.k9mail.feature.funding.api.FundingManager
import app.k9mail.feature.funding.api.FundingNavigation
import app.k9mail.feature.funding.googleplay.GooglePlayFundingManager
import app.k9mail.feature.funding.googleplay.GooglePlayFundingNavigation
import app.k9mail.feature.funding.googleplay.data.DataContract
import app.k9mail.feature.funding.googleplay.data.mapper.ProductDetailsMapper
import app.k9mail.feature.funding.googleplay.domain.ContributionIdProvider
import app.k9mail.feature.funding.googleplay.domain.DomainContract
import app.k9mail.feature.funding.googleplay.ui.contribution.ContributionViewModel
import org.koin.androidx.viewmodel.dsl.viewModel
import org.koin.dsl.module

val featureFundingModule = module {
    single<FundingManager> { GooglePlayFundingManager() }
    single<FundingNavigation> { GooglePlayFundingNavigation() }

    single<DataContract.Mapper.Product> {
        ProductDetailsMapper()
    }

    single<DomainContract.ContributionIdProvider> {
        ContributionIdProvider()
    }

    viewModel { ContributionViewModel() }
}
