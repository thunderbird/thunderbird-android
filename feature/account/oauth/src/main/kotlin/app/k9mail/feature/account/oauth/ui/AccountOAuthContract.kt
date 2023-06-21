package app.k9mail.feature.account.oauth.ui

import android.content.Intent
import app.k9mail.core.ui.compose.common.mvi.UnidirectionalViewModel
import app.k9mail.feature.account.common.ui.WizardNavigationBarState
import app.k9mail.feature.account.oauth.domain.entity.AuthorizationState

interface AccountOAuthContract {

    interface ViewModel : UnidirectionalViewModel<State, Event, Effect> {
        fun initState(state: State)
    }

    data class State(
        val hostname: String = "",
        val emailAddress: String = "",
        val authorizationState: AuthorizationState = AuthorizationState(),
        val wizardNavigationBarState: WizardNavigationBarState = WizardNavigationBarState(
            isNextEnabled = false,
        ),
        val isGoogleSignIn: Boolean = false,
        val error: Error? = null,
        val isLoading: Boolean = false,
    )

    sealed interface Event {
        object SignInClicked : Event
        object OnNextClicked : Event
        object OnBackClicked : Event
        object OnRetryClicked : Event
    }

    sealed interface Effect {
        data class LaunchOAuth(
            val intent: Intent,
        ) : Effect

        data class NavigateNext(
            val state: AuthorizationState,
        ) : Effect
        object NavigateBack : Effect
    }

    sealed interface Error {
        object NotSupported : Error
        object NetworkError : Error
        object UnknownError : Error
    }
}
