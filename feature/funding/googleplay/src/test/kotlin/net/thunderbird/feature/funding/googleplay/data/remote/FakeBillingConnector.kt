package net.thunderbird.feature.funding.googleplay.data.remote

import net.thunderbird.core.outcome.Outcome
import net.thunderbird.feature.funding.googleplay.data.FundingDataContract.Remote
import net.thunderbird.feature.funding.googleplay.domain.FundingDomainContract.ContributionError

internal class FakeBillingConnector : Remote.BillingConnector {
    var connectOutcome: Outcome<*, ContributionError>? = null

    @Suppress("UNCHECKED_CAST")
    override suspend fun <T> connect(
        onConnected: suspend () -> Outcome<T, ContributionError>,
    ): Outcome<T, ContributionError> {
        return (connectOutcome ?: onConnected()) as Outcome<T, ContributionError>
    }

    override fun disconnect() {
        // No-op
    }
}
