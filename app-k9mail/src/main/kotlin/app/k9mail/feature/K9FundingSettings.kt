package app.k9mail.feature

import com.fsck.k9.K9
import net.thunderbird.app.common.feature.funding.configstore.FundingConfigStore
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

    override fun getLastReminderShownTimestamp(): Long = K9.lastFundingReminderShownTimestamp

    override fun setLastReminderShownTimestamp(timestamp: Long) {
        K9.lastFundingReminderShownTimestamp = timestamp
        K9.saveSettingsAsync()
    }

    override fun getReminderShownCount(): Int = K9.fundingReminderCount

    override fun setReminderShownCount(count: Int) {
        K9.fundingReminderCount = count
        K9.saveSettingsAsync()
    }

    override fun getActivityCounterInMillis(): Long = K9.fundingActivityCounterInMillis

    override fun setActivityCounterInMillis(activeTime: Long) {
        K9.fundingActivityCounterInMillis = activeTime
        K9.saveSettingsAsync()
    }
}
