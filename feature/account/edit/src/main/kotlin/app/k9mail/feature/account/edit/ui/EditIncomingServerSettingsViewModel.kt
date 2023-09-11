package app.k9mail.feature.account.edit.ui

import androidx.lifecycle.viewModelScope
import app.k9mail.feature.account.common.domain.AccountDomainContract
import app.k9mail.feature.account.common.domain.entity.InteractionMode
import app.k9mail.feature.account.edit.domain.AccountEditDomainContract
import app.k9mail.feature.account.server.settings.ui.incoming.IncomingServerSettingsContract
import app.k9mail.feature.account.server.settings.ui.incoming.IncomingServerSettingsViewModel
import app.k9mail.feature.account.server.settings.ui.incoming.toIncomingConfigState
import kotlinx.coroutines.launch

class EditIncomingServerSettingsViewModel(
    val accountUuid: String,
    private val accountStateLoader: AccountEditDomainContract.UseCase.LoadAccountState,
    validator: IncomingServerSettingsContract.Validator,
    accountStateRepository: AccountDomainContract.AccountStateRepository,
    initialState: IncomingServerSettingsContract.State = IncomingServerSettingsContract.State(),
) : IncomingServerSettingsViewModel(
    mode = InteractionMode.Edit,
    validator = validator,
    accountStateRepository = accountStateRepository,
    initialState = initialState,
) {

    override fun loadAccountState() {
        viewModelScope.launch {
            val state = accountStateLoader.execute(accountUuid)

            updateState {
                state.toIncomingConfigState()
            }
        }
    }
}
