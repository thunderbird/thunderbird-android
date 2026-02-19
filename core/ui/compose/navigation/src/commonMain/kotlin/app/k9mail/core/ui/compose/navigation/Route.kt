package app.k9mail.core.ui.compose.navigation

/**
 * A Route represents a destination in the app.
 *
 * It is used to navigate to a specific screen using type-safe composable navigation
 * and deep links.
 *
 * @see Navigation
 */
interface Route {
    val basePath: String

    /**
     * The route to navigate to this screen.
     */
    fun route(): String
}
