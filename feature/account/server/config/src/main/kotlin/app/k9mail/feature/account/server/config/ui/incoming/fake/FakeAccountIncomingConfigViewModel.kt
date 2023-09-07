package app.k9mail.feature.account.server.config.ui.incoming.fake

import app.k9mail.core.ui.compose.common.mvi.BaseViewModel
import app.k9mail.feature.account.server.config.ui.incoming.AccountIncomingConfigContract.Effect
import app.k9mail.feature.account.server.config.ui.incoming.AccountIncomingConfigContract.Event
import app.k9mail.feature.account.server.config.ui.incoming.AccountIncomingConfigContract.State
import app.k9mail.feature.account.server.config.ui.incoming.AccountIncomingConfigContract.ViewModel

class FakeAccountIncomingConfigViewModel(
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
