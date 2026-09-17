package net.thunderbird.feature.funding.api

import net.thunderbird.core.configstore.ConfigKey

object FundingConfigKeys {
    // lastFundingReminderShownTimestamp
    val LAST_FUNDING_REMINDER_SHOWN_TIMESTAMP = ConfigKey.LongKey("last_funding_reminder_shown_timestamp")

    // fundingReminderCount
    val FUNDING_REMINDER_COUNT = ConfigKey.IntKey("funding_reminder_count")
}
