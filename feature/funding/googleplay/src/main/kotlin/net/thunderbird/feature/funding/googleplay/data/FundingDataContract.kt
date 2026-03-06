package net.thunderbird.feature.funding.googleplay.data

import com.android.billingclient.api.ProductDetails
import com.android.billingclient.api.Purchase
import com.android.billingclient.api.PurchasesUpdatedListener
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.StateFlow
import net.thunderbird.core.outcome.Outcome
import net.thunderbird.feature.funding.googleplay.domain.FundingDomainContract.ContributionError
import net.thunderbird.feature.funding.googleplay.domain.entity.Contribution
import net.thunderbird.feature.funding.googleplay.domain.entity.OneTimeContribution
import net.thunderbird.feature.funding.googleplay.domain.entity.RecurringContribution
import com.android.billingclient.api.BillingClient as GoogleBillingClient
import com.android.billingclient.api.BillingResult as GoogleBillingResult

internal interface FundingDataContract {

    interface Mapper {
        interface Product {
            fun mapToContribution(product: ProductDetails): Contribution

            fun mapToOneTimeContribution(product: ProductDetails): OneTimeContribution
            fun mapToRecurringContribution(product: ProductDetails): RecurringContribution
        }

        interface BillingResult {
            suspend fun <T> mapToOutcome(
                billingResult: GoogleBillingResult,
                transformSuccess: suspend () -> T,
            ): Outcome<T, ContributionError>
        }
    }

    interface Remote {
        interface ContributionDataSource {

            /**
             * Get all one-time contributions for the given product IDs.
             *
             * @param productIds The list of product IDs to fetch one-time contributions for.
             * @return Outcome flow containing a list of one-time contributions or an error if the operation fails.
             */
            fun getAllOneTime(
                productIds: List<String>,
            ): Flow<Outcome<List<OneTimeContribution>, ContributionError>>

            /**
             * Get all recurring contributions for the given product IDs.
             *
             * @param productIds The list of product IDs to fetch recurring contributions for.
             * @return Outcome flow containing a list of recurring contributions or an error if the operation fails.
             */
            fun getAllRecurring(
                productIds: List<String>,
            ): Flow<Outcome<List<RecurringContribution>, ContributionError>>

            /**
             * Get all purchased contributions.
             *
             * @return Outcome flow containing a list of purchased contributions or an error if the operation fails.
             */
            fun getAllPurchased(): Flow<Outcome<List<Contribution>, ContributionError>>

            /**
             * Flow that emits the last purchased contribution.
             */
            val purchasedContribution: StateFlow<Outcome<Contribution?, ContributionError>>

            /**
             * Purchase a contribution.
             *
             * @param contribution The contribution to purchase.
             * @return Outcome of the purchase.
             */
            suspend fun purchaseContribution(
                contribution: Contribution,
            ): Outcome<Unit, ContributionError>

            /**
             * Clears contribution resources.
             */
            fun clear()
        }

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
            ): List<Contribution>

            suspend fun handleOneTimePurchases(
                clientProvider: BillingClientProvider,
                purchases: List<Purchase>,
            ): List<OneTimeContribution>

            suspend fun handleRecurringPurchases(
                clientProvider: BillingClientProvider,
                purchases: List<Purchase>,
            ): List<RecurringContribution>
        }

        interface BillingClient {

            /**
             * Flow that emits the last purchased contribution.
             */
            val purchasedContribution: StateFlow<Outcome<Contribution?, ContributionError>>

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
            suspend fun loadPurchasedOneTimeContributions(): Outcome<List<OneTimeContribution>, ContributionError>

            /**
             *  Load purchased recurring contributions.
             */
            suspend fun loadPurchasedRecurringContributions(): Outcome<List<RecurringContribution>, ContributionError>

            /**
             * Load the most recent one-time contribution.
             */
            suspend fun loadPurchasedOneTimeContributionHistory(): Outcome<OneTimeContribution?, ContributionError>

            /**
             * Purchase a contribution.
             *
             * @param contribution The contribution to purchase.
             * @return Outcome of the purchase operation, indicating success or failure with an appropriate error.
             */
            suspend fun purchaseContribution(contribution: Contribution): Outcome<Unit, ContributionError>
        }
    }
}
