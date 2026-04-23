package net.thunderbird.feature.thundermail.ui.preview

import androidx.compose.foundation.background
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import app.k9mail.core.ui.compose.common.koin.koinPreview
import app.k9mail.core.ui.compose.designsystem.PreviewWithTheme
import app.k9mail.core.ui.compose.designsystem.atom.text.TextDisplayMediumAutoResize
import net.thunderbird.core.common.provider.BrandTypographyProvider
import net.thunderbird.feature.thundermail.ui.BrandBackgroundModifierProvider
import net.thunderbird.feature.thundermail.ui.RegisteredTrademarkInjector

@Composable
fun ThundermailPreview(
    appName: String = "Thunderbird",
    content: @Composable () -> Unit,
) {
    val backgroundColor = if (isSystemInDarkTheme()) Color(color = 0xFF262C40) else Color(color = 0xFFF0F8FF)
    koinPreview {
        single<BrandBackgroundModifierProvider> {
            BrandBackgroundModifierProvider {
                Modifier.background(backgroundColor)
            }
        }
        single<BrandTypographyProvider> {
            BrandTypographyProvider {
                TextDisplayMediumAutoResize(text = RegisteredTrademarkInjector.inject(appName))
            }
        }
    } WithContent {
        PreviewWithTheme(content = content)
    }
}
