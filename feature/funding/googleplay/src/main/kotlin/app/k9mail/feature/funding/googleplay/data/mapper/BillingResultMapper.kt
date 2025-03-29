package app.k9mail.feature.funding.googleplay.data.mapper

import app.k9mail.feature.funding.googleplay.data.DataContract.Mapper
import app.k9mail.feature.funding.googleplay.domain.DomainContract.BillingError
import com.android.billingclient.api.BillingClient.BillingResponseCode
import com.android.billingclient.api.BillingResult
import net.thunderbird.core.outcome.Outcome

class BillingResultMapper : Mapper.BillingResult {

    override suspend fun <T> mapToOutcome(
        billingResult: BillingResult,
        transformSuccess: suspend () -> T,
    ): Outcome<T, BillingError> {
        return when (billingResult.responseCode) {
            BillingResponseCode.OK -> {
                Outcome.success(transformSuccess())
            }

            else -> {
                Outcome.failure(mapToBillingError(billingResult))
            }
        }
    }

    private fun mapToBillingError(billingResult: BillingResult): BillingError {
        return when (billingResult.responseCode) {
            BillingResponseCode.SERVICE_DISCONNECTED,
            BillingResponseCode.SERVICE_UNAVAILABLE,
            BillingResponseCode.BILLING_UNAVAILABLE,
            BillingResponseCode.NETWORK_ERROR,
            -> BillingError.ServiceDisconnected(billingResult.debugMessage)

            BillingResponseCode.ITEM_ALREADY_OWNED,
            BillingResponseCode.ITEM_NOT_OWNED,
            BillingResponseCode.ITEM_UNAVAILABLE,
            -> BillingError.PurchaseFailed(billingResult.debugMessage)

            BillingResponseCode.USER_CANCELED -> BillingError.UserCancelled(billingResult.debugMessage)

            BillingResponseCode.DEVELOPER_ERROR -> BillingError.DeveloperError(billingResult.debugMessage)

            else -> BillingError.UnknownError(billingResult.debugMessage)
        }
    }
}
