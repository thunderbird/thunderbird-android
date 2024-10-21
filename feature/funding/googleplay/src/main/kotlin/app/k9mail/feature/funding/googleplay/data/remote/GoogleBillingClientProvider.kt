package app.k9mail.feature.funding.googleplay.data.remote

import android.content.Context
import app.k9mail.feature.funding.googleplay.data.DataContract
import com.android.billingclient.api.BillingClient
import com.android.billingclient.api.PendingPurchasesParams
import com.android.billingclient.api.PurchasesUpdatedListener

/**
 * Google Billing client provider.
 *
 *  * It is responsible for creating and managing the billing client instance
 */
class GoogleBillingClientProvider(
    private val context: Context,
) : DataContract.Remote.GoogleBillingClientProvider {

    private var clientInstance: BillingClient? = null

    override val current: BillingClient
        get() = if (clientInstance != null) {
            clientInstance!!
        } else {
            clientInstance = createBillingClient()
            clientInstance!!
        }

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
