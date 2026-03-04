package net.thunderbird.core.ui.common.window

/**
 * WindowSizeClass as defined by supporting different screen sizes.
 *
 * See: https://developer.android.com/guide/topics/large-screens/support-different-screen-sizes#window_size_classes
 */
public enum class WindowSizeClass {
    Small,
    Compact,
    Medium,
    Expanded,
    ;

    public companion object {
        public const val SMALL_MAX_WIDTH: Int = 350
        public const val SMALL_MAX_HEIGHT: Int = 350
        public const val COMPACT_MAX_WIDTH: Int = 600
        public const val COMPACT_MAX_HEIGHT: Int = 480

        public const val MEDIUM_MAX_WIDTH: Int = 840

        public const val MEDIUM_MAX_HEIGHT: Int = 900

        public fun fromWidth(width: Int): WindowSizeClass {
            return when {
                width < SMALL_MAX_WIDTH -> Small
                width < COMPACT_MAX_WIDTH -> Compact
                width < MEDIUM_MAX_WIDTH -> Medium
                else -> Expanded
            }
        }

        public fun fromHeight(height: Int): WindowSizeClass {
            return when {
                height < SMALL_MAX_HEIGHT -> Small
                height < COMPACT_MAX_HEIGHT -> Compact
                height < MEDIUM_MAX_HEIGHT -> Medium
                else -> Expanded
            }
        }
    }
}
