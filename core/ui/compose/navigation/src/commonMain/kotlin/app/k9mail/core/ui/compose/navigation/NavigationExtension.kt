package app.k9mail.core.ui.compose.navigation

import androidx.compose.animation.AnimatedContentScope
import androidx.compose.runtime.Composable
import androidx.navigation.NavBackStackEntry
import androidx.navigation.NavGraphBuilder
import androidx.navigation.compose.composable
import androidx.navigation.navDeepLink

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
