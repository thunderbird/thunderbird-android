package com.fsck.k9.account

import android.content.Context
import app.k9mail.feature.account.setup.AccountSetupExternalContract
import app.k9mail.feature.account.setup.AccountSetupExternalContract.AccountCreator.AccountCreatorResult
import app.k9mail.feature.account.setup.domain.entity.Account
import com.fsck.k9.Account.FolderMode
import com.fsck.k9.Core
import com.fsck.k9.Preferences
import com.fsck.k9.mailstore.SpecialLocalFoldersCreator
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class AccountCreator(
    private val accountCreatorHelper: AccountCreatorHelper,
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
            AccountCreatorResult.Error(e.message ?: "Unknown create account error")
        }
    }

    private fun create(account: Account): String {
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

        newAccount.folderPushMode = FolderMode.ALL
        newAccount.deletePolicy = accountCreatorHelper.getDefaultDeletePolicy(
            newAccount.incomingServerSettings.type,
        )
        newAccount.chipColor = accountCreatorHelper.pickColor()

        localFoldersCreator.createSpecialLocalFolders(newAccount)

        newAccount.markSetupFinished()

        preferences.saveAccount(newAccount)

        Core.setServicesEnabled(context)

        return newAccount.uuid
    }
}
