package app.k9mail.core.ui.compose.common.navigation

import android.net.Uri
import androidx.compose.animation.AnimatedContentScope
import androidx.compose.runtime.Composable
import androidx.core.net.toUri
import androidx.navigation.NamedNavArgument
import androidx.navigation.NavBackStackEntry
import androidx.navigation.NavGraphBuilder
import androidx.navigation.compose.composable
import androidx.navigation.navDeepLink

fun NavGraphBuilder.deepLinkComposable(
    route: String,
    arguments: List<NamedNavArgument> = emptyList(),
    content: @Composable AnimatedContentScope.(NavBackStackEntry) -> Unit,
) {
    composable(
        route = route,
        arguments = arguments,
        deepLinks = listOf(
            navDeepLink { uriPattern = route.toDeepLink() },
        ),
        content = content,
    )
}

fun String.toDeepLink(): String = "app://$this"

fun String.toDeepLinkUri(): Uri = toDeepLink().toUri()
