package app.k9mail.feature.funding.googleplay

import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import app.k9mail.feature.funding.api.FundingManager
import app.k9mail.feature.funding.api.FundingType
import app.k9mail.feature.funding.googleplay.ui.reminder.FundingReminderContract

class GooglePlayFundingManager(
    private val reminder: FundingReminderContract.Reminder,
) : FundingManager {
    override fun getFundingType(): FundingType {
        return FundingType.GOOGLE_PLAY
    }

    override fun addFundingReminder(activity: AppCompatActivity, launcherIntent: Intent) {
        reminder.registerReminder(activity, launcherIntent)
    }
}
