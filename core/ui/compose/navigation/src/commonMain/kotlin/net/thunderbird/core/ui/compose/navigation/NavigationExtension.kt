package net.thunderbird.core.ui.compose.navigation

import androidx.compose.animation.AnimatedContentScope
import androidx.compose.runtime.Composable
import androidx.navigation.NavBackStackEntry
import androidx.navigation.NavGraphBuilder
import androidx.navigation.compose.composable
import androidx.navigation.navDeepLink

/**
 * Extension function to register a composable route with a deep link.
 *
 * This function allows you to easily register a composable route that can be navigated to using a deep link.
 *
 * @param T the type of route
 * @param basePath the base path for the deep link
 * @param content the composable content to display for this route
 */
inline fun <reified T : Route> NavGraphBuilder.deepLinkComposable(
    basePath: String,
    noinline content: @Composable AnimatedContentScope.(NavBackStackEntry) -> Unit,
) {
    composable<T>(
        deepLinks = listOf(
            navDeepLink<T>(
                basePath = basePath,
            ),
        ),
        content = content,
    )
}
