package app.k9mail.feature.account.setup.domain.usecase

import app.k9mail.feature.account.common.domain.entity.Account
import app.k9mail.feature.account.common.domain.entity.AccountOptions
import app.k9mail.feature.account.common.domain.entity.SpecialFolderSettings
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
        specialFolderSettings: SpecialFolderSettings?,
        options: AccountOptions,
    ): AccountCreatorResult {
        val account = Account(
            uuid = uuidGenerator(),
            emailAddress = emailAddress,
            incomingServerSettings = incomingServerSettings,
            outgoingServerSettings = outgoingServerSettings,
            authorizationState = authorizationState,
            specialFolderSettings = specialFolderSettings,
            options = options,
        )

        return accountCreator.createAccount(account)
    }
}
