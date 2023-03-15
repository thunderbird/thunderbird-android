package app.k9mail.ui.catalog.items

import androidx.compose.foundation.Image
import androidx.compose.foundation.lazy.grid.LazyGridScope
import androidx.compose.ui.res.painterResource
import app.k9mail.core.ui.compose.theme.MainTheme

fun LazyGridScope.imageItems() {
    sectionHeaderItem(text = "Images")
    item {
        Image(painter = painterResource(id = MainTheme.images.logo), contentDescription = "logo")
    }
}
