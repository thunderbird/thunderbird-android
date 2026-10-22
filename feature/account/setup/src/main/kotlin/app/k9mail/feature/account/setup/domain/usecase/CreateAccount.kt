package app.k9mail.feature.account.setup.domain.usecase

import app.k9mail.feature.account.common.domain.entity.Account
import app.k9mail.feature.account.common.domain.entity.AccountDisplayOptions
import app.k9mail.feature.account.common.domain.entity.AccountOptions
import app.k9mail.feature.account.common.domain.entity.AccountState
import app.k9mail.feature.account.common.domain.entity.AccountSyncOptions
import app.k9mail.feature.account.setup.AccountSetupExternalContract.AccountCreator
import app.k9mail.feature.account.setup.AccountSetupExternalContract.AccountCreator.AccountCreatorResult
import app.k9mail.feature.account.setup.domain.DomainContract.UseCase
import java.util.UUID
import net.thunderbird.feature.account.AccountId
import net.thunderbird.feature.account.AccountIdFactory

class CreateAccount(
    private val accountCreator: AccountCreator,
    private val uuidGenerator: () -> AccountId = { AccountIdFactory.create() },
) : UseCase.CreateAccount {
    override suspend fun execute(accountState: AccountState): AccountCreatorResult {
        val emailAddress = checkNotNull(accountState.emailAddress) { "Missing email address" }
        val incomingServerSettings = checkNotNull(accountState.incomingServerSettings) {
            "Missing incoming server settings"
        }
        val outgoingServerSettings = checkNotNull(accountState.outgoingServerSettings) {
            "Missing outgoing server settings"
        }
        val displayOptions = checkNotNull(accountState.displayOptions) { "Missing display options" }
        val syncOptions = checkNotNull(accountState.syncOptions) { "Missing sync options" }

        val account = Account(
            id = uuidGenerator(),
            emailAddress = emailAddress,
            incomingServerSettings = incomingServerSettings.copy(),
            outgoingServerSettings = outgoingServerSettings.copy(),
            authorizationState = accountState.authorizationState?.value,
            specialFolderSettings = accountState.specialFolderSettings,
            options = mapOptions(displayOptions, syncOptions),
        )

        return accountCreator.createAccount(account)
    }

    private fun mapOptions(
        displayOptions: AccountDisplayOptions,
        syncOptions: AccountSyncOptions,
    ): AccountOptions {
        return AccountOptions(
            accountName = displayOptions.accountName,
            displayName = displayOptions.displayName,
            emailSignature = displayOptions.emailSignature,
            checkFrequencyInMinutes = syncOptions.checkFrequencyInMinutes,
            messageDisplayCount = syncOptions.messageDisplayCount,
            showNotification = syncOptions.showNotification,
        )
    }
}
