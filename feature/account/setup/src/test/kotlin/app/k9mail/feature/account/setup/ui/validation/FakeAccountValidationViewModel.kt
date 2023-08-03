package app.k9mail.feature.account.setup.ui.validation

import app.k9mail.core.ui.compose.common.mvi.BaseViewModel
import app.k9mail.feature.account.oauth.ui.AccountOAuthContract
import app.k9mail.feature.account.setup.ui.FakeAccountOAuthViewModel
import app.k9mail.feature.account.setup.ui.validation.AccountValidationContract.Effect
import app.k9mail.feature.account.setup.ui.validation.AccountValidationContract.Event
import app.k9mail.feature.account.setup.ui.validation.AccountValidationContract.State
import app.k9mail.feature.account.setup.ui.validation.AccountValidationContract.ViewModel

class FakeAccountValidationViewModel(
    override val oAuthViewModel: AccountOAuthContract.ViewModel = FakeAccountOAuthViewModel(),
    override val isIncomingValidation: Boolean = true,
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
