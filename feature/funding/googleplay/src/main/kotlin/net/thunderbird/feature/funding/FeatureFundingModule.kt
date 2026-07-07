package net.thunderbird.feature.funding

import kotlin.time.ExperimentalTime
import net.thunderbird.feature.funding.api.FundingManager
import net.thunderbird.feature.funding.api.FundingNavigation
import net.thunderbird.feature.funding.googleplay.GooglePlayFundingManager
import net.thunderbird.feature.funding.googleplay.GooglePlayFundingNavigation
import net.thunderbird.feature.funding.googleplay.data.fundingDataModule
import net.thunderbird.feature.funding.googleplay.domain.ContributionIdProvider
import net.thunderbird.feature.funding.googleplay.domain.FundingDomainContract
import net.thunderbird.feature.funding.googleplay.domain.policy.ContributionPreselector
import net.thunderbird.feature.funding.googleplay.domain.usecase.GetAvailableContributions
import net.thunderbird.feature.funding.googleplay.domain.usecase.GetLatestPurchasedContribution
import net.thunderbird.feature.funding.googleplay.ui.contribution.ContributionViewModel
import net.thunderbird.feature.funding.googleplay.ui.contribution.list.ContributionListSliceContract
import net.thunderbird.feature.funding.googleplay.ui.contribution.list.ContributionListSliceFactory
import net.thunderbird.feature.funding.googleplay.ui.contribution.purchase.PurchaseSliceContract
import net.thunderbird.feature.funding.googleplay.ui.contribution.purchase.PurchaseSliceFactory
import net.thunderbird.feature.funding.googleplay.ui.reminder.ActivityLifecycleObserver
import net.thunderbird.feature.funding.googleplay.ui.reminder.FragmentLifecycleObserver
import net.thunderbird.feature.funding.googleplay.ui.reminder.FundingReminder
import net.thunderbird.feature.funding.googleplay.ui.reminder.FundingReminderContract
import net.thunderbird.feature.funding.googleplay.ui.reminder.FundingReminderDialog
import org.koin.core.module.dsl.viewModel
import org.koin.dsl.module

val featureFundingModule = module {
    includes(fundingDataModule)

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
            activityProvider = get(),
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

    single<FundingDomainContract.ContributionIdProvider> {
        ContributionIdProvider()
    }

    single<FundingDomainContract.Policy.ContributionPreselector> {
        ContributionPreselector()
    }

    single<FundingDomainContract.UseCase.GetAvailableContributions> {
        GetAvailableContributions(
            repository = get(),
            contributionIdProvider = get(),
            preselector = get(),
        )
    }

    single<FundingDomainContract.UseCase.GetLatestPurchasedContribution> {
        GetLatestPurchasedContribution(
            repository = get(),
            clock = get(),
        )
    }

    factory<ContributionListSliceContract.Slice.Factory> {
        ContributionListSliceFactory(
            getAvailableContributions = get(),
            logger = get(),
        )
    }

    factory<PurchaseSliceContract.Slice.Factory> {
        PurchaseSliceFactory(
            getLastestPurchase = get(),
            repository = get(),
            logger = get(),
        )
    }

    viewModel {
        ContributionViewModel(
            listSliceFactory = get(),
            purchaseSliceFactory = get(),
            repository = get(),
            logger = get(),
        )
    }
}
