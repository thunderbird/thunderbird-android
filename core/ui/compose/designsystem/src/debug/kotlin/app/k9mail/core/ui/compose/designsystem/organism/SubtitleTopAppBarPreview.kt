package app.k9mail.core.ui.compose.designsystem.organism

import androidx.compose.runtime.Composable
import androidx.compose.ui.tooling.preview.Preview
import app.k9mail.core.ui.compose.designsystem.PreviewWithThemes
import app.k9mail.core.ui.compose.designsystem.atom.button.ButtonIcon
import net.thunderbird.core.ui.compose.designsystem.atom.icon.Icons

@Composable
@Preview(showBackground = true)
internal fun SubtitleTopAppBarPreview() {
    PreviewWithThemes {
        SubtitleTopAppBar(
            title = "Title",
            subtitle = "Subtitle",
        )
    }
}

@Composable
@Preview(showBackground = true)
internal fun SubtitleTopAppBarWithLongSubtitlePreview() {
    PreviewWithThemes {
        SubtitleTopAppBar(
            title = "Title",
            subtitle = "Lorem ipsum dolor sit amet, consectetur adipiscing elit. " +
                "Sed do eiusmod tempor incididunt ut labore et dolore magna aliqua.",
        )
    }
}

@Composable
@Preview(showBackground = true)
internal fun SubtitleTopAppBarWithActionsPreview() {
    PreviewWithThemes {
        SubtitleTopAppBar(
            title = "Title",
            subtitle = "Subtitle",
            actions = {
                ButtonIcon(
                    onClick = {},
                    imageVector = Icons.Outlined.Info,
                )
                ButtonIcon(
                    onClick = {},
                    imageVector = Icons.Outlined.Check,
                )
                ButtonIcon(
                    onClick = {},
                    imageVector = Icons.Outlined.Visibility,
                )
            },
        )
    }
}

@Composable
@Preview(showBackground = true)
internal fun SubtitleTopAppBarWithMenuButtonPreview() {
    PreviewWithThemes {
        SubtitleTopAppBarWithMenuButton(
            title = "Title",
            subtitle = "Subtitle",
            onMenuClick = {},
        )
    }
}

@Composable
@Preview(showBackground = true)
internal fun SubtitleTopAppBarWithBackButtonPreview() {
    PreviewWithThemes {
        SubtitleTopAppBarWithBackButton(
            title = "Title",
            subtitle = "Subtitle",
            onBackClick = {},
        )
    }
}
