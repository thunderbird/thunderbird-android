package net.thunderbird.feature.funding.common.ui

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.Window
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.ViewCompositionStrategy
import androidx.fragment.app.DialogFragment
import androidx.fragment.app.setFragmentResult
import kotlin.getValue
import net.thunderbird.core.ui.theme.api.FeatureThemeProvider
import net.thunderbird.feature.funding.common.R
import net.thunderbird.feature.funding.common.api.FundingReminderContract
import org.koin.android.ext.android.inject

class FundingReminderContainerFragment : DialogFragment() {

    internal companion object {
        const val FUNDING_DIALOG_HEADER = "header"
        const val FUNDING_DIALOG_BODY = "body"
        const val FUNDING_DIALOG_IMAGE = "image"
        const val FUNDING_DIALOG_IMAGE_CONTENT_DESC = "image_desc"
        const val FUNDING_DIALOG_WITH_BANNER = "dialog_with_banner"
    }

    private val themeProvider: FeatureThemeProvider by inject<FeatureThemeProvider>()

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        dialog?.requestWindowFeature(Window.FEATURE_NO_TITLE)

        val headerRes: Int = if ((arguments?.getInt(FUNDING_DIALOG_HEADER) ?: 0) == 0) {
            R.string.funding_reminder_title
        } else {
            arguments?.getInt(FUNDING_DIALOG_HEADER) ?: R.string.funding_reminder_title
        }
        val bodyRes: Int = if ((arguments?.getInt(FUNDING_DIALOG_BODY) ?: 0) == 0) {
            R.string.funding_reminder_appeal_content
        } else {
            arguments?.getInt(FUNDING_DIALOG_BODY) ?: R.string.funding_reminder_appeal_content
        }
        val imageRes: Int = if ((arguments?.getInt(FUNDING_DIALOG_IMAGE) ?: 0) == 0) {
            R.drawable.funding_roc_banner
        } else {
            arguments?.getInt(FUNDING_DIALOG_IMAGE) ?: R.drawable.funding_roc_banner
        }
        val imageDescRes: Int =
            if ((arguments?.getInt(FUNDING_DIALOG_IMAGE_CONTENT_DESC) ?: 0) == 0) {
                R.string.funding_reminder_roc_banner_content_description
            } else {
                arguments?.getInt(FUNDING_DIALOG_IMAGE_CONTENT_DESC)
                    ?: R.string.funding_reminder_roc_banner_content_description
            }
        val isBannerDialog = arguments?.getBoolean(FUNDING_DIALOG_WITH_BANNER) ?: true

        return ComposeView(requireContext()).apply {
            setViewCompositionStrategy(ViewCompositionStrategy.DisposeOnViewTreeLifecycleDestroyed)
            setContent {
                themeProvider.WithTheme(darkTheme = false) {
                    FundingReminderDialog(
                        headerRes = headerRes,
                        bodyRes = bodyRes,
                        imageRes = imageRes,
                        imageDescRes = imageDescRes,
                        isBannerDialog,
                        onDismissClick = ::dismiss,
                        onOkClick = ::handlePositiveButton,
                    )
                }
            }
        }
    }
    private fun handlePositiveButton() {
        setFragmentResult(
            FundingReminderContract.Dialog.FRAGMENT_REQUEST_KEY,
            Bundle().apply {
                putBoolean(FundingReminderContract.Dialog.FRAGMENT_RESULT_SHOW_FUNDING, true)
            },
        )
    }
}
