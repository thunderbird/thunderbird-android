package app.k9mail.feature.navigation.drawer.ui

import app.k9mail.core.ui.compose.testing.BaseFakeViewModel
import app.k9mail.feature.navigation.drawer.ui.DrawerContract.Effect
import app.k9mail.feature.navigation.drawer.ui.DrawerContract.Event
import app.k9mail.feature.navigation.drawer.ui.DrawerContract.State
import app.k9mail.feature.navigation.drawer.ui.DrawerContract.ViewModel

class FakeDrawerViewModel(
    initialState: State = State(),
) : BaseFakeViewModel<State, Event, Effect>(initialState), ViewModel
