package app.k9mail.feature.funding.noop

import app.k9mail.feature.funding.api.FundingManager

class NoOpFundingManager : FundingManager {
    override fun isFundingFeatureIncluded(): Boolean {
        return false
    }
}
