package app.k9mail.feature.funding.googleplay.data

import android.app.Activity
import app.k9mail.feature.funding.googleplay.domain.DomainContract.BillingError
import app.k9mail.feature.funding.googleplay.domain.Outcome
import app.k9mail.feature.funding.googleplay.domain.entity.Contribution
import app.k9mail.feature.funding.googleplay.domain.entity.OneTimeContribution
import app.k9mail.feature.funding.googleplay.domain.entity.RecurringContribution
import com.android.billingclient.api.ProductDetails
import com.android.billingclient.api.Purchase
import com.android.billingclient.api.PurchasesUpdatedListener
import kotlinx.coroutines.flow.StateFlow
import com.android.billingclient.api.BillingClient as GoogleBillingClient
import com.android.billingclient.api.BillingResult as GoogleBillingResult

interface DataContract {

    interface Mapper {
        interface Product {
            fun mapToContribution(product: ProductDetails): Contribution

            fun mapToOneTimeContribution(product: ProductDetails): OneTimeContribution
            fun mapToRecurringContribution(product: ProductDetails): RecurringContribution
        }

        interface BillingResult {
            fun <T> mapToOutcome(
                billingResult: GoogleBillingResult,
                transformSuccess: () -> T,
            ): Outcome<T, BillingError>
        }
    }

    interface Remote {
        interface GoogleBillingClientProvider {
            val current: GoogleBillingClient

            /**
             * Set the listener to be notified of purchase updates.
             */
            fun setPurchasesUpdatedListener(listener: PurchasesUpdatedListener)

            /**
             * Disconnect from the billing service and clear the instance.
             */
            fun clear()
        }

        interface GoogleBillingPurchaseHandler {
            suspend fun handlePurchases(
                clientProvider: GoogleBillingClientProvider,
                purchases: List<Purchase>,
            ): List<Contribution>
        }
    }

    interface BillingClient {

        /**
         * Flow that emits the last purchased contribution.
         */
        val purchasedContribution: StateFlow<Outcome<Contribution?, BillingError>>

        /**
         * Connect to the billing service.
         *
         * @param onConnected Callback to be invoked when the billing service is connected.
         */
        suspend fun <T> connect(onConnected: suspend () -> T): T

        /**
         * Disconnect from the billing service.
         */
        fun disconnect()

        /**
         * Load one-time contributions.
         */
        suspend fun loadOneTimeContributions(
            productIds: List<String>,
        ): List<OneTimeContribution>

        /**
         * Load recurring contributions.
         */
        suspend fun loadRecurringContributions(
            productIds: List<String>,
        ): List<RecurringContribution>

        /**
         * Load purchased contributions.
         */
        suspend fun loadPurchasedContributions(): List<Contribution>

        /**
         * Purchase a contribution.
         */
        suspend fun purchaseContribution(
            activity: Activity,
            contribution: Contribution,
        ): Outcome<Unit, BillingError>
    }
}
