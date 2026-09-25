package net.thunderbird.feature.funding.common.ui

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.Window
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.ViewCompositionStrategy
import androidx.compose.ui.res.stringResource
import androidx.core.os.bundleOf
import androidx.fragment.app.DialogFragment
import androidx.fragment.app.setFragmentResult
import kotlin.getValue
import net.thunderbird.core.common.provider.AppNameProvider
import net.thunderbird.core.ui.theme.api.FeatureThemeProvider
import net.thunderbird.feature.funding.common.R
import net.thunderbird.feature.funding.common.api.FundingReminderContract
import org.koin.android.ext.android.inject

class FundingReminderContainerFragment : DialogFragment() {

    private val themeProvider: FeatureThemeProvider by inject<FeatureThemeProvider>()
    private val appNameProvider: AppNameProvider by inject<AppNameProvider>()

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        dialog?.requestWindowFeature(Window.FEATURE_NO_TITLE)

        return ComposeView(requireContext()).apply {
            setViewCompositionStrategy(ViewCompositionStrategy.DisposeOnViewTreeLifecycleDestroyed)
            setContent {
                themeProvider.WithTheme(darkTheme = false) {
                    FundingReminderDialog(
                        appNameHeader = appNameProvider.appName,
                        appNameBody = if (appNameProvider.appName.lowercase().contains("thunderbird")) {
                            stringResource(R.string.funding_reminder_thunderbird_for_android)
                        } else {
                            appNameProvider.appName
                        },
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
            bundleOf(FundingReminderContract.Dialog.FRAGMENT_RESULT_SHOW_FUNDING to true),
        )
    }
}
