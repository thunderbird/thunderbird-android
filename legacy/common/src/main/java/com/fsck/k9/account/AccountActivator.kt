package com.fsck.k9.account

import android.content.Context
import app.k9mail.feature.settings.import.SettingsImportExternalContract
import com.fsck.k9.Core
import com.fsck.k9.controller.MessagingController
import net.thunderbird.core.android.account.LegacyAccount
import net.thunderbird.core.android.account.LegacyAccountManager
import net.thunderbird.feature.account.AccountId

/**
 * Activate account after server password(s) have been provided on settings import.
 */
class AccountActivator(
    private val context: Context,
    private val accountManager: LegacyAccountManager,
    private val messagingController: MessagingController,
) : SettingsImportExternalContract.AccountActivator {
    override fun enableAccount(accountId: AccountId, incomingServerPassword: String?, outgoingServerPassword: String?) {
        val account = accountManager.findById(accountId) ?: error("Account $accountId not found")

        setAccountPasswords(account, incomingServerPassword, outgoingServerPassword)
        enableAccount(accountId)
    }

    override fun enableAccount(accountId: AccountId) {
        // Start services if necessary
        Core.setServicesEnabled(context)

        // Get list of folders from remote server
        messagingController.refreshFolderList(accountId)
    }

    private fun setAccountPasswords(
        account: LegacyAccount,
        incomingServerPassword: String?,
        outgoingServerPassword: String?,
    ) {
        var updatedAccount = account

        if (incomingServerPassword != null) {
            val newIncoming = updatedAccount.incomingServerSettings.newPassword(incomingServerPassword)
            updatedAccount = updatedAccount.copy(incomingServerSettings = newIncoming)
        }

        if (outgoingServerPassword != null) {
            val newOutgoing = updatedAccount.outgoingServerSettings.newPassword(outgoingServerPassword)
            updatedAccount = updatedAccount.copy(outgoingServerSettings = newOutgoing)
        }

        accountManager.updateSync(updatedAccount)
    }
}
