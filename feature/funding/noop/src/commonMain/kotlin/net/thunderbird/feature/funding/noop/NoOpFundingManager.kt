package net.thunderbird.feature.funding.noop

import net.thunderbird.feature.funding.api.FundingManager
import net.thunderbird.feature.funding.api.FundingType

class NoOpFundingManager : FundingManager {
    override fun getFundingType(): FundingType {
        return FundingType.NO_FUNDING
    }

    override fun addFundingReminder(onOpenFunding: () -> Unit) = Unit
}
