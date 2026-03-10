package net.thunderbird.feature.funding.googleplay.data.remote.bilingclient

import com.android.billingclient.api.BillingResult
import net.thunderbird.core.outcome.Outcome
import net.thunderbird.feature.funding.googleplay.data.FundingDataContract.Mapper
import net.thunderbird.feature.funding.googleplay.domain.FundingDomainContract.ContributionError

internal class FakeBillingResultMapper : Mapper.BillingResult {
    var outcome: Outcome<*, ContributionError>? = null

    @Suppress("UNCHECKED_CAST")
    override suspend fun <T> mapToOutcome(
        billingResult: BillingResult,
        transformSuccess: suspend () -> T,
    ): Outcome<T, ContributionError> {
        return (outcome ?: Outcome.success(transformSuccess())) as Outcome<T, ContributionError>
    }
}
