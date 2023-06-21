package app.k9mail.feature.account.oauth.ui

import app.k9mail.core.ui.compose.common.mvi.BaseViewModel
import app.k9mail.feature.account.oauth.domain.DomainContract.UseCase.GetOAuthRequestIntent
import app.k9mail.feature.account.oauth.domain.entity.AuthorizationIntentResult
import app.k9mail.feature.account.oauth.ui.AccountOAuthContract.Effect
import app.k9mail.feature.account.oauth.ui.AccountOAuthContract.Error
import app.k9mail.feature.account.oauth.ui.AccountOAuthContract.Event
import app.k9mail.feature.account.oauth.ui.AccountOAuthContract.State
import app.k9mail.feature.account.oauth.ui.AccountOAuthContract.ViewModel

class AccountOAuthViewModel(
    initialState: State = State(),
    private val getOAuthRequestIntent: GetOAuthRequestIntent,
) : BaseViewModel<State, Event, Effect>(initialState), ViewModel {

    override fun initState(state: State) {
        updateState {
            state.copy()
        }
    }

    override fun event(event: Event) {
        when (event) {
            Event.SignInClicked -> launchOAuth()

            Event.OnNextClicked -> TODO()

            Event.OnBackClicked -> navigateBack()

            Event.OnRetryClicked -> {
                updateState { state ->
                    state.copy(
                        error = null,
                    )
                }
                launchOAuth()
            }
        }
    }

    private fun launchOAuth() {
        val result = getOAuthRequestIntent.execute(
            hostname = state.value.hostname,
            emailAddress = state.value.emailAddress,
        )

        when (result) {
            AuthorizationIntentResult.NotSupported -> {
                updateState { state ->
                    state.copy(
                        error = Error.NotSupported,
                    )
                }
            }

            is AuthorizationIntentResult.Success -> {
                emitEffect(Effect.LaunchOAuth(result.intent))
            }
        }
    }

    private fun navigateBack() = emitEffect(Effect.NavigateBack)
}
