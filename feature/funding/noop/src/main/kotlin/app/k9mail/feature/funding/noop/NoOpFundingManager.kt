package app.k9mail.feature.funding.noop

import app.k9mail.feature.funding.api.FundingManager
import app.k9mail.feature.funding.api.FundingType

class NoOpFundingManager : FundingManager {
    override fun getFundingType(): FundingType {
        return FundingType.NO_FUNDING
    }
}
