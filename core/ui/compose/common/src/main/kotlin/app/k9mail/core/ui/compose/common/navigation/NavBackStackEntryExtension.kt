package app.k9mail.core.ui.compose.common.navigation

import androidx.navigation.NavBackStackEntry

fun NavBackStackEntry.getStringArgument(key: String): String {
    return arguments?.getString(key) ?: error("Missing argument: $key")
}
