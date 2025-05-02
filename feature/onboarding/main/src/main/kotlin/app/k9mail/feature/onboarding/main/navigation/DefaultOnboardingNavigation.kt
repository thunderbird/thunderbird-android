package app.k9mail.feature.onboarding.main.navigation

import androidx.navigation.NavGraphBuilder
import app.k9mail.core.ui.compose.navigation.deepLinkComposable
import app.k9mail.feature.onboarding.main.navigation.OnboardingRoute.Onboarding

class DefaultOnboardingNavigation : OnboardingNavigation {
    override fun registerRoutes(
        navGraphBuilder: NavGraphBuilder,
        onBack: () -> Unit,
        onFinish: (OnboardingRoute) -> Unit,
    ) {
        with(navGraphBuilder) {
            deepLinkComposable<Onboarding>(Onboarding.BASE_PATH) {
                OnboardingNavHost(
                    onFinish = onFinish,
                )
            }
        }
    }
}
