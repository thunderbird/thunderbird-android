package app.k9mail.core.ui.compose.designsystem

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.sp
import app.k9mail.core.ui.compose.theme.K9Theme
import app.k9mail.core.ui.compose.theme.ThunderbirdTheme
import app.k9mail.core.ui.compose.theme2.MainTheme
import app.k9mail.core.ui.compose.theme2.k9mail.K9MailTheme2

@Composable
fun PreviewWithThemes(
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit,
) {
    Column(
        modifier = modifier,
    ) {
        K9MailTheme2 {
            // TODO remove once design system is migrated to theme2
            K9Theme {
                PreviewSurface {
                    Column {
                        PreviewHeader(themeName = "K9Theme Light")
                        content()
                    }
                }
            }
        }
        K9MailTheme2(darkTheme = true) {
            // TODO remove once design system is migrated to theme2
            K9Theme {
                PreviewSurface {
                    Column {
                        PreviewHeader(themeName = "K9Theme Dark")
                        content()
                    }
                }
            }
        }
    }
}

enum class PreviewThemeType {
    K9MAIL,
    THUNDERBIRD,
}

@Composable
fun PreviewWithTheme(
    themeType: PreviewThemeType = PreviewThemeType.K9MAIL,
    isDarkTheme: Boolean = false,
    content: @Composable () -> Unit,
) {
    when (themeType) {
        PreviewThemeType.K9MAIL -> {
            PreviewWithK9MailTheme(isDarkTheme, content)
        }
        PreviewThemeType.THUNDERBIRD -> {
            PreviewWithThunderbirdTheme(isDarkTheme, content)
        }
    }
}

@Composable
private fun PreviewWithK9MailTheme(
    isDarkTheme: Boolean,
    content: @Composable () -> Unit,
) {
    K9MailTheme2(darkTheme = isDarkTheme) {
        // TODO remove once design system is migrated to theme2
        K9Theme(darkTheme = isDarkTheme) {
            content()
        }
    }
}

@Composable
private fun PreviewWithThunderbirdTheme(
    isDarkTheme: Boolean,
    content: @Composable () -> Unit,
) {
    // TODO replace with Thunderbird theme2 once available
    K9MailTheme2(darkTheme = isDarkTheme) {
        // TODO remove once design system is migrated to theme2
        ThunderbirdTheme(darkTheme = isDarkTheme) {
            content()
        }
    }
}

@Composable
private fun PreviewHeader(
    themeName: String,
) {
    Surface(
        color = MainTheme.colors.primary,
    ) {
        Text(
            text = themeName,
            fontSize = 4.sp,
            modifier = Modifier.padding(
                start = MainTheme.spacings.half,
                end = MainTheme.spacings.half,
            ),
        )
    }
}

@Composable
private fun PreviewSurface(
    content: @Composable () -> Unit,
) {
    Surface(
        color = MainTheme.colors.surface,
        content = content,
    )
}
