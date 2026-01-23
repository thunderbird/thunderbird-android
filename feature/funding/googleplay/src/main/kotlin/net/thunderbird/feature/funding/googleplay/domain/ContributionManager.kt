package net.thunderbird.feature.funding.googleplay.domain

import android.app.Activity
import kotlinx.coroutines.flow.StateFlow
import net.thunderbird.core.outcome.Outcome
import net.thunderbird.core.outcome.flatMapSuccess
import net.thunderbird.core.outcome.mapSuccess
import net.thunderbird.feature.funding.googleplay.data.FundingDataContract
import net.thunderbird.feature.funding.googleplay.domain.FundingDomainContract.ContributionError
import net.thunderbird.feature.funding.googleplay.domain.entity.Contribution
import net.thunderbird.feature.funding.googleplay.domain.entity.OneTimeContribution
import net.thunderbird.feature.funding.googleplay.domain.entity.RecurringContribution

internal class ContributionManager(
    private val billingClient: FundingDataContract.BillingClient,
    private val contributionIdProvider: FundingDomainContract.ContributionIdProvider,
) : FundingDomainContract.ContributionManager {

    override val purchasedContribution: StateFlow<Outcome<Contribution?, ContributionError>> =
        billingClient.purchasedContribution

    override suspend fun loadOneTimeContributions(): Outcome<List<OneTimeContribution>, ContributionError> {
        return billingClient.connect {
            billingClient.loadOneTimeContributions(
                productIds = contributionIdProvider.oneTimeContributionIds,
            ).mapSuccess { contributions ->
                contributions.sortedByDescending { it.price }
            }
        }
    }

    override suspend fun loadRecurringContributions(): Outcome<List<RecurringContribution>, ContributionError> {
        return billingClient.connect {
            billingClient.loadRecurringContributions(
                productIds = contributionIdProvider.recurringContributionIds,
            ).mapSuccess { contributions ->
                contributions.sortedByDescending { it.price }
            }
        }
    }

    override suspend fun loadPurchasedContributions(): Outcome<List<Contribution>, ContributionError> {
        return billingClient.connect {
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
    }

    override suspend fun purchaseContribution(
        activity: Activity,
        contribution: Contribution,
    ): Outcome<Unit, ContributionError> {
        return billingClient.connect {
            billingClient.purchaseContribution(activity, contribution)
        }
    }

    override fun clear() {
        billingClient.disconnect()
    }
}
