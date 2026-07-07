package net.thunderbird.feature.thundermail.internal.common.ui

import androidx.lifecycle.viewModelScope
import app.k9mail.autodiscovery.api.AutoDiscoveryResult
import app.k9mail.feature.account.oauth.ui.AccountOAuthContract
import app.k9mail.feature.account.oauth.ui.AccountOAuthViewModel
import app.k9mail.feature.account.setup.domain.DomainContract.UseCase.GetAutoDiscovery
import app.k9mail.feature.account.setup.domain.toServerSettings
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.flatMapConcat
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import net.thunderbird.core.logging.Logger
import net.thunderbird.core.outcome.handle
import net.thunderbird.feature.thundermail.internal.common.domain.CreateAccountStateUseCase

private const val TAG = "ThundermailOAuthViewModel"

private const val OAUTH_AUTO_DISCOVERY = "oauth-autodiscovery@thundermail.com"

@OptIn(ExperimentalCoroutinesApi::class)
internal class ThundermailOAuthViewModel(
    private val logger: Logger,
    private val accountOAuthViewModel: AccountOAuthViewModel,
    private val getAutoDiscovery: GetAutoDiscovery,
    private val createAccountStateUseCase: CreateAccountStateUseCase,
) : ThundermailContract.ViewModel(initialState = ThundermailContract.State()) {

    init {
        flow { emit(getAutoDiscovery.execute(OAUTH_AUTO_DISCOVERY)) }
            .flatMapConcat { result ->
                when (result) {
                    is AutoDiscoveryResult.Settings -> {
                        val incomingServerSettings = result.incomingServerSettings.toServerSettings(password = null)
                        accountOAuthViewModel.initState(
                            AccountOAuthContract.State(
                                hostname = incomingServerSettings.host,
                            ),
                        )
                        updateState {
                            val outgoingServerSettings = result.outgoingServerSettings
                            it.copy(
                                incomingServerSettings = incomingServerSettings,
                                outgoingServerSettings = outgoingServerSettings.toServerSettings(password = null),
                            )
                        }
                    }

                    else -> Unit
                }
                accountOAuthViewModel.state
            }
            .onEach { state ->
                updateState {
                    it.copy(
                        initialized = true,
                        error = when (val error = state.error) {
                            AccountOAuthContract.Error.BrowserNotAvailable ->
                                ThundermailContract.Error.BrowserNotAvailable

                            AccountOAuthContract.Error.Canceled -> ThundermailContract.Error.Canceled

                            is AccountOAuthContract.Error.Unknown -> ThundermailContract.Error.Unknown(error.error)

                            else -> null
                        },
                    )
                }
            }
            .launchIn(viewModelScope)

        accountOAuthViewModel
            .effect
            .onEach { effect ->
                logger.verbose(TAG) { "accountOAuthViewModel.effect() called with: effect = $effect" }
                when (effect) {
                    is AccountOAuthContract.Effect.LaunchOAuth -> emitEffect(
                        ThundermailContract.Effect.LaunchOAuth(effect.intent),
                    )

                    AccountOAuthContract.Effect.NavigateBack -> Unit

                    is AccountOAuthContract.Effect.NavigateNext -> handleNavigateNext(effect)
                }
            }
            .launchIn(viewModelScope)
    }

    private fun handleNavigateNext(effect: AccountOAuthContract.Effect.NavigateNext) {
        with(state.value) {
            val errorMessage = "Unexpected behaviour. NavigateNext called before AutoDiscoveryResult was received."

            createAccountStateUseCase(
                authorizationState = effect.state,
                incomingServerSettings = requireNotNull(incomingServerSettings) { errorMessage },
                outgoingServerSettings = requireNotNull(outgoingServerSettings) { errorMessage },
            ).handle(
                onSuccess = { emitEffect(ThundermailContract.Effect.NavigateToIncomingServerSettings) },
                onFailure = { failure ->
                    when (failure) {
                        is CreateAccountStateUseCase.Failure.AuthorizationStateMissing -> Unit
                        is CreateAccountStateUseCase.Failure.InvalidAuthorizationState -> Unit
                        is CreateAccountStateUseCase.Failure.IdTokenMissing -> Unit
                        is CreateAccountStateUseCase.Failure.MissingEmail -> Unit
                    }
                },
            )
        }
    }

    override fun event(event: ThundermailContract.Event) {
        when (event) {
            ThundermailContract.Event.SignInClicked ->
                accountOAuthViewModel.event(AccountOAuthContract.Event.SignInClicked)

            is ThundermailContract.Event.OnOAuthResult ->
                accountOAuthViewModel.event(AccountOAuthContract.Event.OnOAuthResult(event.resultCode, event.data))
        }
    }
}
