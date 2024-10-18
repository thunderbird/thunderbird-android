package app.k9mail.feature.funding.api

import android.content.Intent
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
     * @param launcherBaseIntent The feature launcher base intent to add the specific route to.
     */
    fun addFundingReminder(activity: AppCompatActivity, launcherBaseIntent: Intent)
}
