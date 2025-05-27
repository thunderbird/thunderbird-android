package app.k9mail.feature.onboarding.migration.thunderbird

import androidx.compose.runtime.Composable
import app.k9mail.core.ui.compose.common.annotation.PreviewDevices
import app.k9mail.core.ui.compose.designsystem.atom.Surface
import app.k9mail.core.ui.compose.theme2.thunderbird.ThunderbirdTheme2
import net.thunderbird.core.common.provider.BrandNameProvider

@Composable
@PreviewDevices
internal fun TbOnboardingMigrationScreenPreview() {
    ThunderbirdTheme2 {
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
