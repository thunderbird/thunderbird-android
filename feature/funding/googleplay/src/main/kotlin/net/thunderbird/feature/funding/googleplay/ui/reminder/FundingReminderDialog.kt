package net.thunderbird.feature.funding.googleplay.ui.reminder

import androidx.fragment.app.FragmentManager
import net.thunderbird.feature.funding.common.api.FundingReminderContract
import net.thunderbird.feature.funding.common.ui.FundingReminderContainerFragment

class FundingReminderDialog : FundingReminderContract.Dialog {
    override fun show(fragmentManager: FragmentManager) {
        val dialogFragment = FundingReminderContainerFragment()
        dialogFragment.show(fragmentManager, null)
    }
}
