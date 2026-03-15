package net.thunderbird.feature.funding.link

import net.thunderbird.feature.funding.api.FundingManager
import net.thunderbird.feature.funding.api.FundingType

class LinkFundingManager : FundingManager {
    override fun getFundingType(): FundingType {
        return FundingType.LINK
    }

    override fun addFundingReminder(onOpenFunding: () -> Unit) = Unit
}
