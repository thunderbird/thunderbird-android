package net.thunderbird.feature.funding.common.ui.reminder

import android.os.Bundle
import androidx.fragment.app.FragmentManager
import net.thunderbird.feature.funding.common.api.FundingReminderContract
import net.thunderbird.feature.funding.common.ui.FundingReminderContainerFragment
import net.thunderbird.feature.funding.common.ui.FundingReminderContainerFragment.Companion.FUNDING_DIALOG_BODY
import net.thunderbird.feature.funding.common.ui.FundingReminderContainerFragment.Companion.FUNDING_DIALOG_HEADER

class FundingReminderDialog : FundingReminderContract.Dialog {
    override fun show(fragmentManager: FragmentManager, headerRes: Int?, bodyRes: Int?) {
        val dialogFragment = FundingReminderContainerFragment()
        if (headerRes != null && bodyRes != null) {
            val bundle = Bundle()
            bundle.putInt(FUNDING_DIALOG_HEADER, headerRes)
            bundle.putInt(FUNDING_DIALOG_BODY, bodyRes)
            dialogFragment.arguments = bundle
        }
        dialogFragment.show(fragmentManager, null)
    }
}
