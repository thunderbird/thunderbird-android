package app.k9mail.feature.account.edit.ui.server.settings.modify

import androidx.lifecycle.viewModelScope
import app.k9mail.feature.account.common.domain.AccountDomainContract
import app.k9mail.feature.account.common.domain.entity.InteractionMode
import app.k9mail.feature.account.edit.domain.AccountEditDomainContract
import app.k9mail.feature.account.server.settings.ui.incoming.IncomingServerSettingsContract
import app.k9mail.feature.account.server.settings.ui.incoming.IncomingServerSettingsViewModel
import app.k9mail.feature.account.server.settings.ui.incoming.toIncomingServerSettingsState
import kotlinx.coroutines.launch
import net.thunderbird.core.common.mail.Protocols
import net.thunderbird.core.common.oauth.OAuthConfigurationProvider
import net.thunderbird.feature.account.AccountId

class ModifyIncomingServerSettingsViewModel(
    val accountId: AccountId,
    private val accountStateLoader: AccountEditDomainContract.UseCase.LoadAccountState,
    validator: IncomingServerSettingsContract.Validator,
    accountStateRepository: AccountDomainContract.AccountStateRepository,
    private val oauthConfigurationProvider: OAuthConfigurationProvider,
    applicationId: String = "",
    initialState: IncomingServerSettingsContract.State = IncomingServerSettingsContract.State(),
) : IncomingServerSettingsViewModel(
    mode = InteractionMode.Edit,
    validator = validator,
    accountStateRepository = accountStateRepository,
    oauthConfigurationProvider = oauthConfigurationProvider,
    applicationId = applicationId,
    initialState = initialState,
) {

    override fun loadAccountState() {
        viewModelScope.launch {
            val state = accountStateLoader.execute(accountId)
            val isGraphReauthorizationAvailable = state.incomingServerSettings?.let { serverSettings ->
                Protocols.isGraphProtocol(serverSettings.type) &&
                    oauthConfigurationProvider.getConfiguration(serverSettings.host).isCompleteForReauthorization()
            } ?: false

            updateState {
                state.toIncomingServerSettingsState().copy(
                    isGraphReauthorizationAvailable = isGraphReauthorizationAvailable,
                )
            }
        }
    }

    private fun net.thunderbird.core.common.oauth.OAuthConfiguration?.isCompleteForReauthorization(): Boolean {
        return this != null &&
            clientId.isNotBlank() &&
            scopes.isNotEmpty() && scopes.all(String::isNotBlank) &&
            authorizationEndpoint.isNotBlank() &&
            tokenEndpoint.isNotBlank() &&
            redirectUri.isNotBlank()
    }
}
