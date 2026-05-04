package net.thunderbird.feature.thundermail.internal.common.ui

import android.content.Intent
import com.fsck.k9.mail.ServerSettings
import net.thunderbird.core.ui.contract.mvi.BaseViewModel

interface ThundermailContract {
    abstract class ViewModel(initialState: State) : BaseViewModel<State, Event, Effect>(initialState)
    data class State(
        val initialized: Boolean = false,
        val incomingServerSettings: ServerSettings? = null,
        val outgoingServerSettings: ServerSettings? = null,
        val error: Error? = null,
    )

    sealed interface Effect {
        data class LaunchOAuth(
            val intent: Intent,
        ) : Effect

        data object NavigateToIncomingServerSettings : Effect
    }

    sealed interface Event {
        data object SignInClicked : Event
        data class OnOAuthResult(val resultCode: Int, val data: Intent?) : Event
    }

    sealed interface Error {
        data object Canceled : Error
        data object BrowserNotAvailable : Error
        data class Unknown(val error: Exception) : Error
    }
}
