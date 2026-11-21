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
    }

    private val themeProvider: FeatureThemeProvider by inject<FeatureThemeProvider>()

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        dialog?.requestWindowFeature(Window.FEATURE_NO_TITLE)

        val headerRes: Int = arguments?.getInt(FUNDING_DIALOG_HEADER) ?: R.string.funding_reminder_title
        val bodyRes: Int = arguments?.getInt(FUNDING_DIALOG_BODY) ?: R.string.funding_reminder_appeal_content
        val imageRes: Int = arguments?.getInt(FUNDING_DIALOG_IMAGE) ?: R.drawable.funding_roc_banner

        return ComposeView(requireContext()).apply {
            setViewCompositionStrategy(ViewCompositionStrategy.DisposeOnViewTreeLifecycleDestroyed)
            setContent {
                themeProvider.WithTheme(darkTheme = false) {
                    FundingReminderDialog(
                        headerRes = headerRes,
                        bodyRes = bodyRes,
                        imageRes = imageRes,
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
