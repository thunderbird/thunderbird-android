package net.thunderbird.feature.funding.common.ui

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.Window
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.ViewCompositionStrategy
import androidx.compose.ui.res.stringResource
import androidx.fragment.app.DialogFragment
import androidx.fragment.app.setFragmentResult
import kotlin.getValue
import net.thunderbird.core.common.provider.AppNameProvider
import net.thunderbird.core.ui.theme.api.FeatureThemeProvider
import net.thunderbird.feature.funding.common.R
import net.thunderbird.feature.funding.common.api.FundingReminderContract
import org.koin.android.ext.android.inject

class FundingReminderContainerFragment : DialogFragment() {

    internal companion object {
        const val FUNDING_DIALOG_HEADER = "header"
        const val FUNDING_DIALOG_BODY = "body"
    }

    private val themeProvider: FeatureThemeProvider by inject<FeatureThemeProvider>()
    private val appNameProvider: AppNameProvider by inject<AppNameProvider>()

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        dialog?.requestWindowFeature(Window.FEATURE_NO_TITLE)

        val providedHeader: Int? = arguments?.getInt(FUNDING_DIALOG_HEADER)
        val providedBody: Int? = arguments?.getInt(FUNDING_DIALOG_BODY)

        val header: String = if (providedHeader != null) {
            getString(providedHeader)
        } else {
            val appNameForHeader = appNameProvider.appName
            getString(R.string.funding_reminder_title, appNameForHeader)
        }

        val body: String = if (providedBody != null) {
            getString(providedBody)
        } else {
            val appNameForBody = if (appNameProvider.appName.lowercase().contains("thunderbird")) {
                getString(R.string.funding_reminder_thunderbird_for_android)
            } else {
                appNameProvider.appName
            }
            getString(R.string.funding_reminder_appeal_content, appNameForBody)
        }

        return ComposeView(requireContext()).apply {
            setViewCompositionStrategy(ViewCompositionStrategy.DisposeOnViewTreeLifecycleDestroyed)
            setContent {
                themeProvider.WithTheme(darkTheme = false) {
                    FundingReminderDialog(
                        header = header,
                        body = body,
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
