package app.k9mail.feature.onboarding.permissions.ui

import androidx.compose.runtime.Composable
import app.k9mail.core.ui.compose.common.annotation.PreviewDevices
import app.k9mail.feature.account.common.ui.PreviewWithThemeAndKoin

@Composable
@PreviewDevices
internal fun PermissionContentPreview() {
    PreviewWithThemeAndKoin {
        PermissionsContent(
            state = PermissionsContract.State(
                isLoading = false,
                contactsPermissionState = PermissionsContract.UiPermissionState.Granted,
                notificationsPermissionState = PermissionsContract.UiPermissionState.Denied,
                isNotificationsPermissionVisible = true,
                isNextButtonVisible = false,
            ),
            onEvent = {},
        )
    }
}
