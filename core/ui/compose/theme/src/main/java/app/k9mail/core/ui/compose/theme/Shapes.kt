package app.k9mail.core.ui.compose.theme

import androidx.compose.foundation.shape.CornerBasedShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.unit.dp
import androidx.compose.material.Shapes as MaterialShapes

@Immutable
data class Shapes(
    val small: CornerBasedShape = RoundedCornerShape(8.dp),
    val medium: CornerBasedShape = RoundedCornerShape(4.dp),
    val large: CornerBasedShape = RoundedCornerShape(0.dp),
)

internal fun Shapes.toMaterialShapes() = MaterialShapes(
    small = small,
    medium = medium,
    large = large,
)

internal val LocalShapes = staticCompositionLocalOf { Shapes() }
