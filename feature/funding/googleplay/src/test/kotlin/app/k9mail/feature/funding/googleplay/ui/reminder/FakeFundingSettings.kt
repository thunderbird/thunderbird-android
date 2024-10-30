package app.k9mail.feature.funding.googleplay.ui.reminder

import app.k9mail.feature.funding.api.FundingSettings

internal class FakeFundingSettings(
    private var reminderReferenceTimestamp: Long = 0L,
    private var reminderShownTimestamp: Long = 0L,
    private var activityCounterInMillis: Long = 0L,
) : FundingSettings {
    override fun getReminderReferenceTimestamp(): Long {
        return reminderReferenceTimestamp
    }

    override fun setReminderReferenceTimestamp(timestamp: Long) {
        reminderReferenceTimestamp = timestamp
    }

    override fun getReminderShownTimestamp(): Long {
        return reminderShownTimestamp
    }

    override fun setReminderShownTimestamp(timestamp: Long) {
        reminderShownTimestamp = timestamp
    }

    override fun getActivityCounterInMillis(): Long {
        return activityCounterInMillis
    }

    override fun setActivityCounterInMillis(activeTime: Long) {
        activityCounterInMillis = activeTime
    }
}
