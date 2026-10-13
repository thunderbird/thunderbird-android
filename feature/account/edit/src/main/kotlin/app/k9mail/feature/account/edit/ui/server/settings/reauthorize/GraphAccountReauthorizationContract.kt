package app.k9mail.feature.account.edit.ui.server.settings.reauthorize

import app.k9mail.feature.account.oauth.domain.entity.OAuthResult
import app.k9mail.feature.account.oauth.ui.AccountOAuthContract
import net.thunderbird.core.ui.contract.mvi.UnidirectionalViewModel

interface GraphAccountReauthorizationContract {
    interface ViewModel : UnidirectionalViewModel<State, Event, Effect> {
        val accountOAuthViewModel: AccountOAuthContract.ViewModel
    }

    data class State(
        val isInitializing: Boolean = true,
        val isSaving: Boolean = false,
        val isSuccess: Boolean = false,
        val error: Error? = null,
    )

    sealed interface Event {
        data object Load : Event
        data class OnOAuthResult(val result: OAuthResult) : Event
    }

    sealed interface Effect

    sealed interface Error {
        data object NotAvailable : Error
        data object Canceled : Error
        data object MicrosoftAccountMismatch : Error
        data object Failed : Error
    }
}
