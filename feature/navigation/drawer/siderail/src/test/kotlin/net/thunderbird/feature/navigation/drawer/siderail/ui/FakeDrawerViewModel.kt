package net.thunderbird.feature.navigation.drawer.siderail.ui

import app.k9mail.core.ui.compose.testing.BaseFakeViewModel
import net.thunderbird.feature.navigation.drawer.siderail.ui.DrawerContract.Effect
import net.thunderbird.feature.navigation.drawer.siderail.ui.DrawerContract.Event
import net.thunderbird.feature.navigation.drawer.siderail.ui.DrawerContract.State
import net.thunderbird.feature.navigation.drawer.siderail.ui.DrawerContract.ViewModel

internal class FakeDrawerViewModel(
    initialState: State = State(),
) : BaseFakeViewModel<State, Event, Effect>(initialState), ViewModel
