package app.k9mail.feature.funding.googleplay.domain

import android.app.Activity
import app.k9mail.feature.funding.googleplay.data.DataContract
import app.k9mail.feature.funding.googleplay.domain.DomainContract.BillingError
import app.k9mail.feature.funding.googleplay.domain.entity.Contribution
import app.k9mail.feature.funding.googleplay.domain.entity.OneTimeContribution
import app.k9mail.feature.funding.googleplay.domain.entity.RecurringContribution
import kotlinx.coroutines.flow.StateFlow

internal class BillingManager(
    private val billingClient: DataContract.BillingClient,
    private val contributionIdProvider: DomainContract.ContributionIdProvider,
) : DomainContract.BillingManager {

    override val purchasedContribution: StateFlow<Outcome<Contribution?, BillingError>> =
        billingClient.purchasedContribution

    override suspend fun loadOneTimeContributions(): Outcome<List<OneTimeContribution>, BillingError> {
        return billingClient.connect {
            billingClient.loadOneTimeContributions(
                productIds = contributionIdProvider.oneTimeContributionIds,
            ).mapSuccess { contributions ->
                contributions.sortedByDescending { it.price }
            }
        }
    }

    override suspend fun loadRecurringContributions(): Outcome<List<RecurringContribution>, BillingError> {
        return billingClient.connect {
            billingClient.loadRecurringContributions(
                productIds = contributionIdProvider.recurringContributionIds,
            ).mapSuccess { contributions ->
                contributions.sortedByDescending { it.price }
            }
        }
    }

    override suspend fun loadPurchasedContributions(): Outcome<List<Contribution>, BillingError> {
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
    ): Outcome<Unit, BillingError> {
        return billingClient.connect {
            billingClient.purchaseContribution(activity, contribution)
        }
    }

    override fun clear() {
        billingClient.disconnect()
    }
}
