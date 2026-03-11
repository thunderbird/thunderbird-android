package net.thunderbird.feature.funding.googleplay.data.remote

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.flow
import net.thunderbird.core.logging.Logger
import net.thunderbird.core.outcome.Outcome
import net.thunderbird.core.outcome.flatMapSuccess
import net.thunderbird.feature.funding.googleplay.data.FundingDataContract
import net.thunderbird.feature.funding.googleplay.domain.FundingDomainContract.ContributionError
import net.thunderbird.feature.funding.googleplay.domain.entity.Contribution
import net.thunderbird.feature.funding.googleplay.domain.entity.ContributionId
import net.thunderbird.feature.funding.googleplay.domain.entity.OneTimeContribution
import net.thunderbird.feature.funding.googleplay.domain.entity.RecurringContribution

internal class RemoteContributionDataSource(
    private val billingConnector: FundingDataContract.Remote.BillingConnector,
    private val billingClient: FundingDataContract.Remote.BillingClient,
    private val logger: Logger,
) : FundingDataContract.Remote.ContributionDataSource {

    override fun getAllOneTime(
        contributionIds: List<ContributionId>,
    ): Flow<Outcome<List<OneTimeContribution>, ContributionError>> = flow {
        val result = billingConnector.connect {
            billingClient.loadOneTimeContributions(productIds = contributionIds.map { it.value })
        }
        emit(result)
    }

    override fun getAllRecurring(
        contributionIds: List<ContributionId>,
    ): Flow<Outcome<List<RecurringContribution>, ContributionError>> = flow {
        val result = billingConnector.connect {
            billingClient.loadRecurringContributions(productIds = contributionIds.map { it.value })
        }
        emit(result)
    }

    override fun getAllPurchased(): Flow<Outcome<List<Contribution>, ContributionError>> = flow {
        val result = billingConnector.connect {
            billingClient.loadPurchasedRecurringContributions().flatMapSuccess { recurringContributions ->
                if (recurringContributions.isEmpty()) {
                    billingClient.loadPurchasedOneTimeContributionHistory().flatMapSuccess { contribution ->
                        if (contribution != null) {
                            Outcome.success(listOf(contribution))
                        } else {
                            Outcome.success(emptyList())
                        }
                    }
                } else {
                    Outcome.success(recurringContributions.sortedByDescending { it.price })
                }
            }
        }

        emit(result)
    }

    override val purchasedContribution: StateFlow<Outcome<Contribution?, ContributionError>> =
        billingClient.purchasedContribution

    override suspend fun purchaseContribution(
        contributionId: ContributionId,
    ): Outcome<Unit, ContributionError> {
        logger.debug { "Attempting to purchase contributionId: $contributionId" }
        return billingConnector.connect {
            logger.debug { "Initiating purchase flow for contributionId: $contributionId" }
            billingClient.purchaseContribution(contributionId)
        }
    }

    override fun clear() {
        billingClient.disconnect()
    }
}
