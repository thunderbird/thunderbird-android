package net.thunderbird.feature.settings.import.ui

import androidx.compose.runtime.Composable
import app.k9mail.core.ui.compose.common.annotation.PreviewDevices
import app.k9mail.core.ui.compose.designsystem.atom.Surface
import net.thunderbird.core.common.provider.BrandNameProvider
import net.thunderbird.feature.thundermail.ui.preview.ThundermailPreview

@Composable
@PreviewDevices
private fun ImportAccountScreenPreview() {
    ThundermailPreview {
        Surface {
            ImportAccountScreen(
                onQrCodeScan = {},
                onImport = {},
                onBack = {},
                brandNameProvider = object : BrandNameProvider {
                    override val brandName: String = "Thunderbird"
                },
            )
        }
    }
}
