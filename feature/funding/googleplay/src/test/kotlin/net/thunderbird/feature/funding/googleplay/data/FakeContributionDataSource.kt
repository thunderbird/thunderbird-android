package net.thunderbird.feature.funding.googleplay.data

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import net.thunderbird.core.outcome.Outcome
import net.thunderbird.feature.funding.googleplay.domain.FundingDomainContract.ContributionError
import net.thunderbird.feature.funding.googleplay.domain.entity.Contribution
import net.thunderbird.feature.funding.googleplay.domain.entity.OneTimeContribution
import net.thunderbird.feature.funding.googleplay.domain.entity.RecurringContribution

internal class FakeContributionDataSource : FundingDataContract.Remote.ContributionDataSource {
    var oneTimeFlow: Flow<Outcome<List<OneTimeContribution>, ContributionError>> =
        flowOf(Outcome.success(emptyList()))
    var recurringFlow: Flow<Outcome<List<RecurringContribution>, ContributionError>> =
        flowOf(Outcome.success(emptyList()))

    override fun getAllOneTime(
        productIds: List<String>,
    ): Flow<Outcome<List<OneTimeContribution>, ContributionError>> = oneTimeFlow

    override fun getAllRecurring(
        productIds: List<String>,
    ): Flow<Outcome<List<RecurringContribution>, ContributionError>> = recurringFlow

    var purchasedFlow: Flow<Outcome<List<Contribution>, ContributionError>> =
        flowOf(Outcome.success(emptyList()))

    override fun getAllPurchased(): Flow<Outcome<List<Contribution>, ContributionError>> = purchasedFlow
}
