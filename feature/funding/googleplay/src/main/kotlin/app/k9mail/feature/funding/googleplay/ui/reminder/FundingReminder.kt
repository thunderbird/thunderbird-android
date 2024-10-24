package app.k9mail.feature.funding.googleplay.ui.reminder

import android.content.Context
import android.content.pm.PackageManager
import androidx.appcompat.app.AppCompatActivity
import app.k9mail.feature.funding.api.FundingSettings
import kotlinx.datetime.Clock

class FundingReminder(
    private val settings: FundingSettings,
    private val dialog: FundingReminderContract.Dialog,
    private val clock: Clock = Clock.System,
) : FundingReminderContract.Reminder {

    override fun registerReminder(
        activity: AppCompatActivity,
        onOpenFunding: () -> Unit,
    ) {
        // If the reminder reference timestamp is not set, we set it to the first install time.
        if (settings.getReminderReferenceTimestamp() == 0L) {
            resetReminderReferenceTimestamp(activity)
        }

        // If the reminder has already been shown, we don't need to show it again.
        if (wasReminderShown()) {
            return
        }

        if (shouldShowReminder()) {
            dialog.show(activity, onOpenFunding)
        }
    }

    private fun wasReminderShown(): Boolean {
        return settings.getReminderShownTimestamp() != 0L
    }

    private fun shouldShowReminder(): Boolean {
        val currentTime = clock.now().toEpochMilliseconds()

        return settings.getReminderShownTimestamp() == 0L &&
            settings.getReminderReferenceTimestamp() + FUNDING_REMINDER_DELAY_MILLIS <= currentTime
            && settings.getActivityCounterInMillis() >= FUNDING_REMINDER_MIN_ACTIVITY_MILLIS
    }

    private fun resetReminderReferenceTimestamp(context: Context) {
        try {
            val installTime = context.packageManager.getPackageInfo(context.packageName, 0).firstInstallTime
            settings.setReminderReferenceTimestamp(installTime)
        } catch (exception: PackageManager.NameNotFoundException) {
            settings.setReminderReferenceTimestamp(clock.now().toEpochMilliseconds())
        }
    }
}
