package app.k9mail.feature.funding.api

interface FundingManager {
    /**
     * Returns the type of funding.
     */
    fun getFundingType(): FundingType
}
