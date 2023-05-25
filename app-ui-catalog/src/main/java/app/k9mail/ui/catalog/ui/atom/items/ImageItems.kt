package app.k9mail.ui.catalog.ui.atom.items

import androidx.compose.foundation.Image
import androidx.compose.foundation.lazy.grid.LazyGridScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import app.k9mail.core.ui.compose.theme.MainTheme
import app.k9mail.ui.catalog.ui.common.list.itemDefaultPadding
import app.k9mail.ui.catalog.ui.common.list.sectionHeaderItem

fun LazyGridScope.imageItems() {
    sectionHeaderItem(text = "Images")
    item {
        Image(
            painter = painterResource(id = MainTheme.images.logo),
            contentDescription = "logo",
            modifier = Modifier.itemDefaultPadding(),
        )
    }
}
