package net.thunderbird.components.ui.bolt.organism.drawer

import androidx.compose.foundation.layout.padding
import androidx.compose.material3.NavigationDrawerItemDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import net.thunderbird.components.ui.bolt.atom.text.TextTitleSmall
import net.thunderbird.components.ui.bolt.theme.MainTheme

@Composable
fun NavigationDrawerHeadline(
    title: String,
    modifier: Modifier = Modifier,
) {
    NavigationDrawerItemLayout(
        modifier = modifier,
    ) {
        TextTitleSmall(
            text = title,
            color = MainTheme.colors.primary,
            modifier = Modifier
                .padding(NavigationDrawerItemDefaults.ItemPadding)
                .padding(
                    top = MainTheme.spacings.triple,
                    bottom = MainTheme.spacings.double,
                )
                .then(modifier),
        )
    }
}
