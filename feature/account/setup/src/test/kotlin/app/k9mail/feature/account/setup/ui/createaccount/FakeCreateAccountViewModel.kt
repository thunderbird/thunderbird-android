package app.k9mail.feature.account.setup.ui.createaccount

import app.k9mail.core.ui.compose.common.mvi.BaseViewModel
import app.k9mail.feature.account.setup.ui.createaccount.CreateAccountContract.Effect
import app.k9mail.feature.account.setup.ui.createaccount.CreateAccountContract.Event
import app.k9mail.feature.account.setup.ui.createaccount.CreateAccountContract.State
import app.k9mail.feature.account.setup.ui.createaccount.CreateAccountContract.ViewModel

class FakeCreateAccountViewModel(initialState: State = State()) :
    BaseViewModel<State, Event, Effect>(initialState), ViewModel {

    val events = mutableListOf<Event>()

    override fun event(event: Event) {
        events.add(event)
    }

    fun effect(effect: Effect) {
        emitEffect(effect)
    }
}
