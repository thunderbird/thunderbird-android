package net.thunderbird.feature.funding.googleplay.ui.reminder

import android.content.Context
import android.content.pm.PackageManager.NameNotFoundException
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.FragmentManager
import kotlin.time.Clock
import kotlin.time.Duration.Companion.milliseconds
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import net.thunderbird.core.android.common.activity.ActivityProvider
import net.thunderbird.feature.funding.api.FundingSettings
import net.thunderbird.feature.funding.common.api.FUNDING_REMINDER_DELAY_MILLIS
import net.thunderbird.feature.funding.common.api.FUNDING_REMINDER_MIN_ACTIVITY_MILLIS
import net.thunderbird.feature.funding.common.api.FundingReminderContract
import net.thunderbird.feature.funding.common.api.FundingReminderContract.ActivityLifecycleObserver
import net.thunderbird.feature.funding.common.api.FundingReminderContract.FragmentLifecycleObserver

class FundingReminder(
    private val activityProvider: ActivityProvider,
    private val settings: FundingSettings,
    private val fragmentObserver: FragmentLifecycleObserver,
    private val activityCounterObserver: ActivityLifecycleObserver,
    private val dialog: FundingReminderContract.Dialog,
    private val clock: Clock = Clock.System,
    private val scope: CoroutineScope,
) : FundingReminderContract.Reminder {

    init {
        val activity = activityProvider.getCurrent() as AppCompatActivity
        val observedFragmentManager = activity.supportFragmentManager
        activityCounterObserver.register(activity.lifecycle) {
            fragmentObserver.unregister(observedFragmentManager)
            activityCounterObserver.unregister(activity.lifecycle)
        }
    }

    /**
     * Decide to display the reminder and do so if necessary
     * We may choose to refactor this in the future to allow for multiple ongoing campaigns:
     *      https://github.com/thunderbird/thunderbird-android/issues/11557
     */
    override fun registerReminder(
        onOpenFunding: () -> Unit,
    ) {
        scope.launch {
            // Wait a bit so settings can be ready to be used.
            while (wasReminderShown() && !settings.isReady()) {
                delay(250.milliseconds)
            }
            val activity = activityProvider.getCurrent() as? AppCompatActivity ?: return@launch
            // TODO: Let the caller make the decision on which FragmentManager to use.
            val dialogFragmentManager = activity.supportFragmentManager

            // TODO: Let the caller provide this. Or, better yet, let the caller notify FundingReminder when it's a good
            //  time to display the funding reminder dialog.
            val observedFragmentManager = activity.supportFragmentManager

            dialogFragmentManager.setFragmentResultListener(
                FundingReminderContract.Dialog.FRAGMENT_REQUEST_KEY,
                activity,
            ) { _, result ->
                if (result.getBoolean(FundingReminderContract.Dialog.FRAGMENT_RESULT_SHOW_FUNDING, false)) {
                    onOpenFunding()
                }
            }

            // If the reminder reference timestamp is not set, we set it to the first install time.
            if (settings.getReminderReferenceTimestamp() == 0L) {
                resetReminderReferenceTimestamp(activity)
            }

            // If the reminder has already been shown, we don't need to show it again.
            if (wasReminderShown() && wasSecondReminderShown()) {
                return@launch
            }

            when {
                shouldShowReminder() -> fragmentObserver.register(observedFragmentManager) {
                    showFundingReminderDialog(dialogFragmentManager)
                }

                shouldShowSecondReminder() -> fragmentObserver.register(observedFragmentManager) {
                    // TODO make this point to the new dialog: #11526
                    showSecondFundingReminderDialog(dialogFragmentManager)
                }
            }
        }
    }

    private fun wasReminderShown(): Boolean {
        return settings.getReminderShownTimestamp() != 0L || settings.getReminderShownCount() > 0
    }

    private fun wasSecondReminderShown(): Boolean {
        return settings.getReminderShownTimestamp() != 0L &&
            settings.getLastReminderShownActivityAmount() > settings.getReminderShownTimestamp() &&
            settings.getReminderShownCount() >= 2
    }

    private fun shouldShowReminder(): Boolean {
        val currentTime = clock.now().toEpochMilliseconds()

        return settings.getReminderShownTimestamp() == 0L &&
            settings.getReminderReferenceTimestamp() + FUNDING_REMINDER_DELAY_MILLIS <= currentTime &&
            settings.getActivityCounterInMillis() >= FUNDING_REMINDER_MIN_ACTIVITY_MILLIS &&
            settings.getLastReminderShownActivityAmount() == 0L &&
            settings.getReminderShownCount() == 0
    }

    /**
     * The second reminder should display after 30 minutes of activity since the first reminder was shown
     * It should only display if the current reminder has already displayed and has not been displayed already
     */
    private fun shouldShowSecondReminder(): Boolean {
        val activityAtLastReminder = settings.getLastReminderShownActivityAmount()
        val shouldShowTime = activityAtLastReminder + FUNDING_REMINDER_MIN_ACTIVITY_MILLIS
        return settings.getReminderShownTimestamp() > 0L &&
            settings.getActivityCounterInMillis() >= shouldShowTime &&
            settings.getReminderShownCount() > 0 &&
            settings.getReminderShownCount() < 2
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
        scope.launch {
            val now = clock.now().toEpochMilliseconds()
            settings.setReminderShownTimestamp(now)
            settings.setLastReminderShownActivityAmount(settings.getActivityCounterInMillis())
            settings.incrementReminderShownCount()
        }

        dialog.show(fragmentManager)
    }

    private fun showSecondFundingReminderDialog(fragmentManager: FragmentManager) {
        scope.launch {
            val now = clock.now().toEpochMilliseconds()
            val hasSeenFundingReminderBeforeCount = settings.getReminderShownCount() == 0 &&
                settings.getReminderShownTimestamp() != 0L

            settings.setReminderShownTimestamp(now)
            settings.setLastReminderShownActivityAmount(settings.getActivityCounterInMillis())

            // Users who saw the first popup before the counter was added will need to increment this
            // twice as to not potentially see a third one until we release another.
            if (hasSeenFundingReminderBeforeCount) {
                settings.setReminderShownCount(2)
            } else {
                settings.incrementReminderShownCount()
            }
        }
        dialog.show(fragmentManager)
    }
}
