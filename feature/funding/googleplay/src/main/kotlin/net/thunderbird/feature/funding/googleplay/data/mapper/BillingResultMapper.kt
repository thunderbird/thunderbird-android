package net.thunderbird.feature.funding.googleplay.data.mapper

import com.android.billingclient.api.BillingClient.BillingResponseCode
import com.android.billingclient.api.BillingResult
import net.thunderbird.core.outcome.Outcome
import net.thunderbird.feature.funding.googleplay.data.FundingDataContract.Mapper
import net.thunderbird.feature.funding.googleplay.domain.FundingDomainContract.ContributionError

internal class BillingResultMapper : Mapper.BillingResult {

    override suspend fun <T> mapToOutcome(
        billingResult: BillingResult,
        transformSuccess: suspend () -> T,
    ): Outcome<T, ContributionError> {
        return when (billingResult.responseCode) {
            BillingResponseCode.OK -> {
                Outcome.success(transformSuccess())
            }

            else -> {
                Outcome.failure(mapToBillingError(billingResult))
            }
        }
    }

    private fun mapToBillingError(billingResult: BillingResult): ContributionError {
        return when (billingResult.responseCode) {
            BillingResponseCode.SERVICE_DISCONNECTED,
            BillingResponseCode.SERVICE_UNAVAILABLE,
            BillingResponseCode.BILLING_UNAVAILABLE,
            BillingResponseCode.NETWORK_ERROR,
            -> ContributionError.ServiceDisconnected(billingResult.debugMessage)

            BillingResponseCode.ITEM_ALREADY_OWNED,
            BillingResponseCode.ITEM_NOT_OWNED,
            BillingResponseCode.ITEM_UNAVAILABLE,
            -> ContributionError.PurchaseFailed(billingResult.debugMessage)

            BillingResponseCode.USER_CANCELED -> ContributionError.UserCancelled(billingResult.debugMessage)

            BillingResponseCode.DEVELOPER_ERROR -> ContributionError.DeveloperError(billingResult.debugMessage)

            else -> ContributionError.UnknownError(billingResult.debugMessage)
        }
    }
}
