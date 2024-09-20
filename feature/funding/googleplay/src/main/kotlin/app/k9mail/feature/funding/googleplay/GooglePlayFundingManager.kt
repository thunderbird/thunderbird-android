package app.k9mail.feature.funding.googleplay

import app.k9mail.feature.funding.api.FundingManager

class GooglePlayFundingManager : FundingManager {
    override fun isFundingFeatureIncluded(): Boolean {
        return true
    }
}
