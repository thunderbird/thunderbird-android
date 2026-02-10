package net.thunderbird.feature.funding.googleplay.domain

import android.app.Activity
import kotlinx.coroutines.flow.StateFlow
import net.thunderbird.core.outcome.Outcome
import net.thunderbird.feature.funding.googleplay.data.FundingDataContract
import net.thunderbird.feature.funding.googleplay.domain.FundingDomainContract.ContributionError
import net.thunderbird.feature.funding.googleplay.domain.entity.Contribution

internal class ContributionManager(
    private val billingClient: FundingDataContract.Remote.BillingClient,
) : FundingDomainContract.ContributionManager {

    override val purchasedContribution: StateFlow<Outcome<Contribution?, ContributionError>> =
        billingClient.purchasedContribution

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
