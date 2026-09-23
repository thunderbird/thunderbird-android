package net.thunderbird.app.common.feature.funding.configstore

import net.thunderbird.core.configstore.Config
import net.thunderbird.core.configstore.ConfigMapper
import net.thunderbird.feature.funding.api.FundingConfig
import net.thunderbird.feature.funding.api.FundingConfigKeys

class FundingConfigMapper : ConfigMapper<FundingConfig> {
    override fun toConfig(obj: FundingConfig): Config = Config().apply {
        this[FundingConfigKeys.LAST_FUNDING_REMINDER_SHOWN_ACTIVITY_AMOUNT] = obj.lastFundingReminderShownActivityAmount
        this[FundingConfigKeys.FUNDING_REMINDER_COUNT] = obj.fundingReminderCount
    }

    override fun fromConfig(config: Config): FundingConfig = FundingConfig(
        lastFundingReminderShownActivityAmount =
        config[FundingConfigKeys.LAST_FUNDING_REMINDER_SHOWN_ACTIVITY_AMOUNT] ?: 0L,
        fundingReminderCount = config[FundingConfigKeys.FUNDING_REMINDER_COUNT] ?: 0,
    )
}
