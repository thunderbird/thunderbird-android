package app.k9mail.feature.onboarding.permissions.ui

import androidx.compose.runtime.Composable
import app.k9mail.core.ui.compose.designsystem.PreviewWithTheme
import net.thunderbird.core.ui.common.annotation.PreviewDevices

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
