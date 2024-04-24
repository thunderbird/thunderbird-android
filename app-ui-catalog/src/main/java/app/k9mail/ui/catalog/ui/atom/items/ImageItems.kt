package app.k9mail.ui.catalog.ui.atom.items

import androidx.compose.foundation.Image
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.grid.LazyGridScope
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import app.k9mail.core.ui.compose.designsystem.atom.image.FixedScaleImage
import app.k9mail.core.ui.compose.designsystem.atom.text.TextBodySmall
import app.k9mail.core.ui.compose.theme2.MainTheme
import app.k9mail.ui.catalog.ui.common.list.itemDefaultPadding
import app.k9mail.ui.catalog.ui.common.list.sectionHeaderItem
import app.k9mail.ui.catalog.ui.common.list.sectionSubtitleItem

fun LazyGridScope.imageItems() {
    sectionHeaderItem(text = "Images")
    item {
        Image(
            painter = painterResource(id = MainTheme.images.logo),
            contentDescription = "logo",
            modifier = Modifier.itemDefaultPadding(),
        )
    }

    sectionHeaderItem(text = "Images with fixed scale")
    fixedScaleImagesCropped()
    fixedScaleImagesOverflow()
    fixedScaleImagesAlignment()
}

private fun LazyGridScope.fixedScaleImagesCropped() {
    sectionSubtitleItem(text = "Images are cropped by parent container size")
    item {
        FixedScaleImageView(
            description = "Small container",
            width = 40.dp,
            height = 40.dp,
        )
    }
    item {
        FixedScaleImageView(
            description = "Small horizontal container",
            width = 40.dp,
            height = 200.dp,
        )
    }
    item {
        FixedScaleImageView(
            description = "Small vertical container",
            width = 200.dp,
            height = 40.dp,
        )
    }
}

private fun LazyGridScope.fixedScaleImagesOverflow() {
    sectionSubtitleItem(text = "Images overflow parent container size")
    item {
        FixedScaleImageView(
            description = "Small container",
            width = 40.dp,
            height = 40.dp,
            allowOverflow = true,
        )
    }
    item {
        FixedScaleImageView(
            description = "Small horizontal container",
            width = 40.dp,
            height = 200.dp,
            allowOverflow = true,
        )
    }
    item {
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
    item {
        FixedScaleImageView(
            description = "Center",
            width = 200.dp,
            height = 200.dp,
        )
    }
    item {
        FixedScaleImageView(
            description = "Top center",
            width = 200.dp,
            height = 200.dp,
            alignment = Alignment.TopCenter,
        )
    }
    item {
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
        modifier = Modifier.itemDefaultPadding(),
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
