package app.k9mail.core.ui.compose.navigation

import androidx.navigation.NavGraphBuilder

/**
 * A Navigation is responsible for registering routes with the navigation graph.
 *
 * @param T the type of route
 */
interface Navigation<T : Route> {

    /**
     * Register all routes for this navigation.
     *
     * @param onBack the action to perform when the back button is pressed
     * @param onFinish the action to perform when a route is finished
     */
    fun registerRoutes(
        navGraphBuilder: NavGraphBuilder,
        onBack: () -> Unit,
        onFinish: (T) -> Unit,
    )
}
