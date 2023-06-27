package com.fsck.k9.account

import android.content.Context
import app.k9mail.feature.account.setup.domain.ExternalContract
import app.k9mail.feature.account.setup.domain.entity.Account
import com.fsck.k9.Account.FolderMode
import com.fsck.k9.Core
import com.fsck.k9.Preferences
import com.fsck.k9.mailstore.SpecialLocalFoldersCreator

class AccountCreator(
    private val accountCreatorHelper: AccountCreatorHelper,
    private val localFoldersCreator: SpecialLocalFoldersCreator,
    private val preferences: Preferences,
    private val context: Context,
) : ExternalContract.AccountCreator {

    override suspend fun createAccount(account: Account): String {
        val newAccount = preferences.newAccount()

        newAccount.email = account.emailAddress

        newAccount.incomingServerSettings = account.incomingServerSettings
        newAccount.outgoingServerSettings = account.outgoingServerSettings

        newAccount.name = account.options.displayName
        newAccount.senderName = account.options.accountName
        if (account.options.emailSignature != null) {
            newAccount.signature = account.options.emailSignature
        }
        newAccount.isNotifyNewMail = account.options.showNotification
        newAccount.automaticCheckIntervalMinutes = account.options.checkFrequencyInMinutes
        newAccount.displayCount = account.options.messageDisplayCount

        newAccount.folderPushMode = FolderMode.ALL // TODO is this always ALL?
        newAccount.deletePolicy = accountCreatorHelper.getDefaultDeletePolicy(newAccount.incomingServerSettings.type)
        newAccount.chipColor = accountCreatorHelper.pickColor()

        // TODO check this:
        // DI.get(LocalKeyStoreManager.class)
        //      .deleteCertificate(mAccount, newHost, newPort, MailServerDirection.OUTGOING)

        localFoldersCreator.createSpecialLocalFolders(newAccount)

        newAccount.markSetupFinished()

        preferences.saveAccount(newAccount)

        Core.setServicesEnabled(context)

        return newAccount.uuid
    }
}
