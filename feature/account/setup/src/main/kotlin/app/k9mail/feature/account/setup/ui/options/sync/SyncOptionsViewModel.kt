package app.k9mail.feature.account.setup.ui.options.sync

import app.k9mail.core.ui.compose.common.mvi.BaseViewModel
import app.k9mail.feature.account.common.domain.AccountDomainContract
import app.k9mail.feature.account.setup.ui.options.sync.SyncOptionsContract.Effect
import app.k9mail.feature.account.setup.ui.options.sync.SyncOptionsContract.Event
import app.k9mail.feature.account.setup.ui.options.sync.SyncOptionsContract.State
import app.k9mail.feature.account.setup.ui.options.sync.SyncOptionsContract.ViewModel

internal class SyncOptionsViewModel(
    private val accountStateRepository: AccountDomainContract.AccountStateRepository,
    initialState: State? = null,
) : BaseViewModel<State, Event, Effect>(
    initialState = initialState ?: accountStateRepository.getState().toSyncOptionsState(),
),
    ViewModel {

    override fun event(event: Event) {
        when (event) {
            Event.LoadAccountState -> handleOneTimeEvent(event, ::loadAccountState)

            is Event.OnCheckFrequencyChanged -> updateState {
                it.copy(
                    checkFrequency = event.checkFrequency,
                )
            }

            is Event.OnMessageDisplayCountChanged -> updateState { state ->
                state.copy(
                    messageDisplayCount = event.messageDisplayCount,
                )
            }

            is Event.OnShowNotificationChanged -> updateState { state ->
                state.copy(
                    showNotification = event.showNotification,
                )
            }

            Event.OnNextClicked -> submit()
            Event.OnBackClicked -> navigateBack()
        }
    }

    private fun loadAccountState() {
        updateState {
            accountStateRepository.getState().toSyncOptionsState()
        }
    }

    private fun submit() {
        accountStateRepository.setSyncOptions(state.value.toAccountSyncOptions())
        navigateNext()
    }

    private fun navigateBack() = emitEffect(Effect.NavigateBack)

    private fun navigateNext() = emitEffect(Effect.NavigateNext)
}
