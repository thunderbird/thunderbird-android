package app.k9mail.feature.account.setup.ui.specialfolders.fake

import app.k9mail.feature.account.setup.ui.specialfolders.SpecialFoldersContract.Effect
import app.k9mail.feature.account.setup.ui.specialfolders.SpecialFoldersContract.Event
import app.k9mail.feature.account.setup.ui.specialfolders.SpecialFoldersContract.State
import app.k9mail.feature.account.setup.ui.specialfolders.SpecialFoldersContract.ViewModel
import net.thunderbird.core.ui.contract.mvi.BaseViewModel

class FakeSpecialFoldersViewModel(
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
