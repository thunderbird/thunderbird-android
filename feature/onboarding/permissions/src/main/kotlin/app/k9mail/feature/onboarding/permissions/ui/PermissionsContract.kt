package app.k9mail.feature.onboarding.permissions.ui

import app.k9mail.core.ui.compose.common.mvi.UnidirectionalViewModel

interface PermissionsContract {
    interface ViewModel : UnidirectionalViewModel<State, Event, Effect>

    data class State(
        val isLoading: Boolean = true,
        val contactsPermissionState: UiPermissionState = UiPermissionState.Unknown,
        val notificationsPermissionState: UiPermissionState = UiPermissionState.Unknown,
        val isNotificationsPermissionVisible: Boolean = false,
        val isNextButtonVisible: Boolean = false,
    )

    sealed interface Event {
        data object LoadPermissionState : Event

        data object AllowContactsPermissionClicked : Event
        data object AllowNotificationsPermissionClicked : Event

        data class ContactsPermissionResult(val success: Boolean) : Event
        data class NotificationsPermissionResult(val success: Boolean) : Event

        data object NextClicked : Event
    }

    sealed interface Effect {
        data object RequestContactsPermission : Effect
        data object RequestNotificationsPermission : Effect

        data object NavigateNext : Effect
    }

    enum class UiPermissionState {
        Unknown,
        Granted,
        Denied,
    }
}
