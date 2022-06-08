package com.fsck.k9.ui.settings.import

import android.content.Context
import com.fsck.k9.Account
import com.fsck.k9.Core
import com.fsck.k9.Preferences
import com.fsck.k9.controller.MessagingController

/**
 * Activate account after server password(s) have been provided on settings import.
 */
class AccountActivator(
    private val context: Context,
    private val preferences: Preferences,
    private val messagingController: MessagingController
) {
    fun enableAccount(accountUuid: String, incomingServerPassword: String?, outgoingServerPassword: String?) {
        val account = preferences.getAccount(accountUuid) ?: error("Account $accountUuid not found")

        setAccountPasswords(account, incomingServerPassword, outgoingServerPassword)
        enableAccount(account)
    }

    fun enableAccount(accountUuid: String) {
        val account = preferences.getAccount(accountUuid) ?: error("Account $accountUuid not found")

        enableAccount(account)
    }

    private fun enableAccount(account: Account) {
        // Start services if necessary
        Core.setServicesEnabled(context)

        // Get list of folders from remote server
        messagingController.refreshFolderList(account)
    }

    private fun setAccountPasswords(
        account: Account,
        incomingServerPassword: String?,
        outgoingServerPassword: String?
    ) {
        if (incomingServerPassword != null) {
            account.incomingServerSettings = account.incomingServerSettings.newPassword(incomingServerPassword)
        }

        if (outgoingServerPassword != null) {
            account.outgoingServerSettings = account.outgoingServerSettings.newPassword(outgoingServerPassword)
        }

        preferences.saveAccount(account)
    }
}
