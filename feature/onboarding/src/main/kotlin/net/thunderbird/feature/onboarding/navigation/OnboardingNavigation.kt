package net.thunderbird.feature.onboarding.navigation

import androidx.navigation.NavController
import androidx.navigation.NavGraphBuilder
import androidx.navigation.NavOptions
import androidx.navigation.compose.composable
import net.thunderbird.feature.onboarding.OnboardingScreen

const val NAVIGATION_ROUTE_ONBOARDING = "onboarding"

fun NavController.navigateToOnboarding(
    navOptions: NavOptions? = null,
) {
    navigate(NAVIGATION_ROUTE_ONBOARDING, navOptions)
}

fun NavGraphBuilder.onboardingScreen(
    onStartClick: () -> Unit,
    onImportClick: () -> Unit,
) {
    composable(route = NAVIGATION_ROUTE_ONBOARDING) {
        OnboardingScreen(
            onStartClick = onStartClick,
            onImportClick = onImportClick,
        )
    }
}
