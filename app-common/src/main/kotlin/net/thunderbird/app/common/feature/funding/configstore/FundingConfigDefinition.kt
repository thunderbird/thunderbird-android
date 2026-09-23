package net.thunderbird.app.common.feature.funding.configstore

import net.thunderbird.core.configstore.ConfigDefinition
import net.thunderbird.core.configstore.ConfigId
import net.thunderbird.core.configstore.ConfigKey
import net.thunderbird.core.configstore.ConfigMapper
import net.thunderbird.core.configstore.ConfigMigration
import net.thunderbird.feature.funding.api.FundingConfig
import net.thunderbird.feature.funding.api.FundingConfigKeys

class FundingConfigDefinition(override val id: ConfigId) : ConfigDefinition<FundingConfig> {
    override val version: Int = 1
    override val mapper: ConfigMapper<FundingConfig> = FundingConfigMapper()
    override val defaultValue: FundingConfig = FundingConfig.DEFAULT
    override val keys: List<ConfigKey<*>> = listOf(
        FundingConfigKeys.LAST_FUNDING_REMINDER_SHOWN_ACTIVITY_AMOUNT,
        FundingConfigKeys.FUNDING_REMINDER_COUNT,
    )
    override val migration: ConfigMigration = FundingConfigMigration()
}
