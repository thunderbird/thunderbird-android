package net.thunderbird.components.ui.bolt.organism

import androidx.compose.runtime.Composable
import androidx.compose.ui.tooling.preview.Preview
import net.thunderbird.components.ui.bolt.PreviewWithThemes
import net.thunderbird.components.ui.bolt.atom.button.ButtonIcon
import net.thunderbird.components.ui.bolt.atom.icon.Icons

@Composable
@Preview(showBackground = true)
internal fun TopAppBarPreview() {
    PreviewWithThemes {
        TopAppBar(
            title = "Title",
        )
    }
}

@Composable
@Preview(showBackground = true)
internal fun TopAppBarWithActionsPreview() {
    PreviewWithThemes {
        TopAppBar(
            title = "Title",
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
internal fun TopAppBarWithMenuButtonPreview() {
    PreviewWithThemes {
        TopAppBarWithMenuButton(
            title = "Title",
            onMenuClick = {},
        )
    }
}

@Composable
@Preview(showBackground = true)
internal fun TopAppBarWithBackButtonPreview() {
    PreviewWithThemes {
        TopAppBarWithBackButton(
            title = "Title",
            onBackClick = {},
        )
    }
}
