package app.k9mail.core.ui.compose.common.image

import androidx.compose.runtime.Immutable
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.Dp

@Immutable
data class ImageWithBaseline(
    val image: ImageVector,
    val baseline: Dp,
)
