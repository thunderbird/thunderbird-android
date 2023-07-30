package app.k9mail.feature.account.setup.ui

import app.k9mail.core.ui.compose.common.mvi.BaseViewModel
import app.k9mail.feature.account.setup.ui.AccountSetupContract.Effect
import app.k9mail.feature.account.setup.ui.AccountSetupContract.Event
import app.k9mail.feature.account.setup.ui.AccountSetupContract.State
import app.k9mail.feature.account.setup.ui.autodiscovery.FakeAccountAutoDiscoveryViewModel
import app.k9mail.feature.account.setup.ui.incoming.AccountIncomingConfigContract
import app.k9mail.feature.account.setup.ui.incoming.FakeAccountIncomingConfigViewModel
import app.k9mail.feature.account.setup.ui.options.AccountOptionsContract
import app.k9mail.feature.account.setup.ui.options.FakeAccountOptionsViewModel
import app.k9mail.feature.account.setup.ui.outgoing.AccountOutgoingConfigContract
import app.k9mail.feature.account.setup.ui.outgoing.FakeAccountOutgoingConfigViewModel
import app.k9mail.feature.account.setup.ui.validation.AccountValidationContract
import app.k9mail.feature.account.setup.ui.validation.FakeAccountValidationViewModel

internal class FakeAccountSetupViewModel(
    override val autoDiscoveryViewModel: FakeAccountAutoDiscoveryViewModel = FakeAccountAutoDiscoveryViewModel(),
    override val incomingViewModel: AccountIncomingConfigContract.ViewModel = FakeAccountIncomingConfigViewModel(),
    override val incomingValidationViewModel: AccountValidationContract.ViewModel = FakeAccountValidationViewModel(),
    override val outgoingViewModel: AccountOutgoingConfigContract.ViewModel = FakeAccountOutgoingConfigViewModel(),
    override val outgoingValidationViewModel: AccountValidationContract.ViewModel = FakeAccountValidationViewModel(),
    override val optionsViewModel: AccountOptionsContract.ViewModel = FakeAccountOptionsViewModel(),
    initialState: State = State(),
) : BaseViewModel<State, Event, Effect>(initialState), AccountSetupContract.ViewModel {

    val events = mutableListOf<Event>()

    fun state(update: (State) -> (State)) {
        updateState { update(it) }
    }

    override fun event(event: Event) {
        events.add(event)
    }

    fun effect(effect: Effect) {
        emitEffect(effect)
    }
}
