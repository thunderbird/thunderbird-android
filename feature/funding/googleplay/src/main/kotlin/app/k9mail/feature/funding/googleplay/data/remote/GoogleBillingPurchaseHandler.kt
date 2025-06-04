package app.k9mail.feature.funding.googleplay.data.remote

import app.k9mail.feature.funding.googleplay.data.DataContract
import app.k9mail.feature.funding.googleplay.data.DataContract.Remote
import app.k9mail.feature.funding.googleplay.domain.entity.Contribution
import app.k9mail.feature.funding.googleplay.domain.entity.OneTimeContribution
import app.k9mail.feature.funding.googleplay.domain.entity.RecurringContribution
import com.android.billingclient.api.AcknowledgePurchaseParams
import com.android.billingclient.api.BillingClient
import com.android.billingclient.api.BillingClient.BillingResponseCode
import com.android.billingclient.api.BillingClient.ProductType
import com.android.billingclient.api.BillingResult
import com.android.billingclient.api.ConsumeParams
import com.android.billingclient.api.ProductDetails
import com.android.billingclient.api.Purchase
import com.android.billingclient.api.acknowledgePurchase
import com.android.billingclient.api.consumePurchase
import net.thunderbird.core.common.cache.Cache
import net.thunderbird.core.logging.Logger

// TODO propagate errors via Outcome
// TODO optimize purchase handling and reduce duplicate code
@Suppress("TooManyFunctions")
internal class GoogleBillingPurchaseHandler(
    private val productCache: Cache<String, ProductDetails>,
    private val productMapper: DataContract.Mapper.Product,
    private val logger: Logger,
) : Remote.GoogleBillingPurchaseHandler {

    override suspend fun handlePurchases(
        clientProvider: Remote.GoogleBillingClientProvider,
        purchases: List<Purchase>,
    ): List<Contribution> {
        return purchases.flatMap { purchase ->
            handlePurchase(clientProvider.current, purchase)
        }
    }

    override suspend fun handleOneTimePurchases(
        clientProvider: Remote.GoogleBillingClientProvider,
        purchases: List<Purchase>,
    ): List<OneTimeContribution> {
        return purchases.flatMap { purchase ->
            handleOneTimePurchase(clientProvider.current, purchase)
        }
    }

    override suspend fun handleRecurringPurchases(
        clientProvider: Remote.GoogleBillingClientProvider,
        purchases: List<Purchase>,
    ): List<RecurringContribution> {
        return purchases.flatMap { purchase ->
            handleRecurringPurchase(clientProvider.current, purchase)
        }
    }

    private suspend fun handlePurchase(
        billingClient: BillingClient,
        purchase: Purchase,
    ): List<Contribution> {
        // TODO verify purchase with public key
        consumePurchase(billingClient, purchase)
        acknowledgePurchase(billingClient, purchase)

        return extractContributions(purchase)
    }

    private suspend fun handleOneTimePurchase(
        billingClient: BillingClient,
        purchase: Purchase,
    ): List<OneTimeContribution> {
        // TODO verify purchase with public key
        consumePurchase(billingClient, purchase)

        return extractOneTimeContributions(purchase)
    }

    private suspend fun handleRecurringPurchase(
        billingClient: BillingClient,
        purchase: Purchase,
    ): List<RecurringContribution> {
        // TODO verify purchase with public key
        acknowledgePurchase(billingClient, purchase)

        return extractRecurringContributions(purchase)
    }

    private suspend fun acknowledgePurchase(
        billingClient: BillingClient,
        purchase: Purchase,
    ) {
        if (purchase.purchaseState == Purchase.PurchaseState.PURCHASED) {
            if (!purchase.isAcknowledged) {
                val acknowledgePurchaseParams = AcknowledgePurchaseParams.newBuilder()
                    .setPurchaseToken(purchase.purchaseToken)
                    .build()

                val acknowledgeResult: BillingResult = billingClient.acknowledgePurchase(acknowledgePurchaseParams)

                if (acknowledgeResult.responseCode != BillingResponseCode.OK) {
                    // TODO success
                } else {
                    // handle acknowledge error
                    logger.error(message = { "acknowledgePurchase failed" })
                }
            } else {
                logger.error(message = { "purchase already acknowledged" })
            }
        } else {
            logger.error(message = { "purchase not purchased" })
        }
    }

    private suspend fun consumePurchase(
        billingClient: BillingClient,
        purchase: Purchase,
    ) {
        val consumeParams = ConsumeParams.newBuilder()
            .setPurchaseToken(purchase.purchaseToken)
            .build()

        // This could fail but we can ignore the error as we handle purchases
        // the next time the purchases are requested
        billingClient.consumePurchase(consumeParams)
    }

    private fun extractContributions(purchase: Purchase): List<Contribution> {
        if (purchase.purchaseState != Purchase.PurchaseState.PURCHASED) {
            return emptyList()
        }

        return extractOneTimeContributions(purchase) + extractRecurringContributions(purchase)
    }

    private fun extractOneTimeContributions(purchase: Purchase): List<OneTimeContribution> {
        if (purchase.purchaseState != Purchase.PurchaseState.PURCHASED) {
            return emptyList()
        }

        return purchase.products.mapNotNull { product ->
            productCache[product]
        }.filter { it.productType == ProductType.INAPP }
            .map { productMapper.mapToOneTimeContribution(it) }
    }

    private fun extractRecurringContributions(purchase: Purchase): List<RecurringContribution> {
        if (purchase.purchaseState != Purchase.PurchaseState.PURCHASED) {
            return emptyList()
        }

        return purchase.products.mapNotNull { product ->
            productCache[product]
        }.filter { it.productType == ProductType.SUBS }
            .map { productMapper.mapToRecurringContribution(it) }
    }
}
