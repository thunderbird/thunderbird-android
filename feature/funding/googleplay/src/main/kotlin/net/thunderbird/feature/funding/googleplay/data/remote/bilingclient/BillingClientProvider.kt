package net.thunderbird.feature.funding.googleplay.data.remote.bilingclient

import android.content.Context
import com.android.billingclient.api.BillingClient
import com.android.billingclient.api.BillingResult
import com.android.billingclient.api.PendingPurchasesParams
import com.android.billingclient.api.Purchase
import com.android.billingclient.api.PurchasesUpdatedListener
import net.thunderbird.feature.funding.googleplay.data.FundingDataContract

/**
 * Google Billing client provider.
 *
 * It is responsible for creating and managing the billing client instance
 */
class BillingClientProvider(
    private val context: Context,
) : FundingDataContract.Remote.BillingClientProvider {

    private var clientInstance: BillingClient? = null

    override val current: BillingClient
        get() = clientInstance ?: createBillingClient().also { clientInstance = it }

    private val delegatingListener = DelegatingPurchasesUpdatedListener()

    override fun setPurchasesUpdatedListener(listener: PurchasesUpdatedListener) {
        delegatingListener.listener = listener
    }

    private fun createBillingClient(): BillingClient {
        return BillingClient.newBuilder(context)
            .setListener(delegatingListener)
            .enablePendingPurchases(
                PendingPurchasesParams.newBuilder()
                    .enableOneTimeProducts()
                    .build(),
            )
            .build()
    }

    override fun clear() {
        clientInstance?.endConnection()
        delegatingListener.listener = null
        clientInstance = null
    }

    private class DelegatingPurchasesUpdatedListener : PurchasesUpdatedListener {
        var listener: PurchasesUpdatedListener? = null

        override fun onPurchasesUpdated(billingResult: BillingResult, purchases: MutableList<Purchase>?) {
            listener?.onPurchasesUpdated(billingResult, purchases)
        }
    }
}
