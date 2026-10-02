package app.k9mail.feature.account.edit.domain.usecase

import app.k9mail.feature.account.common.domain.entity.AccountState
import app.k9mail.feature.account.common.domain.entity.AuthorizationState
import app.k9mail.feature.account.edit.AccountEditExternalContract.AccountServerSettingsUpdater
import app.k9mail.feature.account.edit.AccountEditExternalContract.AccountUpdaterResult
import app.k9mail.feature.account.edit.domain.AccountEditDomainContract.UseCase
import com.fsck.k9.mail.ServerSettings
import net.thunderbird.feature.account.AccountId

class SaveServerSettings(
    private val getAccountState: UseCase.GetAccountState,
    private val serverSettingsUpdater: AccountServerSettingsUpdater,
) : UseCase.SaveServerSettings {
    override suspend fun execute(accountId: AccountId, isIncoming: Boolean) {
        val accountState = getAccountState.execute(accountId)

        val serverSettings = accountState.getServerSettings(isIncoming)
        val authorizationState = accountState.authorizationState

        if (serverSettings != null) {
            updateServerSettings(accountId, isIncoming, serverSettings, authorizationState)
        } else {
            error("Server settings not found")
        }
    }

    private suspend fun updateServerSettings(
        accountId: AccountId,
        isIncoming: Boolean,
        serverSettings: ServerSettings,
        authorizationState: AuthorizationState?,
    ) {
        val result = serverSettingsUpdater.updateServerSettings(
            accountId = accountId,
            isIncoming = isIncoming,
            serverSettings = serverSettings,
            authorizationState = authorizationState,
        )

        if (result is AccountUpdaterResult.Failure) {
            error("Server settings update failed")
        }
    }

    private fun AccountState.getServerSettings(isIncoming: Boolean): ServerSettings? {
        return if (isIncoming) incomingServerSettings else outgoingServerSettings
    }
}
