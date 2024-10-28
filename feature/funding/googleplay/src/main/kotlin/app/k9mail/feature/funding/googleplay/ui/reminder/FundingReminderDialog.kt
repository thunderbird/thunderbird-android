package app.k9mail.feature.funding.googleplay.ui.reminder

import androidx.fragment.app.FragmentManager

class FundingReminderDialog : FundingReminderContract.Dialog {
    override fun show(fragmentManager: FragmentManager) {
        val dialogFragment = FundingReminderDialogFragment()
        dialogFragment.show(fragmentManager, null)
    }
}
