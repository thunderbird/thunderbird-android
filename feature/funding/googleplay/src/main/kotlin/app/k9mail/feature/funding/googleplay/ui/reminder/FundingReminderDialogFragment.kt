package app.k9mail.feature.funding.googleplay.ui.reminder

import android.app.Dialog
import android.os.Bundle
import androidx.core.os.bundleOf
import androidx.fragment.app.DialogFragment
import androidx.fragment.app.setFragmentResult
import app.k9mail.feature.funding.googleplay.R
import com.google.android.material.dialog.MaterialAlertDialogBuilder

internal class FundingReminderDialogFragment : DialogFragment() {
    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        val contentView = layoutInflater.inflate(R.layout.funding_googleplay_contribution_reminder, null)

        return MaterialAlertDialogBuilder(requireContext())
            .setIcon(R.drawable.funding_googleplay_contribution_reminder_icon)
            .setTitle(R.string.funding_googleplay_contribution_reminder_title)
            .setView(contentView)
            .setPositiveButton(R.string.funding_googleplay_contribution_reminder_positive_button) { _, _ ->
                handlePositiveButton()
            }
            .setNegativeButton(R.string.funding_googleplay_contribution_reminder_negative_button, null)
            .create()
    }

    private fun handlePositiveButton() {
        setFragmentResult(
            FundingReminderContract.Dialog.FRAGMENT_REQUEST_KEY,
            bundleOf(FundingReminderContract.Dialog.FRAGMENT_RESULT_SHOW_FUNDING to true),
        )
    }
}
