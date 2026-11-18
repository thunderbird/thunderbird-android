package net.thunderbird.feature.funding.common.ui.reminder

import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.FragmentManager
import kotlin.time.Clock
import kotlin.time.Duration.Companion.milliseconds
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import net.thunderbird.core.android.common.activity.ActivityProvider
import net.thunderbird.feature.funding.api.FundingSettings
import net.thunderbird.feature.funding.common.R
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

    private companion object {
        const val REMINDERS_TO_SHOW = 3
    }

    /**
     * Decide to display the reminder and do so if necessary
     * We may choose to refactor this in the future to allow for multiple ongoing campaigns:
     *      https://github.com/thunderbird/thunderbird-android/issues/11557
     */
    @Suppress("MagicNumber")
    override fun registerReminder(
        onOpenFunding: () -> Unit,
    ) {
        scope.launch {
            // Wait a bit so settings can be ready to be used.
            while (!settings.isReady()) {
                delay(250.milliseconds)
            }

            // If all reminders have already been shown, we don't need to show any again.
            if (allRemindersShown()) {
                return@launch
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

            // Set reference to first launch of activity setting up reminder
            if (settings.getReminderReferenceTimestamp() == 0L) {
                resetReminderReferenceTimestamp()
            }

            if (shouldShowDonationAppeal(1) || shouldShowDonationAppeal(2)) {
                fragmentObserver.register(observedFragmentManager) {
                    showFundingReminderDialog(dialogFragmentManager)
                }
            } else if (shouldShowDonationAppeal(3)) {
                fragmentObserver.register(observedFragmentManager) {
                    showFundingReminderDialog(
                        dialogFragmentManager,
                        R.string.funding_reminder_eoy_title,
                        R.string.funding_reminder_eoy_content,
                    )
                }
            }
        }
    }

    private fun allRemindersShown(): Boolean {
        return settings.getReminderShownTimestamp() != 0L &&
            settings.getReminderShownCount() >= REMINDERS_TO_SHOW &&
            settings.getReminderReferenceTimestamp() != 0L
    }

    private fun shouldShowDonationAppeal(appealCount: Int): Boolean {
        val currentTime = clock.now().toEpochMilliseconds()
        val baseTimestamp =
            if (appealCount == 1) settings.getReminderReferenceTimestamp() else settings.getReminderShownTimestamp()

        return settings.getReminderShownCount() == appealCount - 1 &&
            settings.getReminderReferenceTimestamp() != 0L &&
            baseTimestamp + (FUNDING_REMINDER_DELAY_MILLIS * appealCount) <= currentTime
    }

    private suspend fun resetReminderReferenceTimestamp() {
        settings.setReminderReferenceTimestamp(clock.now().toEpochMilliseconds())
    }

    private fun showFundingReminderDialog(
        fragmentManager: FragmentManager,
        headerRes: Int? = null,
        bodyRes: Int? = null,
    ) {
        // We're about to show the funding reminder dialog. So mark it as being shown. This way, if there's an error,
        // we err on the side of the dialog not being shown rather than it being shown more than once.
        scope.launch {
            val now = clock.now().toEpochMilliseconds()
            settings.setReminderShownTimestamp(now)
            settings.incrementReminderShownCount()
        }

        dialog.show(fragmentManager, headerRes, bodyRes)
    }
}
