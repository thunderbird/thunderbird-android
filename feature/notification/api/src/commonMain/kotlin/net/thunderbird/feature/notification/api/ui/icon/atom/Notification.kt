package net.thunderbird.feature.notification.api.ui.icon.atom

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.path
import androidx.compose.ui.unit.dp

@Suppress("MagicNumber", "MaxLineLength")
internal val Notification: ImageVector
    get() {
        val current = _notification
        if (current != null) return current

        return ImageVector.Builder(
            name = "net.thunderbird.feature.notification.api.ui.icon.atom.Notification",
            defaultWidth = 24.0.dp,
            defaultHeight = 24.0.dp,
            viewportWidth = 24.0f,
            viewportHeight = 24.0f,
        ).apply {
            path(
                fill = SolidColor(Color(0xFF1A202C)),
                fillAlpha = 0.2f,
                strokeAlpha = 0.2f,
            ) {
                moveTo(x = 12.0f, y = 3.5f)
                curveTo(x1 = 8.953f, y1 = 3.5f, x2 = 6.5f, y2 = 5.953f, x3 = 6.5f, y3 = 9.0f)
                verticalLineTo(y = 12.0f)
                verticalLineTo(y = 13.5f)
                curveTo(x1 = 5.392f, y1 = 13.5f, x2 = 4.5f, y2 = 14.392f, x3 = 4.5f, y3 = 15.5f)
                verticalLineTo(y = 17.0f)
                curveTo(x1 = 4.5f, y1 = 17.277f, x2 = 4.723f, y2 = 17.5f, x3 = 5.0f, y3 = 17.5f)
                horizontalLineTo(x = 6.5f)
                horizontalLineTo(x = 7.0f)
                horizontalLineTo(x = 17.0f)
                horizontalLineTo(x = 17.5f)
                horizontalLineTo(x = 19.0f)
                curveTo(x1 = 19.277f, y1 = 17.5f, x2 = 19.5f, y2 = 17.277f, x3 = 19.5f, y3 = 17.0f)
                verticalLineTo(y = 15.5f)
                curveTo(x1 = 19.5f, y1 = 14.392f, x2 = 18.608f, y2 = 13.5f, x3 = 17.5f, y3 = 13.5f)
                verticalLineTo(y = 10.5f)
                verticalLineTo(y = 9.0f)
                curveTo(x1 = 17.5f, y1 = 5.953f, x2 = 15.047f, y2 = 3.5f, x3 = 12.0f, y3 = 3.5f)
                close()
            }
            path(
                fill = SolidColor(Color(0xFF1A202C)),
            ) {
                moveTo(x = 12.0f, y = 3.0f)
                curveTo(x1 = 8.68466f, y1 = 3.0f, x2 = 6.0f, y2 = 5.68465f, x3 = 6.0f, y3 = 9.0f)
                verticalLineTo(y = 12.0f)
                verticalLineTo(y = 13.207f)
                curveTo(x1 = 4.89477f, y1 = 13.4651f, x2 = 4.0f, y2 = 14.3184f, x3 = 4.0f, y3 = 15.5f)
                verticalLineTo(y = 17.0f)
                curveTo(x1 = 4.0f, y1 = 17.5454f, x2 = 4.45465f, y2 = 18.0f, x3 = 5.0f, y3 = 18.0f)
                horizontalLineTo(x = 6.5f)
                horizontalLineTo(x = 7.0f)
                horizontalLineTo(x = 9.0f)
                curveTo(x1 = 9.0f, y1 = 19.6534f, x2 = 10.3467f, y2 = 21.0f, x3 = 12.0f, y3 = 21.0f)
                curveTo(x1 = 13.6533f, y1 = 21.0f, x2 = 15.0f, y2 = 19.6534f, x3 = 15.0f, y3 = 18.0f)
                horizontalLineTo(x = 17.0f)
                horizontalLineTo(x = 17.5f)
                horizontalLineTo(x = 19.0f)
                curveTo(x1 = 19.5454f, y1 = 18.0f, x2 = 20.0f, y2 = 17.5454f, x3 = 20.0f, y3 = 17.0f)
                verticalLineTo(y = 15.5f)
                curveTo(x1 = 20.0f, y1 = 14.3184f, x2 = 19.1052f, y2 = 13.4651f, x3 = 18.0f, y3 = 13.207f)
                verticalLineTo(y = 10.5f)
                verticalLineTo(y = 9.0f)
                curveTo(x1 = 18.0f, y1 = 5.68465f, x2 = 15.3153f, y2 = 3.0f, x3 = 12.0f, y3 = 3.0f)
                close()
                moveTo(x = 12.0f, y = 4.0f)
                curveTo(x1 = 14.7786f, y1 = 4.0f, x2 = 17.0f, y2 = 6.22136f, x3 = 17.0f, y3 = 9.0f)
                verticalLineTo(y = 10.5f)
                verticalLineTo(y = 13.5f)
                curveTo(x1 = 17.0f, y1 = 13.6326f, x2 = 17.0527f, y2 = 13.7598f, x3 = 17.1465f, y3 = 13.8535f)
                curveTo(x1 = 17.2402f, y1 = 13.9473f, x2 = 17.3674f, y2 = 14.0f, x3 = 17.5f, y3 = 14.0f)
                curveTo(x1 = 18.3396f, y1 = 14.0f, x2 = 19.0f, y2 = 14.6603f, x3 = 19.0f, y3 = 15.5f)
                verticalLineTo(y = 17.0f)
                horizontalLineTo(x = 17.5f)
                horizontalLineTo(x = 17.0f)
                horizontalLineTo(x = 14.5f)
                horizontalLineTo(x = 9.5f)
                horizontalLineTo(x = 7.0f)
                horizontalLineTo(x = 6.5f)
                horizontalLineTo(x = 5.0f)
                verticalLineTo(y = 15.5f)
                curveTo(x1 = 5.0f, y1 = 14.6603f, x2 = 5.66036f, y2 = 14.0f, x3 = 6.5f, y3 = 14.0f)
                curveTo(x1 = 6.6326f, y1 = 14.0f, x2 = 6.75977f, y2 = 13.9473f, x3 = 6.85354f, y3 = 13.8535f)
                curveTo(x1 = 6.9473f, y1 = 13.7598f, x2 = 6.99999f, y2 = 13.6326f, x3 = 7.0f, y3 = 13.5f)
                verticalLineTo(y = 12.0f)
                verticalLineTo(y = 9.0f)
                curveTo(x1 = 7.0f, y1 = 6.22136f, x2 = 9.22136f, y2 = 4.0f, x3 = 12.0f, y3 = 4.0f)
                close()
                moveTo(x = 10.0f, y = 18.0f)
                horizontalLineTo(x = 14.0f)
                curveTo(x1 = 14.0f, y1 = 19.1166f, x2 = 13.1166f, y2 = 20.0f, x3 = 12.0f, y3 = 20.0f)
                curveTo(x1 = 10.8834f, y1 = 20.0f, x2 = 10.0f, y2 = 19.1166f, x3 = 10.0f, y3 = 18.0f)
                close()
            }
        }.build().also { _notification = it }
    }

@Suppress("ObjectPropertyName")
private var _notification: ImageVector? = null
