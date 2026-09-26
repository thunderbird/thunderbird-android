package net.thunderbird.feature.funding

import net.thunderbird.feature.funding.api.FundingManager
import net.thunderbird.feature.funding.api.FundingNavigation
import net.thunderbird.feature.funding.common.api.FundingReminderContract
import net.thunderbird.feature.funding.common.ui.reminder.ActivityLifecycleObserver
import net.thunderbird.feature.funding.common.ui.reminder.FragmentLifecycleObserver
import net.thunderbird.feature.funding.common.ui.reminder.FundingReminder
import net.thunderbird.feature.funding.common.ui.reminder.FundingReminderDialog
import net.thunderbird.feature.funding.link.LinkFundingManager
import net.thunderbird.feature.funding.link.LinkFundingNavigation
import org.koin.core.qualifier.named
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
        ActivityLifecycleObserver(
            settings = get(),
        )
    }

    single<FundingReminderContract.Reminder> {
        FundingReminder(
            activityProvider = get(),
            settings = get(),
            fragmentObserver = get(),
            activityCounterObserver = get(),
            dialog = get(),
            scope = get(named("ConfigStoreScope")),
        )
    }

    single<FundingManager> {
        LinkFundingManager(
            reminder = get(),
        )
    }
    single<FundingNavigation> { LinkFundingNavigation() }
}
