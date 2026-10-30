package app.k9mail.feature.account.setup.ui.createaccount

import app.k9mail.feature.account.setup.AccountSetupExternalContract.AccountCreator.AccountCreatorResult.Error
import net.thunderbird.components.ui.bolt.molecule.LoadingErrorState
import net.thunderbird.core.ui.contract.mvi.UnidirectionalViewModel
import net.thunderbird.feature.account.AccountId

interface CreateAccountContract {

    interface ViewModel : UnidirectionalViewModel<State, Event, Effect>

    data class State(
        override val isLoading: Boolean = true,
        override val error: Error? = null,
    ) : LoadingErrorState<Error>

    sealed interface Event {
        data object CreateAccount : Event
        data object OnBackClicked : Event
    }

    sealed interface Effect {
        data class NavigateNext(val accountId: AccountId) : Effect
        data object NavigateBack : Effect
    }
}
