package app.k9mail.core.ui.compose.designsystem.organism.drawer

import androidx.compose.foundation.layout.padding
import androidx.compose.material3.NavigationDrawerItemDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.AnnotatedString
import app.k9mail.core.ui.compose.designsystem.atom.text.TextLabelLarge
import androidx.compose.material3.NavigationDrawerItem as Material3NavigationDrawerItem

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
        label = { TextLabelLarge(text = label, maxLines = 2) },
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
fun NavigationDrawerItem(
    label: AnnotatedString,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    icon: (@Composable () -> Unit)? = null,
    badge: (@Composable () -> Unit)? = null,
) {
    Material3NavigationDrawerItem(
        label = { TextLabelLarge(text = label, maxLines = 2) },
        selected = selected,
        onClick = onClick,
        modifier = Modifier
            .padding(NavigationDrawerItemDefaults.ItemPadding)
            .then(modifier),
        icon = icon,
        badge = badge,
    )
}
