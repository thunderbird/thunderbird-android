package net.thunderbird.feature.funding.api

interface FundingManager {
    /**
     * Returns the type of funding.
     */
    fun getFundingType(): FundingType

    /**
     * Adds a funding reminder.
     *
     * The reminder is registered to the current lifecycle of the Activity.
     *
     * @param onOpenFunding The callback to be called when the user opens the funding.
     */
    fun addFundingReminder(onOpenFunding: () -> Unit)
}
