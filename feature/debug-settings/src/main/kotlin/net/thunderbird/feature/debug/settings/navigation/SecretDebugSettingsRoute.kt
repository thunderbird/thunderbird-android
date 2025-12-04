package net.thunderbird.feature.debug.settings.navigation

import androidx.annotation.StringRes
import app.k9mail.core.ui.compose.navigation.Route
import kotlinx.serialization.Serializable
import net.thunderbird.feature.debug.settings.R

@Serializable
data class SecretDebugSettingsRoute(val tab: Tab = Tab.Notification) : Route {
    override val basePath: String = "$SECRET_DEBUG_SETTINGS_BASE_PATH/{tab}"
    override fun route(): String = "$SECRET_DEBUG_SETTINGS_BASE_PATH/${tab.name}"

    @Suppress("detekt.UnnecessaryAnnotationUseSiteTarget") // https://github.com/detekt/detekt/issues/8212
    enum class Tab(@param:StringRes val titleRes: Int) {
        Notification(titleRes = R.string.debug_settings_notifications_title),
        FeatureFlag(titleRes = R.string.debug_settings_feature_flag_title),
    }

    companion object {
        const val SECRET_DEBUG_SETTINGS_BASE_PATH = "app://secret_debug_settings"
    }
}
