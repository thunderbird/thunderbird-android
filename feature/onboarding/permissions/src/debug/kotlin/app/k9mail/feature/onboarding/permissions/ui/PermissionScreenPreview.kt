package app.k9mail.feature.onboarding.permissions.ui

import androidx.compose.runtime.Composable
import androidx.lifecycle.viewmodel.compose.viewModel
import app.k9mail.core.android.permissions.PermissionState
import kotlinx.coroutines.Dispatchers
import net.thunderbird.components.ui.bolt.PreviewWithTheme
import net.thunderbird.components.ui.bolt.common.annotation.PreviewDevices
import net.thunderbird.core.common.provider.BrandNameProvider

@Composable
@PreviewDevices
internal fun PermissionScreenPreview() {
    PreviewWithTheme {
        PermissionsScreen(
            viewModel = viewModel {
                PermissionsViewModel(
                    checkPermission = { PermissionState.Denied },
                    backgroundDispatcher = Dispatchers.Main.immediate,
                )
            },
            brandNameProvider = object : BrandNameProvider {
                override val brandName: String = "BrandName"
            },
            onNext = {},
        )
    }
}
