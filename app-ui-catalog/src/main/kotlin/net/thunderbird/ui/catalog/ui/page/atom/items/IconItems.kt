package net.thunderbird.ui.catalog.ui.page.atom.items

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.grid.LazyGridScope
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.painterResource
import app.k9mail.core.ui.compose.designsystem.atom.text.TextBodySmall
import app.k9mail.core.ui.compose.theme2.MainTheme
import net.thunderbird.core.ui.compose.designsystem.atom.icon.Icon
import net.thunderbird.core.ui.compose.designsystem.atom.icon.Icons
import net.thunderbird.ui.catalog.ui.page.common.list.defaultItem
import net.thunderbird.ui.catalog.ui.page.common.list.defaultItemPadding
import net.thunderbird.ui.catalog.ui.page.common.list.sectionHeaderItem
import net.thunderbird.ui.catalog.ui.page.common.list.sectionSubtitleItem
import androidx.compose.material3.Icon as Material3Icon
import app.k9mail.core.ui.legacy.designsystem.atom.icon.Icons as LegacyIcons

fun LazyGridScope.iconItems() {
    sectionHeaderItem(
        text = "Compose Icons",
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
    sectionSubtitleItem(text = "Filled")
    getIconsFor(Icons.Filled)
    sectionSubtitleItem(text = "Outlined")
    getIconsFor(Icons.Outlined)
    sectionHeaderItem(
        text = "Legacy Icons",
    )
    sectionSubtitleItem(text = "Filled")
    getLegacyIconsFor(LegacyIcons.Filled)
    sectionSubtitleItem(text = "Outlined")
    getLegacyIconsFor(LegacyIcons.Outlined)
}

private inline fun <reified T> LazyGridScope.getIconsFor(icons: T) {
    for (method in T::class.java.methods) {
        if (exclusions.contains(method.name)) {
            continue
        } else if (method.name.startsWith("get")) {
            defaultItem {
                method.isAccessible = true
                val imageVector = method.invoke(icons) as ImageVector
                IconItem(
                    name = method.name.replaceFirst("get", ""),
                    imageVector = imageVector,
                )
            }
        }
    }
}

private inline fun <reified T> LazyGridScope.getLegacyIconsFor(icons: T) {
    for (method in T::class.java.methods) {
        if (exclusions.contains(method.name)) {
            continue
        } else if (method.name.startsWith("get")) {
            defaultItem {
                method.isAccessible = true
                val drawableResId = method.invoke(icons) as Int
                LegacyIconItem(
                    name = method.name.replaceFirst("get", ""),
                    drawableResId = drawableResId,
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

@Composable
private fun LegacyIconItem(
    name: String,
    drawableResId: Int,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = Modifier
            .padding(defaultItemPadding())
            .then(modifier),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(MainTheme.spacings.default),
    ) {
        Material3Icon(
            painter = painterResource(id = drawableResId),
            contentDescription = null,
        )
        TextBodySmall(text = name)
    }
}
