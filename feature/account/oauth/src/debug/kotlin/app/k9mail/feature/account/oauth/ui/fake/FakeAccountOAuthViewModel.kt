package app.k9mail.feature.account.oauth.ui.fake

import app.k9mail.core.ui.compose.common.mvi.BaseViewModel
import app.k9mail.feature.account.oauth.ui.AccountOAuthContract.Effect
import app.k9mail.feature.account.oauth.ui.AccountOAuthContract.Event
import app.k9mail.feature.account.oauth.ui.AccountOAuthContract.State
import app.k9mail.feature.account.oauth.ui.AccountOAuthContract.ViewModel

class FakeAccountOAuthViewModel : BaseViewModel<State, Event, Effect>(State()), ViewModel {
    override fun initState(state: State) = Unit
    override fun event(event: Event) = Unit
}
