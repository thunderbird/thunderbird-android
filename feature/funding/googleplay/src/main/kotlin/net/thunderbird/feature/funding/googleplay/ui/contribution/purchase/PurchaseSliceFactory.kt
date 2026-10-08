package net.thunderbird.feature.funding.googleplay.ui.contribution.purchase

import kotlinx.coroutines.CoroutineScope
import net.thunderbird.core.logging.Logger
import net.thunderbird.feature.funding.googleplay.domain.FundingDomainContract
import net.thunderbird.feature.funding.googleplay.domain.FundingDomainContract.UseCase

internal class PurchaseSliceFactory(
    private val getLastestPurchase: UseCase.GetLatestPurchasedContribution,
    private val repository: FundingDomainContract.ContributionRepository,
    private val logger: Logger,
) : PurchaseSliceContract.Slice.Factory {

    override fun create(scope: CoroutineScope): PurchaseSliceContract.Slice =
        PurchaseSlice(
            getLastestPurchase = getLastestPurchase,
            repository = repository,
            logger = logger,
            scope = scope,
        )
}
