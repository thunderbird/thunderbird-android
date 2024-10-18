package app.k9mail.feature.funding.googleplay.ui.reminder

import android.content.Intent
import androidx.appcompat.app.AppCompatActivity

const val FUNDING_REMINDER_DELAY_MILLIS = 30 * 60 * 1000L

interface FundingReminderContract {

    interface Reminder {
        fun registerReminder(activity: AppCompatActivity, launcherIntent: Intent)
    }
}
