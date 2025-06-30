package app.k9mail.feature.account.edit.navigation

import app.k9mail.core.ui.compose.navigation.Route
import kotlinx.serialization.Serializable

sealed interface AccountEditRoute : Route {

    @Serializable
    data class IncomingServerSettings(
        val accountId: String,
    ) : AccountEditRoute {
        override val basePath: String = BASE_PATH

        override fun route(): String = "$basePath/$accountId"

        companion object {
            const val BASE_PATH = "$ACCOUNT_EDIT_BASE_PATH/incoming"
        }
    }

    @Serializable
    data class OutgoingServerSettings(val accountId: String) : AccountEditRoute {
        override val basePath: String = BASE_PATH

        override fun route(): String = "$basePath/$accountId"

        companion object {
            const val BASE_PATH = "$ACCOUNT_EDIT_BASE_PATH/outgoing"
        }
    }

    companion object {
        const val ACCOUNT_EDIT_BASE_PATH = "app://account/edit"
    }
}
