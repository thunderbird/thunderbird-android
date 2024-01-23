package app.k9mail.feature.onboarding.main.navigation

import androidx.navigation.NavGraphBuilder
import app.k9mail.core.ui.compose.common.navigation.deepLinkComposable

const val NAVIGATION_ROUTE_ONBOARDING = "onboarding"

fun NavGraphBuilder.onboardingRoute(
    onFinish: (String?) -> Unit,
) {
    deepLinkComposable(route = NAVIGATION_ROUTE_ONBOARDING) {
        OnboardingNavHost(
            onFinish = onFinish,
        )
    }
}
