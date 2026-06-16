package net.thunderbird.components.ui.bolt.organism

import androidx.compose.runtime.Composable
import androidx.compose.ui.tooling.preview.Preview
import net.thunderbird.components.ui.bolt.PreviewWithThemes
import net.thunderbird.components.ui.bolt.atom.button.ButtonIcon
import net.thunderbird.components.ui.bolt.atom.icon.Icons

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
