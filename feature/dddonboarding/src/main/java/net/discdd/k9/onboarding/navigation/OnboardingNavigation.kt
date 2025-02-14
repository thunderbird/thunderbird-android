package net.discdd.k9.onboarding.navigation

import androidx.navigation.NavGraphBuilder
import app.k9mail.core.ui.compose.common.navigation.deepLinkComposable

const val NAVIGATION_ROUTE_DDD_ONBOARDING = "ddd_onboarding"

fun NavGraphBuilder.dddOnboardingRoute(
    onFinish: (String?) -> Unit,
) {
    deepLinkComposable(route = NAVIGATION_ROUTE_DDD_ONBOARDING) {
        OnboardingNavHost(
            onFinish = onFinish
        )
    }
}
