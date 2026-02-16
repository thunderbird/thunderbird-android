package net.thunderbird.feature.funding.noop

import androidx.appcompat.app.AppCompatActivity
import net.thunderbird.feature.funding.api.FundingManager
import net.thunderbird.feature.funding.api.FundingType

class NoOpFundingManager : FundingManager {
    override fun getFundingType(): FundingType {
        return FundingType.NO_FUNDING
    }

    override fun addFundingReminder(activity: AppCompatActivity, onOpenFunding: () -> Unit) = Unit
}
