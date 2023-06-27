package app.k9mail.feature.account.setup.domain.usecase

import app.k9mail.feature.account.setup.AccountSetupExternalContract.AccountCreator
import app.k9mail.feature.account.setup.AccountSetupExternalContract.AccountCreator.AccountCreatorResult
import app.k9mail.feature.account.setup.domain.DomainContract.UseCase
import app.k9mail.feature.account.setup.domain.entity.Account
import app.k9mail.feature.account.setup.domain.entity.AccountOptions
import com.fsck.k9.mail.ServerSettings

class CreateAccount(
    private val accountCreator: AccountCreator,
) : UseCase.CreateAccount {
    override suspend fun execute(
        emailAddress: String,
        incomingServerSettings: ServerSettings,
        outgoingServerSettings: ServerSettings,
        options: AccountOptions,
    ): String {
        val account = Account(
            emailAddress = emailAddress,
            incomingServerSettings = incomingServerSettings,
            outgoingServerSettings = outgoingServerSettings,
            options = options,
        )

        return when (val result = accountCreator.createAccount(account)) {
            is AccountCreatorResult.Success -> result.accountUuid
            is AccountCreatorResult.Error -> "" // TODO change to meaningful error
        }
    }
}
