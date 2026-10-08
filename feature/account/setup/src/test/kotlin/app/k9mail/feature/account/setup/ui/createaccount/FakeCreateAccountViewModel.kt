package app.k9mail.feature.account.setup.ui.createaccount

import app.k9mail.feature.account.setup.ui.createaccount.CreateAccountContract.Effect
import app.k9mail.feature.account.setup.ui.createaccount.CreateAccountContract.Event
import app.k9mail.feature.account.setup.ui.createaccount.CreateAccountContract.State
import app.k9mail.feature.account.setup.ui.createaccount.CreateAccountContract.ViewModel
import net.thunderbird.core.ui.contract.mvi.BaseViewModel

class FakeCreateAccountViewModel(
    initialState: State = State(),
) : BaseViewModel<State, Event, Effect>(initialState), ViewModel {

    val events = mutableListOf<Event>()

    override fun event(event: Event) {
        events.add(event)
    }

    fun effect(effect: Effect) {
        emitEffect(effect)
    }
}
