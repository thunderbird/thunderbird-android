package app.k9mail.feature.account.setup.ui.createaccount

import app.k9mail.core.ui.compose.common.mvi.UnidirectionalViewModel
import app.k9mail.core.ui.compose.designsystem.molecule.LoadingErrorState
import app.k9mail.feature.account.setup.AccountSetupExternalContract.AccountCreator.AccountCreatorResult.Error
import app.k9mail.feature.account.setup.domain.entity.AccountUuid

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
        data class NavigateNext(val accountUuid: AccountUuid) : Effect
        data object NavigateBack : Effect
    }
}
