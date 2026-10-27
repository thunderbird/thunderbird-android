package app.k9mail.feature

import net.thunderbird.feature.funding.api.FundingSettings

@Suppress("TooManyFunctions", "EmptyFunctionBlock")
internal class K9FundingSettings : FundingSettings {

    override fun isReady(): Boolean = true

    override fun getReminderReferenceTimestamp(): Long = 0L

    override fun setReminderReferenceTimestamp(timestamp: Long) {}

    override fun getReminderShownTimestamp() = 0L

    override fun setReminderShownTimestamp(timestamp: Long) {}

    override fun getLastReminderShownActivityAmount(): Long = 0L

    override suspend fun setLastReminderShownActivityAmount(activityInMillis: Long) {}

    override fun getReminderShownCount(): Int = 100

    override suspend fun setReminderShownCount(count: Int) {}

    override suspend fun incrementReminderShownCount() {}

    override fun getActivityCounterInMillis(): Long = 0L

    override fun setActivityCounterInMillis(activeTime: Long) {}
}
