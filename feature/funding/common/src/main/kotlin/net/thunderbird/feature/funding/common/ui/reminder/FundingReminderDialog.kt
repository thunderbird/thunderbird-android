package net.thunderbird.feature.funding.common.ui.reminder

import android.os.Bundle
import androidx.annotation.DrawableRes
import androidx.fragment.app.FragmentManager
import net.thunderbird.core.common.resources.StringRes
import net.thunderbird.feature.funding.common.api.FundingReminderContract
import net.thunderbird.feature.funding.common.ui.FundingReminderContainerFragment
import net.thunderbird.feature.funding.common.ui.FundingReminderContainerFragment.Companion.FUNDING_DIALOG_BODY
import net.thunderbird.feature.funding.common.ui.FundingReminderContainerFragment.Companion.FUNDING_DIALOG_HEADER
import net.thunderbird.feature.funding.common.ui.FundingReminderContainerFragment.Companion.FUNDING_DIALOG_IMAGE

class FundingReminderDialog : FundingReminderContract.Dialog {
    override fun show(
        fragmentManager: FragmentManager,
        @StringRes headerRes: Int?,
        @StringRes bodyRes: Int?,
        @DrawableRes imageRes: Int?,
    ) {
        val dialogFragment = FundingReminderContainerFragment()
        if (headerRes != null || bodyRes != null || imageRes != null) {
            val bundle = Bundle()
            if (headerRes != null) bundle.putInt(FUNDING_DIALOG_HEADER, headerRes)
            if (bodyRes != null) bundle.putInt(FUNDING_DIALOG_BODY, bodyRes)
            if (imageRes != null) bundle.putInt(FUNDING_DIALOG_IMAGE, imageRes)
            dialogFragment.arguments = bundle
        }
        dialogFragment.show(fragmentManager, null)
    }
}
