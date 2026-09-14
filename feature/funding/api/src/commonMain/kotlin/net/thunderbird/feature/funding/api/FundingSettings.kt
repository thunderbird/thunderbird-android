package net.thunderbird.feature.funding.api

interface FundingSettings {
    fun getReminderReferenceTimestamp(): Long
    fun setReminderReferenceTimestamp(timestamp: Long)

    fun getReminderShownTimestamp(): Long
    fun setReminderShownTimestamp(timestamp: Long)

    fun getLastReminderShownTimestamp(): Long
    fun setLastReminderShownTimestamp(timestamp: Long)

    fun getReminderShownCount(): Int
    fun setReminderShownCount(count: Int)

    fun getActivityCounterInMillis(): Long
    fun setActivityCounterInMillis(activeTime: Long)
}
