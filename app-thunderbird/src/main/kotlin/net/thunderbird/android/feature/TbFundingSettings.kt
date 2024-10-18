package net.thunderbird.android.feature

import app.k9mail.feature.funding.api.FundingSettings
import com.fsck.k9.K9

class TbFundingSettings : FundingSettings {
    override fun getFundingReminderShownTimestamp() = K9.fundingReminderShownTimestamp

    override fun setFundingReminderShownTimestamp(timestamp: Long) {
        K9.fundingReminderShownTimestamp = timestamp
        K9.saveSettingsAsync()
    }
}
