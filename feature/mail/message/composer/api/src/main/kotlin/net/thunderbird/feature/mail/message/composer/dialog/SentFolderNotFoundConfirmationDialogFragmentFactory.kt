package net.thunderbird.feature.mail.message.composer.dialog

import androidx.fragment.app.FragmentManager

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
