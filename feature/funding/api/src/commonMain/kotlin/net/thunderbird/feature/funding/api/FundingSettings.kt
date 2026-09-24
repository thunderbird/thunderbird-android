package net.thunderbird.feature.funding.api

interface FundingSettings {
    fun isReady(): Boolean
    fun getReminderReferenceTimestamp(): Long
    fun setReminderReferenceTimestamp(timestamp: Long)

    fun getReminderShownTimestamp(): Long
    fun setReminderShownTimestamp(timestamp: Long)

    fun getLastReminderShownActivityAmount(): Long
    suspend fun setLastReminderShownActivityAmount(activityInMillis: Long)

    fun getReminderShownCount(): Int
    suspend fun setReminderShownCount(count: Int)
    suspend fun incrementReminderShownCount()

    fun getActivityCounterInMillis(): Long
    fun setActivityCounterInMillis(activeTime: Long)
}
