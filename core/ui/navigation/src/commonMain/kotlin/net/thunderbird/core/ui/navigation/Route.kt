package net.thunderbird.core.ui.navigation

/**
 * A Route represents a destination in the app.
 *
 * It is used to navigate to a specific screen using type-safe composable navigation
 * and deep links.
 *
 * @see Navigation
 */
public interface Route {
    public val basePath: String

    /**
     * The route to navigate to this screen.
     */
    public fun route(): String
}
