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

/**
 * A Composable function that displays a preview of the content in both Thunderbird and K-9 Mail themes.
 *
 * It uses the current system theme (light or dark) for both previews.
 *
 * @param modifier The modifier to be applied to the layout.
 * @param useRow Whether to display the previews in a row or column. Defaults to `false` (column).
 * @param useScrim Whether to display a scrim behind the content. Defaults to `false`.
 * @param scrimAlpha The alpha value for the scrim. Defaults to `0.8f`.
 * @param scrimPadding The padding for the scrim. Defaults to `MainTheme.spacings.triple`.
 * @param arrangement The arrangement for the previews. Defaults to `Arrangement.spacedBy(MainTheme.spacings.triple)`.
 * @param content The content to be displayed in the previews.
 *
 * @see app.k9mail.core.ui.compose.theme2.default.defaultThemeSpacings for MainTheme.spacings
 */
@Composable
fun PreviewWithThemesLightDark(
    modifier: Modifier = Modifier,
    useRow: Boolean = false,
    useScrim: Boolean = false,
    scrimAlpha: Float = 0.8f,
    scrimPadding: PaddingValues = PaddingValues(24.dp),
    arrangement: Arrangement.HorizontalOrVertical = Arrangement.spacedBy(24.dp),
    content: @Composable () -> Unit,
) {
    val movableContent = remember {
        movableContentOf {
            PreviewWithThemeLightDark(
                themeType = PreviewThemeType.THUNDERBIRD,
                useScrim = useScrim,
                scrimAlpha = scrimAlpha,
                scrimPadding = scrimPadding,
                content = content,
            )
            PreviewWithThemeLightDark(
                themeType = PreviewThemeType.K9MAIL,
                useScrim = useScrim,
                scrimAlpha = scrimAlpha,
                scrimPadding = scrimPadding,
                content = content,
            )
        }
    }

    if (useRow) {
        Row(
            horizontalArrangement = arrangement,
            modifier = modifier,
        ) {
            movableContent()
        }
    } else {
        Column(
            verticalArrangement = arrangement,
            modifier = modifier,
        ) {
            movableContent()
        }
    }
}

@Suppress("ModifierMissing")
@Composable
fun PreviewWithThemeLightDark(
    themeType: PreviewThemeType = PreviewThemeType.THUNDERBIRD,
    useScrim: Boolean = false,
    scrimAlpha: Float = 0f,
    scrimPadding: PaddingValues = PaddingValues(0.dp),
    content: @Composable (() -> Unit),
) {
    val movableContent = remember { movableContentOf { content() } }
    PreviewWithTheme(
        themeType = themeType,
        isDarkTheme = isSystemInDarkTheme(),
    ) {
        PreviewSurface {
            if (useScrim) {
                Box(
                    modifier = Modifier
                        .background(MainTheme.colors.scrim.copy(alpha = scrimAlpha))
                        .padding(scrimPadding),
                ) {
                    movableContent()
                }
            } else {
                movableContent()
            }
            PreviewHeader(themeName = themeType.name)
        }
    }
}
