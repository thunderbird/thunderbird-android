package net.thunderbird.components.ui.bolt

import androidx.compose.runtime.Composable
import net.thunderbird.components.ui.bolt.theme.thunderbird.ThunderbirdTheme2
import net.thunderbird.components.ui.testing.ComposeUiTestScope

/**
 * Sets the content of the Compose UI test scope with the Thunderbird theme.
 *
 * TODO: Move to bolt testing once migrated.
 */
fun ComposeUiTestScope.setContentWithTheme(
    content: @Composable () -> Unit,
) {
    setContent {
        ThunderbirdTheme2 {
            content()
        }
    }
}
