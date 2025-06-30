package net.thunderbird.feature.account.settings.impl.ui.general

import app.k9mail.core.ui.compose.testing.BaseFakeViewModel
import net.thunderbird.feature.account.settings.impl.ui.general.GeneralSettingsContract.Effect
import net.thunderbird.feature.account.settings.impl.ui.general.GeneralSettingsContract.Event
import net.thunderbird.feature.account.settings.impl.ui.general.GeneralSettingsContract.State
import net.thunderbird.feature.account.settings.impl.ui.general.GeneralSettingsContract.ViewModel

internal class FakeGeneralSettingsViewModel(
    initialState: State = State(),
) : BaseFakeViewModel<State, Event, Effect>(initialState), ViewModel
