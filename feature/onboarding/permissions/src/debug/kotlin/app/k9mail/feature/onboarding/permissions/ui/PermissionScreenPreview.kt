package app.k9mail.feature.onboarding.permissions.ui

import androidx.compose.runtime.Composable
import androidx.lifecycle.viewmodel.compose.viewModel
import app.k9mail.core.android.permissions.PermissionState
import app.k9mail.core.ui.compose.common.annotation.PreviewDevices
import kotlinx.coroutines.Dispatchers
import net.thunderbird.core.common.provider.BrandNameProvider
import net.thunderbird.feature.thundermail.ui.preview.ThundermailPreview

@Composable
@PreviewDevices
internal fun PermissionScreenPreview() {
    ThundermailPreview {
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
