package net.thunderbird.app.common.feature.funding.configstore

import net.thunderbird.core.configstore.Config
import net.thunderbird.core.configstore.ConfigMapper
import net.thunderbird.feature.funding.api.FundingConfig
import net.thunderbird.feature.funding.api.FundingConfigKeys

class FundingConfigMapper : ConfigMapper<FundingConfig> {
    override fun toConfig(obj: FundingConfig): Config = Config().apply {
        obj.reminderReferenceTimestamp?.let {
            this[FundingConfigKeys.REMINDER_REFERENCE_TIMESTAMP] = it
        }
        obj.lastFundingReminderShownTimestamp?.let {
            this[FundingConfigKeys.LAST_FUNDING_REMINDER_SHOWN] = it
        }
        obj.fundingReminderCount?.let {
            this[FundingConfigKeys.FUNDING_REMINDER_COUNT] = it
        }
    }

    override fun fromConfig(config: Config): FundingConfig = FundingConfig(
        reminderReferenceTimestamp = config[FundingConfigKeys.REMINDER_REFERENCE_TIMESTAMP],
        lastFundingReminderShownTimestamp = config[FundingConfigKeys.LAST_FUNDING_REMINDER_SHOWN],
        fundingReminderCount = config[FundingConfigKeys.FUNDING_REMINDER_COUNT],
    )
}
