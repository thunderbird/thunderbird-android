package net.thunderbird.android.feature

import com.fsck.k9.K9
import net.thunderbird.feature.funding.api.FundingConfig
import net.thunderbird.feature.funding.api.FundingConfigStore
import net.thunderbird.feature.funding.api.FundingSettings

@Suppress("TooManyFunctions")
internal class TbFundingSettings(
    private val fundingConfigStore: FundingConfigStore,
) : FundingSettings {

    override fun isReady(): Boolean = fundingConfigStore.configStateFlow.value != FundingConfig.DEFAULT

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

    override fun getLastReminderShownActivityAmount(): Long {
        return fundingConfigStore.configStateFlow.value.lastFundingReminderShownActivityAmount ?: 0L
    }

    override suspend fun setLastReminderShownActivityAmount(activityInMillis: Long) {
        fundingConfigStore.update {
            val oldConfig = it ?: FundingConfig.DEFAULT
            oldConfig.copy(
                lastFundingReminderShownActivityAmount = activityInMillis,
            )
        }
    }

    override fun getReminderShownCount(): Int {
        return fundingConfigStore.configStateFlow.value.fundingReminderCount ?: 0
    }

    override suspend fun setReminderShownCount(count: Int) {
        fundingConfigStore.update {
            val oldConfig = it ?: FundingConfig.DEFAULT
            oldConfig.copy(
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
