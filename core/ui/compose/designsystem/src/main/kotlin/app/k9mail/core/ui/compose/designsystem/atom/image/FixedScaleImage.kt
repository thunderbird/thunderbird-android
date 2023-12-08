package app.k9mail.core.ui.compose.designsystem.atom.image

import androidx.annotation.DrawableRes
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.FixedScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import app.k9mail.core.ui.compose.theme.MainTheme
import app.k9mail.core.ui.compose.theme.ThunderbirdTheme

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

@Composable
@Preview(showBackground = true)
internal fun FixedScaleImageBottomCenterPreview() {
    ThunderbirdTheme {
        Box(
            modifier = Modifier
                .width(200.dp)
                .height(200.dp),
        ) {
            FixedScaleImage(
                id = MainTheme.images.logo,
                alignment = Alignment.BottomCenter,
            )
        }
    }
}

@Composable
@Preview(showBackground = true)
internal fun FixedScaleImageCroppedPreview() {
    ThunderbirdTheme {
        Box(
            modifier = Modifier
                .width(40.dp)
                .height(40.dp),
        ) {
            FixedScaleImage(
                id = MainTheme.images.logo,
            )
        }
    }
}

@Composable
@Preview(showBackground = true)
internal fun FixedScaleImageHorizontallyCroppedPreview() {
    ThunderbirdTheme {
        Box(
            modifier = Modifier
                .width(200.dp)
                .height(20.dp),
        ) {
            FixedScaleImage(
                id = MainTheme.images.logo,
            )
        }
    }
}

@Composable
@Preview(showBackground = true)
internal fun FixedScaleImageVerticallyCroppedPreview() {
    ThunderbirdTheme {
        Box(
            modifier = Modifier
                .width(20.dp)
                .height(200.dp),
        ) {
            FixedScaleImage(
                id = MainTheme.images.logo,
            )
        }
    }
}
