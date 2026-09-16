package net.thunderbird.android.feature

import com.fsck.k9.K9
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import net.thunderbird.app.common.feature.funding.configstore.FundingConfig
import net.thunderbird.app.common.feature.funding.configstore.FundingConfigStore
import net.thunderbird.feature.funding.api.FundingSettings

internal class TbFundingSettings(
    private val fundingConfigStore: FundingConfigStore,
    private val scope: CoroutineScope,
) : FundingSettings {
    override fun getReminderReferenceTimestamp(): Long = K9.fundingReminderReferenceTimestamp

    override fun setReminderReferenceTimestamp(timestamp: Long) {
        K9.fundingReminderReferenceTimestamp = timestamp
        K9.saveSettingsAsync()
    }

    override fun getReminderShownTimestamp() = K9.fundingReminderShownTimestamp

    override fun setReminderShownTimestamp(timestamp: Long) {
        K9.fundingReminderShownTimestamp = timestamp
        K9.saveSettingsAsync()
    }

    override fun getLastReminderShownTimestamp(): Long {
        return fundingConfigStore.dataStateFlow().value.lastFundingReminderShownTimestamp
    }

    override fun setLastReminderShownTimestamp(timestamp: Long) {
        scope.launch {
            fundingConfigStore.update {
                val oldConfig = it ?: FundingConfig.DEFAULT
                oldConfig.copy(lastFundingReminderShownTimestamp = timestamp)
            }
        }
    }

    override fun getReminderShownCount(): Int {
        return fundingConfigStore.dataStateFlow().value.fundingReminderCount
    }

    override fun setReminderShownCount(count: Int) {
        scope.launch {
            fundingConfigStore.update {
                val oldConfig = it ?: FundingConfig.DEFAULT
                oldConfig.copy(fundingReminderCount = count)
            }
        }
    }

    override fun getActivityCounterInMillis(): Long = K9.fundingActivityCounterInMillis

    override fun setActivityCounterInMillis(activeTime: Long) {
        K9.fundingActivityCounterInMillis = activeTime
        K9.saveSettingsAsync()
    }
}
