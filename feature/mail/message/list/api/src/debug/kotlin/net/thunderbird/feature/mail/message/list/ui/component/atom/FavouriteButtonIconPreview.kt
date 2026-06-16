package net.thunderbird.feature.mail.message.list.ui.component.atom

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import net.thunderbird.components.ui.bolt.PreviewLightDarkLandscape
import net.thunderbird.components.ui.bolt.PreviewWithThemesLightDark
import net.thunderbird.components.ui.bolt.atom.text.TextLabelLarge
import net.thunderbird.components.ui.bolt.theme.MainTheme

@PreviewLightDarkLandscape
@Composable
private fun FavouriteButtonIconPreview() {
    PreviewWithThemesLightDark(useRow = true) {
        Column(
            verticalArrangement = Arrangement.spacedBy(MainTheme.spacings.default),
            modifier = Modifier.padding(
                vertical = MainTheme.spacings.quadruple,
                horizontal = MainTheme.spacings.default,
            ),
        ) {
            TextLabelLarge(text = "Favourite = false")
            FavouriteButtonIcon(favourite = false, onFavouriteChange = {})
            Spacer(modifier = Modifier.height(MainTheme.spacings.default))
            TextLabelLarge(text = "Favourite = true")
            FavouriteButtonIcon(favourite = true, onFavouriteChange = {})
        }
    }
}
