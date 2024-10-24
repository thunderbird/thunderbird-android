package app.k9mail.feature.funding.api

import androidx.appcompat.app.AppCompatActivity

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
     * @param activity The activity to register the reminder to.
     * @param onOpenFunding The callback to be called when the user opens the funding.
     */
    fun addFundingReminder(activity: AppCompatActivity, onOpenFunding: () -> Unit)
}
