package app.k9mail.feature.funding.googleplay

import app.k9mail.feature.funding.api.FundingManager
import app.k9mail.feature.funding.api.FundingType

class GooglePlayFundingManager : FundingManager {
    override fun getFundingType(): FundingType {
        return FundingType.GOOGLE_PLAY
    }
}
