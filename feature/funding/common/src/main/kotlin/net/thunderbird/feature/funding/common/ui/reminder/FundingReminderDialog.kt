package net.thunderbird.feature.funding.common.ui.reminder

import android.os.Bundle
import androidx.fragment.app.FragmentManager
import net.thunderbird.feature.funding.common.api.FundingReminderContract
import net.thunderbird.feature.funding.common.api.FundingReminderDialogState
import net.thunderbird.feature.funding.common.ui.FundingReminderContainerFragment
import net.thunderbird.feature.funding.common.ui.FundingReminderContainerFragment.Companion.FUNDING_DIALOG_BODY
import net.thunderbird.feature.funding.common.ui.FundingReminderContainerFragment.Companion.FUNDING_DIALOG_HEADER
import net.thunderbird.feature.funding.common.ui.FundingReminderContainerFragment.Companion.FUNDING_DIALOG_IMAGE
import net.thunderbird.feature.funding.common.ui.FundingReminderContainerFragment.Companion.FUNDING_DIALOG_IMAGE_CONTENT_DESC
import net.thunderbird.feature.funding.common.ui.FundingReminderContainerFragment.Companion.FUNDING_DIALOG_WITH_BANNER

class FundingReminderDialog : FundingReminderContract.Dialog {
    override fun show(
        fragmentManager: FragmentManager,
        dialogState: FundingReminderDialogState?,
    ) {
        val dialogFragment = FundingReminderContainerFragment()
        val bundle = Bundle()
        if (dialogState != null) {
            if (dialogState.headerRes != null) bundle.putInt(FUNDING_DIALOG_HEADER, dialogState.headerRes)
            if (dialogState.bodyRes != null) bundle.putInt(FUNDING_DIALOG_BODY, dialogState.bodyRes)
            if (dialogState.imageRes != null) bundle.putInt(FUNDING_DIALOG_IMAGE, dialogState.imageRes)
            if (dialogState.imageDescRes != null) {
                bundle.putInt(
                    FUNDING_DIALOG_IMAGE_CONTENT_DESC,
                    dialogState.imageDescRes,
                )
            }
            bundle.putBoolean(
                FUNDING_DIALOG_WITH_BANNER,
                dialogState.isBannerDialog,
            )
        }
        dialogFragment.arguments = bundle

        dialogFragment.show(fragmentManager, null)
    }
}
