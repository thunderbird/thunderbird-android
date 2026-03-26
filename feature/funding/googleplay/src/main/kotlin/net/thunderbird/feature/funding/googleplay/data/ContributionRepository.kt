package net.thunderbird.feature.funding.googleplay.data

import kotlinx.coroutines.flow.Flow
import net.thunderbird.core.outcome.Outcome
import net.thunderbird.feature.funding.googleplay.data.FundingDataContract.Remote.ContributionDataSource
import net.thunderbird.feature.funding.googleplay.domain.FundingDomainContract
import net.thunderbird.feature.funding.googleplay.domain.FundingDomainContract.ContributionError
import net.thunderbird.feature.funding.googleplay.domain.entity.ContributionId
import net.thunderbird.feature.funding.googleplay.domain.entity.OneTimeContribution
import net.thunderbird.feature.funding.googleplay.domain.entity.PurchasedContribution
import net.thunderbird.feature.funding.googleplay.domain.entity.RecurringContribution

internal class ContributionRepository(
    private val remoteContributionDataSource: ContributionDataSource,
) : FundingDomainContract.ContributionRepository {

    override fun getAllOneTime(
        contributionIds: List<ContributionId>,
    ): Flow<Outcome<List<OneTimeContribution>, ContributionError>> =
        remoteContributionDataSource.getAllOneTime(
            contributionIds = contributionIds,
        )

    override fun getAllRecurring(
        contributionIds: List<ContributionId>,
    ): Flow<Outcome<List<RecurringContribution>, ContributionError>> =
        remoteContributionDataSource.getAllRecurring(
            contributionIds = contributionIds,
        )

    override fun getAllPurchased(): Flow<Outcome<List<PurchasedContribution>, ContributionError>> =
        remoteContributionDataSource.getAllPurchased()

    override suspend fun purchaseContribution(contributionId: ContributionId): Outcome<Unit, ContributionError> =
        remoteContributionDataSource.purchaseContribution(contributionId)

    override fun clear() = remoteContributionDataSource.clear()
}
