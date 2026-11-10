package net.thunderbird.feature.funding.api

import net.thunderbird.core.configstore.ConfigKey

object FundingConfigKeys {
    // reminderReferenceTimestamp
    val REMINDER_REFERENCE_TIMESTAMP = ConfigKey.LongKey("reminder_reference_timestamp")

    // lastFundingReminderShownActivityAmount
    val LAST_FUNDING_REMINDER_SHOWN =
        ConfigKey.LongKey("last_funding_reminder_shown")

    // fundingReminderCount
    val FUNDING_REMINDER_COUNT = ConfigKey.IntKey("funding_reminder_count")

    /** DEPRECATED KEYS **/

    // lastFundingReminderShownActivityAmount
    val DEPRECATED_LAST_FUNDING_REMINDER_SHOWN_ACTIVITY_AMOUNT =
        ConfigKey.LongKey("last_funding_reminder_shown_activity_ammount")
}
