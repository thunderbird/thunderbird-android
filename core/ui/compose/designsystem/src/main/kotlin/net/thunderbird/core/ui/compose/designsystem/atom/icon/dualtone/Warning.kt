package net.thunderbird.core.ui.compose.designsystem.atom.icon.dualtone

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Column
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.path
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import app.k9mail.core.ui.compose.designsystem.atom.icon.Icons
import net.thunderbird.core.ui.compose.designsystem.atom.icon.Icon

@Suppress("MagicNumber", "UnusedReceiverParameter")
val Icons.DualTone.Warning: ImageVector
    get() {
        val current = _warningDualTone
        if (current != null) return current

        return ImageVector.Builder(
            name = "app.k9mail.core.ui.compose.theme2.MainTheme.WarningDualTone",
            defaultWidth = 24.0.dp,
            defaultHeight = 24.0.dp,
            viewportWidth = 24.0f,
            viewportHeight = 24.0f,
        ).apply {
            path(
                fill = SolidColor(Color(0xFF4C4D58)),
                fillAlpha = 0.2f,
                strokeAlpha = 0.2f,
            ) {
                moveTo(x = 20.2f, y = 20.25f)
                horizontalLineTo(x = 3.8f)
                arcToRelative(
                    a = 1.5f,
                    b = 1.5f,
                    theta = 0.0f,
                    isMoreThanHalf = false,
                    isPositiveArc = true,
                    dx1 = -1.35f,
                    dy1 = -2.24f,
                )
                lineToRelative(dx = 8.2f, dy = -14.24f)
                arcToRelative(
                    a = 1.57f,
                    b = 1.57f,
                    theta = 0.0f,
                    isMoreThanHalf = false,
                    isPositiveArc = true,
                    dx1 = 2.7f,
                    dy1 = 0.0f,
                )
                lineToRelative(dx = 8.2f, dy = 14.24f)
                arcToRelative(
                    a = 1.5f,
                    b = 1.5f,
                    theta = 0.0f,
                    isMoreThanHalf = false,
                    isPositiveArc = true,
                    dx1 = -1.35f,
                    dy1 = 2.24f,
                )
            }
            path(
                fill = SolidColor(Color(0xFF4C4D58)),
            ) {
                moveTo(x = 22.2f, y = 17.63f)
                lineTo(x = 14.0f, y = 3.4f)
                arcToRelative(
                    a = 2.32f,
                    b = 2.32f,
                    theta = 0.0f,
                    isMoreThanHalf = false,
                    isPositiveArc = false,
                    dx1 = -4.0f,
                    dy1 = 0.0f,
                )
                lineTo(x = 1.8f, y = 17.63f)
                arcToRelative(
                    a = 2.2f,
                    b = 2.2f,
                    theta = 0.0f,
                    isMoreThanHalf = false,
                    isPositiveArc = false,
                    dx1 = 0.0f,
                    dy1 = 2.23f,
                )
                arcToRelative(
                    a = 2.3f,
                    b = 2.3f,
                    theta = 0.0f,
                    isMoreThanHalf = false,
                    isPositiveArc = false,
                    dx1 = 2.0f,
                    dy1 = 1.14f,
                )
                horizontalLineToRelative(dx = 16.4f)
                arcToRelative(
                    a = 2.3f,
                    b = 2.3f,
                    theta = 0.0f,
                    isMoreThanHalf = false,
                    isPositiveArc = false,
                    dx1 = 2.3f,
                    dy1 = -2.25f,
                )
                arcToRelative(
                    a = 2.0f,
                    b = 2.0f,
                    theta = 0.0f,
                    isMoreThanHalf = false,
                    isPositiveArc = false,
                    dx1 = -0.3f,
                    dy1 = -1.12f,
                )
                moveToRelative(dx = -1.3f, dy = 1.48f)
                arcToRelative(
                    a = 0.8f,
                    b = 0.8f,
                    theta = 0.0f,
                    isMoreThanHalf = false,
                    isPositiveArc = true,
                    dx1 = -0.7f,
                    dy1 = 0.39f,
                )
                horizontalLineTo(x = 3.8f)
                arcToRelative(
                    a = 0.8f,
                    b = 0.8f,
                    theta = 0.0f,
                    isMoreThanHalf = false,
                    isPositiveArc = true,
                    dx1 = -0.7f,
                    dy1 = -0.4f,
                )
                arcToRelative(
                    a = 0.7f,
                    b = 0.7f,
                    theta = 0.0f,
                    isMoreThanHalf = false,
                    isPositiveArc = true,
                    dx1 = 0.0f,
                    dy1 = -0.72f,
                )
                lineToRelative(dx = 8.2f, dy = -14.24f)
                arcToRelative(
                    a = 0.82f,
                    b = 0.82f,
                    theta = 0.0f,
                    isMoreThanHalf = false,
                    isPositiveArc = true,
                    dx1 = 1.4f,
                    dy1 = 0.0f,
                )
                lineToRelative(dx = 8.2f, dy = 14.24f)
                arcToRelative(
                    a = 0.7f,
                    b = 0.7f,
                    theta = 0.0f,
                    isMoreThanHalf = false,
                    isPositiveArc = true,
                    dx1 = 0.0f,
                    dy1 = 0.73f,
                )
                moveToRelative(dx = -9.65f, dy = -5.61f)
                verticalLineTo(y = 9.75f)
                arcToRelative(
                    a = 0.75f,
                    b = 0.75f,
                    theta = 0.0f,
                    isMoreThanHalf = true,
                    isPositiveArc = true,
                    dx1 = 1.5f,
                    dy1 = 0.0f,
                )
                verticalLineToRelative(dy = 3.75f)
                arcToRelative(
                    a = 0.75f,
                    b = 0.75f,
                    theta = 0.0f,
                    isMoreThanHalf = false,
                    isPositiveArc = true,
                    dx1 = -1.5f,
                    dy1 = 0.0f,
                )
                moveToRelative(dx = 1.88f, dy = 3.38f)
                arcToRelative(
                    a = 1.13f,
                    b = 1.13f,
                    theta = 0.0f,
                    isMoreThanHalf = true,
                    isPositiveArc = true,
                    dx1 = -2.26f,
                    dy1 = 0.0f,
                )
                arcToRelative(
                    a = 1.13f,
                    b = 1.13f,
                    theta = 0.0f,
                    isMoreThanHalf = false,
                    isPositiveArc = true,
                    dx1 = 2.26f,
                    dy1 = 0.0f,
                )
            }
        }.build().also { _warningDualTone = it }
    }

@Suppress("ObjectPropertyName")
private var _warningDualTone: ImageVector? = null

@Preview(showBackground = true)
@Composable
private fun Preview() {
    Column {
        Image(imageVector = Icons.DualTone.Warning, contentDescription = null)
        Icon(imageVector = Icons.DualTone.Warning, contentDescription = null, tint = Color.Red)
    }
}
