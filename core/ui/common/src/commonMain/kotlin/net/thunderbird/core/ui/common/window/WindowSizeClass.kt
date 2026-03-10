package net.thunderbird.core.ui.common.window

import androidx.compose.runtime.Immutable
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp

@Immutable
public data class WindowSizeClass(
    val heightSizeClass: WindowHeightSizeClass,
    val widthSizeClass: WindowWidthSizeClass,
) {
    public companion object {
        public fun calculateFromSize(
            size: DpSize,
        ): WindowSizeClass {
            val heightSizeClass = WindowHeightSizeClass.fromHeight(size.height)
            val widthSizeClass = WindowWidthSizeClass.fromWidth(size.width)
            return WindowSizeClass(heightSizeClass, widthSizeClass)
        }
    }
}

/**
 * Width-based window size class.
 *
 * A window size class represents a breakpoint that can be used to build responsive layouts. Each window size class
 * breakpoint represents a majority case for typical device scenarios so your layouts will work well on most devices
 * and configurations.
 *
 * For more details see <a href="https://developer.android.com/guide/topics/large-screens/support-different-screen-sizes#window_size_classes" class="external" target="_blank">Window size classes documentation.
 */
@Immutable
public enum class WindowWidthSizeClass {
    Small,
    Compact,
    Medium,
    Expanded,
    ;

    public companion object {
        public val BREAKPOINT_SMALL: Dp = 350.dp
        public val BREAKPOINT_COMPACT: Dp = 600.dp
        public val BREAKPOINT_MEDIUM: Dp = 840.dp

        internal fun fromWidth(width: Dp): WindowWidthSizeClass {
            require(width >= 0.dp) { "Width must be positive" }
            return when {
                width < BREAKPOINT_SMALL -> Small
                width < BREAKPOINT_COMPACT -> Compact
                width < BREAKPOINT_MEDIUM -> Medium
                else -> Expanded
            }
        }
    }
}

/**
 * Height-based window size class.
 *
 * A window size class represents a breakpoint that can be used to build responsive layouts. Each window size class
 * breakpoint represents a majority case for typical device scenarios so your layouts will work well on most devices
 * and configurations.
 *
 * For more details see <a href="https://developer.android.com/guide/topics/large-screens/support-different-screen-sizes#window_size_classes" class="external" target="_blank">Window size classes documentation.
 */
@Immutable
public enum class WindowHeightSizeClass {
    Small,
    Compact,
    Medium,
    Expanded,
    ;

    public companion object {
        public val BREAKPOINT_SMALL: Dp = 350.dp
        public val BREAKPOINT_COMPACT: Dp = 480.dp
        public val BREAKPOINT_MEDIUM: Dp = 900.dp

        internal fun fromHeight(height: Dp): WindowHeightSizeClass {
            require(height >= 0.dp) { "Height must be positive" }
            return when {
                height < BREAKPOINT_SMALL -> Small
                height < BREAKPOINT_COMPACT -> Compact
                height < BREAKPOINT_MEDIUM -> Medium
                else -> Expanded
            }
        }
    }
}
