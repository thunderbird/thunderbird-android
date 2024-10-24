package app.k9mail.feature.funding.googleplay.ui.reminder

import android.os.Handler
import android.os.Looper
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.LifecycleOwner
import app.k9mail.feature.funding.api.FundingSettings
import app.k9mail.feature.funding.googleplay.R
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import kotlinx.datetime.Clock

class FundingReminder(
    private val settings: FundingSettings,
    private val clock: Clock = Clock.System,
) : FundingReminderContract.Reminder {

    private val handler = Handler(Looper.getMainLooper())
    private var showDialogRunnable: Runnable? = null

    override fun registerReminder(
        activity: AppCompatActivity,
        onOpenFunding: () -> Unit,
    ) {
        if (!shouldShowFundingReminder()) {
            return
        }

        showDialogRunnable = createShowDialogRunnable(activity, onOpenFunding)
        activity.lifecycle.addObserver(
            object : DefaultLifecycleObserver {
                override fun onCreate(owner: LifecycleOwner) {
                    super.onCreate(owner)
                    handler.postDelayed(showDialogRunnable!!, FUNDING_REMINDER_DELAY_MILLIS)
                }

                override fun onDestroy(owner: LifecycleOwner) {
                    super.onDestroy(owner)
                    handler.removeCallbacks(showDialogRunnable!!)
                }
            },
        )
    }

    private fun shouldShowFundingReminder(): Boolean {
        return settings.getReminderShownTimestamp() == 0L
    }

    private fun createShowDialogRunnable(activity: AppCompatActivity, onOpenFunding: () -> Unit): Runnable {
        return Runnable {
            showFundingReminderDialog(activity, onOpenFunding)
        }
    }

    private fun showFundingReminderDialog(activity: AppCompatActivity, onOpenFunding: () -> Unit) {
        MaterialAlertDialogBuilder(activity)
            .setIcon(R.drawable.funding_googleplay_contribution_reminder_icon)
            .setTitle(R.string.funding_googleplay_contribution_reminder_title)
            .setMessage(R.string.funding_googleplay_contribution_reminder_message)
            .setPositiveButton(R.string.funding_googleplay_contribution_reminder_positive_button) { _, _ ->
                onOpenFunding()
            }
            .setNegativeButton(R.string.funding_googleplay_contribution_reminder_negative_button, null)
            .setOnDismissListener {
                settings.setReminderShownTimestamp(clock.now().toEpochMilliseconds())
            }.show()
    }
}
