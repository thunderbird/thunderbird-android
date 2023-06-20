package app.k9mail.feature.account.setup.ui

import app.k9mail.core.ui.compose.common.mvi.UnidirectionalViewModel
import app.k9mail.feature.account.setup.ui.autodiscovery.AccountAutoDiscoveryContract
import app.k9mail.feature.account.setup.ui.incoming.AccountIncomingConfigContract
import app.k9mail.feature.account.setup.ui.options.AccountOptionsContract
import app.k9mail.feature.account.setup.ui.outgoing.AccountOutgoingConfigContract

interface AccountSetupContract {

    enum class SetupStep {
        AUTO_CONFIG,
        INCOMING_CONFIG,
        OUTGOING_CONFIG,
        OPTIONS,
    }

    interface ViewModel : UnidirectionalViewModel<State, Event, Effect>

    data class State(
        val setupStep: SetupStep = SetupStep.AUTO_CONFIG,
    )

    sealed interface Event {
        object OnNext : Event

        data class OnAutoDiscoveryFinished(
            val state: AccountAutoDiscoveryContract.State,
        ) : Event

        object OnBack : Event
    }

    sealed interface Effect {

        data class UpdateIncomingConfig(
            val state: AccountIncomingConfigContract.State,
        ) : Effect

        data class UpdateOutgoingConfig(
            val state: AccountOutgoingConfigContract.State,
        ) : Effect

        data class UpdateOptions(
            val state: AccountOptionsContract.State,
        ) : Effect

        object NavigateNext : Effect
        object NavigateBack : Effect
    }
}
