package net.thunderbird.feature.account.settings.impl.domain.usecase

import kotlinx.coroutines.flow.firstOrNull
import net.thunderbird.components.core.outcome.Outcome
import net.thunderbird.core.android.account.LegacyAccountRepository
import net.thunderbird.core.common.mail.NativeApiMailServerSettings
import net.thunderbird.core.common.mail.Protocols
import net.thunderbird.feature.account.AccountId
import net.thunderbird.feature.account.settings.impl.domain.AccountSettingsDomainContract
import net.thunderbird.feature.account.settings.impl.domain.AccountSettingsDomainContract.AccountSettingError
import net.thunderbird.feature.account.settings.impl.domain.AccountSettingsDomainContract.UseCase

internal class UpdateApiClientId(
    private val repository: LegacyAccountRepository,
) : UseCase.UpdateApiClientId {

    override suspend fun invoke(accountId: AccountId, clientId: String): Outcome<Unit, AccountSettingError> =
        if (clientId.isBlank()) {
            Outcome.failure(AccountSettingError.UnsupportedFormat("Client ID cannot be empty"))
        } else {
            updateClientId(accountId, clientId)
        }

    private suspend fun updateClientId(accountId: AccountId, clientId: String): Outcome<Unit, AccountSettingError> =
        repository.observeById(accountId).firstOrNull()?.let { account ->
            val serverSettings = account.incomingServerSettings
            when {
                serverSettings.type != Protocols.GMAIL_API && !Protocols.isGraphProtocol(serverSettings.type) ->
                    Outcome.failure(
                        AccountSettingError.UnsupportedFormat(
                            "Client ID can only be changed for Gmail and Graph accounts",
                        ),
                    )

                else -> {
                    repository.update(
                        account.copy(
                            incomingServerSettings = serverSettings.copy(
                                extra = serverSettings.extra +
                                    (NativeApiMailServerSettings.EXTRA_OAUTH_CLIENT_ID to clientId.trim()),
                            ),
                        ),
                    )
                    Outcome.success(Unit)
                }
            }
        } ?: Outcome.failure(AccountSettingError.NotFound("Account not found"))
}
