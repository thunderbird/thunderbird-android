package app.k9mail.feature.account.server.settings.ui.incoming

import app.k9mail.core.ui.compose.common.mvi.BaseViewModel
import app.k9mail.feature.account.common.domain.entity.InteractionMode
import app.k9mail.feature.account.server.settings.ui.incoming.IncomingServerSettingsContract.Effect
import app.k9mail.feature.account.server.settings.ui.incoming.IncomingServerSettingsContract.Event
import app.k9mail.feature.account.server.settings.ui.incoming.IncomingServerSettingsContract.State
import app.k9mail.feature.account.server.settings.ui.incoming.IncomingServerSettingsContract.ViewModel

class FakeIncomingServerSettingsViewModel(
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
