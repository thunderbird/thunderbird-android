package app.k9mail.feature.account.edit

import app.k9mail.feature.account.common.domain.entity.AuthorizationState
import com.fsck.k9.mail.ServerSettings
import net.thunderbird.feature.account.AccountId

interface AccountEditExternalContract {

    sealed interface AccountUpdaterResult {
        data class Success(val accountId: AccountId) : AccountUpdaterResult
        data class Failure(val error: AccountUpdaterFailure) : AccountUpdaterResult
    }

    sealed interface AccountUpdaterFailure {
        data class AccountNotFound(val accountId: AccountId) : AccountUpdaterFailure
        data class UnknownError(val error: Exception) : AccountUpdaterFailure
    }

    fun interface AccountServerSettingsUpdater {
        suspend fun updateServerSettings(
            accountId: AccountId,
            isIncoming: Boolean,
            serverSettings: ServerSettings,
            authorizationState: AuthorizationState?,
        ): AccountUpdaterResult
    }
}
