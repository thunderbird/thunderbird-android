package app.k9mail.feature.onboarding.main.navigation

import app.k9mail.core.ui.compose.navigation.Route
import kotlinx.serialization.Serializable

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

    companion object {
        const val ONBOARDING_BASE_PATH = "app://onboarding"
    }
}
