package net.thunderbird.feature.account.server.settings.ui.outgoing

import app.k9mail.feature.account.common.domain.entity.InteractionMode
import net.thunderbird.core.ui.contract.mvi.BaseViewModel
import net.thunderbird.feature.account.server.settings.ui.outgoing.OutgoingServerSettingsContract.Effect
import net.thunderbird.feature.account.server.settings.ui.outgoing.OutgoingServerSettingsContract.Event
import net.thunderbird.feature.account.server.settings.ui.outgoing.OutgoingServerSettingsContract.State
import net.thunderbird.feature.account.server.settings.ui.outgoing.OutgoingServerSettingsContract.ViewModel

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
