package app.k9mail.feature.account.setup.navigation

import kotlinx.serialization.Serializable
import net.thunderbird.core.ui.navigation.Route

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

    data object ThundermailSignIn : AccountSetupRoute {
        override val basePath: String = ACCOUNT_SETUP_BASE_PATH

        override fun route(): String = "$basePath/thundermail-sign-in"
    }

    data object ThundermailScanQrCode : AccountSetupRoute {
        override val basePath: String = ACCOUNT_SETUP_BASE_PATH

        override fun route(): String = "$basePath/thundermail-scan-qr-code"
    }

    companion object {
        const val ACCOUNT_SETUP_BASE_PATH = "app://account/setup"
    }
}
