package net.thunderbird.feature.funding.googleplay.data.remote.bilingclient

import android.content.Context
import com.android.billingclient.api.BillingClient
import com.android.billingclient.api.PendingPurchasesParams
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

    private var listener: PurchasesUpdatedListener? = null

    override fun setPurchasesUpdatedListener(listener: PurchasesUpdatedListener) {
        this.listener = listener
    }

    private fun createBillingClient(): BillingClient {
        require(listener != null) { "PurchasesUpdatedListener must be set before creating the billing client" }

        return BillingClient.newBuilder(context)
            .setListener(listener!!)
            .enablePendingPurchases(
                PendingPurchasesParams.newBuilder()
                    .enableOneTimeProducts()
                    .build(),
            )
            .build()
    }

    override fun clear() {
        clientInstance?.endConnection()
        clientInstance = null
    }
}
