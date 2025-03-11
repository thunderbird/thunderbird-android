package app.k9mail.feature.funding.googleplay

import androidx.navigation.NavGraphBuilder
import app.k9mail.core.ui.compose.navigation.deepLinkComposable
import app.k9mail.feature.funding.api.FundingNavigation
import app.k9mail.feature.funding.api.FundingRoute
import app.k9mail.feature.funding.api.FundingRoute.Contribution
import app.k9mail.feature.funding.googleplay.ui.contribution.ContributionScreen

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
