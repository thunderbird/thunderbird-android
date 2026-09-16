package net.thunderbird.app.common.feature.funding.configstore

import net.thunderbird.core.configstore.Config
import net.thunderbird.core.configstore.ConfigMapper

class FundingConfigMapper: ConfigMapper<FundingConfig> {
    override fun toConfig(obj: FundingConfig): Config = Config().apply {
        this[FundingConfigKeys.LAST_FUNDING_REMINDER_SHOWN_TIMESTAMP] = obj.lastFundingReminderShownTimestamp
        this[FundingConfigKeys.FUNDING_REMINDER_COUNT] = obj.fundingReminderCount
    }

    override fun fromConfig(config: Config): FundingConfig = FundingConfig(
        lastFundingReminderShownTimestamp =
            config[FundingConfigKeys.LAST_FUNDING_REMINDER_SHOWN_TIMESTAMP]?.let { 0L } as Long,
        fundingReminderCount =
            config[FundingConfigKeys.FUNDING_REMINDER_COUNT]?.let { 0 } as Int
    )
}
