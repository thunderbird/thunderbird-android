package net.thunderbird.core.ui.compose.designsystem.atom.button

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import app.k9mail.core.ui.compose.designsystem.PreviewLightDarkLandscape
import app.k9mail.core.ui.compose.designsystem.PreviewWithThemesLightDark
import app.k9mail.core.ui.compose.designsystem.atom.text.TextLabelLarge
import app.k9mail.core.ui.compose.theme2.MainTheme

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
