package net.thunderbird.core.ui.compose.designsystem.molecule.swipe

/**
 * Represents the direction of a swipe gesture in a swipeable layout.
 */
enum class SwipeDirection {
    /** Represents a swipe gesture starting from the left (or start) and moving to the right (or end). **/
    StartToEnd,

    /** Represents a swipe gesture starting from the right (or end) and moving to the left (or start). **/
    EndToStart,

    /** Represents the default or neutral state where no swipe action is in progress. **/
    Settled,
}
