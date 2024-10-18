package app.k9mail.feature.funding.link

import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import app.k9mail.feature.funding.api.FundingManager
import app.k9mail.feature.funding.api.FundingType

class LinkFundingManager : FundingManager {
    override fun getFundingType(): FundingType {
        return FundingType.LINK
    }

    override fun addFundingReminder(activity: AppCompatActivity, launcherBaseIntent: Intent) = Unit
}
