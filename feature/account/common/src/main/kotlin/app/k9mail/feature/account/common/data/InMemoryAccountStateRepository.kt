package app.k9mail.feature.account.common.data

import app.k9mail.feature.account.common.domain.AccountDomainContract
import app.k9mail.feature.account.common.domain.entity.AccountOptions
import app.k9mail.feature.account.common.domain.entity.AccountState
import app.k9mail.feature.account.common.domain.entity.AuthorizationState
import com.fsck.k9.mail.ServerSettings
import com.fsck.k9.mail.oauth.AuthStateStorage

class InMemoryAccountStateRepository(
    private var state: AccountState = AccountState(),
) : AccountDomainContract.AccountStateRepository, AuthStateStorage {

    override fun getState(): AccountState {
        return state
    }

    override fun save(accountState: AccountState) {
        state = accountState
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
        state = AccountState()
    }

    override fun getAuthorizationState(): String? {
        return state.authorizationState?.state
    }

    override fun updateAuthorizationState(authorizationState: String?) {
        state = state.copy(authorizationState = AuthorizationState(authorizationState))
    }
}
