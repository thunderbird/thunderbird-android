package net.thunderbird.app.common.feature.funding.configstore

import net.thunderbird.core.configstore.Config
import net.thunderbird.core.configstore.ConfigMapper
import net.thunderbird.feature.funding.api.FundingConfig
import net.thunderbird.feature.funding.api.FundingConfigKeys

class FundingConfigMapper : ConfigMapper<FundingConfig> {
    override fun toConfig(obj: FundingConfig): Config = Config().apply {
        obj.lastFundingReminderShownActivityAmount?.let {
            this[FundingConfigKeys.LAST_FUNDING_REMINDER_SHOWN_ACTIVITY_AMOUNT] = it
        }
        obj.fundingReminderCount?.let {
            this[FundingConfigKeys.FUNDING_REMINDER_COUNT] = it
        }
    }

    override fun fromConfig(config: Config): FundingConfig = FundingConfig(
        lastFundingReminderShownActivityAmount = config[FundingConfigKeys.LAST_FUNDING_REMINDER_SHOWN_ACTIVITY_AMOUNT],
        fundingReminderCount = config[FundingConfigKeys.FUNDING_REMINDER_COUNT],
    )
}
