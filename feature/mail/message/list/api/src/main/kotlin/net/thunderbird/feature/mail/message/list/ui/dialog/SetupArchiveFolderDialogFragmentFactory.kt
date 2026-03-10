package net.thunderbird.feature.mail.message.list.ui.dialog

import androidx.fragment.app.FragmentManager

interface SetupArchiveFolderDialogFragmentFactory {
    companion object {
        const val RESULT_CODE_DISMISS_REQUEST_KEY = "SetupArchiveFolderDialogFragmentFactory_dialog_dismiss"
    }

    fun show(accountUuid: String, fragmentManager: FragmentManager)
}
