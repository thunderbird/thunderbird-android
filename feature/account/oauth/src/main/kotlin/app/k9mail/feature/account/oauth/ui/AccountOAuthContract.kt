package app.k9mail.feature.account.oauth.ui

import android.content.Intent
import app.k9mail.core.ui.compose.common.mvi.UnidirectionalViewModel
import app.k9mail.feature.account.common.domain.entity.AuthorizationState
import app.k9mail.feature.account.common.ui.WizardNavigationBarState

interface AccountOAuthContract {

    interface ViewModel : UnidirectionalViewModel<State, Event, Effect> {
        fun initState(state: State)
    }

    data class State(
        val hostname: String = "",
        val emailAddress: String = "",
        val wizardNavigationBarState: WizardNavigationBarState = WizardNavigationBarState(
            isNextEnabled = false,
        ),
        val isGoogleSignIn: Boolean = false,
        val error: Error? = null,
        val isLoading: Boolean = false,
    )

    sealed interface Event {
        data class OnOAuthResult(
            val resultCode: Int,
            val data: Intent?,
        ) : Event

        object SignInClicked : Event
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
        object Canceled : Error

        object BrowserNotAvailable : Error
        data class Unknown(val error: Exception) : Error
    }
}
