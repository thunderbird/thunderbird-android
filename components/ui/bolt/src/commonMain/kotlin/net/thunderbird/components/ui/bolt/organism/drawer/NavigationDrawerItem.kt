package net.thunderbird.components.ui.bolt.organism.drawer

import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.AccountBox
import androidx.compose.material3.NavigationDrawerItem as Material3NavigationDrawerItem
import androidx.compose.material3.NavigationDrawerItemDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import net.thunderbird.components.ui.bolt.PreviewWithThemes
import net.thunderbird.components.ui.bolt.atom.icon.Icon
import net.thunderbird.components.ui.bolt.atom.text.TextLabelLarge

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
    NavigationDrawerItem(
        label = {
            TextLabelLarge(
                text = label,
                overflow = TextOverflow.Ellipsis,
                maxLines = 2,
            )
        },
        selected = selected,
        onClick = onClick,
        modifier = modifier,
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
    NavigationDrawerItem(
        label = {
            TextLabelLarge(
                text = label,
                overflow = TextOverflow.Ellipsis,
                maxLines = 2,
            )
        },
        selected = selected,
        onClick = onClick,
        modifier = modifier,
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

@Composable
@Preview(showBackground = true)
internal fun NavigationDrawerItemSelectedPreview() {
    PreviewWithThemes {
        NavigationDrawerItem(
            label = "DrawerItem",
            selected = true,
            onClick = {},
        )
    }
}

@Composable
@Preview(showBackground = true)
internal fun NavigationDrawerItemUnselectedPreview() {
    PreviewWithThemes {
        NavigationDrawerItem(
            label = "DrawerItem",
            selected = false,
            onClick = {},
        )
    }
}

@Composable
@Preview(showBackground = true)
internal fun NavigationDrawerItemWithIconPreview() {
    PreviewWithThemes {
        NavigationDrawerItem(
            label = "DrawerItem",
            selected = false,
            onClick = {},
            icon = {
                Icon(
                    imageVector = Icons.Outlined.AccountBox,
                )
            },
        )
    }
}

@Composable
@Preview(showBackground = true)
internal fun NavigationDrawerItemWithLabelBadgePreview() {
    PreviewWithThemes {
        NavigationDrawerItem(
            label = "DrawerItem",
            selected = false,
            onClick = {},
            badge = {
                TextLabelLarge(
                    text = "100+",
                )
            },
        )
    }
}
