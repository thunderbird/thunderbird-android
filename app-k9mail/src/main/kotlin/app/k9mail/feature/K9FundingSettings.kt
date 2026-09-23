package app.k9mail.feature

import com.fsck.k9.K9
import net.thunderbird.feature.funding.api.FundingConfig
import net.thunderbird.feature.funding.api.FundingConfigStore
import net.thunderbird.feature.funding.api.FundingSettings

internal class K9FundingSettings(
    private val fundingConfigStore: FundingConfigStore,
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
        return fundingConfigStore.configStateFlow.value.lastFundingReminderShownTimestamp
    }

    override suspend fun setLastReminderShownTimestamp(timestamp: Long) {
        fundingConfigStore.update {
            val oldConfig = it ?: FundingConfig.DEFAULT
            oldConfig.copy(
                lastFundingReminderShownTimestamp = timestamp,
                fundingReminderCount = oldConfig.fundingReminderCount,
            )
        }
    }

    override fun getReminderShownCount(): Int {
        return fundingConfigStore.configStateFlow.value.fundingReminderCount
    }

    override suspend fun setReminderShownCount(count: Int) {
        fundingConfigStore.update {
            val oldConfig = it ?: FundingConfig.DEFAULT
            oldConfig.copy(
                lastFundingReminderShownTimestamp = oldConfig.lastFundingReminderShownTimestamp,
                fundingReminderCount = count,
            )
        }
    }

    override suspend fun incrementReminderShownCount() {
        setReminderShownCount(getReminderShownCount() + 1)
    }

    override fun getActivityCounterInMillis(): Long = K9.fundingActivityCounterInMillis

    override fun setActivityCounterInMillis(activeTime: Long) {
        K9.fundingActivityCounterInMillis = activeTime
        K9.saveSettingsAsync()
    }
}
