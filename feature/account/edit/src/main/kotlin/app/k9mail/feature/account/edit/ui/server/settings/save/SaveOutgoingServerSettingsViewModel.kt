package app.k9mail.feature.account.edit.ui.server.settings.save

import app.k9mail.feature.account.edit.domain.AccountEditDomainContract.UseCase
import app.k9mail.feature.account.edit.ui.server.settings.save.SaveServerSettingsContract.State

class SaveOutgoingServerSettingsViewModel(
    accountUuid: String,
    saveServerSettings: UseCase.SaveServerSettings,
    initialState: State = State(),
) : BaseSaveServerSettingsViewModel(
    accountUuid = accountUuid,
    isIncoming = false,
    saveServerSettings = saveServerSettings,
    initialState = initialState,
)
