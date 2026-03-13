package net.thunderbird.feature.funding.googleplay

import net.thunderbird.feature.funding.api.FundingManager
import net.thunderbird.feature.funding.api.FundingType
import net.thunderbird.feature.funding.googleplay.ui.reminder.FundingReminderContract

class GooglePlayFundingManager(
    private val reminder: FundingReminderContract.Reminder,
) : FundingManager {
    override fun getFundingType(): FundingType {
        return FundingType.GOOGLE_PLAY
    }

    override fun addFundingReminder(onOpenFunding: () -> Unit) {
        reminder.registerReminder(onOpenFunding)
    }
}
