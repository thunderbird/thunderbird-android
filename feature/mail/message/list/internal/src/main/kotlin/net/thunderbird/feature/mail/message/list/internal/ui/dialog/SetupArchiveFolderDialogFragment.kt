package net.thunderbird.feature.mail.message.list.internal.ui.dialog

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.Window
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.ViewCompositionStrategy
import androidx.core.os.bundleOf
import androidx.fragment.app.DialogFragment
import androidx.fragment.app.FragmentManager
import androidx.fragment.app.setFragmentResult
import net.thunderbird.core.ui.theme.api.FeatureThemeProvider
import net.thunderbird.feature.mail.message.list.ui.dialog.SetupArchiveFolderDialogFragmentFactory
import org.koin.android.ext.android.inject

internal class SetupArchiveFolderDialogFragment : DialogFragment() {
    private val themeProvider: FeatureThemeProvider by inject<FeatureThemeProvider>()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View? {
        val accountUuid = requireNotNull(requireArguments().getString(ACCOUNT_UUID_ARG)) {
            "The $ACCOUNT_UUID_ARG argument is missing from the arguments bundle."
        }
        dialog?.requestWindowFeature(Window.FEATURE_NO_TITLE)
        return ComposeView(requireContext()).apply {
            setViewCompositionStrategy(ViewCompositionStrategy.DisposeOnViewTreeLifecycleDestroyed)
            setContent {
                themeProvider.WithTheme {
                    SetupArchiveFolderDialog(
                        accountUuid = accountUuid,
                        onDismissDialog = {
                            dismiss()
                            setFragmentResult(
                                SetupArchiveFolderDialogFragmentFactory.RESULT_CODE_DISMISS_REQUEST_KEY,
                                Bundle.EMPTY,
                            )
                        },
                    )
                }
            }
        }
    }

    companion object Factory : SetupArchiveFolderDialogFragmentFactory {
        private const val TAG = "SetupArchiveFolderDialogFragmentFactory"
        private const val ACCOUNT_UUID_ARG = "SetupArchiveFolderDialogFragmentFactory_accountUuid"

        override fun show(accountUuid: String, fragmentManager: FragmentManager) {
            SetupArchiveFolderDialogFragment()
                .apply {
                    arguments = bundleOf(ACCOUNT_UUID_ARG to accountUuid)
                }
                .show(fragmentManager, TAG)
        }
    }
}
