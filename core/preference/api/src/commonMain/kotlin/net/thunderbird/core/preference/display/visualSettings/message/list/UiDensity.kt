package net.thunderbird.core.preference.display.visualSettings.message.list

/**
 * Represents the different density levels for the user interface, specifically for the message list.
 * This determines the spacing of UI elements.
 */
enum class UiDensity {
    /**
     * A dense layout with minimal spacing, allowing more items to be visible on the screen at once.
     */
    Compact,

    /** The standard, default density level, offering a balanced layout. */
    Default,

    /**
     * Provides the most spacing between items, resulting in a less cluttered and more spread-out view.
     */
    Relaxed,
}
