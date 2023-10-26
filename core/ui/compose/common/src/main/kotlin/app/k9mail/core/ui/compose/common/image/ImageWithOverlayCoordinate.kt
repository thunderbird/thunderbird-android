package app.k9mail.core.ui.compose.common.image

import androidx.compose.runtime.Immutable
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.Dp

/**
 * An image with a coordinate to draw a (smaller) overlay icon on top of it.
 *
 * Example: An icon representing an Android permission with an overlay icon to indicate whether the permission has been
 * granted.
 */
@Immutable
data class ImageWithOverlayCoordinate(
    val image: ImageVector,
    val overlayOffsetX: Dp,
    val overlayOffsetY: Dp,
)
