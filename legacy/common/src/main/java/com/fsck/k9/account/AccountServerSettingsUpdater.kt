package com.fsck.k9.account

import app.k9mail.feature.account.common.domain.entity.AuthorizationState
import app.k9mail.feature.account.edit.AccountEditExternalContract
import app.k9mail.feature.account.edit.AccountEditExternalContract.AccountUpdaterFailure
import app.k9mail.feature.account.edit.AccountEditExternalContract.AccountUpdaterResult
import com.fsck.k9.mail.ServerSettings
import com.fsck.k9.mail.store.imap.ImapStoreSettings
import com.fsck.k9.mail.store.imap.ImapStoreSettings.autoDetectNamespace
import com.fsck.k9.mail.store.imap.ImapStoreSettings.isSendClientInfo
import com.fsck.k9.mail.store.imap.ImapStoreSettings.isUseCompression
import com.fsck.k9.mail.store.imap.ImapStoreSettings.pathPrefix
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import net.thunderbird.core.android.account.LegacyAccountManager
import net.thunderbird.core.common.mail.Protocols
import net.thunderbird.feature.account.AccountId
import net.thunderbird.legacy.logging.Log

class AccountServerSettingsUpdater(
    private val accountManager: LegacyAccountManager,
    private val coroutineDispatcher: CoroutineDispatcher = Dispatchers.IO,
) : AccountEditExternalContract.AccountServerSettingsUpdater {

    @Suppress("TooGenericExceptionCaught")
    override suspend fun updateServerSettings(
        accountId: AccountId,
        isIncoming: Boolean,
        serverSettings: ServerSettings,
        authorizationState: AuthorizationState?,
    ): AccountUpdaterResult {
        return try {
            withContext(coroutineDispatcher) {
                updateSettings(accountId, isIncoming, serverSettings, authorizationState)
            }
        } catch (error: Exception) {
            Log.e(error, "Error while updating account server settings with UUID %s", accountId)

            AccountUpdaterResult.Failure(AccountUpdaterFailure.UnknownError(error))
        }
    }

    private fun updateSettings(
        accountId: AccountId,
        isIncoming: Boolean,
        serverSettings: ServerSettings,
        authorizationState: AuthorizationState?,
    ): AccountUpdaterResult {
        val account = accountManager.findById(accountId) ?: return AccountUpdaterResult.Failure(
            AccountUpdaterFailure.AccountNotFound(accountId),
        )

        val updatedAccount = if (isIncoming) {
            if (serverSettings.type == Protocols.IMAP) {
                account.copy(
                    useCompression = serverSettings.isUseCompression,
                    isSendClientInfoEnabled = serverSettings.isSendClientInfo,
                    incomingServerSettings = serverSettings.copy(
                        extra = ImapStoreSettings.createExtra(
                            autoDetectNamespace = serverSettings.autoDetectNamespace,
                            pathPrefix = serverSettings.pathPrefix,
                        ),
                    ),
                    oAuthState = authorizationState?.value ?: account.oAuthState,
                )
            } else {
                account.copy(
                    incomingServerSettings = serverSettings,
                    oAuthState = authorizationState?.value ?: account.oAuthState,
                )
            }
        } else {
            account.copy(
                outgoingServerSettings = serverSettings,
                oAuthState = authorizationState?.value ?: account.oAuthState,
            )
        }

        accountManager.updateSync(updatedAccount)

        return AccountUpdaterResult.Success(accountId)
    }
}
