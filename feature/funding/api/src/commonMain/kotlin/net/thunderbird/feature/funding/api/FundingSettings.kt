package net.thunderbird.feature.funding.api

interface FundingSettings {
    fun isReady(): Boolean
    fun getReminderReferenceTimestamp(): Long
    suspend fun setReminderReferenceTimestamp(timestamp: Long)
    fun getReminderShownTimestamp(): Long
    suspend fun setReminderShownTimestamp(timestamp: Long)
    fun getReminderShownCount(): Int
    suspend fun setReminderShownCount(count: Int)
    suspend fun incrementReminderShownCount()
}
