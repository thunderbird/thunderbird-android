package app.k9mail.core.ui.compose.designsystem.organism.drawer

import androidx.compose.runtime.Composable
import androidx.compose.ui.tooling.preview.Preview
import app.k9mail.core.ui.compose.designsystem.PreviewWithThemes

@Composable
@Preview(showBackground = true)
internal fun NavigationDrawerItemBadgePreview() {
    PreviewWithThemes {
        NavigationDrawerItemBadge(
            label = "100+",
        )
    }
}
