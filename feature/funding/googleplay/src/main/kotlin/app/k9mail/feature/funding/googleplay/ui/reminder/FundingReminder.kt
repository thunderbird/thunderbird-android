package app.k9mail.feature.funding.googleplay.ui.reminder

import android.content.Context
import android.content.pm.PackageManager.NameNotFoundException
import androidx.appcompat.app.AppCompatActivity
import app.k9mail.feature.funding.api.FundingSettings
import kotlinx.datetime.Clock
import app.k9mail.feature.funding.googleplay.ui.reminder.FundingReminderContract.ActivityLifecycleObserver
import app.k9mail.feature.funding.googleplay.ui.reminder.FundingReminderContract.FragmentLifecycleObserver

class FundingReminder(
    private val settings: FundingSettings,
    private val fragmentObserver: FragmentLifecycleObserver,
    private val activityCounterObserver: ActivityLifecycleObserver,
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

        // We register the activity counter observer to keep track of the time the user spends in the app.
        // We also ensure that the observers are unregistered when the activity is destroyed.
        activityCounterObserver.register(activity.lifecycle) {
            fragmentObserver.unregister(activity.supportFragmentManager)
            activityCounterObserver.unregister(activity.lifecycle)
        }

        // If the reminder has already been shown, we don't need to show it again.
        if (wasReminderShown()) {
            return
        }

        if (shouldShowReminder()) {
            fragmentObserver.register(activity.supportFragmentManager) {
                dialog.show(activity, onOpenFunding)
            }
        }
    }

    private fun wasReminderShown(): Boolean {
        return settings.getReminderShownTimestamp() != 0L
    }

    private fun shouldShowReminder(): Boolean {
        val currentTime = clock.now().toEpochMilliseconds()

        return settings.getReminderShownTimestamp() == 0L &&
            settings.getReminderReferenceTimestamp() + FUNDING_REMINDER_DELAY_MILLIS <= currentTime &&
            settings.getActivityCounterInMillis() >= FUNDING_REMINDER_MIN_ACTIVITY_MILLIS
    }

    @Suppress("SwallowedException")
    private fun resetReminderReferenceTimestamp(context: Context) {
        try {
            val installTime = context.packageManager.getPackageInfo(context.packageName, 0).firstInstallTime
            settings.setReminderReferenceTimestamp(installTime)
        } catch (exception: NameNotFoundException) {
            settings.setReminderReferenceTimestamp(clock.now().toEpochMilliseconds())
        }
    }
}
