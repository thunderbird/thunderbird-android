package net.thunderbird.core.ui.compose.designsystem.atom.icon.outlined

import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.tooling.preview.Preview
import net.thunderbird.core.ui.compose.designsystem.atom.icon.Icon
import net.thunderbird.core.ui.compose.designsystem.atom.icon.icon
import net.thunderbird.core.ui.compose.designsystem.atom.icon.iconPath

@Suppress("MagicNumber")
internal val OutlinedEncrypted: ImageVector by lazy {
    icon(name = "OutlinedEncrypted") {
        iconPath {
            moveTo(x = 10.8f, y = 14.5f)
            horizontalLineToRelative(dx = 2.4f)
            lineToRelative(dx = -0.45f, dy = -2.69f)
            arcToRelative(
                a = 1.4f,
                b = 1.4f,
                theta = 0.0f,
                isMoreThanHalf = false,
                isPositiveArc = false,
                dx1 = 0.55f,
                dy1 = -0.54f,
            )
            arcToRelative(
                a = 1.6f,
                b = 1.6f,
                theta = 0.0f,
                isMoreThanHalf = false,
                isPositiveArc = false,
                dx1 = 0.2f,
                dy1 = -0.78f,
            )
            arcToRelative(
                a = 1.4f,
                b = 1.4f,
                theta = 0.0f,
                isMoreThanHalf = false,
                isPositiveArc = false,
                dx1 = -0.44f,
                dy1 = -1.05f,
            )
            arcTo(
                horizontalEllipseRadius = 1.5f,
                verticalEllipseRadius = 1.5f,
                theta = 0.0f,
                isMoreThanHalf = false,
                isPositiveArc = false,
                x1 = 12.0f,
                y1 = 9.0f,
            )
            arcToRelative(
                a = 1.4f,
                b = 1.4f,
                theta = 0.0f,
                isMoreThanHalf = false,
                isPositiveArc = false,
                dx1 = -1.06f,
                dy1 = 0.44f,
            )
            arcToRelative(
                a = 1.5f,
                b = 1.5f,
                theta = 0.0f,
                isMoreThanHalf = false,
                isPositiveArc = false,
                dx1 = -0.44f,
                dy1 = 1.06f,
            )
            arcToRelative(
                a = 1.5f,
                b = 1.5f,
                theta = 0.0f,
                isMoreThanHalf = false,
                isPositiveArc = false,
                dx1 = 0.2f,
                dy1 = 0.78f,
            )
            arcToRelative(
                a = 1.4f,
                b = 1.4f,
                theta = 0.0f,
                isMoreThanHalf = false,
                isPositiveArc = false,
                dx1 = 0.55f,
                dy1 = 0.53f,
            )
            close()
            moveTo(x = 12.0f, y = 20.0f)
            arcToRelative(
                a = 8.0f,
                b = 8.0f,
                theta = 0.0f,
                isMoreThanHalf = false,
                isPositiveArc = true,
                dx1 = -4.66f,
                dy1 = -3.18f,
            )
            arcToRelative(
                a = 9.0f,
                b = 9.0f,
                theta = 0.0f,
                isMoreThanHalf = false,
                isPositiveArc = true,
                dx1 = -1.84f,
                dy1 = -5.55f,
            )
            verticalLineTo(y = 6.5f)
            lineTo(x = 12.0f, y = 4.0f)
            lineToRelative(dx = 6.5f, dy = 2.5f)
            verticalLineToRelative(dy = 4.77f)
            arcToRelative(
                a = 9.0f,
                b = 9.0f,
                theta = 0.0f,
                isMoreThanHalf = false,
                isPositiveArc = true,
                dx1 = -1.84f,
                dy1 = 5.55f,
            )
            arcTo(
                horizontalEllipseRadius = 8.0f,
                verticalEllipseRadius = 8.0f,
                theta = 0.0f,
                isMoreThanHalf = false,
                isPositiveArc = true,
                x1 = 12.0f,
                y1 = 20.0f,
            )
            moveToRelative(dx = 0.0f, dy = -1.56f)
            arcToRelative(
                a = 6.8f,
                b = 6.8f,
                theta = 0.0f,
                isMoreThanHalf = false,
                isPositiveArc = false,
                dx1 = 3.58f,
                dy1 = -2.69f,
            )
            arcTo(
                horizontalEllipseRadius = 7.6f,
                verticalEllipseRadius = 7.6f,
                theta = 0.0f,
                isMoreThanHalf = false,
                isPositiveArc = false,
                x1 = 17.0f,
                y1 = 11.27f,
            )
            verticalLineTo(y = 7.52f)
            lineTo(x = 12.0f, y = 5.6f)
            lineTo(x = 7.0f, y = 7.52f)
            verticalLineToRelative(dy = 3.75f)
            arcToRelative(
                a = 7.6f,
                b = 7.6f,
                theta = 0.0f,
                isMoreThanHalf = false,
                isPositiveArc = false,
                dx1 = 1.42f,
                dy1 = 4.48f,
            )
            arcTo(
                horizontalEllipseRadius = 6.8f,
                verticalEllipseRadius = 6.8f,
                theta = 0.0f,
                isMoreThanHalf = false,
                isPositiveArc = false,
                x1 = 12.0f,
                y1 = 18.44f,
            )
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun IconPreview() {
    Icon(imageVector = OutlinedEncrypted)
}
