package net.thunderbird.components.ui.bolt

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.sp
import net.thunderbird.components.ui.bolt.theme.BoltTheme
import net.thunderbird.components.ui.bolt.theme.k9mail.K9MailBoltTheme
import net.thunderbird.components.ui.bolt.theme.thunderbird.ThunderbirdBoltTheme

@Composable
fun PreviewWithThemes(
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit,
) {
    Column(
        modifier = modifier,
    ) {
        K9MailBoltTheme {
            PreviewSurface {
                Column {
                    PreviewHeader(themeName = "K9Theme Light")
                    content()
                }
            }
        }
        K9MailBoltTheme(darkTheme = true) {
            PreviewSurface {
                Column {
                    PreviewHeader(themeName = "K9Theme Dark")
                    content()
                }
            }
        }
        ThunderbirdBoltTheme {
            PreviewSurface {
                Column {
                    PreviewHeader(themeName = "ThunderbirdTheme Light")
                    content()
                }
            }
        }
        ThunderbirdBoltTheme(darkTheme = true) {
            PreviewSurface {
                Column {
                    PreviewHeader(themeName = "ThunderbirdTheme Dark")
                    content()
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
    themeType: PreviewThemeType = PreviewThemeType.THUNDERBIRD,
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
    K9MailBoltTheme(
        darkTheme = isDarkTheme,
        content = content,
    )
}

@Composable
private fun PreviewWithThunderbirdTheme(
    isDarkTheme: Boolean,
    content: @Composable () -> Unit,
) {
    ThunderbirdBoltTheme(
        darkTheme = isDarkTheme,
        content = content,
    )
}

@Composable
internal fun PreviewHeader(
    themeName: String,
) {
    Surface(
        color = BoltTheme.colors.primary,
    ) {
        Text(
            text = themeName,
            fontSize = 4.sp,
            modifier = Modifier.padding(
                start = BoltTheme.spacings.half,
                end = BoltTheme.spacings.half,
            ),
        )
    }
}

@Composable
internal fun PreviewSurface(
    content: @Composable () -> Unit,
) {
    Surface(
        color = BoltTheme.colors.surface,
        content = content,
    )
}
