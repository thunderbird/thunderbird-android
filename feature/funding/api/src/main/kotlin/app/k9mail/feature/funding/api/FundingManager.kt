package app.k9mail.feature.funding.api

interface FundingManager {

    /**
     * Returns `true` if the app has a funding feature included.
     */
    fun isFundingFeatureIncluded(): Boolean
}
