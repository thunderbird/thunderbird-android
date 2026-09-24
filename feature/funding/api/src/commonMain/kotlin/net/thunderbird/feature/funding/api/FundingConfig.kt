package net.thunderbird.feature.funding.api

import kotlinx.serialization.Serializable

@Serializable
data class FundingConfig(
    val lastFundingReminderShownActivityAmount: Long,
    val fundingReminderCount: Int,
) {
    companion object {
        val DEFAULT = FundingConfig(
            lastFundingReminderShownActivityAmount = 0L,
            fundingReminderCount = 0,
        )
    }
}
