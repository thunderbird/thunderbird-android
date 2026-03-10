package app.k9mail.core.ui.compose.designsystem.atom.image

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.inset
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.rememberVectorPainter
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalInspectionMode
import androidx.compose.ui.unit.Dp
import com.skydoves.landscapist.ImageOptions
import com.skydoves.landscapist.coil3.CoilImage

/**
 * A composable that displays an image from a remote URL using Coil.
 *
 * @param url The URL of the remote image to display.
 * @param modifier The modifier to be applied to the image.
 * @param placeholder An optional composable to display while the image is loading or if it fails to load.
 * @param alignment The alignment of the image within its bounds. Default is [Alignment.Center].
 * @param contentDescription A description of the image for accessibility purposes.
 * @param contentScale The scaling strategy for the image. Default is [ContentScale.Crop].
 * @param previewPlaceholder An optional [Painter] to be used as a placeholder in preview mode. If not provided, no
 *                           placeholder will be shown in preview mode.
 */
@Composable
fun RemoteImage(
    url: String,
    modifier: Modifier = Modifier,
    placeholder: @Composable (() -> Unit)? = null,
    alignment: Alignment = Alignment.Center,
    contentDescription: String? = null,
    contentScale: ContentScale = ContentScale.Crop,
    previewPlaceholder: Painter? = null,
) {
    CoilImage(
        imageModel = { url },
        imageOptions = ImageOptions(
            alignment = alignment,
            contentDescription = contentDescription,
            contentScale = contentScale,
        ),
        failure = {
            placeholder?.invoke()
        },
        loading = {
            placeholder?.invoke()
        },
        modifier = modifier,
        previewPlaceholder = previewPlaceholder,
    )
}

/**
 * Returns a [Painter] that draws a placeholder using the provided [image] and [tint] color when in
 * preview mode.
 *
 * @param image The [ImageVector] to be used as the placeholder image.
 * @param tint The [Color] to tint the placeholder image.
 * @param padding Optional padding around the image. Default is [Dp.Unspecified], which means no padding.
 * @return A [Painter] for the placeholder in preview mode, or null if
 */
@Composable
fun rememberPreviewPlaceholder(
    image: ImageVector,
    tint: Color,
    padding: Dp = Dp.Unspecified,
): Painter? {
    if (!LocalInspectionMode.current) return null

    val painter = rememberVectorPainter(image = image)
    val colorFilter = remember(tint) { ColorFilter.tint(tint) }
    val density = LocalDensity.current
    val paddingPx = if (padding != Dp.Unspecified) {
        with(density) { padding.toPx() }
    } else {
        0f
    }

    return remember(painter, colorFilter) {
        object : Painter() {
            override val intrinsicSize: Size = painter.intrinsicSize

            override fun DrawScope.onDraw() {
                inset(paddingPx, paddingPx, paddingPx, paddingPx) {
                    with(painter) {
                        draw(
                            size = this@inset.size,
                            colorFilter = colorFilter,
                        )
                    }
                }
            }
        }
    }
}
