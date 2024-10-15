package app.k9mail.core.ui.compose.designsystem.atom.image

import androidx.annotation.DrawableRes
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.FixedScale
import androidx.compose.ui.res.painterResource

/**
 * An image that has a fixed size and does not scale with the available space. It could be cropped, if the size of the
 * container is smaller than the image. Use allowOverflow to control this behavior.
 * The [alignment] allows to control the position of the image in the container.
 */
@Composable
fun FixedScaleImage(
    @DrawableRes id: Int,
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
        painter = painterResource(id),
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
