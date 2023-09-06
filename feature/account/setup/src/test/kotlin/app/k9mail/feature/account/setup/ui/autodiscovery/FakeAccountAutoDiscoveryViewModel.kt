package app.k9mail.feature.account.setup.ui.autodiscovery

import app.k9mail.core.ui.compose.common.mvi.BaseViewModel
import app.k9mail.feature.account.oauth.ui.AccountOAuthContract
import app.k9mail.feature.account.oauth.ui.fake.FakeAccountOAuthViewModel
import app.k9mail.feature.account.setup.ui.autodiscovery.AccountAutoDiscoveryContract.Effect
import app.k9mail.feature.account.setup.ui.autodiscovery.AccountAutoDiscoveryContract.Event
import app.k9mail.feature.account.setup.ui.autodiscovery.AccountAutoDiscoveryContract.State

class FakeAccountAutoDiscoveryViewModel(
    initialState: State = State(),
) : BaseViewModel<State, Event, Effect>(initialState), AccountAutoDiscoveryContract.ViewModel {

    val events = mutableListOf<Event>()

    override val oAuthViewModel: AccountOAuthContract.ViewModel = FakeAccountOAuthViewModel()

    override fun initState(state: State) {
        updateState { state }
    }

    override fun event(event: Event) {
        events.add(event)
    }

    fun effect(effect: Effect) {
        emitEffect(effect)
    }
}
