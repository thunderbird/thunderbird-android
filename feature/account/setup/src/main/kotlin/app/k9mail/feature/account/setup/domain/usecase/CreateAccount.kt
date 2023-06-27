package app.k9mail.feature.account.setup.domain.usecase

import app.k9mail.feature.account.setup.domain.DomainContract.UseCase
import app.k9mail.feature.account.setup.domain.ExternalContract
import app.k9mail.feature.account.setup.domain.entity.Account
import app.k9mail.feature.account.setup.domain.entity.AccountOptions
import com.fsck.k9.mail.ServerSettings

class CreateAccount(
    private val accountCreator: ExternalContract.AccountCreator,
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

        return accountCreator.createAccount(account)
    }
}
