package app.k9mail.feature.funding.googleplay.ui.reminder

import android.content.Context
import android.content.pm.PackageManager.NameNotFoundException
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.FragmentManager
import app.k9mail.feature.funding.api.FundingSettings
import app.k9mail.feature.funding.googleplay.ui.reminder.FundingReminderContract.ActivityLifecycleObserver
import app.k9mail.feature.funding.googleplay.ui.reminder.FundingReminderContract.FragmentLifecycleObserver
import kotlinx.datetime.Clock

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
        // TODO: Let the caller make the decision on which FragmentManager to use.
        val dialogFragmentManager = activity.supportFragmentManager

        // TODO: Let the caller provide this. Or, better yet, let the caller notify FundingReminder when it's a good
        //  time to display the funding reminder dialog.
        val observedFragmentManager = activity.supportFragmentManager

        dialogFragmentManager.setFragmentResultListener(
            FundingReminderContract.Dialog.FRAGMENT_REQUEST_KEY,
            activity
        ) { _, result ->
            if (result.getBoolean(FundingReminderContract.Dialog.FRAGMENT_RESULT_SHOW_FUNDING, false)) {
                onOpenFunding()
            }
        }

        // If the reminder reference timestamp is not set, we set it to the first install time.
        if (settings.getReminderReferenceTimestamp() == 0L) {
            resetReminderReferenceTimestamp(activity)
        }

        // We register the activity counter observer to keep track of the time the user spends in the app.
        // We also ensure that the observers are unregistered when the activity is destroyed.
        activityCounterObserver.register(activity.lifecycle) {
            fragmentObserver.unregister(observedFragmentManager)
            activityCounterObserver.unregister(activity.lifecycle)
        }

        // If the reminder has already been shown, we don't need to show it again.
        if (wasReminderShown()) {
            return
        }

        if (shouldShowReminder()) {
            fragmentObserver.register(observedFragmentManager) {
                showFundingReminderDialog(dialogFragmentManager)
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

    private fun showFundingReminderDialog(fragmentManager: FragmentManager) {
        // We're about to show the funding reminder dialog. So mark it as being shown. This way, if there's an error,
        // we err on the side of the dialog not being shown rather than it being shown more than once.
        settings.setReminderShownTimestamp(clock.now().toEpochMilliseconds())

        dialog.show(fragmentManager)
    }
}
