package net.thunderbird.components.ui.catalog.ui.page.atom.items

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.grid.LazyGridScope
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import app.k9mail.core.ui.compose.designsystem.atom.text.TextBodySmall
import kotlin.reflect.full.declaredMemberProperties
import net.thunderbird.core.ui.compose.designsystem.atom.icon.BadgeIcons
import net.thunderbird.core.ui.compose.designsystem.atom.icon.Icon
import net.thunderbird.core.ui.compose.designsystem.atom.icon.Icons
import net.thunderbird.core.ui.compose.theme2.MainTheme
import net.thunderbird.components.ui.catalog.ui.page.common.list.defaultItem
import net.thunderbird.components.ui.catalog.ui.page.common.list.defaultItemPadding
import net.thunderbird.components.ui.catalog.ui.page.common.list.sectionHeaderItem
import net.thunderbird.components.ui.catalog.ui.page.common.list.sectionSubtitleItem

fun LazyGridScope.iconItems() {
    sectionHeaderItem(
        text = "Icons",
    )
    sectionSubtitleItem(text = "Sizes")
    defaultItem {
        IconItem(
            name = "Small",
            imageVector = Icons.Outlined.Info,
            modifier = Modifier.size(MainTheme.sizes.iconSmall),
        )
    }
    defaultItem {
        IconItem(
            name = "Default",
            imageVector = Icons.Outlined.Info,
            modifier = Modifier.size(MainTheme.sizes.icon),
        )
    }
    defaultItem {
        IconItem(
            name = "Large",
            imageVector = Icons.Outlined.Info,
            modifier = Modifier.size(MainTheme.sizes.iconLarge),
        )
    }
    sectionSubtitleItem(text = "DualTone")
    getIconsFor(Icons.DualTone)
    sectionSubtitleItem(text = "Filled")
    getIconsFor(Icons.Filled)
    sectionSubtitleItem(text = "Outlined")
    getIconsFor(Icons.Outlined)

    sectionHeaderItem(text = "Badge Icons")
    sectionSubtitleItem(text = "Filled")
    getIconsFor(BadgeIcons.Filled)
}

private inline fun <reified T : Any> LazyGridScope.getIconsFor(icons: T) {
    for (property in T::class.declaredMemberProperties) {
        if (exclusions.contains(property.name)) {
            continue
        } else if (property.returnType.classifier == ImageVector::class) {
            defaultItem {
                val imageVector = property.get(icons) as ImageVector
                IconItem(
                    name = property.name,
                    imageVector = imageVector,
                )
            }
        }
    }
}

private val exclusions = listOf("getClass")

@Composable
private fun IconItem(
    name: String,
    imageVector: ImageVector,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = Modifier
            .padding(defaultItemPadding()),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(MainTheme.spacings.default),
    ) {
        Icon(
            imageVector = imageVector,
            modifier = modifier,
        )
        TextBodySmall(text = name)
    }
}
