package net.thunderbird.feature.funding.googleplay.data.remote.bilingclient

import android.app.Activity
import com.android.billingclient.api.BillingClient.ProductType
import com.android.billingclient.api.BillingFlowParams
import com.android.billingclient.api.BillingFlowParams.ProductDetailsParams
import com.android.billingclient.api.BillingResult
import com.android.billingclient.api.ProductDetails
import com.android.billingclient.api.ProductDetailsResult
import com.android.billingclient.api.Purchase
import com.android.billingclient.api.PurchasesResult
import com.android.billingclient.api.PurchasesUpdatedListener
import com.android.billingclient.api.QueryProductDetailsParams
import com.android.billingclient.api.QueryPurchaseHistoryParams
import com.android.billingclient.api.QueryPurchasesParams
import com.android.billingclient.api.queryProductDetails
import com.android.billingclient.api.queryPurchaseHistory
import com.android.billingclient.api.queryPurchasesAsync
import kotlin.coroutines.CoroutineContext
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import net.thunderbird.core.common.cache.Cache
import net.thunderbird.core.logging.Logger
import net.thunderbird.core.outcome.Outcome
import net.thunderbird.core.outcome.handleAsync
import net.thunderbird.core.outcome.mapFailure
import net.thunderbird.feature.funding.googleplay.data.FundingDataContract
import net.thunderbird.feature.funding.googleplay.data.FundingDataContract.Remote
import net.thunderbird.feature.funding.googleplay.domain.FundingDomainContract.ContributionError
import net.thunderbird.feature.funding.googleplay.domain.entity.Contribution
import net.thunderbird.feature.funding.googleplay.domain.entity.OneTimeContribution
import net.thunderbird.feature.funding.googleplay.domain.entity.RecurringContribution

internal typealias OneTimeContributionOutcome = Outcome<List<OneTimeContribution>, ContributionError>
internal typealias RecurringContributionOutcome = Outcome<List<RecurringContribution>, ContributionError>

