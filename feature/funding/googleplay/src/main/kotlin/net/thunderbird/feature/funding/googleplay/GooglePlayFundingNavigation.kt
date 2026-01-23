package net.thunderbird.feature.funding.googleplay

import androidx.navigation.NavGraphBuilder
import app.k9mail.core.ui.compose.navigation.deepLinkComposable
import net.thunderbird.feature.funding.api.FundingNavigation
import net.thunderbird.feature.funding.api.FundingRoute
import net.thunderbird.feature.funding.api.FundingRoute.Contribution
import net.thunderbird.feature.funding.googleplay.ui.contribution.ContributionScreen

class GooglePlayFundingNavigation : FundingNavigation {

    override fun registerRoutes(
        navGraphBuilder: NavGraphBuilder,
        onBack: () -> Unit,
        onFinish: (FundingRoute) -> Unit,
    ) {
        with(navGraphBuilder) {
            deepLinkComposable<Contribution>(Contribution.BASE_PATH) {
                ContributionScreen(
                    onBack = onBack,
                )
            }
        }
    }
}
