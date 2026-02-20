package app.k9mail.core.ui.compose.common.window

/**
 * WindowSizeClass as defined by supporting different screen sizes.
 *
 * See: https://developer.android.com/guide/topics/large-screens/support-different-screen-sizes#window_size_classes
 */
enum class WindowSizeClass {
    Small,
    Compact,
    Medium,
    Expanded,
    ;

    companion object {
        const val SMALL_MAX_WIDTH = 350
        const val SMALL_MAX_HEIGHT = 350
        const val COMPACT_MAX_WIDTH = 600
        const val COMPACT_MAX_HEIGHT = 480

        const val MEDIUM_MAX_WIDTH = 840

        const val MEDIUM_MAX_HEIGHT = 900

        fun fromWidth(width: Int): WindowSizeClass {
            return when {
                width < SMALL_MAX_WIDTH -> Small
                width < COMPACT_MAX_WIDTH -> Compact
                width < MEDIUM_MAX_WIDTH -> Medium
                else -> Expanded
            }
        }

        fun fromHeight(height: Int): WindowSizeClass {
            return when {
                height < SMALL_MAX_HEIGHT -> Small
                height < COMPACT_MAX_HEIGHT -> Compact
                height < MEDIUM_MAX_HEIGHT -> Medium
                else -> Expanded
            }
        }
    }
}
