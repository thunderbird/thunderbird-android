package app.k9mail.core.ui.compose.navigation

import android.net.Uri
import androidx.core.net.toUri

/**
 * A Route represents a destination in the app.
 *
 * It is used to navigate to a specific screen using type-safe composable navigation
 * and deep links.
 *
 * @see Navigation
 */
interface Route {
    val deepLink: String

    fun toDeepLinkUri(): Uri = deepLink.toUri()
}
