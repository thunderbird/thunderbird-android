package app.k9mail.core.ui.compose.designsystem.organism.drawer

import androidx.compose.foundation.layout.padding
import androidx.compose.material3.NavigationDrawerItemDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import app.k9mail.core.ui.compose.designsystem.atom.text.TextTitleSmall
import app.k9mail.core.ui.compose.theme2.MainTheme

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
