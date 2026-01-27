package net.thunderbird.core.ui.compose.designsystem.atom.icon

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathFillType
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.vector.DefaultFillType
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.PathBuilder
import androidx.compose.ui.graphics.vector.path
import androidx.compose.ui.unit.dp

/**
 * The default dimension (width and height) for icons.
 */
internal const val ICON_DIMENSION = 24f

/**
 * Utility to construct an icon with default size information.
 *
 * @param name the full name of the icon
 * @param autoMirror determines if the vector asset should be auto-mirrored in RTL layouts.
 * @param viewportWidth the viewport width of the icon
 * @param viewportHeight the viewport height of the icon
 * @param block the builder block to create the icon
 *
 */
internal inline fun icon(
    name: String,
    autoMirror: Boolean = false,
    viewportWidth: Float = ICON_DIMENSION,
    viewportHeight: Float = ICON_DIMENSION,
    block: ImageVector.Builder.() -> ImageVector.Builder,
): ImageVector = ImageVector.Builder(
    name = name,
    defaultWidth = ICON_DIMENSION.dp,
    defaultHeight = ICON_DIMENSION.dp,
    viewportWidth = viewportWidth,
    viewportHeight = viewportHeight,
    autoMirror = autoMirror,
).block().build()

/**
 * Adds a vector path to this icon with defaults.
 *
 * @param fill fill for this path
 * @param fillAlpha fill alpha for this path
 * @param strokeAlpha stroke alpha for this path
 * @param pathFillType [PathFillType] for this path
 * @param pathBuilder builder lambda to add commands to this path
 */
internal inline fun ImageVector.Builder.iconPath(
    fill: SolidColor = SolidColor(Color.Black),
    fillAlpha: Float = 1f,
    strokeAlpha: Float = 1f,
    pathFillType: PathFillType = DefaultFillType,
    pathBuilder: PathBuilder.() -> Unit,
) =
    path(
        fill = fill,
        fillAlpha = fillAlpha,
        stroke = null,
        strokeAlpha = strokeAlpha,
        strokeLineWidth = 1f,
        strokeLineCap = StrokeCap.Butt,
        strokeLineJoin = StrokeJoin.Bevel,
        strokeLineMiter = 1f,
        pathFillType = pathFillType,
        pathBuilder = pathBuilder,
    )
