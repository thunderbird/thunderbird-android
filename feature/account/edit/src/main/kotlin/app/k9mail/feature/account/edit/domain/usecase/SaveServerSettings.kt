package app.k9mail.feature.account.edit.domain.usecase

import app.k9mail.feature.account.edit.AccountEditExternalContract.AccountServerSettingsUpdater
import app.k9mail.feature.account.edit.AccountEditExternalContract.AccountUpdaterResult
import app.k9mail.feature.account.edit.domain.AccountEditDomainContract.UseCase
import com.fsck.k9.mail.ServerSettings

class SaveServerSettings(
    private val getAccountState: UseCase.GetAccountState,
    private val serverSettingsUpdater: AccountServerSettingsUpdater,
) : UseCase.SaveServerSettings {
    override suspend fun execute(accountUuid: String, isIncoming: Boolean) {
        val serverSettings = loadServerSettings(accountUuid, isIncoming)

        if (serverSettings != null) {
            updateServerSettings(accountUuid, isIncoming, serverSettings)
        } else {
            error("Server settings not found")
        }
    }

    private suspend fun loadServerSettings(accountUuid: String, isIncoming: Boolean): ServerSettings? {
        val accountState = getAccountState.execute(accountUuid)
        return if (isIncoming) {
            accountState.incomingServerSettings
        } else {
            accountState.outgoingServerSettings
        }
    }

    private suspend fun updateServerSettings(accountUuid: String, isIncoming: Boolean, serverSettings: ServerSettings) {
        val result = serverSettingsUpdater.updateServerSettings(
            accountUuid = accountUuid,
            isIncoming = isIncoming,
            serverSettings = serverSettings,
        )

        if (result is AccountUpdaterResult.Failure) {
            error("Server settings update failed")
        }
    }
}
