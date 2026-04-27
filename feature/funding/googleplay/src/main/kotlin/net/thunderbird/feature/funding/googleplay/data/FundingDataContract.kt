package net.thunderbird.feature.funding.googleplay.data

import com.android.billingclient.api.ProductDetails
import com.android.billingclient.api.Purchase
import com.android.billingclient.api.PurchaseHistoryRecord
import com.android.billingclient.api.PurchasesUpdatedListener
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.StateFlow
import net.thunderbird.core.common.cache.Cache
import net.thunderbird.core.outcome.Outcome
import net.thunderbird.feature.funding.googleplay.domain.FundingDomainContract.ContributionError
import net.thunderbird.feature.funding.googleplay.domain.entity.Contribution
import net.thunderbird.feature.funding.googleplay.domain.entity.ContributionId
import net.thunderbird.feature.funding.googleplay.domain.entity.OneTimeContribution
import net.thunderbird.feature.funding.googleplay.domain.entity.PurchasedContribution
import net.thunderbird.feature.funding.googleplay.domain.entity.RecurringContribution
import com.android.billingclient.api.BillingClient as GoogleBillingClient

internal interface FundingDataContract {

    interface Mapper {
        interface Product {
            fun mapToContribution(product: ProductDetails): Contribution

            fun mapToOneTimeContribution(product: ProductDetails): OneTimeContribution
            fun mapToRecurringContribution(product: ProductDetails): RecurringContribution

            fun mapToPurchasedContribution(
                purchase: Purchase,
                productDetails: ProductDetails,
            ): PurchasedContribution

            fun mapHistoryToPurchasedContribution(
                purchase: PurchaseHistoryRecord,
                productDetails: ProductDetails,
            ): PurchasedContribution
        }
    }

    interface Remote {
        interface ContributionDataSource {

            /**
             * Get all one-time contributions for the given product IDs.
             *
             * @param contributionIds The list of contribution IDs to fetch one-time contributions for.
             * @return Outcome flow containing a list of one-time contributions or an error if the operation fails.
             */
            fun getAllOneTime(
                contributionIds: List<ContributionId>,
            ): Flow<Outcome<List<OneTimeContribution>, ContributionError>>

            /**
             * Get all recurring contributions for the given product IDs.
             *
             * @param contributionIds The list of contribution IDs to fetch recurring contributions for.
             * @return Outcome flow containing a list of recurring contributions or an error if the operation fails.
             */
            fun getAllRecurring(
                contributionIds: List<ContributionId>,
            ): Flow<Outcome<List<RecurringContribution>, ContributionError>>

            /**
             * Get all purchased contributions.
             *
             * @return Outcome flow containing a list of purchased contributions or an error if the operation fails.
             */
            fun getAllPurchased(): Flow<Outcome<List<PurchasedContribution>, ContributionError>>

            /**
             * Purchase a contribution.
             *
             * @param contributionId The contribution id to purchase.
             * @return Outcome of the purchase.
             */
            suspend fun purchaseContribution(
                contributionId: ContributionId,
            ): Outcome<Unit, ContributionError>

            /**
             * Clears contribution resources.
             */
            fun clear()
        }

        interface BillingProductCache : Cache<ContributionId, ProductDetails>

        interface BillingClientProvider {

            /**
             * The current billing client instance.
             */
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

        interface BillingPurchaseHandler {
            suspend fun handlePurchases(
                clientProvider: BillingClientProvider,
                purchases: List<Purchase>,
            ): List<PurchasedContribution>

            fun handleOneTimePurchases(
                clientProvider: BillingClientProvider,
                purchases: List<Purchase>,
            ): List<PurchasedContribution>

            fun handleRecurringPurchases(
                clientProvider: BillingClientProvider,
                purchases: List<Purchase>,
            ): List<PurchasedContribution>
        }

        interface BillingConnector {
            /**
             * Connect to the billing service.
             *
             * @param onConnected Callback to be invoked when the billing service is connected.
             */
            suspend fun <T> connect(
                onConnected: suspend () -> Outcome<T, ContributionError>,
            ): Outcome<T, ContributionError>

            /**
             * Disconnect from the billing service.
             */
            fun disconnect()
        }

        interface BillingClient {

            /**
             * Disconnect from the billing service.
             */
            fun disconnect()

            /**
             * Flow that emits the last purchased contribution.
             */
            val purchasedContribution: StateFlow<Outcome<PurchasedContribution?, ContributionError>>

            /**
             * Load one-time contributions.
             */
            suspend fun loadOneTimeContributions(
                productIds: List<String>,
            ): Outcome<List<OneTimeContribution>, ContributionError>

            /**
             * Load recurring contributions.
             */
            suspend fun loadRecurringContributions(
                productIds: List<String>,
            ): Outcome<List<RecurringContribution>, ContributionError>

            /**
             * Load purchased one-time contributions.
             */
            suspend fun loadPurchasedOneTimeContributions(): Outcome<List<PurchasedContribution>, ContributionError>

            /**
             *  Load purchased recurring contributions.
             */
            suspend fun loadPurchasedRecurringContributions(): Outcome<List<PurchasedContribution>, ContributionError>

            /**
             * Load the most recent one-time contribution.
             */
            suspend fun loadPurchasedOneTimeContributionHistory(): Outcome<PurchasedContribution?, ContributionError>

            /**
             * Purchase a contribution.
             *
             * @param contributionId The contribution id to purchase.
             * @return Outcome of the purchase operation, indicating success or failure with an appropriate error.
             */
            suspend fun purchaseContribution(contributionId: ContributionId): Outcome<Unit, ContributionError>
        }
    }
}
