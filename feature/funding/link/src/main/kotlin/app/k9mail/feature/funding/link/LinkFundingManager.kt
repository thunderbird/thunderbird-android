package app.k9mail.feature.funding.link

import app.k9mail.feature.funding.api.FundingManager
import app.k9mail.feature.funding.api.FundingType

class LinkFundingManager : FundingManager {
    override fun getFundingType(): FundingType {
        return FundingType.LINK
    }
}
