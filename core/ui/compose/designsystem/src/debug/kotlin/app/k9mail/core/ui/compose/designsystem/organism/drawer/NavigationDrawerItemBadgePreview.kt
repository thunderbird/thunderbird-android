package app.k9mail.core.ui.compose.designsystem.organism.drawer

import androidx.compose.runtime.Composable
import androidx.compose.ui.tooling.preview.Preview
import app.k9mail.core.ui.compose.designsystem.PreviewWithThemes
import app.k9mail.core.ui.compose.designsystem.atom.icon.Icons

@Composable
@Preview(showBackground = true)
internal fun NavigationDrawerItemBadgePreview() {
    PreviewWithThemes {
        NavigationDrawerItemBadge(
            label = "99+",
        )
    }
}

@Composable
@Preview(showBackground = true)
internal fun NavigationDrawerItemBadgeWithIconPreview() {
    PreviewWithThemes {
        NavigationDrawerItemBadge(
            label = "99+",
            imageVector = Icons.Outlined.Info,
        )
    }
}
