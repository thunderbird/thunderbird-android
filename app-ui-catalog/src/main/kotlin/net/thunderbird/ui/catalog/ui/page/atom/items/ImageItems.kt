package net.thunderbird.ui.catalog.ui.page.atom.items

import androidx.compose.foundation.Image
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.grid.LazyGridScope
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import app.k9mail.core.ui.compose.designsystem.atom.icon.Icon
import app.k9mail.core.ui.compose.designsystem.atom.icon.Icons
import app.k9mail.core.ui.compose.designsystem.atom.image.FixedScaleImage
import app.k9mail.core.ui.compose.designsystem.atom.image.RemoteImage
import app.k9mail.core.ui.compose.designsystem.atom.text.TextBodySmall
import app.k9mail.core.ui.compose.theme2.MainTheme
import net.thunderbird.ui.catalog.ui.page.common.list.defaultItem
import net.thunderbird.ui.catalog.ui.page.common.list.defaultItemPadding
import net.thunderbird.ui.catalog.ui.page.common.list.fullSpanItem
import net.thunderbird.ui.catalog.ui.page.common.list.sectionHeaderItem
import net.thunderbird.ui.catalog.ui.page.common.list.sectionSubtitleItem

fun LazyGridScope.imageItems() {
    sectionHeaderItem(text = "Images")
    defaultItem {
        Image(
            painter = painterResource(id = MainTheme.images.logo),
            contentDescription = "logo",
            modifier = Modifier.padding(defaultItemPadding()),
        )
    }

    sectionHeaderItem(text = "Images with fixed scale")
    fixedScaleImagesCropped()
    fixedScaleImagesOverflow()
    fixedScaleImagesAlignment()

    sectionHeaderItem(text = "Remote images")
    remoteImage(
        url = "https://www.thunderbird.net/media/img/thunderbird/favicon-196.png",
        description = "Weblink",
    )
}

private fun LazyGridScope.fixedScaleImagesCropped() {
    sectionSubtitleItem(text = "Images are cropped by parent container size")
    fullSpanItem {
        FixedScaleImageView(
            description = "Small container",
            width = 40.dp,
            height = 40.dp,
        )
    }
    fullSpanItem {
        FixedScaleImageView(
            description = "Small horizontal container",
            width = 40.dp,
            height = 200.dp,
        )
    }
    fullSpanItem {
        FixedScaleImageView(
            description = "Small vertical container",
            width = 200.dp,
            height = 40.dp,
        )
    }
}

private fun LazyGridScope.fixedScaleImagesOverflow() {
    sectionSubtitleItem(text = "Images overflow parent container size")
    fullSpanItem {
        FixedScaleImageView(
            description = "Small container",
            width = 40.dp,
            height = 40.dp,
            allowOverflow = true,
        )
    }
    fullSpanItem {
        FixedScaleImageView(
            description = "Small horizontal container",
            width = 40.dp,
            height = 200.dp,
            allowOverflow = true,
        )
    }
    fullSpanItem {
        FixedScaleImageView(
            description = "Small vertical container",
            width = 200.dp,
            height = 40.dp,
            allowOverflow = true,
        )
    }
}

private fun LazyGridScope.fixedScaleImagesAlignment() {
    sectionSubtitleItem(text = "Images with different alignments")
    fullSpanItem {
        FixedScaleImageView(
            description = "Center",
            width = 200.dp,
            height = 200.dp,
        )
    }
    fullSpanItem {
        FixedScaleImageView(
            description = "Top center",
            width = 200.dp,
            height = 200.dp,
            alignment = Alignment.TopCenter,
        )
    }
    fullSpanItem {
        FixedScaleImageView(
            description = "Bottom center",
            width = 200.dp,
            height = 200.dp,
            alignment = Alignment.BottomCenter,
        )
    }
}

@Composable
private fun FixedScaleImageView(
    description: String,
    width: Dp,
    height: Dp,
    alignment: Alignment = Alignment.Center,
    allowOverflow: Boolean = false,
) {
    Column(
        modifier = Modifier.padding(defaultItemPadding()),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Box(
            modifier = Modifier
                .width(width)
                .height(height)
                .border(1.dp, MainTheme.colors.primary, MainTheme.shapes.small),
        ) {
            FixedScaleImage(
                id = MainTheme.images.logo,
                alignment = alignment,
                allowOverflow = allowOverflow,
            )
        }
        TextBodySmall(text = description)
    }
}

private fun LazyGridScope.remoteImage(
    url: String,
    description: String,
) {
    fullSpanItem {
        Column(
            modifier = Modifier.padding(defaultItemPadding()),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            RemoteImage(
                url = url,
                modifier = Modifier.size(MainTheme.sizes.large),
                placeholder = {
                    Icon(
                        imageVector = Icons.Filled.Star,
                    )
                },
            )
            TextBodySmall(text = description)
        }
    }
}
