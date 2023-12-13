package app.k9mail.feature.account.common.data

import app.k9mail.feature.account.common.domain.AccountDomainContract
import app.k9mail.feature.account.common.domain.entity.AccountOptions
import app.k9mail.feature.account.common.domain.entity.AccountState
import app.k9mail.feature.account.common.domain.entity.AuthorizationState
import app.k9mail.feature.account.common.domain.entity.SpecialFolderSettings
import com.fsck.k9.mail.ServerSettings
import com.fsck.k9.mail.oauth.AuthStateStorage

@Suppress("TooManyFunctions")
class InMemoryAccountStateRepository(
    private var state: AccountState = AccountState(),
) : AccountDomainContract.AccountStateRepository, AuthStateStorage {

    override fun getState(): AccountState {
        return state
    }

    override fun setState(accountState: AccountState) {
        state = accountState
    }

    override fun setEmailAddress(emailAddress: String) {
        state = state.copy(emailAddress = emailAddress)
    }

    override fun setIncomingServerSettings(serverSettings: ServerSettings) {
        state = state.copy(incomingServerSettings = serverSettings)
    }

    override fun setOutgoingServerSettings(serverSettings: ServerSettings) {
        state = state.copy(outgoingServerSettings = serverSettings)
    }

    override fun setAuthorizationState(authorizationState: AuthorizationState) {
        state = state.copy(authorizationState = authorizationState)
    }

    override fun setSpecialFolderSettings(specialFolderSettings: SpecialFolderSettings) {
        state = state.copy(specialFolderSettings = specialFolderSettings)
    }

    override fun setOptions(options: AccountOptions) {
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
