package net.thunderbird.feature.funding.googleplay.data.remote

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.flow
import net.thunderbird.core.outcome.Outcome
import net.thunderbird.core.outcome.flatMapSuccess
import net.thunderbird.feature.funding.googleplay.data.FundingDataContract
import net.thunderbird.feature.funding.googleplay.domain.FundingDomainContract.ContributionError
import net.thunderbird.feature.funding.googleplay.domain.entity.Contribution
import net.thunderbird.feature.funding.googleplay.domain.entity.OneTimeContribution
import net.thunderbird.feature.funding.googleplay.domain.entity.RecurringContribution

internal class RemoteContributionDataSource(
    private val billingClient: FundingDataContract.Remote.BillingClient,
) : FundingDataContract.Remote.ContributionDataSource {

    override fun getAllOneTime(
        productIds: List<String>,
    ): Flow<Outcome<List<OneTimeContribution>, ContributionError>> = flow {
        val result = billingClient.connect {
            billingClient.loadOneTimeContributions(productIds = productIds)
        }
        emit(result)
    }

    override fun getAllRecurring(
        productIds: List<String>,
    ): Flow<Outcome<List<RecurringContribution>, ContributionError>> = flow {
        val result = billingClient.connect {
            billingClient.loadRecurringContributions(productIds = productIds)
        }
        emit(result)
    }

    override fun getAllPurchased(): Flow<Outcome<List<Contribution>, ContributionError>> = flow {
        val result = billingClient.connect {
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
        contribution: Contribution,
    ): Outcome<Unit, ContributionError> {
        return billingClient.connect {
            billingClient.purchaseContribution(contribution)
        }
    }

    override fun clear() {
        billingClient.disconnect()
    }
}
