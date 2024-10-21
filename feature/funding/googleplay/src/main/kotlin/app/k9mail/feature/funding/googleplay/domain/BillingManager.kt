package app.k9mail.feature.funding.googleplay.domain

import android.app.Activity
import app.k9mail.feature.funding.googleplay.data.DataContract
import app.k9mail.feature.funding.googleplay.domain.DomainContract.BillingError
import app.k9mail.feature.funding.googleplay.domain.entity.Contribution
import app.k9mail.feature.funding.googleplay.domain.entity.OneTimeContribution
import app.k9mail.feature.funding.googleplay.domain.entity.RecurringContribution
import kotlinx.coroutines.flow.StateFlow

class BillingManager(
    private val billingClient: DataContract.BillingClient,
    private val contributionIdProvider: DomainContract.ContributionIdProvider,
) : DomainContract.BillingManager {

    override val purchasedContribution: StateFlow<Outcome<Contribution?, BillingError>> =
        billingClient.purchasedContribution

    override suspend fun loadOneTimeContributions(): List<OneTimeContribution> {
        return billingClient.connect {
            billingClient.loadOneTimeContributions(contributionIdProvider.oneTimeContributionIds)
                .sortedByDescending { it.price }
        }
    }

    override suspend fun loadRecurringContributions(): List<RecurringContribution> {
        return billingClient.connect {
            billingClient.loadRecurringContributions(contributionIdProvider.recurringContributionIds)
                .sortedByDescending { it.price }
        }
    }

    override suspend fun loadPurchasedContributions(): List<Contribution> {
        return billingClient.connect {
            billingClient.loadPurchasedContributions()
                .sortedByDescending { it.price }
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
