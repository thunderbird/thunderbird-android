package app.k9mail.feature.funding.noop

import androidx.appcompat.app.AppCompatActivity
import app.k9mail.feature.funding.api.FundingManager
import app.k9mail.feature.funding.api.FundingType

class NoOpFundingManager : FundingManager {
    override fun getFundingType(): FundingType {
        return FundingType.NO_FUNDING
    }

    override fun addFundingReminder(activity: AppCompatActivity, onOpenFunding: () -> Unit) = Unit
}
