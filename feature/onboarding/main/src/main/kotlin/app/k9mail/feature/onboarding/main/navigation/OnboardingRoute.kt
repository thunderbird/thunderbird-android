package app.k9mail.feature.onboarding.main.navigation

import kotlinx.serialization.Serializable
import net.thunderbird.core.ui.navigation.Route

interface OnboardingRoute : Route {

    @Serializable
    data class Onboarding(
        val accountId: String? = null,
    ) : OnboardingRoute {
        override val basePath: String = BASE_PATH

        override fun route(): String = basePath

        companion object {
            const val BASE_PATH = ONBOARDING_BASE_PATH
        }
    }

    data object ThundermailSignIn : OnboardingRoute {
        override val basePath: String = ONBOARDING_BASE_PATH

        override fun route(): String = "$basePath/thundermail-sign-in"
    }

    data object ThundermailScanQrCode : OnboardingRoute {
        override val basePath: String = ONBOARDING_BASE_PATH

        override fun route(): String = "$basePath/thundermail-scan-qr-code"
    }

    companion object {
        const val ONBOARDING_BASE_PATH = "app://onboarding"
    }
}
