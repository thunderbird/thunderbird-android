package net.thunderbird.app.common.feature.funding.api

import kotlinx.serialization.Serializable

@Serializable
data class FundingConfig(
    val lastFundingReminderShownTimestamp: Long,
    val fundingReminderCount: Int,
) {
    companion object {
        val DEFAULT = FundingConfig(
            lastFundingReminderShownTimestamp = 0L,
            fundingReminderCount = 0,
        )
    }
}
