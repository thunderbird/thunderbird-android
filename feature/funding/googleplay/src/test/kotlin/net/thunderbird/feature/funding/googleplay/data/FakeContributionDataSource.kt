package net.thunderbird.feature.funding.googleplay.data

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import net.thunderbird.core.outcome.Outcome
import net.thunderbird.feature.funding.googleplay.domain.FundingDomainContract.ContributionError
import net.thunderbird.feature.funding.googleplay.domain.entity.ContributionId
import net.thunderbird.feature.funding.googleplay.domain.entity.OneTimeContribution
import net.thunderbird.feature.funding.googleplay.domain.entity.PurchasedContribution
import net.thunderbird.feature.funding.googleplay.domain.entity.RecurringContribution

internal class FakeContributionDataSource : FundingDataContract.Remote.ContributionDataSource {
    var oneTimeFlow: Flow<Outcome<List<OneTimeContribution>, ContributionError>> =
        flowOf(Outcome.success(emptyList()))
    var recurringFlow: Flow<Outcome<List<RecurringContribution>, ContributionError>> =
        flowOf(Outcome.success(emptyList()))

    override fun getAllOneTime(
        contributionIds: List<ContributionId>,
    ): Flow<Outcome<List<OneTimeContribution>, ContributionError>> = oneTimeFlow

    override fun getAllRecurring(
        contributionIds: List<ContributionId>,
    ): Flow<Outcome<List<RecurringContribution>, ContributionError>> = recurringFlow

    var purchasedFlow: Flow<Outcome<List<PurchasedContribution>, ContributionError>> =
        flowOf(Outcome.success(emptyList()))

    override fun getAllPurchased(): Flow<Outcome<List<PurchasedContribution>, ContributionError>> = purchasedFlow

    override suspend fun purchaseContribution(
        contributionId: ContributionId,
    ): Outcome<Unit, ContributionError> = Outcome.success(Unit)

    override fun clear() = Unit
}
