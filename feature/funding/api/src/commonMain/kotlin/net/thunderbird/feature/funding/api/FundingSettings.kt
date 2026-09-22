package net.thunderbird.feature.funding.api

interface FundingSettings {
    fun getReminderReferenceTimestamp(): Long
    fun setReminderReferenceTimestamp(timestamp: Long)

    fun getReminderShownTimestamp(): Long
    fun setReminderShownTimestamp(timestamp: Long)

    fun getLastReminderShownTimestamp(): Long
    suspend fun setLastReminderShownTimestamp(timestamp: Long)

    fun getReminderShownCount(): Int
    suspend fun setReminderShownCount(count: Int)
    suspend fun incrementReminderShownCount()

    fun getActivityCounterInMillis(): Long
    fun setActivityCounterInMillis(activeTime: Long)
}
