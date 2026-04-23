package app.k9mail.feature.onboarding.migration.thunderbird

import androidx.compose.runtime.Composable
import app.k9mail.core.ui.compose.common.annotation.PreviewDevices
import app.k9mail.core.ui.compose.designsystem.atom.Surface
import net.thunderbird.core.common.provider.BrandNameProvider
import net.thunderbird.feature.thundermail.ui.preview.ThundermailPreview

@Composable
@PreviewDevices
internal fun TbOnboardingMigrationScreenPreview() {
    ThundermailPreview {
        Surface {
            TbOnboardingMigrationScreen(
                onQrCodeScan = {},
                onAddAccount = {},
                onImport = {},
                brandNameProvider = object : BrandNameProvider {
                    override val brandName: String = "Thunderbird"
                },
            )
        }
    }
}
