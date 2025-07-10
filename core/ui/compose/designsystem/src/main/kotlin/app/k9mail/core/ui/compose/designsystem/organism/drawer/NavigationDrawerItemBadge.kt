package app.k9mail.core.ui.compose.designsystem.organism.drawer

import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import app.k9mail.core.ui.compose.designsystem.atom.icon.Icon
import app.k9mail.core.ui.compose.designsystem.atom.text.TextLabelLarge
import app.k9mail.core.ui.compose.theme2.MainTheme

/**
 * A badge for a navigation drawer item with an optional icon.
 *
 * @param label The label to display.
 * @param modifier The modifier to apply.
 * @param imageVector The image vector to display (optional).
 */
@Composable
fun NavigationDrawerItemBadge(
    label: String,
    modifier: Modifier = Modifier,
    imageVector: ImageVector? = null,
) {
    Row(
        modifier = modifier,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        TextLabelLarge(
            text = label,
        )
        if (imageVector != null) {
            Icon(
                imageVector = imageVector,
                modifier = Modifier.size(MainTheme.sizes.iconSmall)
                    .padding(start = MainTheme.spacings.quarter),
            )
        }
    }
}
