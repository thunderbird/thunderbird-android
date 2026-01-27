package net.thunderbird.feature.account.avatar.ui

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import app.k9mail.core.ui.compose.designsystem.atom.text.TextTitleLarge
import app.k9mail.core.ui.compose.designsystem.atom.text.TextTitleMedium

/**
 * Composable function to display a monogram avatar.
 *
 * @param monogram The monogram text to display.
 * @param color The background color of the avatar.
 * @param size The size of the avatar.
 * @param modifier The modifier to be applied to the avatar.
 */
@Composable
internal fun AvatarMonogram(
    monogram: String,
    color: Color,
    size: AvatarSize,
    modifier: Modifier = Modifier,
) {
    when (size) {
        AvatarSize.MEDIUM -> {
            TextTitleMedium(
                text = monogram.uppercase(),
                color = color,
                modifier = modifier,
            )
        }

        AvatarSize.LARGE -> {
            TextTitleLarge(
                text = monogram.uppercase(),
                color = color,
                modifier = modifier,
            )
        }
    }
}
