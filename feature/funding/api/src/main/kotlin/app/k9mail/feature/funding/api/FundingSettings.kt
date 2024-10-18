package app.k9mail.feature.funding.api

interface FundingSettings {
    fun getFundingReminderShownTimestamp(): Long
    fun setFundingReminderShownTimestamp(timestamp: Long)
}
