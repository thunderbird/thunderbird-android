package net.thunderbird.feature.account.settings.api

import app.k9mail.core.ui.compose.navigation.Route
import kotlinx.serialization.Serializable

sealed interface AccountSettingsRoute : Route {

    @Serializable
    data class GeneralSettings(val accountId: String) : AccountSettingsRoute {
        override val basePath: String = BASE_PATH

        override fun route(): String = "$basePath/$accountId"

        companion object {
            const val BASE_PATH = "$ACCOUNT_SETTINGS_BASE_PATH/general"
        }
    }

    companion object {
        const val ACCOUNT_SETTINGS_BASE_PATH = "app://account/settings"
    }
}
