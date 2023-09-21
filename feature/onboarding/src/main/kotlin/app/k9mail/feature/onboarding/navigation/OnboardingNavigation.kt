package app.k9mail.feature.onboarding.navigation

import androidx.navigation.NavController
import androidx.navigation.NavGraphBuilder
import androidx.navigation.NavOptions
import androidx.navigation.compose.composable
import androidx.navigation.navDeepLink
import app.k9mail.feature.onboarding.ui.OnboardingScreen

const val NAVIGATION_ROUTE_ONBOARDING = "/onboarding"
const val DEEPLINK_ONBOARDING = "app://$NAVIGATION_ROUTE_ONBOARDING"

fun NavController.navigateToOnboarding(
    navOptions: NavOptions? = null,
) {
    navigate(NAVIGATION_ROUTE_ONBOARDING, navOptions)
}

fun NavGraphBuilder.onboardingRoute(
    onStart: () -> Unit,
    onImport: () -> Unit,
) {
    composable(
        route = NAVIGATION_ROUTE_ONBOARDING,
        deepLinks = listOf(
            navDeepLink { uriPattern = DEEPLINK_ONBOARDING },
        ),
    ) {
        OnboardingScreen(
            onStartClick = onStart,
            onImportClick = onImport,
        )
    }
}
