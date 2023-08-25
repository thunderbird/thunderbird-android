package app.k9mail.feature.account.setup.ui.options

import app.k9mail.core.ui.compose.common.mvi.BaseViewModel
import app.k9mail.feature.account.setup.ui.options.AccountOptionsContract.Effect
import app.k9mail.feature.account.setup.ui.options.AccountOptionsContract.Event
import app.k9mail.feature.account.setup.ui.options.AccountOptionsContract.State
import app.k9mail.feature.account.setup.ui.options.AccountOptionsContract.ViewModel

class FakeAccountOptionsViewModel(
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
