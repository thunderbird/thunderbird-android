package app.k9mail.feature.funding.api

interface FundingSettings {
    fun getReminderReferenceTimestamp(): Long
    fun setReminderReferenceTimestamp(timestamp: Long)

    fun getReminderShownTimestamp(): Long
    fun setReminderShownTimestamp(timestamp: Long)

    fun getActivityCounterInMillis(): Long
    fun setActivityCounterInMillis(activeTime: Long)
}
