package app.k9mail.feature.onboarding.permissions.ui

import androidx.compose.runtime.Composable
import app.k9mail.core.android.permissions.PermissionState
import app.k9mail.core.common.provider.BrandNameProvider
import app.k9mail.core.ui.compose.common.annotation.PreviewDevices
import app.k9mail.core.ui.compose.designsystem.PreviewWithTheme
import kotlinx.coroutines.Dispatchers

@Composable
@PreviewDevices
internal fun PermissionScreenPreview() {
    PreviewWithTheme {
        PermissionsScreen(
            viewModel = PermissionsViewModel(
                checkPermission = { PermissionState.Denied },
                backgroundDispatcher = Dispatchers.Main.immediate,
            ),
            brandNameProvider = object : BrandNameProvider {
                override val brandName: String = "BrandName"
            },
            onNext = {},
        )
    }
}
