package com.fsck.k9.account

import android.content.Context
import app.k9mail.core.common.mail.Protocols
import app.k9mail.feature.account.common.domain.entity.Account
import app.k9mail.feature.account.common.domain.entity.SpecialFolderOption
import app.k9mail.feature.account.common.domain.entity.SpecialFolderSettings
import app.k9mail.feature.account.setup.AccountSetupExternalContract
import app.k9mail.feature.account.setup.AccountSetupExternalContract.AccountCreator.AccountCreatorResult
import app.k9mail.legacy.account.Account.SpecialFolderSelection
import com.fsck.k9.Core
import com.fsck.k9.K9
import com.fsck.k9.Preferences
import com.fsck.k9.controller.MessagingController
import com.fsck.k9.logging.Timber
import com.fsck.k9.mail.ServerSettings
import com.fsck.k9.mail.store.imap.ImapStoreSettings.autoDetectNamespace
import com.fsck.k9.mail.store.imap.ImapStoreSettings.createExtra
import com.fsck.k9.mail.store.imap.ImapStoreSettings.isSendClientInfo
import com.fsck.k9.mail.store.imap.ImapStoreSettings.isUseCompression
import com.fsck.k9.mail.store.imap.ImapStoreSettings.pathPrefix
import com.fsck.k9.mailstore.SpecialFolderUpdater
import com.fsck.k9.mailstore.SpecialLocalFoldersCreator
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import app.k9mail.legacy.account.Account as K9Account

// TODO Move to feature/account/setup
class AccountCreator(
    private val accountColorPicker: AccountColorPicker,
    private val localFoldersCreator: SpecialLocalFoldersCreator,
    private val preferences: Preferences,
    private val context: Context,
    private val messagingController: MessagingController,
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

        newAccount.deletePolicy = DeletePolicyHelper.getDefaultDeletePolicy(newAccount.incomingServerSettings.type)
        newAccount.chipColor = accountColorPicker.pickColor()

        localFoldersCreator.createSpecialLocalFolders(newAccount)

        account.specialFolderSettings?.let { specialFolderSettings ->
            newAccount.setSpecialFolders(specialFolderSettings)
        }

        newAccount.markSetupFinished()

        preferences.saveAccount(newAccount)

        if (preferences.getAccounts().size > 1) {
            K9.isShowUnifiedInbox = true
            K9.saveSettingsAsync()
        }

        Core.setServicesEnabled(context)

        messagingController.refreshFolderListBlocking(newAccount)

        if (account.options.checkFrequencyInMinutes == -1) {
            messagingController.checkMail(newAccount, false, true, false, null)
        }

        return newAccount.uuid
    }

    /**
     * Set special folders by name.
     *
     * Since the folder list hasn't been synced yet, we don't have database IDs for the folders. So we use the same
     * mechanism that is used when importing settings. See [SpecialFolderUpdater] for details.
     */
    private fun K9Account.setSpecialFolders(specialFolders: SpecialFolderSettings) {
        importedArchiveFolder = specialFolders.archiveSpecialFolderOption.toFolderServerId()
        archiveFolderSelection = specialFolders.archiveSpecialFolderOption.toFolderSelection()

        importedDraftsFolder = specialFolders.draftsSpecialFolderOption.toFolderServerId()
        draftsFolderSelection = specialFolders.draftsSpecialFolderOption.toFolderSelection()

        importedSentFolder = specialFolders.sentSpecialFolderOption.toFolderServerId()
        sentFolderSelection = specialFolders.sentSpecialFolderOption.toFolderSelection()

        importedSpamFolder = specialFolders.spamSpecialFolderOption.toFolderServerId()
        spamFolderSelection = specialFolders.spamSpecialFolderOption.toFolderSelection()

        importedTrashFolder = specialFolders.trashSpecialFolderOption.toFolderServerId()
        trashFolderSelection = specialFolders.trashSpecialFolderOption.toFolderSelection()
    }

    private fun SpecialFolderOption.toFolderServerId(): String? {
        return when (this) {
            is SpecialFolderOption.None -> null
            is SpecialFolderOption.Regular -> remoteFolder.serverId.serverId
            is SpecialFolderOption.Special -> remoteFolder.serverId.serverId
        }
    }

    private fun SpecialFolderOption.toFolderSelection(): SpecialFolderSelection {
        return when (this) {
            is SpecialFolderOption.None -> {
                if (isAutomatic) SpecialFolderSelection.AUTOMATIC else SpecialFolderSelection.MANUAL
            }
            is SpecialFolderOption.Regular -> {
                SpecialFolderSelection.MANUAL
            }
            is SpecialFolderOption.Special -> {
                if (isAutomatic) SpecialFolderSelection.AUTOMATIC else SpecialFolderSelection.MANUAL
            }
        }
    }
}

private fun K9Account.setIncomingServerSettings(serverSettings: ServerSettings) {
    if (serverSettings.type == Protocols.IMAP) {
        useCompression = serverSettings.isUseCompression
        isSendClientInfoEnabled = serverSettings.isSendClientInfo
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
