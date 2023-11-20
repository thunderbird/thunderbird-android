package app.k9mail.feature.account.edit

import app.k9mail.feature.account.common.domain.entity.AuthorizationState
import com.fsck.k9.mail.ServerSettings

interface AccountEditExternalContract {

    sealed interface AccountUpdaterResult {
        data class Success(val accountUuid: String) : AccountUpdaterResult
        data class Failure(val error: AccountUpdaterFailure) : AccountUpdaterResult
    }

    sealed interface AccountUpdaterFailure {
        data class AccountNotFound(val accountUuid: String) : AccountUpdaterFailure
        data class UnknownError(val error: Exception) : AccountUpdaterFailure
    }

    fun interface AccountServerSettingsUpdater {
        suspend fun updateServerSettings(
            accountUuid: String,
            isIncoming: Boolean,
            serverSettings: ServerSettings,
            authorizationState: AuthorizationState?,
        ): AccountUpdaterResult
    }
}
