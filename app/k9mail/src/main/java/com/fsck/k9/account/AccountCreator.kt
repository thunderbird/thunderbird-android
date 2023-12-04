package com.fsck.k9.account

import android.content.Context
import app.k9mail.core.common.mail.Protocols
import app.k9mail.feature.account.common.domain.entity.Account
import app.k9mail.feature.account.setup.AccountSetupExternalContract
import app.k9mail.feature.account.setup.AccountSetupExternalContract.AccountCreator.AccountCreatorResult
import com.fsck.k9.Account.FolderMode
import com.fsck.k9.Core
import com.fsck.k9.Preferences
import com.fsck.k9.logging.Timber
import com.fsck.k9.mail.ServerSettings
import com.fsck.k9.mail.store.imap.ImapStoreSettings.autoDetectNamespace
import com.fsck.k9.mail.store.imap.ImapStoreSettings.createExtra
import com.fsck.k9.mail.store.imap.ImapStoreSettings.isSendClientId
import com.fsck.k9.mail.store.imap.ImapStoreSettings.isUseCompression
import com.fsck.k9.mail.store.imap.ImapStoreSettings.pathPrefix
import com.fsck.k9.mailstore.SpecialLocalFoldersCreator
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import com.fsck.k9.Account as K9Account

// TODO Move to feature/account/setup
class AccountCreator(
    private val accountColorPicker: AccountColorPicker,
    private val localFoldersCreator: SpecialLocalFoldersCreator,
    private val preferences: Preferences,
    private val context: Context,
    private val coroutineDispatcher: CoroutineDispatcher = Dispatchers.IO,
) : AccountSetupExternalContract.AccountCreator {

    @Suppress("TooGenericExceptionCaught")
    override suspend fun createAccount(account: Account): AccountCreatorResult {
        return try {
            withContext(coroutineDispatcher) { AccountCreatorResult.Success(create(account)) }
        } catch (e: Exception) {
            Timber.e(e, "Error while creating new account")

            AccountCreatorResult.Error(e.message ?: "Unknown create account error")
        }
    }

    private fun create(account: Account): String {
        val newAccount = preferences.newAccount(account.uuid)

        newAccount.email = account.emailAddress

        newAccount.setIncomingServerSettings(account.incomingServerSettings)
        newAccount.outgoingServerSettings = account.outgoingServerSettings

        newAccount.oAuthState = account.authorizationState

        newAccount.name = account.options.accountName
        newAccount.senderName = account.options.displayName
        if (account.options.emailSignature != null) {
            newAccount.signatureUse = true
            newAccount.signature = account.options.emailSignature
        }
        newAccount.isNotifyNewMail = account.options.showNotification
        newAccount.automaticCheckIntervalMinutes = account.options.checkFrequencyInMinutes
        newAccount.displayCount = account.options.messageDisplayCount

        newAccount.folderPushMode = FolderMode.NONE
        newAccount.deletePolicy = DeletePolicyHelper.getDefaultDeletePolicy(newAccount.incomingServerSettings.type)
        newAccount.chipColor = accountColorPicker.pickColor()

        localFoldersCreator.createSpecialLocalFolders(newAccount)

        newAccount.markSetupFinished()

        preferences.saveAccount(newAccount)

        Core.setServicesEnabled(context)

        return newAccount.uuid
    }
}

private fun K9Account.setIncomingServerSettings(serverSettings: ServerSettings) {
    if (serverSettings.type == Protocols.IMAP) {
        useCompression = serverSettings.isUseCompression
        isSendClientIdEnabled = serverSettings.isSendClientId
        incomingServerSettings = serverSettings.copy(
            extra = createExtra(
                autoDetectNamespace = serverSettings.autoDetectNamespace,
                pathPrefix = serverSettings.pathPrefix,
            ),
        )
    } else {
        incomingServerSettings = serverSettings
    }
}
