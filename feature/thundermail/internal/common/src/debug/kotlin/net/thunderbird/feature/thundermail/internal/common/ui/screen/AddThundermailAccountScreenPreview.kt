package net.thunderbird.feature.thundermail.internal.common.ui.screen

import androidx.compose.foundation.background
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.tooling.preview.PreviewLightDark
import app.k9mail.core.ui.compose.common.koin.koinPreview
import app.k9mail.core.ui.compose.designsystem.PreviewWithThemeLightDark
import net.thunderbird.core.ui.compose.theme2.MainTheme
import net.thunderbird.feature.thundermail.ui.BrandBackgroundModifierProvider

@PreviewLightDark
@Composable
private fun Preview() {
    val backgroundColor = if (isSystemInDarkTheme()) Color(color = 0xFF262C40) else Color(color = 0xFFF0F8FF)
    PreviewWithThemeLightDark {
        koinPreview {
            single<BrandBackgroundModifierProvider> {
                BrandBackgroundModifierProvider {
                    Modifier.background(backgroundColor)
                }
            }
        } WithContent {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(backgroundColor)
                    .padding(MainTheme.spacings.quadruple),
            ) {
                AddThundermailAccountScreen(
                    header = {},
                    onScanQrCodeClick = {},
                    onSetupAnotherAccountClick = {},
                    dispatch = { },
                )
            }
        }
    }
}
