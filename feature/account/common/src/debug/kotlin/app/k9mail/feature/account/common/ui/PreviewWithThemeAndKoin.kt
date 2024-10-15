package app.k9mail.feature.account.common.ui

import androidx.compose.runtime.Composable
import app.k9mail.core.common.provider.BrandNameProvider
import app.k9mail.core.ui.compose.common.koin.koinPreview
import app.k9mail.core.ui.compose.designsystem.PreviewWithTheme

@Composable
fun PreviewWithThemeAndKoin(content: @Composable () -> Unit) {
    koinPreview {
        single<BrandNameProvider> {
            object : BrandNameProvider {
                override val brandName = "BrandName"
            }
        }
    } WithContent {
        PreviewWithTheme {
            content()
        }
    }
}
