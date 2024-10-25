package app.k9mail.feature.funding

import app.k9mail.core.common.cache.Cache
import app.k9mail.core.common.cache.InMemoryCache
import app.k9mail.feature.funding.api.FundingManager
import app.k9mail.feature.funding.api.FundingNavigation
import app.k9mail.feature.funding.googleplay.GooglePlayFundingManager
import app.k9mail.feature.funding.googleplay.GooglePlayFundingNavigation
import app.k9mail.feature.funding.googleplay.data.DataContract
import app.k9mail.feature.funding.googleplay.data.GoogleBillingClient
import app.k9mail.feature.funding.googleplay.data.mapper.BillingResultMapper
import app.k9mail.feature.funding.googleplay.data.mapper.ProductDetailsMapper
import app.k9mail.feature.funding.googleplay.data.remote.GoogleBillingClientProvider
import app.k9mail.feature.funding.googleplay.data.remote.GoogleBillingPurchaseHandler
import app.k9mail.feature.funding.googleplay.domain.BillingManager
import app.k9mail.feature.funding.googleplay.domain.ContributionIdProvider
import app.k9mail.feature.funding.googleplay.domain.DomainContract
import app.k9mail.feature.funding.googleplay.domain.usecase.GetAvailableContributions
import app.k9mail.feature.funding.googleplay.ui.contribution.ContributionViewModel
import app.k9mail.feature.funding.googleplay.ui.reminder.FragmentLifecycleObserver
import app.k9mail.feature.funding.googleplay.ui.reminder.FundingReminder
import app.k9mail.feature.funding.googleplay.ui.reminder.FundingReminderContract
import app.k9mail.feature.funding.googleplay.ui.reminder.FundingReminderDialog
import com.android.billingclient.api.ProductDetails
import org.koin.androidx.viewmodel.dsl.viewModel
import org.koin.dsl.module

val featureFundingModule = module {
    single<FundingReminderContract.Dialog> {
        FundingReminderDialog(
            settings = get(),
        )
    }

    single<FundingReminderContract.FragmentLifecycleObserver> {
        FragmentLifecycleObserver(
            targetFragmentTag = "MessageViewContainerFragment",
        )
    }

    single<FundingReminderContract.Reminder> {
        FundingReminder(
            settings = get(),
            dialog = get(),
            fragmentObserver = get(),
        )
    }

    single<FundingManager> {
        GooglePlayFundingManager(
            reminder = get(),
        )
    }

    single<FundingNavigation> { GooglePlayFundingNavigation() }

    single<DataContract.Mapper.Product> {
        ProductDetailsMapper()
    }

    single<DataContract.Mapper.BillingResult> {
        BillingResultMapper()
    }

    single<DataContract.Remote.GoogleBillingClientProvider> {
        GoogleBillingClientProvider(
            context = get(),
        )
    }

    single<Cache<String, ProductDetails>> {
        InMemoryCache()
    }

    single<DataContract.Remote.GoogleBillingPurchaseHandler> {
        GoogleBillingPurchaseHandler(
            productCache = get(),
            productMapper = get(),
        )
    }

    single<DataContract.BillingClient> {
        GoogleBillingClient(
            clientProvider = get(),
            productMapper = get(),
            resultMapper = get(),
            productCache = get(),
            purchaseHandler = get(),
        )
    }

    single<DomainContract.ContributionIdProvider> {
        ContributionIdProvider()
    }

    single<DomainContract.BillingManager> {
        BillingManager(
            billingClient = get(),
            contributionIdProvider = get(),
        )
    }

    single<DomainContract.UseCase.GetAvailableContributions> {
        GetAvailableContributions(
            billingManager = get(),
        )
    }

    viewModel {
        ContributionViewModel(
            billingManager = get(),
            getAvailableContributions = get(),
        )
    }
}
