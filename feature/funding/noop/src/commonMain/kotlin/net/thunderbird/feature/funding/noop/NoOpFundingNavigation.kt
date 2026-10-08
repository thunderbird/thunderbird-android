package net.thunderbird.feature.funding.noop

import androidx.navigation.NavGraphBuilder
import net.thunderbird.feature.funding.api.FundingNavigation
import net.thunderbird.feature.funding.api.FundingRoute

class NoOpFundingNavigation : FundingNavigation {
    override fun registerRoutes(
        navGraphBuilder: NavGraphBuilder,
        onBack: () -> Unit,
        onFinish: (FundingRoute) -> Unit,
    ) {
        // no-op
    }
}
