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
 * The default dimension (width and height) for badge icons.
 */
internal const val BADGE_ICON_DIMENSION = 12f

/**
 * Utility to construct an badge icon with default size information.
 *
 * @param name the full name of the badge
 * @param autoMirror determines if the vector asset should be auto-mirrored in RTL layouts.
 * @param block the builder block to create the badge
 */
internal inline fun badgeIcon(
    name: String,
    autoMirror: Boolean = false,
    block: ImageVector.Builder.() -> ImageVector.Builder,
): ImageVector = ImageVector.Builder(
    name = name,
    defaultWidth = BADGE_ICON_DIMENSION.dp,
    defaultHeight = BADGE_ICON_DIMENSION.dp,
    viewportWidth = BADGE_ICON_DIMENSION,
    viewportHeight = BADGE_ICON_DIMENSION,
    autoMirror = autoMirror,
).block().build()

/**
 * Adds a vector path to this badge icon with defaults.
 *
 * @param fill fill for this path
 * @param fillAlpha fill alpha for this path
 * @param stroke stroke for this path
 * @param strokeAlpha stroke alpha for this path
 * @param pathFillType [PathFillType] for this path
 * @param pathBuilder builder lambda to add commands to this path
 */
internal inline fun ImageVector.Builder.badgeIconPath(
    fill: SolidColor = SolidColor(Color.Black),
    fillAlpha: Float = 1f,
    stroke: SolidColor? = null,
    strokeAlpha: Float = 1f,
    pathFillType: PathFillType = DefaultFillType,
    pathBuilder: PathBuilder.() -> Unit,
) =
    path(
        fill = fill,
        fillAlpha = fillAlpha,
        stroke = stroke,
        strokeAlpha = strokeAlpha,
        strokeLineWidth = 1f,
        strokeLineCap = StrokeCap.Butt,
        strokeLineJoin = StrokeJoin.Bevel,
        strokeLineMiter = 1f,
        pathFillType = pathFillType,
        pathBuilder = pathBuilder,
    )
