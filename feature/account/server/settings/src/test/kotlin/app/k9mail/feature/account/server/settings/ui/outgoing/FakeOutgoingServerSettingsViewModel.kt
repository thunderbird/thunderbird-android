package app.k9mail.feature.account.server.settings.ui.outgoing

import app.k9mail.core.ui.compose.common.mvi.BaseViewModel
import app.k9mail.feature.account.common.domain.entity.InteractionMode
import app.k9mail.feature.account.server.settings.ui.outgoing.OutgoingServerSettingsContract.Effect
import app.k9mail.feature.account.server.settings.ui.outgoing.OutgoingServerSettingsContract.Event
import app.k9mail.feature.account.server.settings.ui.outgoing.OutgoingServerSettingsContract.State
import app.k9mail.feature.account.server.settings.ui.outgoing.OutgoingServerSettingsContract.ViewModel

class FakeOutgoingServerSettingsViewModel(
    override val mode: InteractionMode = InteractionMode.Create,
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
