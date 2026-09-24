package net.thunderbird.feature.funding.googleplay.ui.reminder

import net.thunderbird.feature.funding.api.FundingSettings

internal class FakeFundingSettings(
    private var reminderReferenceTimestamp: Long = 0L,
    private var reminderShownTimestamp: Long = 0L,
    private var lastReminderShownActivityAmount: Long = 0L,
    private var fundingReminderCount: Int = 0,
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

    override fun getLastReminderShownActivityAmount(): Long = lastReminderShownActivityAmount

    override suspend fun setLastReminderShownActivityAmount(activityInMillis: Long) {
        lastReminderShownActivityAmount = activityInMillis
    }

    override fun getReminderShownCount(): Int = fundingReminderCount

    override suspend fun setReminderShownCount(count: Int) {
        fundingReminderCount = count
    }

    override suspend fun incrementReminderShownCount() {
        fundingReminderCount++
    }

    override fun getActivityCounterInMillis(): Long {
        return activityCounterInMillis
    }

    override fun setActivityCounterInMillis(activeTime: Long) {
        activityCounterInMillis = activeTime
    }
}
