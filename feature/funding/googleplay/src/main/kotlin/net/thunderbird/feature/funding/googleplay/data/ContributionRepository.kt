package net.thunderbird.feature.funding.googleplay.data

import kotlinx.coroutines.flow.Flow
import net.thunderbird.core.outcome.Outcome
import net.thunderbird.feature.funding.googleplay.data.FundingDataContract.Remote.ContributionDataSource
import net.thunderbird.feature.funding.googleplay.domain.FundingDomainContract
import net.thunderbird.feature.funding.googleplay.domain.FundingDomainContract.ContributionError
import net.thunderbird.feature.funding.googleplay.domain.entity.Contribution
import net.thunderbird.feature.funding.googleplay.domain.entity.OneTimeContribution
import net.thunderbird.feature.funding.googleplay.domain.entity.RecurringContribution

internal class ContributionRepository(
    private val remoteContributionDataSource: ContributionDataSource,
) : FundingDomainContract.ContributionRepository {

    override fun getAllOneTime(productIds: List<String>): Flow<Outcome<List<OneTimeContribution>, ContributionError>> =
        remoteContributionDataSource.getAllOneTime(
            productIds = productIds,
        )

    override fun getAllRecurring(
        productIds: List<String>,
    ): Flow<Outcome<List<RecurringContribution>, ContributionError>> =
        remoteContributionDataSource.getAllRecurring(
            productIds = productIds,
        )

    override fun getAllPurchased(): Flow<Outcome<List<Contribution>, ContributionError>> =
        remoteContributionDataSource.getAllPurchased()
}
