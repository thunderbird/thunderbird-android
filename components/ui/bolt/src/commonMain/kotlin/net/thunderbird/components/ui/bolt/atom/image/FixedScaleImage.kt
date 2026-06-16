package net.thunderbird.components.ui.bolt.atom.image

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.FixedScale
import androidx.compose.ui.tooling.preview.Preview
import net.thunderbird.components.ui.bolt.PreviewWithTheme
import net.thunderbird.components.ui.bolt.theme.MainTheme
import org.jetbrains.compose.resources.DrawableResource
import org.jetbrains.compose.resources.painterResource

/**
 * An image that has a fixed size and does not scale with the available space. It could be cropped, if the size of the
 * container is smaller than the image. Use allowOverflow to control this behavior.
 * The [alignment] allows to control the position of the image in the container.
 */
@Composable
fun FixedScaleImage(
    resource: DrawableResource,
    modifier: Modifier = Modifier,
    scale: Float = 1f,
    alignment: Alignment = Alignment.Center,
    allowOverflow: Boolean = false,
    contentDescription: String? = null,
) {
    Image(
        modifier = Modifier
            .fillMaxSize()
            .wrapContentSize(align = alignment, unbounded = allowOverflow)
            .then(modifier),
        painter = painterResource(resource),
        contentDescription = contentDescription,
        contentScale = FixedScale(scale),
    )
}

/**
 * An image that has a fixed size and does not scale with the available space. It could be cropped, if the size of the
 * container is smaller than the image. Use allowOverflow to control this behavior.
 * The [alignment] allows to control the position of the image in the container.
 */
@Composable
fun FixedScaleImage(
    imageVector: ImageVector,
    modifier: Modifier = Modifier,
    scale: Float = 1f,
    alignment: Alignment = Alignment.Center,
    allowOverflow: Boolean = false,
    contentDescription: String? = null,
) {
    Image(
        modifier = Modifier
            .fillMaxSize()
            .wrapContentSize(align = alignment, unbounded = allowOverflow)
            .then(modifier),
        imageVector = imageVector,
        contentDescription = contentDescription,
        contentScale = FixedScale(scale),
    )
}

@Composable
@Preview(showBackground = true)
internal fun FixedScaleImageBottomCenterPreview() {
    PreviewWithTheme {
        Box(
            modifier = Modifier
                .width(MainTheme.sizes.huge)
                .height(MainTheme.sizes.huge),
        ) {
            FixedScaleImage(
                resource = MainTheme.images.logo,
                alignment = Alignment.BottomCenter,
            )
        }
    }
}

@Composable
@Preview(showBackground = true)
internal fun FixedScaleImageCroppedPreview() {
    PreviewWithTheme {
        Box(
            modifier = Modifier
                .width(MainTheme.sizes.medium)
                .height(MainTheme.sizes.medium),
        ) {
            FixedScaleImage(
                resource = MainTheme.images.logo,
            )
        }
    }
}

@Composable
@Preview(showBackground = true)
internal fun FixedScaleImageHorizontallyCroppedPreview() {
    PreviewWithTheme {
        Box(
            modifier = Modifier
                .width(MainTheme.sizes.huge)
                .height(MainTheme.sizes.medium),
        ) {
            FixedScaleImage(
                resource = MainTheme.images.logo,
            )
        }
    }
}

@Composable
@Preview(showBackground = true)
internal fun FixedScaleImageVerticallyCroppedPreview() {
    PreviewWithTheme {
        Box(
            modifier = Modifier
                .width(MainTheme.sizes.medium)
                .height(MainTheme.sizes.huge),
        ) {
            FixedScaleImage(
                resource = MainTheme.images.logo,
            )
        }
    }
}
