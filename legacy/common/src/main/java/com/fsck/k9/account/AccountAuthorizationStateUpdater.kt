package com.fsck.k9.account

import app.k9mail.feature.account.common.domain.entity.AuthorizationState
import app.k9mail.feature.account.edit.AccountEditExternalContract
import app.k9mail.feature.account.edit.AccountEditExternalContract.AccountUpdaterFailure
import app.k9mail.feature.account.edit.AccountEditExternalContract.AccountUpdaterResult
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import net.thunderbird.core.android.account.LegacyAccountDtoManager
import net.thunderbird.core.common.mail.Protocols
import net.thunderbird.feature.account.AccountId
import net.thunderbird.legacy.logging.Log

class AccountAuthorizationStateUpdater(
    private val accountManager: LegacyAccountDtoManager,
    private val coroutineDispatcher: CoroutineDispatcher = Dispatchers.IO,
) : AccountEditExternalContract.AccountAuthorizationStateUpdater {

    @Suppress("TooGenericExceptionCaught")
    override suspend fun updateAuthorizationState(
        accountId: AccountId,
        expectedAuthorizationState: AuthorizationState?,
        newAuthorizationState: AuthorizationState,
    ): AccountUpdaterResult {
        return try {
            withContext(coroutineDispatcher) {
                update(accountId, expectedAuthorizationState, newAuthorizationState)
            }
        } catch (error: Exception) {
            Log.e(error, "Error while updating OAuth state for account UUID %s", accountId)
            AccountUpdaterResult.Failure(AccountUpdaterFailure.UnknownError(error))
        }
    }

    @Suppress("TooGenericExceptionCaught")
    private fun update(
        accountId: AccountId,
        expectedAuthorizationState: AuthorizationState?,
        newAuthorizationState: AuthorizationState,
    ): AccountUpdaterResult {
        val account = accountManager.getById(accountId) ?: return AccountUpdaterResult.Failure(
            AccountUpdaterFailure.AccountNotFound(accountId),
        )

        return if (!Protocols.isGraphProtocol(account.incomingServerSettings.type) ||
            account.oAuthState != expectedAuthorizationState?.value
        ) {
            AccountUpdaterResult.Failure(
                AccountUpdaterFailure.UnknownError(IllegalStateException("Graph account authorization changed")),
            )
        } else {
            val previousOAuthState = account.oAuthState
            account.oAuthState = newAuthorizationState.value
            try {
                accountManager.saveAccount(account)
                AccountUpdaterResult.Success(accountId)
            } catch (error: Exception) {
                account.oAuthState = previousOAuthState
                throw error
            }
        }
    }
}