@Suppress("TooManyFunctions")
internal class BillingClient(
    private val clientProvider: Remote.BillingClientProvider,
    private val productMapper: FundingDataContract.Mapper.Product,
    private val resultMapper: FundingDataContract.Mapper.BillingResult,
    private val productCache: Cache<String, ProductDetails>,
    private val purchaseHandler: Remote.BillingPurchaseHandler,
    private val logger: Logger,
    backgroundDispatcher: CoroutineContext = Dispatchers.IO,
) : Remote.BillingClient, PurchasesUpdatedListener {

    init {
        clientProvider.setPurchasesUpdatedListener(this)
    }

    private val coroutineScope = CoroutineScope(backgroundDispatcher)

    private val _purchasedContribution = MutableStateFlow<Outcome<Contribution?, ContributionError>>(
        value = Outcome.success(null),
    )
    override val purchasedContribution: StateFlow<Outcome<Contribution?, ContributionError>> =
        _purchasedContribution.asStateFlow()

    override suspend fun <T> connect(
        onConnected: suspend () -> Outcome<T, ContributionError>,
    ): Outcome<T, ContributionError> {
        val connectionResult = clientProvider.current.startConnection()
        val result = resultMapper.mapToOutcome(connectionResult) {}

        return when (result) {
            is Outcome.Success -> {
                onConnected()
            }

            is Outcome.Failure -> result
        }
    }

    override fun disconnect() {
        productCache.clear()
        _purchasedContribution.value = Outcome.success(null)
        clientProvider.clear()
    }

    override suspend fun loadOneTimeContributions(productIds: List<String>): OneTimeContributionOutcome {
        val oneTimeProductsResult = queryProducts(ProductType.INAPP, productIds)
        return resultMapper.mapToOutcome(oneTimeProductsResult.billingResult) {
            oneTimeProductsResult.productDetailsList.orEmpty().map {
                val contribution = productMapper.mapToOneTimeContribution(it)
                productCache[it.productId] = it
                contribution
            }
        }.mapFailure { billingError, _ ->
            logger.error(
                message = {
                    "Error loading one-time products: ${oneTimeProductsResult.billingResult.debugMessage}"
                },
            )
            billingError
        }
    }

    override suspend fun loadRecurringContributions(productIds: List<String>): RecurringContributionOutcome {
        val recurringProductsResult = queryProducts(ProductType.SUBS, productIds)
        return resultMapper.mapToOutcome(recurringProductsResult.billingResult) {
            recurringProductsResult.productDetailsList.orEmpty().map {
                val contribution = productMapper.mapToRecurringContribution(it)
                productCache[it.productId] = it
                contribution
            }
        }.mapFailure { billingError, _ ->
            logger.error(
                message = {
                    "Error loading recurring products: ${recurringProductsResult.billingResult.debugMessage}"
                },
            )
            billingError
        }
    }

    override suspend fun loadPurchasedOneTimeContributions(): OneTimeContributionOutcome {
        val purchasesResult = queryPurchase(ProductType.INAPP)
        return resultMapper.mapToOutcome(purchasesResult.billingResult) {
            purchaseHandler.handleOneTimePurchases(clientProvider, purchasesResult.purchasesList)
        }.mapFailure { billingError, _ ->
            logger.error(
                message = {
                    "Error loading one-time purchases: ${purchasesResult.billingResult.debugMessage}"
                },
            )
            billingError
        }
    }

    override suspend fun loadPurchasedRecurringContributions(): RecurringContributionOutcome {
        val purchasesResult = queryPurchase(ProductType.SUBS)
        return resultMapper.mapToOutcome(purchasesResult.billingResult) {
            purchaseHandler.handleRecurringPurchases(clientProvider, purchasesResult.purchasesList)
        }.mapFailure { billingError, _ ->
            logger.error(
                message = {
                    "Error loading recurring purchases: ${purchasesResult.billingResult.debugMessage}"
                },
            )
            billingError
        }
    }

    override suspend fun loadPurchasedOneTimeContributionHistory(): Outcome<OneTimeContribution?, ContributionError> {
        val queryPurchaseHistoryParams = QueryPurchaseHistoryParams.newBuilder()
            .setProductType(ProductType.INAPP)
            .build()

        val purchasesResult = clientProvider.current.queryPurchaseHistory(queryPurchaseHistoryParams)
        return resultMapper.mapToOutcome(purchasesResult.billingResult) {
            val recentPurchaseId =
                purchasesResult.purchaseHistoryRecordList.orEmpty().firstOrNull()?.products?.firstOrNull {
                    productCache.hasKey(it)
                }

            if (recentPurchaseId != null) {
                val recentPurchase = productCache[recentPurchaseId]
                productMapper.mapToOneTimeContribution(recentPurchase!!)
            } else {
                logger.error(message = { "No recent purchase found: ${purchasesResult.billingResult.debugMessage}" })
                null
            }
        }.mapFailure { billingError, _ ->
            logger.error(
                message = {
                    "Error loading one-time purchase history: ${purchasesResult.billingResult.debugMessage}"
                },
            )
            billingError
        }
    }

    private suspend fun queryProducts(
        productType: String,
        productIds: List<String>,
    ): ProductDetailsResult {
        val productList = productIds.map { mapIdToProduct(productType, it) }

        val queryProductDetailsParams = QueryProductDetailsParams.newBuilder()
            .setProductList(productList)
            .build()

        return clientProvider.current.queryProductDetails(queryProductDetailsParams)
    }

    private fun mapIdToProduct(
        productType: String,
        productId: String,
    ): QueryProductDetailsParams.Product {
        return QueryProductDetailsParams.Product.newBuilder()
            .setProductType(productType)
            .setProductId(productId)
            .build()
    }

    private suspend fun queryPurchase(productType: String): PurchasesResult {
        val queryPurchaseParams = QueryPurchasesParams.newBuilder()
            .setProductType(productType)
            .build()

        return clientProvider.current.queryPurchasesAsync(queryPurchaseParams)
    }

    override suspend fun purchaseContribution(
        activity: Activity,
        contribution: Contribution,
    ): Outcome<Unit, ContributionError> {
        val productDetails = productCache[contribution.id]
            ?: return Outcome.failure(ContributionError.PurchaseFailed("ProductDetails not found: ${contribution.id}"))
        val offerToken = productDetails.subscriptionOfferDetails?.firstOrNull()?.offerToken

        val productDetailsParamsList = listOf(
            ProductDetailsParams.newBuilder()
                .setProductDetails(productDetails)
                .apply {
                    if (offerToken != null) {
                        setOfferToken(offerToken)
                    }
                }
                .build(),
        )

        val billingFlowParams = BillingFlowParams.newBuilder()
            .setProductDetailsParamsList(productDetailsParamsList)
            .build()

        val billingResult = clientProvider.current.launchBillingFlow(activity, billingFlowParams)
        return resultMapper.mapToOutcome(billingResult) { }.mapFailure(
            transformFailure = { error, _ ->
                logger.error(message = { "Error launching billing flow: ${error.message}" })
                error
            },
        )
    }

    override fun onPurchasesUpdated(billingResult: BillingResult, purchases: MutableList<Purchase>?) {
        coroutineScope.launch {
            resultMapper.mapToOutcome(billingResult) { }.handleAsync(
                onSuccess = {
                    if (purchases != null) {
                        val contributions = purchaseHandler.handlePurchases(clientProvider, purchases)
                        if (contributions.isNotEmpty()) {
                            _purchasedContribution.emit(
                                Outcome.success(
                                    contributions.firstOrNull(),
                                ),
                            )
                        }
                    }
                },
                onFailure = { error ->
                    logger.error(
                        message = {
                            "Error onPurchasesUpdated: " +
                                "${billingResult.responseCode}: ${billingResult.debugMessage}"
                        },
                    )
                    _purchasedContribution.value = Outcome.failure(error)
                },
            )
        }
    }
}
