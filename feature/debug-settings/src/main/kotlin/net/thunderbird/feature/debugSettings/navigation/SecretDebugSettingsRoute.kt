package net.thunderbird.feature.debugSettings.navigation

import app.k9mail.core.ui.compose.navigation.Route
import kotlinx.serialization.Serializable

sealed interface SecretDebugSettingsRoute : Route {
    @Serializable
    data object Notification : SecretDebugSettingsRoute {
        override val basePath: String = "$SECRET_DEBUG_SETTINGS/notification"

        override fun route(): String = basePath
    }

    companion object {
        const val SECRET_DEBUG_SETTINGS = "app://secret_debug_settings"
    }
}
