package net.thunderbird.core.ui.compose.common.modifier

import android.annotation.SuppressLint
import android.content.res.Configuration
import android.os.Build
import android.view.ViewConfiguration
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalResources
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/**
 * Applies padding to avoid unsafe drawing areas such as notches and system bars.
 *
 * For API level 23 and above, it uses the reliable [WindowInsets.safeDrawing] method.
 * For API levels 21 and 22, it falls back to manually calculating the status bar and
 * navigation bar heights to apply appropriate padding.
 *
 * @return A [Modifier] with safe drawing padding applied.
 */
@SuppressLint("InternalInsetResource", "UnnecessaryComposedModifier")
@Composable
fun Modifier.safeDrawingPaddingCompat(): Modifier = composed {
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
        this.windowInsetsPadding(WindowInsets.safeDrawing)
    } else {
        val legacyPaddings = calculateLegacyPaddings()

        this.padding(
            top = legacyPaddings.top,
            bottom = legacyPaddings.bottom,
        )
    }
}

private data class LegacyPaddings(val top: Dp, val bottom: Dp)

/**
 * Calculates system bar padding for pre-Marshmallow devices (API 21, 22)
 * by reading system dimension resources.
 */
@SuppressLint("InternalInsetResource", "DiscouragedApi")
@Composable
private fun calculateLegacyPaddings(): LegacyPaddings {
    val context = LocalContext.current
    val resources = LocalResources.current
    val density = LocalDensity.current
    val configuration = LocalConfiguration.current

    // Calculate top padding for the status bar
    val statusBarResourceId = resources.getIdentifier("status_bar_height", "dimen", "android")
    val topPadding = if (statusBarResourceId > 0) {
        with(density) { resources.getDimensionPixelSize(statusBarResourceId).toDp() }
    } else {
        24.dp // Default fallback
    }

    // Calculate bottom padding, but only if a software navigation bar is likely present
    val hasSoftwareNavBar = !ViewConfiguration.get(context).hasPermanentMenuKey()
    val isPortrait = configuration.orientation == Configuration.ORIENTATION_PORTRAIT
    val navBarResourceId = resources.getIdentifier("navigation_bar_height", "dimen", "android")
    val bottomPadding = if (hasSoftwareNavBar && isPortrait && navBarResourceId > 0) {
        with(density) { resources.getDimensionPixelSize(navBarResourceId).toDp() }
    } else {
        0.dp
    }
    return LegacyPaddings(top = topPadding, bottom = bottomPadding)
}
