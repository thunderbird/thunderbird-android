package app.k9mail.feature.account.setup.data

import app.k9mail.feature.account.oauth.domain.entity.AuthorizationState
import app.k9mail.feature.account.setup.domain.DomainContract
import app.k9mail.feature.account.setup.domain.entity.AccountOptions
import app.k9mail.feature.account.setup.domain.entity.AccountSetupState
import com.fsck.k9.mail.ServerSettings
import com.fsck.k9.mail.oauth.AuthStateStorage

class InMemoryAccountSetupStateRepository(
    private var state: AccountSetupState = AccountSetupState(),
) : DomainContract.AccountSetupStateRepository, AuthStateStorage {

    override fun getState(): AccountSetupState {
        return state
    }

    override fun save(accountSetupState: AccountSetupState) {
        state = accountSetupState
    }

    override fun saveEmailAddress(emailAddress: String) {
        state = state.copy(emailAddress = emailAddress)
    }

    override fun saveIncomingServerSettings(serverSettings: ServerSettings) {
        state = state.copy(incomingServerSettings = serverSettings)
    }

    override fun saveOutgoingServerSettings(serverSettings: ServerSettings) {
        state = state.copy(outgoingServerSettings = serverSettings)
    }

    override fun saveAuthorizationState(authorizationState: AuthorizationState) {
        state = state.copy(authorizationState = authorizationState)
    }

    override fun saveOptions(options: AccountOptions) {
        state = state.copy(options = options)
    }

    override fun clear() {
        state = AccountSetupState()
    }

    override fun getAuthorizationState(): String? {
        return state.authorizationState?.state
    }

    override fun updateAuthorizationState(authorizationState: String?) {
        state = state.copy(authorizationState = AuthorizationState(authorizationState))
    }
}
