package app.k9mail.feature.account.edit.ui.server.settings.save

import app.k9mail.feature.account.edit.domain.AccountEditDomainContract.UseCase
import app.k9mail.feature.account.edit.ui.server.settings.save.SaveServerSettingsContract.State

class SaveIncomingServerSettingsViewModel(
    accountUuid: String,
    saveServerSettings: UseCase.SaveServerSettings,
    initialState: State = State(),
) : BaseSaveServerSettingsViewModel(
    accountUuid = accountUuid,
    isIncoming = true,
    saveServerSettings = saveServerSettings,
    initialState = initialState,
)
