package app.k9mail.feature.onboarding.permissions.ui

import androidx.compose.runtime.Composable
import app.k9mail.core.android.permissions.PermissionState
import app.k9mail.core.ui.compose.common.annotation.PreviewDevices
import app.k9mail.feature.account.common.ui.PreviewWithThemeAndKoin
import kotlinx.coroutines.Dispatchers

@Composable
@PreviewDevices
internal fun PermissionScreenPreview() {
    PreviewWithThemeAndKoin {
        PermissionsScreen(
            viewModel = PermissionsViewModel(
                checkPermission = { PermissionState.Denied },
                backgroundDispatcher = Dispatchers.Main.immediate,
            ),
            onNext = {},
        )
    }
}
