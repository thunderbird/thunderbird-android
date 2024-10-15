package app.k9mail.feature.funding.link

import androidx.navigation.NavGraphBuilder
import app.k9mail.feature.funding.api.FundingNavigation
import app.k9mail.feature.funding.api.FundingRoute

class LinkFundingNavigation : FundingNavigation {
    override fun registerRoutes(
        navGraphBuilder: NavGraphBuilder,
        onBack: () -> Unit,
        onFinish: (FundingRoute) -> Unit,
    ) {
        // no-op
    }
}
