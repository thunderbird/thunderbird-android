package app.k9mail.core.ui.compose.designsystem.atom.icon.filled

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.path
import androidx.compose.ui.unit.dp
import app.k9mail.core.ui.compose.designsystem.atom.icon.Icons

@Suppress("MagicNumber", "MaxLineLength", "UnusedReceiverParameter")
val Icons.Filled.Notification: ImageVector
    get() {
        val current = _notification
        if (current != null) return current

        return ImageVector.Builder(
            name = "app.k9mail.core.ui.compose.theme2.MainTheme.Notification",
            defaultWidth = 24.0.dp,
            defaultHeight = 24.0.dp,
            viewportWidth = 24.0f,
            viewportHeight = 24.0f,
        ).apply {
            // M12 3.5 C8.953 3.5 6.5 5.953 6.5 9 V12 V13.5 C5.392 13.5 4.5 14.392 4.5 15.5 V17 C4.5 17.277 4.723 17.5 5 17.5 H6.5 H7 H17 H17.5 H19 C19.277 17.5 19.5 17.277 19.5 17 V15.5 C19.5 14.392 18.608 13.5 17.5 13.5 V10.5 V9 C17.5 5.953 15.047 3.5 12 3.5Z
            path(
                fill = SolidColor(Color(0xFF1A202C)),
                fillAlpha = 0.2f,
                strokeAlpha = 0.2f,
            ) {
                // M 12 3.5
                moveTo(x = 12.0f, y = 3.5f)
                // C 8.953 3.5 6.5 5.953 6.5 9
                curveTo(
                    x1 = 8.953f,
                    y1 = 3.5f,
                    x2 = 6.5f,
                    y2 = 5.953f,
                    x3 = 6.5f,
                    y3 = 9.0f,
                )
                // V 12
                verticalLineTo(y = 12.0f)
                // V 13.5
                verticalLineTo(y = 13.5f)
                // C 5.392 13.5 4.5 14.392 4.5 15.5
                curveTo(
                    x1 = 5.392f,
                    y1 = 13.5f,
                    x2 = 4.5f,
                    y2 = 14.392f,
                    x3 = 4.5f,
                    y3 = 15.5f,
                )
                // V 17
                verticalLineTo(y = 17.0f)
                // C 4.5 17.277 4.723 17.5 5 17.5
                curveTo(
                    x1 = 4.5f,
                    y1 = 17.277f,
                    x2 = 4.723f,
                    y2 = 17.5f,
                    x3 = 5.0f,
                    y3 = 17.5f,
                )
                // H 6.5
                horizontalLineTo(x = 6.5f)
                // H 7
                horizontalLineTo(x = 7.0f)
                // H 17
                horizontalLineTo(x = 17.0f)
                // H 17.5
                horizontalLineTo(x = 17.5f)
                // H 19
                horizontalLineTo(x = 19.0f)
                // C 19.277 17.5 19.5 17.277 19.5 17
                curveTo(
                    x1 = 19.277f,
                    y1 = 17.5f,
                    x2 = 19.5f,
                    y2 = 17.277f,
                    x3 = 19.5f,
                    y3 = 17.0f,
                )
                // V 15.5
                verticalLineTo(y = 15.5f)
                // C 19.5 14.392 18.608 13.5 17.5 13.5
                curveTo(
                    x1 = 19.5f,
                    y1 = 14.392f,
                    x2 = 18.608f,
                    y2 = 13.5f,
                    x3 = 17.5f,
                    y3 = 13.5f,
                )
                // V 10.5
                verticalLineTo(y = 10.5f)
                // V 9
                verticalLineTo(y = 9.0f)
                // C 17.5 5.953 15.047 3.5 12 3.5z
                curveTo(
                    x1 = 17.5f,
                    y1 = 5.953f,
                    x2 = 15.047f,
                    y2 = 3.5f,
                    x3 = 12.0f,
                    y3 = 3.5f,
                )
                close()
            }
            // M12 3 C8.68466 3 6 5.68465 6 9 V12 V13.207 C4.89477 13.4651 4 14.3184 4 15.5 V17 C4 17.5454 4.45465 18 5 18 H6.5 H7 H9 C9 19.6534 10.3467 21 12 21 C13.6533 21 15 19.6534 15 18 H17 H17.5 H19 C19.5454 18 20 17.5454 20 17 V15.5 C20 14.3184 19.1052 13.4651 18 13.207 V10.5 V9 C18 5.68465 15.3153 3 12 3Z M12 4 C14.7786 4 17 6.22136 17 9 V10.5 V13.5 C17 13.6326 17.0527 13.7598 17.1465 13.8535 C17.2402 13.9473 17.3674 14 17.5 14 C18.3396 14 19 14.6603 19 15.5 V17 H17.5 H17 H14.5 H9.5 H7 H6.5 H5 V15.5 C5 14.6603 5.66036 14 6.5 14 C6.6326 14 6.75977 13.9473 6.85354 13.8535 C6.9473 13.7598 6.99999 13.6326 7 13.5 V12 V9 C7 6.22136 9.22136 4 12 4Z M10 18 H14 C14 19.1166 13.1166 20 12 20 C10.8834 20 10 19.1166 10 18Z
            path(
                fill = SolidColor(Color(0xFF1A202C)),
            ) {
                // M 12 3
                moveTo(x = 12.0f, y = 3.0f)
                // C 8.68466 3 6 5.68465 6 9
                curveTo(
                    x1 = 8.68466f,
                    y1 = 3.0f,
                    x2 = 6.0f,
                    y2 = 5.68465f,
                    x3 = 6.0f,
                    y3 = 9.0f,
                )
                // V 12
                verticalLineTo(y = 12.0f)
                // V 13.207
                verticalLineTo(y = 13.207f)
                // C 4.89477 13.4651 4 14.3184 4 15.5
                curveTo(
                    x1 = 4.89477f,
                    y1 = 13.4651f,
                    x2 = 4.0f,
                    y2 = 14.3184f,
                    x3 = 4.0f,
                    y3 = 15.5f,
                )
                // V 17
                verticalLineTo(y = 17.0f)
                // C 4 17.5454 4.45465 18 5 18
                curveTo(
                    x1 = 4.0f,
                    y1 = 17.5454f,
                    x2 = 4.45465f,
                    y2 = 18.0f,
                    x3 = 5.0f,
                    y3 = 18.0f,
                )
                // H 6.5
                horizontalLineTo(x = 6.5f)
                // H 7
                horizontalLineTo(x = 7.0f)
                // H 9
                horizontalLineTo(x = 9.0f)
                // C 9 19.6534 10.3467 21 12 21
                curveTo(
                    x1 = 9.0f,
                    y1 = 19.6534f,
                    x2 = 10.3467f,
                    y2 = 21.0f,
                    x3 = 12.0f,
                    y3 = 21.0f,
                )
                // C 13.6533 21 15 19.6534 15 18
                curveTo(
                    x1 = 13.6533f,
                    y1 = 21.0f,
                    x2 = 15.0f,
                    y2 = 19.6534f,
                    x3 = 15.0f,
                    y3 = 18.0f,
                )
                // H 17
                horizontalLineTo(x = 17.0f)
                // H 17.5
                horizontalLineTo(x = 17.5f)
                // H 19
                horizontalLineTo(x = 19.0f)
                // C 19.5454 18 20 17.5454 20 17
                curveTo(
                    x1 = 19.5454f,
                    y1 = 18.0f,
                    x2 = 20.0f,
                    y2 = 17.5454f,
                    x3 = 20.0f,
                    y3 = 17.0f,
                )
                // V 15.5
                verticalLineTo(y = 15.5f)
                // C 20 14.3184 19.1052 13.4651 18 13.207
                curveTo(
                    x1 = 20.0f,
                    y1 = 14.3184f,
                    x2 = 19.1052f,
                    y2 = 13.4651f,
                    x3 = 18.0f,
                    y3 = 13.207f,
                )
                // V 10.5
                verticalLineTo(y = 10.5f)
                // V 9
                verticalLineTo(y = 9.0f)
                // C 18 5.68465 15.3153 3 12 3z
                curveTo(
                    x1 = 18.0f,
                    y1 = 5.68465f,
                    x2 = 15.3153f,
                    y2 = 3.0f,
                    x3 = 12.0f,
                    y3 = 3.0f,
                )
                close()
                // M 12 4
                moveTo(x = 12.0f, y = 4.0f)
                // C 14.7786 4 17 6.22136 17 9
                curveTo(
                    x1 = 14.7786f,
                    y1 = 4.0f,
                    x2 = 17.0f,
                    y2 = 6.22136f,
                    x3 = 17.0f,
                    y3 = 9.0f,
                )
                // V 10.5
                verticalLineTo(y = 10.5f)
                // V 13.5
                verticalLineTo(y = 13.5f)
                // C 17 13.6326 17.0527 13.7598 17.1465 13.8535
                curveTo(
                    x1 = 17.0f,
                    y1 = 13.6326f,
                    x2 = 17.0527f,
                    y2 = 13.7598f,
                    x3 = 17.1465f,
                    y3 = 13.8535f,
                )
                // C 17.2402 13.9473 17.3674 14 17.5 14
                curveTo(
                    x1 = 17.2402f,
                    y1 = 13.9473f,
                    x2 = 17.3674f,
                    y2 = 14.0f,
                    x3 = 17.5f,
                    y3 = 14.0f,
                )
                // C 18.3396 14 19 14.6603 19 15.5
                curveTo(
                    x1 = 18.3396f,
                    y1 = 14.0f,
                    x2 = 19.0f,
                    y2 = 14.6603f,
                    x3 = 19.0f,
                    y3 = 15.5f,
                )
                // V 17
                verticalLineTo(y = 17.0f)
                // H 17.5
                horizontalLineTo(x = 17.5f)
                // H 17
                horizontalLineTo(x = 17.0f)
                // H 14.5
                horizontalLineTo(x = 14.5f)
                // H 9.5
                horizontalLineTo(x = 9.5f)
                // H 7
                horizontalLineTo(x = 7.0f)
                // H 6.5
                horizontalLineTo(x = 6.5f)
                // H 5
                horizontalLineTo(x = 5.0f)
                // V 15.5
                verticalLineTo(y = 15.5f)
                // C 5 14.6603 5.66036 14 6.5 14
                curveTo(
                    x1 = 5.0f,
                    y1 = 14.6603f,
                    x2 = 5.66036f,
                    y2 = 14.0f,
                    x3 = 6.5f,
                    y3 = 14.0f,
                )
                // C 6.6326 14 6.75977 13.9473 6.85354 13.8535
                curveTo(
                    x1 = 6.6326f,
                    y1 = 14.0f,
                    x2 = 6.75977f,
                    y2 = 13.9473f,
                    x3 = 6.85354f,
                    y3 = 13.8535f,
                )
                // C 6.9473 13.7598 6.99999 13.6326 7 13.5
                curveTo(
                    x1 = 6.9473f,
                    y1 = 13.7598f,
                    x2 = 6.99999f,
                    y2 = 13.6326f,
                    x3 = 7.0f,
                    y3 = 13.5f,
                )
                // V 12
                verticalLineTo(y = 12.0f)
                // V 9
                verticalLineTo(y = 9.0f)
                // C 7 6.22136 9.22136 4 12 4z
                curveTo(
                    x1 = 7.0f,
                    y1 = 6.22136f,
                    x2 = 9.22136f,
                    y2 = 4.0f,
                    x3 = 12.0f,
                    y3 = 4.0f,
                )
                close()
                // M 10 18
                moveTo(x = 10.0f, y = 18.0f)
                // H 14
                horizontalLineTo(x = 14.0f)
                // C 14 19.1166 13.1166 20 12 20
                curveTo(
                    x1 = 14.0f,
                    y1 = 19.1166f,
                    x2 = 13.1166f,
                    y2 = 20.0f,
                    x3 = 12.0f,
                    y3 = 20.0f,
                )
                // C 10.8834 20 10 19.1166 10 18z
                curveTo(
                    x1 = 10.8834f,
                    y1 = 20.0f,
                    x2 = 10.0f,
                    y2 = 19.1166f,
                    x3 = 10.0f,
                    y3 = 18.0f,
                )
                close()
            }
        }.build().also { _notification = it }
    }

@Suppress("ObjectPropertyName")
private var _notification: ImageVector? = null
