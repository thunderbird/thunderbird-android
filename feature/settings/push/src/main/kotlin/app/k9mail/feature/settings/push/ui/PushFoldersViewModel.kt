package app.k9mail.feature.settings.push.ui

import androidx.lifecycle.viewModelScope
import app.k9mail.core.ui.compose.common.mvi.BaseViewModel
import app.k9mail.feature.settings.push.ui.PushFoldersContract.Effect
import app.k9mail.feature.settings.push.ui.PushFoldersContract.Event
import app.k9mail.feature.settings.push.ui.PushFoldersContract.State
import app.k9mail.feature.settings.push.ui.PushFoldersContract.ViewModel
import com.fsck.k9.controller.push.AlarmPermissionManager
import com.fsck.k9.preferences.AccountManager
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import timber.log.Timber

internal class PushFoldersViewModel(
    private val accountUuid: String,
    private val accountManager: AccountManager,
    private val alarmPermissionManager: AlarmPermissionManager,
    private val backgroundDispatcher: CoroutineDispatcher = Dispatchers.IO,
) : BaseViewModel<State, Event, Effect>(initialState = State(isLoading = true)), ViewModel {
    init {
        initializeShowPermissionPromptValue()
        loadAccount()
    }

    private fun initializeShowPermissionPromptValue() {
        updateState { state ->
            state.copy(showPermissionPrompt = !alarmPermissionManager.canScheduleExactAlarms())
        }
    }

    private fun loadAccount() {
        viewModelScope.launch {
            val account = withContext(backgroundDispatcher) {
                accountManager.getAccount(accountUuid)
            }

            if (account == null) {
                Timber.w("Account not found: %s", accountUuid)
                emitEffect(Effect.NavigateBack)
            } else {
                updateState { state ->
                    state.copy(
                        isLoading = false,
                        selectedOption = account.folderPushMode,
                    )
                }
            }
        }
    }

    override fun event(event: Event) {
        when (event) {
            Event.BackClicked -> emitEffect(Effect.NavigateBack)
            Event.GrantAlarmPermissionClicked -> emitEffect(Effect.RequestAlarmPermission)
            is Event.AlarmPermissionResult -> updateShowPermissionPromptValue()
            is Event.OptionSelected -> handleOptionSelected(event)
        }
    }

    private fun updateShowPermissionPromptValue() {
        updateState { state ->
            state.copy(showPermissionPrompt = !alarmPermissionManager.canScheduleExactAlarms())
        }
    }

    private fun handleOptionSelected(event: Event.OptionSelected) {
        val selectedOption = event.option

        updateState { state ->
            state.copy(selectedOption = selectedOption)
        }

        emitEffect(Effect.OptionSelected(selectedOption))
    }
}
