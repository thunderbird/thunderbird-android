package net.thunderbird.feature.funding.common.ui.reminder

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
import net.thunderbird.feature.funding.common.api.FundingReminderContract
import net.thunderbird.feature.funding.common.api.FundingReminderContract.FragmentLifecycleObserver

class FundingReminder(
    private val activityProvider: ActivityProvider,
    private val settings: FundingSettings,
    private val fragmentObserver: FragmentLifecycleObserver,
    private val dialog: FundingReminderContract.Dialog,
    private val clock: Clock = Clock.System,
    private val scope: CoroutineScope,
) : FundingReminderContract.Reminder {

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
            settings.getReminderShownCount() >= 2
    }

    private fun shouldShowReminder(): Boolean {
        val currentTime = clock.now().toEpochMilliseconds()

        return settings.getReminderShownTimestamp() == 0L &&
            settings.getReminderReferenceTimestamp() + FUNDING_REMINDER_DELAY_MILLIS <= currentTime &&
            settings.getReminderShownCount() == 0
    }

    /**
     * The second reminder should display after 3 days since the first reminder was shown
     * It should only display if the current reminder has already displayed and has not been displayed already
     */
    private fun shouldShowSecondReminder(): Boolean {
        return settings.getReminderShownCount() in 1..<2 &&
            settings.getReminderShownTimestamp() != 0L &&
            settings.getReminderShownTimestamp() + FUNDING_REMINDER_DELAY_MILLIS < clock.now().toEpochMilliseconds() &&
            settings.getReminderReferenceTimestamp() != 0L
    }

    @Suppress("SwallowedException")
    private fun resetReminderReferenceTimestamp(context: Context) {
        scope.launch {
            try {
                val installTime = context.packageManager.getPackageInfo(context.packageName, 0).firstInstallTime
                settings.setReminderReferenceTimestamp(installTime)
            } catch (exception: NameNotFoundException) {
                settings.setReminderReferenceTimestamp(clock.now().toEpochMilliseconds())
            }
        }
    }

    private fun showFundingReminderDialog(fragmentManager: FragmentManager) {
        // We're about to show the funding reminder dialog. So mark it as being shown. This way, if there's an error,
        // we err on the side of the dialog not being shown rather than it being shown more than once.
        scope.launch {
            val now = clock.now().toEpochMilliseconds()
            settings.setReminderShownTimestamp(now)
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
