package net.thunderbird.feature.funding.link

import androidx.appcompat.app.AppCompatActivity
import net.thunderbird.feature.funding.api.FundingManager
import net.thunderbird.feature.funding.api.FundingType

class LinkFundingManager : FundingManager {
    override fun getFundingType(): FundingType {
        return FundingType.LINK
    }

    override fun addFundingReminder(activity: AppCompatActivity, onOpenFunding: () -> Unit) = Unit
}
