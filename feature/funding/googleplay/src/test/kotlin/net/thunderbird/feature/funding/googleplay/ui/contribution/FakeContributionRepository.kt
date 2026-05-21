package net.thunderbird.feature.funding.googleplay.ui.contribution

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import net.thunderbird.core.outcome.Outcome
import net.thunderbird.feature.funding.googleplay.domain.FundingDomainContract
import net.thunderbird.feature.funding.googleplay.domain.FundingDomainContract.ContributionError
import net.thunderbird.feature.funding.googleplay.domain.entity.ContributionId
import net.thunderbird.feature.funding.googleplay.domain.entity.OneTimeContribution
import net.thunderbird.feature.funding.googleplay.domain.entity.PurchasedContribution
import net.thunderbird.feature.funding.googleplay.domain.entity.RecurringContribution

internal class FakeContributionRepository : FundingDomainContract.ContributionRepository {

    var oneTimeFlow: Flow<Outcome<List<OneTimeContribution>, ContributionError>> =
        flowOf(Outcome.success(emptyList()))

    override fun getAllOneTime(
        contributionIds: List<ContributionId>,
    ): Flow<Outcome<List<OneTimeContribution>, ContributionError>> =
        oneTimeFlow

    var recurringFlow: Flow<Outcome<List<RecurringContribution>, ContributionError>> =
        flowOf(Outcome.success(emptyList()))

    override fun getAllRecurring(
        contributionIds: List<ContributionId>,
    ): Flow<Outcome<List<RecurringContribution>, ContributionError>> = recurringFlow

    var purchasedFlow: Flow<Outcome<List<PurchasedContribution>, ContributionError>> =
        flowOf(Outcome.success(emptyList()))

    override fun getAllPurchased(): Flow<Outcome<List<PurchasedContribution>, ContributionError>> =
        purchasedFlow

    var purchaseResult: Outcome<Unit, ContributionError> = Outcome.success(Unit)
    override suspend fun purchaseContribution(contributionId: ContributionId) =
        purchaseResult

    override fun clear() = Unit
}
