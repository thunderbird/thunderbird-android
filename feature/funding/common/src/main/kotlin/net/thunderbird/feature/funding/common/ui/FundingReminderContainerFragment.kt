package net.thunderbird.feature.funding.common.ui

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.Window
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.ViewCompositionStrategy
import androidx.core.os.bundleOf
import androidx.fragment.app.DialogFragment
import androidx.fragment.app.setFragmentResult
import kotlin.getValue
import net.thunderbird.core.ui.theme.api.FeatureThemeProvider
import net.thunderbird.feature.funding.common.api.FundingReminderContract
import org.koin.android.ext.android.inject

class FundingReminderContainerFragment : DialogFragment() {

    private val themeProvider: FeatureThemeProvider by inject<FeatureThemeProvider>()

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        dialog?.requestWindowFeature(Window.FEATURE_NO_TITLE)

        return ComposeView(requireContext()).apply {
            setViewCompositionStrategy(ViewCompositionStrategy.DisposeOnViewTreeLifecycleDestroyed)
            setContent {
                themeProvider.WithTheme(darkTheme = false) {
                    FundingReminderContent(
                        appNameHeader = "Thunderbird",
                        appNameBody = "Thunderbird for Android",
                        onClickDismiss = {
                            dismiss()
                        },
                        onClickOk = {
                            handlePositiveButton()
                        },
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
