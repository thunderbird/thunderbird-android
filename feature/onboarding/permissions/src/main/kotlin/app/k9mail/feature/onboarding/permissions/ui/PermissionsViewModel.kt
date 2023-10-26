package app.k9mail.feature.onboarding.permissions.ui

import androidx.lifecycle.viewModelScope
import app.k9mail.core.android.permissions.Permission
import app.k9mail.core.android.permissions.PermissionState
import app.k9mail.core.ui.compose.common.mvi.BaseViewModel
import app.k9mail.feature.onboarding.permissions.domain.PermissionsDomainContract.UseCase
import app.k9mail.feature.onboarding.permissions.ui.PermissionsContract.Effect
import app.k9mail.feature.onboarding.permissions.ui.PermissionsContract.Event
import app.k9mail.feature.onboarding.permissions.ui.PermissionsContract.State
import app.k9mail.feature.onboarding.permissions.ui.PermissionsContract.UiPermissionState
import app.k9mail.feature.onboarding.permissions.ui.PermissionsContract.ViewModel
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class PermissionsViewModel(
    private val checkPermission: UseCase.CheckPermission,
    private val backgroundDispatcher: CoroutineDispatcher = Dispatchers.IO,
) : BaseViewModel<State, Event, Effect>(initialState = State(isLoading = true)), ViewModel {

    override fun event(event: Event) {
        when (event) {
            Event.LoadPermissionState -> handleOneTimeEvent(event, ::loadPermissionState)
            Event.AllowContactsPermissionClicked -> handleAllowContactsPermissionClicked()
            Event.AllowNotificationsPermissionClicked -> handleAllowNotificationsPermissionClicked()
            is Event.ContactsPermissionResult -> handleContactsPermissionResult(event.success)
            is Event.NotificationsPermissionResult -> handleNotificationsPermissionResult(event.success)
            Event.NextClicked -> handleNextClicked()
        }
    }

    private fun loadPermissionState() {
        viewModelScope.launch {
            val (contactsPermissionState, notificationsPermissionState) = withContext(backgroundDispatcher) {
                arrayOf(
                    checkPermission(Permission.Contacts),
                    checkPermission(Permission.Notifications),
                )
            }

            val contactsUiPermissionState = when (contactsPermissionState) {
                PermissionState.GrantedImplicitly -> error("Unexpected case")
                PermissionState.Granted -> UiPermissionState.Granted
                PermissionState.Denied -> UiPermissionState.Unknown
            }
            val notificationsUiPermissionState = when (notificationsPermissionState) {
                PermissionState.GrantedImplicitly -> UiPermissionState.Unknown
                PermissionState.Granted -> UiPermissionState.Granted
                PermissionState.Denied -> UiPermissionState.Unknown
            }
            val isNotificationsPermissionVisible = notificationsPermissionState != PermissionState.GrantedImplicitly

            updateState { state ->
                state.copy(
                    isLoading = false,
                    contactsPermissionState = contactsUiPermissionState,
                    notificationsPermissionState = notificationsUiPermissionState,
                    isNotificationsPermissionVisible = isNotificationsPermissionVisible,
                )
            }
            updateNextButtonState()
        }
    }

    private fun handleAllowContactsPermissionClicked() {
        emitEffect(Effect.RequestContactsPermission)
    }

    private fun handleAllowNotificationsPermissionClicked() {
        emitEffect(Effect.RequestNotificationsPermission)
    }

    private fun handleContactsPermissionResult(success: Boolean) {
        updateState { state ->
            state.copy(
                contactsPermissionState = if (success) UiPermissionState.Granted else UiPermissionState.Denied,
            )
        }
        updateNextButtonState()
    }

    private fun handleNotificationsPermissionResult(success: Boolean) {
        updateState { state ->
            state.copy(
                notificationsPermissionState = if (success) UiPermissionState.Granted else UiPermissionState.Denied,
            )
        }
        updateNextButtonState()
    }

    private fun updateNextButtonState() {
        updateState { state ->
            val isContactsPermissionGranted = state.contactsPermissionState == UiPermissionState.Granted
            val isNotificationsPermissionGrantedOrHidden = !state.isNotificationsPermissionVisible ||
                state.notificationsPermissionState == UiPermissionState.Granted

            state.copy(
                isNextButtonVisible = isContactsPermissionGranted && isNotificationsPermissionGrantedOrHidden,
            )
        }
    }

    private fun handleNextClicked() {
        emitEffect(Effect.NavigateNext)
    }
}
