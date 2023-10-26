package app.k9mail.feature.onboarding.main.navigation

import androidx.navigation.NavController
import androidx.navigation.NavGraphBuilder
import androidx.navigation.NavOptions
import app.k9mail.core.ui.compose.common.navigation.deepLinkComposable

const val NAVIGATION_ROUTE_ONBOARDING = "onboarding"

fun NavController.navigateToOnboarding(
    navOptions: NavOptions? = null,
) {
    navigate(NAVIGATION_ROUTE_ONBOARDING, navOptions)
}

fun NavGraphBuilder.onboardingRoute(
    onImport: () -> Unit,
    onBack: () -> Unit,
    onFinish: (String) -> Unit,
) {
    deepLinkComposable(route = NAVIGATION_ROUTE_ONBOARDING) {
        OnboardingNavHost(
            onImport = onImport,
            onBack = onBack,
            onFinish = onFinish,
        )
    }
}
