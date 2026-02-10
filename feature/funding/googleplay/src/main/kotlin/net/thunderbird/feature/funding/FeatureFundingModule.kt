package net.thunderbird.feature.funding

import kotlin.time.ExperimentalTime
import net.thunderbird.feature.funding.api.FundingManager
import net.thunderbird.feature.funding.api.FundingNavigation
import net.thunderbird.feature.funding.googleplay.GooglePlayFundingManager
import net.thunderbird.feature.funding.googleplay.GooglePlayFundingNavigation
import net.thunderbird.feature.funding.googleplay.data.fundingDataModule
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

    single<FundingDomainContract.ContributionManager> {
        ContributionManager(
            billingClient = get(),
        )
    }

    single<FundingDomainContract.UseCase.GetAvailableContributions> {
        GetAvailableContributions(
            repository = get(),
            contributionIdProvider = get(),
        )
    }

    viewModel {
        ContributionViewModel(
            getAvailableContributions = get(),
            contributionManager = get(),
        )
    }
}
