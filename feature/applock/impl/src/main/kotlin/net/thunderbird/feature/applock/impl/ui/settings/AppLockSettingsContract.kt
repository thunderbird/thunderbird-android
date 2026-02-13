package net.thunderbird.feature.applock.impl.ui.settings

import app.k9mail.core.ui.compose.common.mvi.UnidirectionalViewModel
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.persistentListOf
import net.thunderbird.feature.applock.api.AppLockAuthenticator

internal interface AppLockSettingsContract {

    interface ViewModel : UnidirectionalViewModel<State, Event, Effect>

    data class State(
        val isEnabled: Boolean = false,
        val isAuthenticationAvailable: Boolean = false,
        val timeoutMinutes: Int = 0,
        val timeoutOptions: ImmutableList<Int> = DEFAULT_TIMEOUT_OPTIONS,
    ) {
        companion object {
            val DEFAULT_TIMEOUT_OPTIONS = persistentListOf(0, 1, 3, 5)
        }
    }

    sealed interface Event {
        data class OnEnableChanged(val enabled: Boolean) : Event
        data class OnTimeoutChanged(val minutes: Int) : Event
        data class OnAuthenticatorReady(val authenticator: AppLockAuthenticator) : Event
        data object OnResume : Event
        data object OnBackPressed : Event
    }

    sealed interface Effect {
        data object NavigateBack : Effect
        data object RequestAuthentication : Effect
    }
}
