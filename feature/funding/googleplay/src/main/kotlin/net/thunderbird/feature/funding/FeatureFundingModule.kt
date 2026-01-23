package net.thunderbird.feature.funding

import com.android.billingclient.api.ProductDetails
import kotlin.time.ExperimentalTime
import net.thunderbird.core.common.cache.Cache
import net.thunderbird.core.common.cache.InMemoryCache
import net.thunderbird.feature.funding.api.FundingManager
import net.thunderbird.feature.funding.api.FundingNavigation
import net.thunderbird.feature.funding.googleplay.GooglePlayFundingManager
import net.thunderbird.feature.funding.googleplay.GooglePlayFundingNavigation
import net.thunderbird.feature.funding.googleplay.data.FundingDataContract
import net.thunderbird.feature.funding.googleplay.data.GoogleBillingClient
import net.thunderbird.feature.funding.googleplay.data.mapper.BillingResultMapper
import net.thunderbird.feature.funding.googleplay.data.mapper.ProductDetailsMapper
import net.thunderbird.feature.funding.googleplay.data.remote.GoogleBillingClientProvider
import net.thunderbird.feature.funding.googleplay.data.remote.GoogleBillingPurchaseHandler
import net.thunderbird.feature.funding.googleplay.domain.ContributionIdProvider
import net.thunderbird.feature.funding.googleplay.domain.ContributionManager
import net.thunderbird.feature.funding.googleplay.domain.FundingDomainContract
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

    single<FundingDataContract.Mapper.Product> {
        ProductDetailsMapper()
    }

    single<FundingDataContract.Mapper.BillingResult> {
        BillingResultMapper()
    }

    single<FundingDataContract.Remote.GoogleBillingClientProvider> {
        GoogleBillingClientProvider(
            context = get(),
        )
    }

    single<Cache<String, ProductDetails>> {
        InMemoryCache()
    }

    single<FundingDataContract.Remote.GoogleBillingPurchaseHandler> {
        GoogleBillingPurchaseHandler(
            productCache = get(),
            productMapper = get(),
            logger = get(),
        )
    }

    single<FundingDataContract.BillingClient> {
        GoogleBillingClient(
            clientProvider = get(),
            productMapper = get(),
            resultMapper = get(),
            productCache = get(),
            purchaseHandler = get(),
            logger = get(),
        )
    }

    single<FundingDomainContract.ContributionIdProvider> {
        ContributionIdProvider()
    }

    single<FundingDomainContract.ContributionManager> {
        ContributionManager(
            billingClient = get(),
            contributionIdProvider = get(),
        )
    }

    single<FundingDomainContract.UseCase.GetAvailableContributions> {
        GetAvailableContributions(
            billingManager = get(),
        )
    }

    viewModel {
        ContributionViewModel(
            getAvailableContributions = get(),
            contributionManager = get(),
        )
    }
}
