package app.k9mail.feature.account.setup.ui

import app.k9mail.core.ui.compose.common.mvi.BaseViewModel
import app.k9mail.feature.account.setup.ui.AccountSetupContract.Effect
import app.k9mail.feature.account.setup.ui.AccountSetupContract.Event
import app.k9mail.feature.account.setup.ui.AccountSetupContract.State

internal class FakeAccountSetupViewModel(
    initialState: State = State(),
) : BaseViewModel<State, Event, Effect>(initialState), AccountSetupContract.ViewModel {

    val events = mutableListOf<Event>()

    fun state(update: (State) -> (State)) {
        updateState { update(it) }
    }

    override fun event(event: Event) {
        events.add(event)
    }

    fun effect(effect: Effect) {
        emitEffect(effect)
    }
}
