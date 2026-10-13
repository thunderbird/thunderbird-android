package app.k9mail.feature.account.edit.ui.server.settings.reauthorize

import app.k9mail.feature.account.oauth.domain.entity.OAuthResult
import app.k9mail.feature.account.oauth.ui.AccountOAuthContract
import net.thunderbird.core.ui.contract.mvi.UnidirectionalViewModel

interface ApiAccountReauthorizationContract {
    interface ViewModel : UnidirectionalViewModel<State, Event, Effect> {
        val accountOAuthViewModel: AccountOAuthContract.ViewModel
    }

    data class State(
        val provider: Provider? = null,
        val isInitializing: Boolean = true,
        val isSaving: Boolean = false,
        val isSuccess: Boolean = false,
        val error: Error? = null,
    )

    enum class Provider {
        Google,
        Microsoft,
        Zoho,
    }

    sealed interface Event {
        data object Load : Event
        data class OnOAuthResult(val result: OAuthResult) : Event
    }

    sealed interface Effect

    enum class Error {
        NotAvailable,
        Canceled,
        IdentityMismatch,
        Failed,
    }
}
