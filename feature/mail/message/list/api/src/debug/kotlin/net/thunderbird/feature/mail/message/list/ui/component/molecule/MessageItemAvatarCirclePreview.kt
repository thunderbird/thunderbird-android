package net.thunderbird.feature.mail.message.list.ui.component.molecule

import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.tooling.preview.AndroidUiModes.UI_MODE_NIGHT_YES
import androidx.compose.ui.tooling.preview.AndroidUiModes.UI_MODE_TYPE_NORMAL
import androidx.compose.ui.tooling.preview.Preview
import app.k9mail.core.ui.compose.designsystem.PreviewWithThemeLightDark
import app.k9mail.core.ui.compose.theme2.MainTheme
import net.thunderbird.core.ui.compose.designsystem.atom.icon.Icons
import net.thunderbird.feature.mail.message.list.ui.state.Avatar

@Composable
@Preview(
    name = "Light",
    device = "spec:width=512dp,height=1024dp,dpi=420",
)
@Preview(
    name = "Dark",
    device = "spec:width=512dp,height=1024dp,dpi=420",
    uiMode = UI_MODE_NIGHT_YES or UI_MODE_TYPE_NORMAL,
)
private fun MessageItemAvatarCirclePreview() {
    PreviewWithThemeLightDark {
        FlowRow(
            modifier = Modifier.padding(top = MainTheme.spacings.triple),
        ) {
            MessageItemAvatarCircle(
                avatar = Avatar.Monogram("AB"),
                colors = MessageItemAvatarCircleDefaults.colorsFrom(MainTheme.colors.primary),
                onClick = {},
            )
            MessageItemAvatarCircle(
                avatar = Avatar.Image("https://picsum.photos/250/250"),
                colors = MessageItemAvatarCircleDefaults.colorsFrom(MainTheme.colors.secondary),
                onClick = {},
            )
            MessageItemAvatarCircle(
                avatar = Avatar.Icon(Icons.Outlined.Flower),
                colors = MessageItemAvatarCircleDefaults.colorsFrom(Color.Green),
                onClick = {},
            )
            Spacer(modifier = Modifier.fillMaxWidth())
            repeat(HUE_SLICES * HUE_RINGS) {
                MessageItemAvatarCircle(
                    avatar = Avatar.Monogram(randomizeMonogramInitials(seed = it)),
                    colors = MessageItemAvatarCircleDefaults.colorsFrom(Color.randomize(it)),
                    onClick = {},
                )
            }
        }
    }
}

private const val HUE_FULL_ANGLE = 360f
private const val HUE_SATURATION = 1.0f
private const val HUE_SLICES = 12
private const val HUE_RINGS = 7
private const val HUE_LIGHTNESS_MIN = 0.2f
private const val HUE_LIGHTNESS_MAX = 0.7f

private const val MONOGRAM_FIRST_LETTER_MULTIPLIER = 7
private const val MONOGRAM_SECOND_LETTER_MULTIPLIER = 13
private const val ALPHABET_SIZE = 26

private fun Color.Companion.randomize(seed: Int): Color {
    val hueIndex = seed % HUE_SLICES
    val ringIndex = seed / HUE_SLICES
    return Color.hsl(
        hue = (hueIndex * HUE_FULL_ANGLE / HUE_SLICES) % HUE_FULL_ANGLE,
        saturation = HUE_SATURATION,
        lightness = HUE_LIGHTNESS_MAX - (HUE_LIGHTNESS_MAX - HUE_LIGHTNESS_MIN) * ringIndex / (HUE_RINGS - 1),
    )
}

private fun randomizeMonogramInitials(seed: Int) =
    "${('A' + (seed * MONOGRAM_FIRST_LETTER_MULTIPLIER) % ALPHABET_SIZE)}" +
        "${('A' + (seed * MONOGRAM_SECOND_LETTER_MULTIPLIER) % ALPHABET_SIZE)}"

