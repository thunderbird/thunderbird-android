package net.thunderbird.feature.funding.googleplay.data.remote.bilingclient

import com.android.billingclient.api.AcknowledgePurchaseParams
import com.android.billingclient.api.BillingClient
import com.android.billingclient.api.BillingResult
import com.android.billingclient.api.ConsumeParams
import com.android.billingclient.api.Purchase
import com.android.billingclient.api.acknowledgePurchase
import com.android.billingclient.api.consumePurchase
import kotlin.coroutines.CoroutineContext
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import net.thunderbird.core.logging.Logger
import net.thunderbird.feature.funding.googleplay.data.FundingDataContract
import net.thunderbird.feature.funding.googleplay.data.FundingDataContract.Remote
import net.thunderbird.feature.funding.googleplay.domain.entity.Contribution
import net.thunderbird.feature.funding.googleplay.domain.entity.OneTimeContribution
import net.thunderbird.feature.funding.googleplay.domain.entity.RecurringContribution

// TODO propagate errors via Outcome
// TODO optimize purchase handling and reduce duplicate code
@Suppress("TooManyFunctions")
internal class BillingPurchaseHandler(
    private val productCache: Remote.BillingProductCache,
    private val productMapper: FundingDataContract.Mapper.Product,
    private val logger: Logger,
    private val backgroundDispatcher: CoroutineContext = Dispatchers.IO,
) : Remote.BillingPurchaseHandler {

    private val coroutineScope = CoroutineScope(backgroundDispatcher)

    override suspend fun handlePurchases(
        clientProvider: Remote.BillingClientProvider,
        purchases: List<Purchase>,
    ): List<Contribution> {
        return purchases.flatMap { purchase ->
            handlePurchase(clientProvider.current, purchase)
        }
    }

    override fun handleOneTimePurchases(
        clientProvider: Remote.BillingClientProvider,
        purchases: List<Purchase>,
    ): List<OneTimeContribution> {
        return purchases.flatMap { purchase ->
            handleOneTimePurchase(clientProvider.current, purchase)
        }
    }

    override fun handleRecurringPurchases(
        clientProvider: Remote.BillingClientProvider,
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

    private fun handleOneTimePurchase(
        billingClient: BillingClient,
        purchase: Purchase,
    ): List<OneTimeContribution> {
        coroutineScope.launch {
            // TODO verify purchase with public key
            consumePurchase(billingClient, purchase)
        }

        return extractOneTimeContributions(purchase)
    }

    private fun handleRecurringPurchase(
        billingClient: BillingClient,
        purchase: Purchase,
    ): List<RecurringContribution> {
        coroutineScope.launch {
            // TODO verify purchase with public key
            acknowledgePurchase(billingClient, purchase)
        }

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

                if (acknowledgeResult.responseCode == BillingClient.BillingResponseCode.OK) {
                    logger.info { "acknowledgePurchase success" }
                } else {
                    logger.error(message = { "acknowledgePurchase failed: ${acknowledgeResult.debugMessage}" })
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
        }.filter { it.productType == BillingClient.ProductType.INAPP }
            .map { productMapper.mapToOneTimeContribution(it) }
    }

    private fun extractRecurringContributions(purchase: Purchase): List<RecurringContribution> {
        if (purchase.purchaseState != Purchase.PurchaseState.PURCHASED) {
            return emptyList()
        }

        return purchase.products.mapNotNull { product ->
            productCache[product]
        }.filter { it.productType == BillingClient.ProductType.SUBS }
            .map { productMapper.mapToRecurringContribution(it) }
    }
}
