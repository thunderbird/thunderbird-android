package net.thunderbird.core.ui.compose.designsystem.atom.icon.filled

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.materialIcon
import androidx.compose.material.icons.materialPath
import androidx.compose.ui.graphics.vector.ImageVector

@Suppress("MagicNumber")
val Icons.Filled.Dot: ImageVector
    get() {
        if (instance != null) {
            return instance!!
        }
        instance = materialIcon(name = "Filled.Dot") {
            materialPath {
                moveTo(12.0f, 6.0f)
                curveToRelative(-3.31f, 0.0f, -6.0f, 2.69f, -6.0f, 6.0f)
                reflectiveCurveToRelative(2.69f, 6.0f, 6.0f, 6.0f)
                reflectiveCurveToRelative(6.0f, -2.69f, 6.0f, -6.0f)
                reflectiveCurveToRelative(-2.69f, -6.0f, -6.0f, -6.0f)
                close()
            }
        }
        return instance!!
    }

private var instance: ImageVector? = null
