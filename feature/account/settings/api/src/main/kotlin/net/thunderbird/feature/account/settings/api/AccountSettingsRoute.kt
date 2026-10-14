package net.thunderbird.feature.account.settings.api

import kotlinx.serialization.Serializable
import net.thunderbird.core.ui.navigation.Route

sealed interface AccountSettingsRoute : Route {

    @Serializable
    data class GeneralSettings(val accountId: String) : AccountSettingsRoute {
        override val basePath: String = BASE_PATH

        override fun route(): String = "$basePath/$accountId"

        companion object {
            const val BASE_PATH = "$ACCOUNT_SETTINGS_BASE_PATH/general"
        }
    }

    @Serializable
    data class ReadingMailSettings(val accountId: String) : AccountSettingsRoute {
        override val basePath: String = BASE_PATH

        override fun route(): String = "$basePath/$accountId"

        companion object {
            const val BASE_PATH = "$ACCOUNT_SETTINGS_BASE_PATH/reading_mail"
        }
    }

    @Serializable
    data class FetchingMailSettings(val accountId: String) : AccountSettingsRoute {
        override val basePath: String = BASE_PATH

        override fun route(): String = "$basePath/$accountId"

        companion object {
            const val BASE_PATH = "$ACCOUNT_SETTINGS_BASE_PATH/fetching_mail"
        }
    }

    @Serializable
    data class SendingMailSettings(val accountId: String) : AccountSettingsRoute {
        override val basePath: String = BASE_PATH

        override fun route(): String = "$basePath/$accountId"

        companion object {
            const val BASE_PATH = "$ACCOUNT_SETTINGS_BASE_PATH/sending_mail"
        }
    }

    @Serializable
    data class CompositionSettings(val accountId: String) : AccountSettingsRoute {
        override val basePath: String = BASE_PATH

        override fun route(): String = "$basePath/$accountId"

        companion object {
            const val BASE_PATH = "$ACCOUNT_SETTINGS_BASE_PATH/sending_mail/composition"
        }
    }

    @Serializable
    data class ManageIdentities(val accountId: String) : AccountSettingsRoute {
        override val basePath: String = BASE_PATH

        override fun route(): String = "$basePath/$accountId"

        companion object {
            const val BASE_PATH = "$ACCOUNT_SETTINGS_BASE_PATH/sending_mail/manage_identities"
        }
    }

    @Serializable
    data class AdvancedFetchingMailSettings(val accountId: String) : AccountSettingsRoute {
        override val basePath: String = BASE_PATH

        override fun route(): String = "$basePath/$accountId"

        companion object {
            const val BASE_PATH = "$ACCOUNT_SETTINGS_BASE_PATH/fetching_mail/advanced"
        }
    }

    @Serializable
    data class SearchSettings(val accountId: String) : AccountSettingsRoute {
        override val basePath: String = BASE_PATH

        override fun route(): String = "$basePath/$accountId"

        companion object {
            const val BASE_PATH = "$ACCOUNT_SETTINGS_BASE_PATH/search"
        }
    }

    companion object {
        const val ACCOUNT_SETTINGS_BASE_PATH = "app://account/settings"
    }
}
