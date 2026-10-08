package net.thunderbird.feature.funding.googleplay.data.remote.bilingclient

import com.android.billingclient.api.BillingClient
import com.android.billingclient.api.PurchasesUpdatedListener
import net.thunderbird.feature.funding.googleplay.data.FundingDataContract.Remote

internal class FakeBillingClientProvider : Remote.BillingClientProvider {
    var listener: PurchasesUpdatedListener? = null
    var billingClient: BillingClient? = null

    override val current: BillingClient
        get() = billingClient ?: error("BillingClient not set")

    override fun setPurchasesUpdatedListener(listener: PurchasesUpdatedListener) {
        this.listener = listener
    }

    override fun clear() {
        // No-op
    }
}
