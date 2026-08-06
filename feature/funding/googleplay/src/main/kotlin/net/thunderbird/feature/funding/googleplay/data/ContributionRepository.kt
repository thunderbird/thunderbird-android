package net.thunderbird.feature.funding.googleplay.data

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.onEach
import net.thunderbird.core.outcome.Outcome
import net.thunderbird.feature.funding.googleplay.data.FundingDataContract.Local.ContributionPurchaseDataSource
import net.thunderbird.feature.funding.googleplay.data.FundingDataContract.Remote.ContributionDataSource
import net.thunderbird.feature.funding.googleplay.domain.FundingDomainContract
import net.thunderbird.feature.funding.googleplay.domain.FundingDomainContract.ContributionError
import net.thunderbird.feature.funding.googleplay.domain.entity.ContributionId
import net.thunderbird.feature.funding.googleplay.domain.entity.OneTimeContribution
import net.thunderbird.feature.funding.googleplay.domain.entity.PurchasedContribution
import net.thunderbird.feature.funding.googleplay.domain.entity.RecurringContribution

internal class ContributionRepository(
    private val remoteDataSource: ContributionDataSource,
    private val localDataSource: ContributionPurchaseDataSource,
) : FundingDomainContract.ContributionRepository {

    override fun getAllOneTime(
        contributionIds: List<ContributionId>,
    ): Flow<Outcome<List<OneTimeContribution>, ContributionError>> =
        remoteDataSource.getAllOneTime(
            contributionIds = contributionIds,
        )

    override fun getAllRecurring(
        contributionIds: List<ContributionId>,
    ): Flow<Outcome<List<RecurringContribution>, ContributionError>> =
        remoteDataSource.getAllRecurring(
            contributionIds = contributionIds,
        )

    override fun getAllPurchased(): Flow<Outcome<List<PurchasedContribution>, ContributionError>> = combine(
        remoteDataSource.getAllPurchased(),
        localDataSource.get(),
        remoteDataSource.purchasedContribution.onEach { outcome ->
            (outcome as? Outcome.Success)?.data?.let { contribution ->
                if (contribution.contribution is OneTimeContribution) {
                    localDataSource.update(contribution)
                }
            }
        },
    ) { remoteOutcome, localOutcome, _ ->
        when {
            remoteOutcome is Outcome.Failure -> Outcome.failure(remoteOutcome.error)

            localOutcome is Outcome.Failure -> Outcome.failure(localOutcome.error)

            else -> {
                val remotePurchases = (remoteOutcome as Outcome.Success).data
                val localPurchase = (localOutcome as Outcome.Success).data
                Outcome.success(remotePurchases + listOfNotNull(localPurchase))
            }
        }
    }

    override suspend fun purchaseContribution(contributionId: ContributionId): Outcome<Unit, ContributionError> =
        remoteDataSource.purchaseContribution(contributionId)

    override fun clear() = remoteDataSource.clear()
}
