package app.k9mail.feature

import com.fsck.k9.R
import net.thunderbird.feature.funding.api.FundingConfig
import net.thunderbird.feature.funding.api.FundingConfigStore
import net.thunderbird.feature.funding.api.FundingSettings

@Suppress("TooManyFunctions")
internal class K9FundingSettings(
    private val fundingConfigStore: FundingConfigStore,
) : FundingSettings {

    override fun isReady(): Boolean = fundingConfigStore.configStateFlow.value != FundingConfig.DEFAULT

    override fun getReminderReferenceTimestamp(): Long {
        return fundingConfigStore.configStateFlow.value.reminderReferenceTimestamp ?: 0
    }

    override suspend fun setReminderReferenceTimestamp(timestamp: Long) {
        fundingConfigStore.update {
            val oldConfig = it ?: FundingConfig.DEFAULT
            oldConfig.copy(
                reminderReferenceTimestamp = timestamp,
            )
        }
    }

    override fun getReminderShownTimestamp(): Long {
        return fundingConfigStore.configStateFlow.value.lastFundingReminderShownTimestamp ?: 0L
    }

    override suspend fun setReminderShownTimestamp(timestamp: Long) {
        fundingConfigStore.update {
            val oldConfig = it ?: FundingConfig.DEFAULT
            oldConfig.copy(
                lastFundingReminderShownTimestamp = timestamp,
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

    override fun getReminderDefaultBannerResource(): Int {
        return R.drawable.funding_k9_contribution_reminder_icon
    }

    override fun getReminderDefaultBannerResourceContentDescription(): Int {
        return R.string.funding_k9_dialog_icon_content_description
    }

    override fun isUsingBannerForDialog(): Boolean = false
}
