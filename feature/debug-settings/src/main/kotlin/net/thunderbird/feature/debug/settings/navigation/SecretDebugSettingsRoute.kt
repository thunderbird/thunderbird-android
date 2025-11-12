package net.thunderbird.feature.debug.settings.navigation

import androidx.annotation.StringRes
import app.k9mail.core.ui.compose.navigation.Route
import kotlinx.serialization.Serializable
import net.thunderbird.feature.debug.settings.R

private const val SECRET_DEBUG_SETTINGS = "app://secret_debug_settings"

@Serializable
data class SecretDebugSettingsRoute(val tab: Tab = Tab.Notification) : Route {
    override val basePath: String = "$SECRET_DEBUG_SETTINGS/${tab.name}"
    override fun route(): String = basePath
    enum class Tab(@param:StringRes val titleRes: Int) {
        Notification(titleRes = R.string.debug_settings_notifications_title),
        FeatureFlag(titleRes = R.string.debug_settings_feature_flag_title),
        ;
    }
}
