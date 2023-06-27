package com.fsck.k9.account

import android.content.Context
import app.k9mail.feature.account.setup.domain.ExternalContract
import app.k9mail.feature.account.setup.domain.entity.Account
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
        newAccount.senderName = account.senderName

        newAccount.incomingServerSettings = account.incomingServerSettings
        newAccount.outgoingServerSettings = account.outgoingServerSettings

        newAccount.deletePolicy = accountCreatorHelper.getDefaultDeletePolicy(newAccount.incomingServerSettings.type)
        newAccount.chipColor = accountCreatorHelper.pickColor()

        localFoldersCreator.createSpecialLocalFolders(newAccount)

        newAccount.markSetupFinished()

        preferences.saveAccount(newAccount)

        Core.setServicesEnabled(context)

        return newAccount.uuid
    }
}
