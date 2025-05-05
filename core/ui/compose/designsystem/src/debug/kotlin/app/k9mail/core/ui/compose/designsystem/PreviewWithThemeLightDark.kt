package app.k9mail.core.ui.compose.designsystem
import androidx.compose.foundation.background
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.movableContentOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import app.k9mail.core.ui.compose.theme2.MainTheme

@Composable
fun PreviewWithThemeLightDark(
    useRow: Boolean = false,
    useScrim: Boolean = false,
    scrimAlpha: Float = 0.8f,
    scrimPadding: PaddingValues = PaddingValues(24.dp),
    arrangement: Arrangement.HorizontalOrVertical = Arrangement.spacedBy(24.dp),
    content: @Composable () -> Unit,
) {
    val movableContent = remember {
        movableContentOf {
            PreviewWithTheme(
                themeType = PreviewThemeType.THUNDERBIRD,
                isDarkTheme = isSystemInDarkTheme(),
            ) {
                PreviewSurface {
                    if (useScrim) {
                        Box(
                            modifier = Modifier
                                .background(MainTheme.colors.scrim.copy(alpha = scrimAlpha))
                                .padding(scrimPadding),
                        ) {
                            content()
                        }
                    } else {
                        content()
                    }
                    PreviewHeader(themeName = PreviewThemeType.THUNDERBIRD.name)
                }
            }
            PreviewWithTheme(
                themeType = PreviewThemeType.K9MAIL,
                isDarkTheme = isSystemInDarkTheme(),
            ) {
                PreviewSurface {
                    if (useScrim) {
                        Box(
                            modifier = Modifier
                                .background(MainTheme.colors.scrim.copy(alpha = scrimAlpha))
                                .padding(scrimPadding),
                        ) {
                            content()
                        }
                    } else {
                        content()
                    }
                    PreviewHeader(themeName = PreviewThemeType.K9MAIL.name)
                }
            }
        }
    }

    if (useRow) {
        Row(
            horizontalArrangement = arrangement,
        ) {
            movableContent()
        }
    } else {
        Column(
            verticalArrangement = arrangement,
        ) {
            movableContent()
        }
    }
}
