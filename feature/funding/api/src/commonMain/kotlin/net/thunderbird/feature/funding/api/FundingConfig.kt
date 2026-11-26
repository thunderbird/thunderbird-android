package net.thunderbird.feature.funding.api

import kotlinx.serialization.Serializable

@Serializable
data class FundingConfig(
    val reminderReferenceTimestamp: Long?,
    val lastFundingReminderShownTimestamp: Long?,
    val fundingReminderCount: Int?,
) {
    companion object {
        val DEFAULT = FundingConfig(
            reminderReferenceTimestamp = null,
            lastFundingReminderShownTimestamp = null,
            fundingReminderCount = null,
        )
    }
}
