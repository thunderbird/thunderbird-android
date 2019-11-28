package com.fsck.k9.ui.settings.import

import android.content.Context
import com.fsck.k9.Account
import com.fsck.k9.Core
import com.fsck.k9.Preferences
import com.fsck.k9.backend.BackendManager
import com.fsck.k9.controller.MessagingController

/**
 * Activate account after server password(s) have been provided on settings import.
 */
class AccountActivator(
        private val context: Context,
        private val preferences: Preferences,
        private val messagingController: MessagingController,
        private val backendManager: BackendManager
) {
    fun enableAccount(accountUuid: String, incomingServerPassword: String?, outgoingServerPassword: String?) {
        val account = preferences.getAccount(accountUuid)

        setAccountPasswords(account, incomingServerPassword, outgoingServerPassword)

        // Start services if necessary
        Core.setServicesEnabled(context)

        // Get list of folders from remote server
        messagingController.listFolders(account, true, null)
    }

    private fun setAccountPasswords(
            account: Account,
            incomingServerPassword: String?,
            outgoingServerPassword: String?
    ) {
        if (incomingServerPassword != null) {
            val incomingServerSettings = backendManager.decodeStoreUri(account.storeUri)
            val newIncomingServerSettings = incomingServerSettings.newPassword(incomingServerPassword)
            account.storeUri = backendManager.createStoreUri(newIncomingServerSettings)
        }

        if (outgoingServerPassword != null) {
            val outgoingServerSettings = backendManager.decodeTransportUri(account.transportUri)
            val newOutgoingServerSettings = outgoingServerSettings.newPassword(outgoingServerPassword)
            account.transportUri = backendManager.createTransportUri(newOutgoingServerSettings)
        }

        account.isEnabled = true

        preferences.saveAccount(account)
    }
}
