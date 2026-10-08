package app.k9mail.core.ui.compose.common.window

/**
 * Represents the foldable state of a device.
 */
enum class FoldableState {
    /** Device is in a folded state (small screen, hinge angle typically < 90°) */
    FOLDED,

    /** Device is in an unfolded state (large screen, hinge angle typically > 120°) */
    UNFOLDED,

    /** Device is not a foldable or state cannot be determined */
    UNKNOWN,
}
