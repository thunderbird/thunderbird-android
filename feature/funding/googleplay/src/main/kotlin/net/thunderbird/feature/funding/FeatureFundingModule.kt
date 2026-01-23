package net.thunderbird.feature.funding

import com.android.billingclient.api.ProductDetails
import kotlin.time.ExperimentalTime
import net.thunderbird.core.common.cache.Cache
import net.thunderbird.core.common.cache.InMemoryCache
import net.thunderbird.feature.funding.api.FundingManager
import net.thunderbird.feature.funding.api.FundingNavigation
import net.thunderbird.feature.funding.googleplay.GooglePlayFundingManager
import net.thunderbird.feature.funding.googleplay.GooglePlayFundingNavigation
import net.thunderbird.feature.funding.googleplay.data.DataContract
import net.thunderbird.feature.funding.googleplay.data.GoogleBillingClient
import net.thunderbird.feature.funding.googleplay.data.mapper.BillingResultMapper
import net.thunderbird.feature.funding.googleplay.data.mapper.ProductDetailsMapper
import net.thunderbird.feature.funding.googleplay.data.remote.GoogleBillingClientProvider
import net.thunderbird.feature.funding.googleplay.data.remote.GoogleBillingPurchaseHandler
import net.thunderbird.feature.funding.googleplay.domain.BillingManager
import net.thunderbird.feature.funding.googleplay.domain.ContributionIdProvider
import net.thunderbird.feature.funding.googleplay.domain.DomainContract
import net.thunderbird.feature.funding.googleplay.domain.usecase.GetAvailableContributions
import net.thunderbird.feature.funding.googleplay.ui.contribution.ContributionViewModel
import net.thunderbird.feature.funding.googleplay.ui.reminder.ActivityLifecycleObserver
import net.thunderbird.feature.funding.googleplay.ui.reminder.FragmentLifecycleObserver
import net.thunderbird.feature.funding.googleplay.ui.reminder.FundingReminder
import net.thunderbird.feature.funding.googleplay.ui.reminder.FundingReminderContract
import net.thunderbird.feature.funding.googleplay.ui.reminder.FundingReminderDialog
import org.koin.core.module.dsl.viewModel
import org.koin.dsl.module

val featureFundingModule = module {
    single<FundingReminderContract.Dialog> {
        FundingReminderDialog()
    }

    single<FundingReminderContract.FragmentLifecycleObserver> {
        FragmentLifecycleObserver(
            targetFragmentTag = "MessageViewContainerFragment",
        )
    }

    single<FundingReminderContract.ActivityLifecycleObserver> {
        @OptIn(ExperimentalTime::class)
        ActivityLifecycleObserver(
            settings = get(),
        )
    }

    single<FundingReminderContract.Reminder> {
        @OptIn(ExperimentalTime::class)
        FundingReminder(
            settings = get(),
            fragmentObserver = get(),
            activityCounterObserver = get(),
            dialog = get(),
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
            logger = get(),
        )
    }

    single<DataContract.BillingClient> {
        GoogleBillingClient(
            clientProvider = get(),
            productMapper = get(),
            resultMapper = get(),
            productCache = get(),
            purchaseHandler = get(),
            logger = get(),
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
