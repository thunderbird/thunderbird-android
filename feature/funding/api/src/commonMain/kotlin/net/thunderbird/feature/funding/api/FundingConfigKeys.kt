package net.thunderbird.feature.funding.api

import net.thunderbird.core.configstore.ConfigKey

object FundingConfigKeys {
    // lastFundingReminderShownActivityAmount
    val LAST_FUNDING_REMINDER_SHOWN_ACTIVITY_AMOUNT =
        ConfigKey.LongKey("last_funding_reminder_shown_activity_ammount")

    // fundingReminderCount
    val FUNDING_REMINDER_COUNT = ConfigKey.IntKey("funding_reminder_count")
}
