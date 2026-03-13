package net.thunderbird.feature.funding.googleplay.data.remote.bilingclient

import com.android.billingclient.api.BillingClient.BillingResponseCode
import com.android.billingclient.api.BillingResult
import net.thunderbird.core.outcome.Outcome
import net.thunderbird.feature.funding.googleplay.domain.FundingDomainContract.ContributionError

internal fun <T> BillingResult.mapToOutcome(
    transformSuccess: () -> T,
): Outcome<T, ContributionError> {
    return when (responseCode) {
        BillingResponseCode.OK -> {
            Outcome.success(transformSuccess())
        }

        else -> {
            Outcome.failure(mapToBillingError())
        }
    }
}

private fun BillingResult.mapToBillingError(): ContributionError {
    return when (responseCode) {
        BillingResponseCode.SERVICE_DISCONNECTED,
        BillingResponseCode.SERVICE_UNAVAILABLE,
        BillingResponseCode.BILLING_UNAVAILABLE,
        BillingResponseCode.NETWORK_ERROR,
        -> ContributionError.ServiceDisconnected(debugMessage)

        BillingResponseCode.ITEM_ALREADY_OWNED,
        BillingResponseCode.ITEM_NOT_OWNED,
        BillingResponseCode.ITEM_UNAVAILABLE,
        -> ContributionError.PurchaseFailed(debugMessage)

        BillingResponseCode.USER_CANCELED -> ContributionError.UserCancelled(debugMessage)

        BillingResponseCode.DEVELOPER_ERROR -> ContributionError.DeveloperError(debugMessage)

        else -> ContributionError.UnknownError(debugMessage)
    }
}
