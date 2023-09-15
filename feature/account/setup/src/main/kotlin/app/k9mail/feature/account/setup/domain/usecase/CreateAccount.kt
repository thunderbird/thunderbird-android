package app.k9mail.feature.account.setup.domain.usecase

import app.k9mail.feature.account.common.domain.entity.Account
import app.k9mail.feature.account.common.domain.entity.AccountOptions
import app.k9mail.feature.account.setup.AccountSetupExternalContract.AccountCreator
import app.k9mail.feature.account.setup.AccountSetupExternalContract.AccountCreator.AccountCreatorResult
import app.k9mail.feature.account.setup.domain.DomainContract.UseCase
import com.fsck.k9.mail.ServerSettings
import java.util.UUID

class CreateAccount(
    private val accountCreator: AccountCreator,
    private val uuidGenerator: () -> String = { UUID.randomUUID().toString() },
) : UseCase.CreateAccount {
    override suspend fun execute(
        emailAddress: String,
        incomingServerSettings: ServerSettings,
        outgoingServerSettings: ServerSettings,
        authorizationState: String?,
        options: AccountOptions,
    ): String {
        val account = Account(
            uuid = uuidGenerator(),
            emailAddress = emailAddress,
            incomingServerSettings = incomingServerSettings,
            outgoingServerSettings = outgoingServerSettings,
            authorizationState = authorizationState,
            options = options,
        )

        return when (val result = accountCreator.createAccount(account)) {
            is AccountCreatorResult.Success -> result.accountUuid
            is AccountCreatorResult.Error -> "" // TODO change to meaningful error
        }
    }
}
