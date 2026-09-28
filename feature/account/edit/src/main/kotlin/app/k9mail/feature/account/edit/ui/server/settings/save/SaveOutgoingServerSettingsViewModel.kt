package app.k9mail.feature.account.edit.ui.server.settings.save

import app.k9mail.feature.account.edit.domain.AccountEditDomainContract.UseCase
import app.k9mail.feature.account.edit.ui.server.settings.save.SaveServerSettingsContract.State
import net.thunderbird.feature.account.AccountId

class SaveOutgoingServerSettingsViewModel(
    accountId: AccountId,
    saveServerSettings: UseCase.SaveServerSettings,
    initialState: State = State(),
) : BaseSaveServerSettingsViewModel(
    accountId = accountId,
    isIncoming = false,
    saveServerSettings = saveServerSettings,
    initialState = initialState,
)
