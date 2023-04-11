package app.k9mail.feature.preview.navigation

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import net.thunderbird.feature.onboarding.navigation.NAVIGATION_ROUTE_ONBOARDING
import net.thunderbird.feature.onboarding.navigation.onboardingScreen

@Composable
fun FeatureNavHost(
    navController: NavHostController,
    modifier: Modifier = Modifier,
    startDestination: String = NAVIGATION_ROUTE_ONBOARDING,
) {
    NavHost(
        navController = navController,
        startDestination = startDestination,
        modifier = modifier,
    ) {
        onboardingScreen(
            onStartClick = { /* TODO */ },
            onImportClick = { /* TODO */ },
        )
    }
}
