package app.k9mail.feature.onboarding.migration.thunderbird

import androidx.compose.runtime.Composable
import app.k9mail.core.common.provider.AppNameProvider
import app.k9mail.core.ui.compose.common.annotation.PreviewDevices
import app.k9mail.core.ui.compose.designsystem.atom.Surface
import app.k9mail.core.ui.compose.theme2.thunderbird.ThunderbirdTheme2

@Composable
@PreviewDevices
internal fun TbOnboardingMigrationScreenPreview() {
    ThunderbirdTheme2 {
        Surface {
            TbOnboardingMigrationScreen(
                onQrCodeScanClick = {},
                onAddAccountClick = {},
                appNameProvider = object : AppNameProvider {
                    override val appName: String = "Thunderbird"
                },
            )
        }
    }
}
