package app.k9mail.feature

import com.fsck.k9.K9
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import net.thunderbird.feature.funding.api.FundingConfig
import net.thunderbird.feature.funding.api.FundingConfigStore
import net.thunderbird.feature.funding.api.FundingSettings

internal class K9FundingSettings(
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
        return fundingConfigStore.configAsStateFlow().value.lastFundingReminderShownTimestamp
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
        return fundingConfigStore.configAsStateFlow().value.fundingReminderCount
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
