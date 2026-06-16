package app.k9mail.feature.onboarding.permissions.ui

import androidx.compose.runtime.Composable
import net.thunderbird.components.ui.bolt.PreviewWithTheme
import net.thunderbird.components.ui.bolt.common.annotation.PreviewDevices

@Composable
@PreviewDevices
internal fun PermissionContentPreview() {
    PreviewWithTheme {
        PermissionsContent(
            state = PermissionsContract.State(
                isLoading = false,
                contactsPermissionState = PermissionsContract.UiPermissionState.Granted,
                notificationsPermissionState = PermissionsContract.UiPermissionState.Denied,
                isNotificationsPermissionVisible = true,
                isNextButtonVisible = false,
            ),
            onEvent = {},
            brandName = "BrandName",
        )
    }
}
