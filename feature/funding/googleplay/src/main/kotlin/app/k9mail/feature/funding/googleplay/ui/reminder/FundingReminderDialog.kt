package app.k9mail.feature.funding.googleplay.ui.reminder

import androidx.appcompat.app.AppCompatActivity
import app.k9mail.feature.funding.api.FundingSettings
import app.k9mail.feature.funding.googleplay.R
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import kotlinx.datetime.Clock

class FundingReminderDialog(
    private val settings: FundingSettings,
    private val clock: Clock = Clock.System,
) : FundingReminderContract.Dialog {
    override fun show(activity: AppCompatActivity, onOpenFunding: () -> Unit) {
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
