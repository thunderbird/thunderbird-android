package net.thunderbird.feature.funding.api

interface FundingSettings {
    fun isReady(): Boolean
    fun getReminderReferenceTimestamp(): Long
    fun setReminderReferenceTimestamp(timestamp: Long)
    fun getReminderShownTimestamp(): Long
    fun setReminderShownTimestamp(timestamp: Long)
    fun getReminderShownCount(): Int
    suspend fun setReminderShownCount(count: Int)
    suspend fun incrementReminderShownCount()
}
