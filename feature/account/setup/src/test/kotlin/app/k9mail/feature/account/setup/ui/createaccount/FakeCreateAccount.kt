package app.k9mail.feature.account.setup.ui.createaccount

import app.k9mail.feature.account.common.domain.entity.AccountOptions
import app.k9mail.feature.account.common.domain.entity.SpecialFolderSettings
import app.k9mail.feature.account.setup.AccountSetupExternalContract.AccountCreator.AccountCreatorResult
import app.k9mail.feature.account.setup.domain.DomainContract.UseCase.CreateAccount
import com.fsck.k9.mail.ServerSettings

class FakeCreateAccount : CreateAccount {
    val recordedInvocations = mutableListOf<CreateAccountArguments>()

    var result: AccountCreatorResult = AccountCreatorResult.Success("default result")

    override suspend fun execute(
        emailAddress: String,
        incomingServerSettings: ServerSettings,
        outgoingServerSettings: ServerSettings,
        authorizationState: String?,
        specialFolderSettings: SpecialFolderSettings?,
        options: AccountOptions,
    ): AccountCreatorResult {
        recordedInvocations.add(
            CreateAccountArguments(
                emailAddress,
                incomingServerSettings,
                outgoingServerSettings,
                authorizationState,
                specialFolderSettings,
                options,
            ),
        )

        return result
    }
}

data class CreateAccountArguments(
    val emailAddress: String,
    val incomingServerSettings: ServerSettings,
    val outgoingServerSettings: ServerSettings,
    val authorizationState: String?,
    val specialFolderSettings: SpecialFolderSettings?,
    val options: AccountOptions,
)
