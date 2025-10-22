package net.thunderbird.feature.mail.message.composer.dialog

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.Window
import androidx.compose.ui.platform.ComposeView
import androidx.core.os.bundleOf
import androidx.fragment.app.DialogFragment
import androidx.fragment.app.FragmentManager
import androidx.fragment.app.setFragmentResult
import net.thunderbird.core.ui.theme.api.FeatureThemeProvider
import net.thunderbird.feature.mail.message.composer.dialog.SentFolderNotFoundConfirmationDialogFragmentFactory.Companion.ACCOUNT_UUID_ARG
import net.thunderbird.feature.mail.message.composer.dialog.SentFolderNotFoundConfirmationDialogFragmentFactory.Companion.RESULT_CODE_ASSIGN_SENT_FOLDER_REQUEST_KEY
import net.thunderbird.feature.mail.message.composer.dialog.SentFolderNotFoundConfirmationDialogFragmentFactory.Companion.RESULT_CODE_SEND_AND_DELETE_REQUEST_KEY
import org.koin.android.ext.android.inject

class SentFolderNotFoundConfirmationDialogFragment : DialogFragment() {
    private val themeProvider: FeatureThemeProvider by inject<FeatureThemeProvider>()
    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        val accountUuid = requireNotNull(requireArguments().getString(ACCOUNT_UUID_ARG)) {
            "The $ACCOUNT_UUID_ARG argument is missing from the arguments bundle."
        }
        dialog?.requestWindowFeature(Window.FEATURE_NO_TITLE)
        return ComposeView(requireContext()).apply {
            setContent {
                themeProvider.WithTheme {
                    SentFolderNotFoundConfirmationDialog(
                        showDialog = true,
                        onAssignSentFolderClick = {
                            dismiss()
                            setFragmentResult(
                                requestKey = RESULT_CODE_ASSIGN_SENT_FOLDER_REQUEST_KEY,
                                result = bundleOf(ACCOUNT_UUID_ARG to accountUuid),
                            )
                        },
                        onSendAndDeleteClick = {
                            dismiss()
                            setFragmentResult(
                                requestKey = RESULT_CODE_SEND_AND_DELETE_REQUEST_KEY,
                                result = bundleOf(ACCOUNT_UUID_ARG to accountUuid),
                            )
                        },
                        onDismiss = ::dismiss,
                    )
                }
            }
        }
    }

    companion object Factory : SentFolderNotFoundConfirmationDialogFragmentFactory {
        private const val TAG = "SentFolderNotFoundConfirmationDialogFragment"
        override fun show(accountUuid: String, fragmentManager: FragmentManager) {
            SentFolderNotFoundConfirmationDialogFragment().apply {
                arguments = bundleOf(ACCOUNT_UUID_ARG to accountUuid)
                show(fragmentManager, TAG)
            }
        }
    }
}

interface SentFolderNotFoundConfirmationDialogFragmentFactory {
    companion object {
        const val RESULT_CODE_ASSIGN_SENT_FOLDER_REQUEST_KEY =
            "SentFolderNotFoundConfirmationDialogFragmentFactory_assign_sent_folder"
        const val RESULT_CODE_SEND_AND_DELETE_REQUEST_KEY =
            "SentFolderNotFoundConfirmationDialogFragmentFactory_send_and_delete"
        const val ACCOUNT_UUID_ARG = "SetupArchiveFolderDialogFragmentFactory_accountUuid"
    }

    fun show(accountUuid: String, fragmentManager: FragmentManager)
}
