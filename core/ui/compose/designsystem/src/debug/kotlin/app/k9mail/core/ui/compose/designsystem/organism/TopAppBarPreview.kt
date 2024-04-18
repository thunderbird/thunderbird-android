package app.k9mail.core.ui.compose.designsystem.organism

import androidx.compose.runtime.Composable
import androidx.compose.ui.tooling.preview.Preview
import app.k9mail.core.ui.compose.designsystem.PreviewWithThemes
import app.k9mail.core.ui.compose.designsystem.atom.button.ButtonIcon
import app.k9mail.core.ui.compose.designsystem.atom.icon.Icons

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
                    imageVector = Icons.Outlined.info,
                )
                ButtonIcon(
                    onClick = {},
                    imageVector = Icons.Outlined.check,
                )
                ButtonIcon(
                    onClick = {},
                    imageVector = Icons.Outlined.celebration,
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
