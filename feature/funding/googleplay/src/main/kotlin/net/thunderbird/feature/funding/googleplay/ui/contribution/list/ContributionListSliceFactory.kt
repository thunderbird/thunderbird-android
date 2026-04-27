package net.thunderbird.feature.funding.googleplay.ui.contribution.list

import kotlinx.coroutines.CoroutineScope
import net.thunderbird.core.logging.Logger
import net.thunderbird.feature.funding.googleplay.domain.FundingDomainContract

internal class ContributionListSliceFactory(
    private val getAvailableContributions: FundingDomainContract.UseCase.GetAvailableContributions,
    private val logger: Logger,
) : ContributionListSliceContract.Slice.Factory {

    override fun create(scope: CoroutineScope): ContributionListSliceContract.Slice =
        ContributionListSlice(
            getAvailableContributions = getAvailableContributions,
            logger = logger,
            scope = scope,
        )
}
