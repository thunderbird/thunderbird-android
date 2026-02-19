package app.k9mail.feature.account.setup.navigation

import kotlinx.serialization.Serializable
import net.thunderbird.core.ui.compose.navigation.Route

sealed interface AccountSetupRoute : Route {

    @Serializable
    data class AccountSetup(
        val accountId: String? = null,
    ) : AccountSetupRoute {
        override val basePath: String = BASE_PATH

        override fun route(): String = basePath

        companion object {
            const val BASE_PATH = ACCOUNT_SETUP_BASE_PATH
        }
    }

    companion object {
        const val ACCOUNT_SETUP_BASE_PATH = "app://account/setup"
    }
}
