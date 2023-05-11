package app.k9mail.feature.account.setup.ui

import app.k9mail.feature.account.setup.ui.AccountSetupContract.Effect
import app.k9mail.feature.account.setup.ui.AccountSetupContract.Event
import app.k9mail.feature.account.setup.ui.AccountSetupContract.State
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow

internal class FakeAccountSetupViewModel(
    initialState: State = State(),
) : AccountSetupContract.ViewModel {

    val mutableState = MutableStateFlow(initialState)
    val mutableEffect = MutableSharedFlow<Effect>()
    val events = mutableListOf<Event>()

    override val state: StateFlow<State> = mutableState.asStateFlow()
    override val effect: SharedFlow<Effect> = mutableEffect.asSharedFlow()
    override fun event(event: Event) {
        events.add(event)
    }
}
