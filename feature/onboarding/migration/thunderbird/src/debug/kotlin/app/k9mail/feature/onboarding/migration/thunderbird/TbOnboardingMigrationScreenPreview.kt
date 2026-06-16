package app.k9mail.feature.onboarding.migration.thunderbird

import androidx.compose.runtime.Composable
import net.thunderbird.components.ui.bolt.atom.Surface
import net.thunderbird.components.ui.bolt.common.annotation.PreviewDevices
import net.thunderbird.components.ui.bolt.theme.thunderbird.ThunderbirdTheme2
import net.thunderbird.core.common.provider.BrandNameProvider

@Composable
@PreviewDevices
internal fun TbOnboardingMigrationScreenPreview() {
    ThunderbirdTheme2 {
        Surface {
            TbOnboardingMigrationScreen(
                onThundermailClick = {},
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
