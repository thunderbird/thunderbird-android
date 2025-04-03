package app.k9mail.core.ui.compose.designsystem.organism.drawer

import androidx.compose.foundation.layout.padding
import androidx.compose.material3.NavigationDrawerItemDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.style.TextOverflow
import app.k9mail.core.ui.compose.designsystem.atom.text.TextLabelLarge
import androidx.compose.material3.NavigationDrawerItem as Material3NavigationDrawerItem

/**
 * A navigation drawer item that can be used in a navigation drawer.
 *
 * @param label The content of the label to be displayed in the item as a String.
 * @param selected Whether this item is selected.
 * @param onClick The callback to be invoked when this item is clicked.
 * @param modifier The [Modifier] to be applied to this item.
 * @param icon An optional composable that represents an icon for this item.
 * @param badge An optional composable that represents a badge for this item.
 */
@Composable
fun NavigationDrawerItem(
    label: String,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    icon: (@Composable () -> Unit)? = null,
    badge: (@Composable () -> Unit)? = null,
) {
    Material3NavigationDrawerItem(
        label = {
            TextLabelLarge(
                text = label,
                overflow = TextOverflow.Ellipsis,
                maxLines = 2,
            )
        },
        selected = selected,
        onClick = onClick,
        modifier = Modifier
            .padding(NavigationDrawerItemDefaults.ItemPadding)
            .then(modifier),
        icon = icon,
        badge = badge,
    )
}

/**
 * A navigation drawer item that can be used in a navigation drawer.
 *
 * @param label The content of the label to be displayed in the item as AnnotatedString.
 * @param selected Whether this item is selected.
 * @param onClick The callback to be invoked when this item is clicked.
 * @param modifier The [Modifier] to be applied to this item.
 * @param icon An optional composable that represents an icon for this item.
 * @param badge An optional composable that represents a badge for this item.
 */
@Composable
fun NavigationDrawerItem(
    label: AnnotatedString,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    icon: (@Composable () -> Unit)? = null,
    badge: (@Composable () -> Unit)? = null,
) {
    Material3NavigationDrawerItem(
        label = {
            TextLabelLarge(
                text = label,
                overflow = TextOverflow.Ellipsis,
                maxLines = 2,
            )
        },
        selected = selected,
        onClick = onClick,
        modifier = Modifier
            .padding(NavigationDrawerItemDefaults.ItemPadding)
            .then(modifier),
        icon = icon,
        badge = badge,
    )
}

/**
 * A navigation drawer item that can be used in a navigation drawer.
 *
 * @param label The content of the label to be displayed in the item.
 * @param selected Whether this item is selected.
 * @param onClick The callback to be invoked when this item is clicked.
 * @param modifier The [Modifier] to be applied to this item.
 * @param icon An optional composable that represents an icon for this item.
 * @param badge An optional composable that represents a badge for this item.
 */
@Composable
fun NavigationDrawerItem(
    label: @Composable () -> Unit,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    icon: (@Composable () -> Unit)? = null,
    badge: (@Composable () -> Unit)? = null,
) {
    Material3NavigationDrawerItem(
        label = label,
        selected = selected,
        onClick = onClick,
        modifier = Modifier
            .padding(NavigationDrawerItemDefaults.ItemPadding)
            .then(modifier),
        icon = icon,
        badge = badge,
    )
}
