package app.k9mail.feature.funding.googleplay.data

import android.app.Activity
import app.k9mail.core.common.cache.Cache
import app.k9mail.feature.funding.googleplay.data.DataContract.Remote
import app.k9mail.feature.funding.googleplay.domain.DomainContract.BillingError
import app.k9mail.feature.funding.googleplay.domain.Outcome
import app.k9mail.feature.funding.googleplay.domain.entity.Contribution
import app.k9mail.feature.funding.googleplay.domain.entity.OneTimeContribution
import app.k9mail.feature.funding.googleplay.domain.entity.RecurringContribution
import app.k9mail.feature.funding.googleplay.domain.handle
import app.k9mail.feature.funding.googleplay.domain.mapFailure
import com.android.billingclient.api.BillingClient.BillingResponseCode
import com.android.billingclient.api.BillingClient.ProductType
import com.android.billingclient.api.BillingClientStateListener
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
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.suspendCancellableCoroutine
import timber.log.Timber

@Suppress("TooManyFunctions")
internal class GoogleBillingClient(
    private val clientProvider: Remote.GoogleBillingClientProvider,
    private val productMapper: DataContract.Mapper.Product,
    private val resultMapper: DataContract.Mapper.BillingResult,
    private val productCache: Cache<String, ProductDetails>,
    private val purchaseHandler: Remote.GoogleBillingPurchaseHandler,
    backgroundDispatcher: CoroutineContext = Dispatchers.IO,
) : DataContract.BillingClient, PurchasesUpdatedListener {

    init {
        clientProvider.setPurchasesUpdatedListener(this)
    }

    private val coroutineScope = CoroutineScope(backgroundDispatcher)

    private val _purchasedContribution = MutableStateFlow<Outcome<Contribution?, BillingError>>(
        value = Outcome.success(null),
    )
    override val purchasedContribution: StateFlow<Outcome<Contribution?, BillingError>> =
        _purchasedContribution.asStateFlow()

    override suspend fun <T> connect(onConnected: suspend () -> T): T {
        return suspendCancellableCoroutine { continuation ->
            clientProvider.current.startConnection(
                object : BillingClientStateListener {
                    override fun onBillingSetupFinished(billingResult: BillingResult) {
                        if (billingResult.responseCode == BillingResponseCode.OK) {
                            continuation.resumeWith(
                                Result.runCatching {
                                    runBlocking { onConnected() }
                                },
                            )
                        } else {
                            continuation.resumeWith(
                                Result.failure(
                                    IllegalStateException(
                                        "Error connecting to billing service: ${billingResult.responseCode}",
                                    ),
                                ),
                            )
                        }
                    }

                    override fun onBillingServiceDisconnected() {
                        continuation.resumeWith(
                            Result.failure(
                                IllegalStateException("Billing service disconnected"),
                            ),
                        )
                    }
                },
            )
        }
    }

    override fun disconnect() {
        productCache.clear()
        _purchasedContribution.value = Outcome.success(null)
        clientProvider.clear()
    }

    override suspend fun loadOneTimeContributions(productIds: List<String>): List<OneTimeContribution> {
        val oneTimeProductsResult = queryProducts(ProductType.INAPP, productIds)
        return if (oneTimeProductsResult.billingResult.responseCode == BillingResponseCode.OK) {
            oneTimeProductsResult.productDetailsList.orEmpty().map {
                val contribution = productMapper.mapToOneTimeContribution(it)
                productCache[it.productId] = it
                contribution
            }
        } else {
            Timber.e(
                "Error loading one-time products: ${oneTimeProductsResult.billingResult.responseCode}",
            )
            emptyList()
        }
    }

    override suspend fun loadRecurringContributions(productIds: List<String>): List<RecurringContribution> {
        val recurringProductsResult = queryProducts(ProductType.SUBS, productIds)
        return if (recurringProductsResult.billingResult.responseCode == BillingResponseCode.OK) {
            recurringProductsResult.productDetailsList.orEmpty().map {
                val contribution = productMapper.mapToRecurringContribution(it)
                productCache[it.productId] = it
                contribution
            }
        } else {
            Timber.e(
                "Error querying recurring products: ${recurringProductsResult.billingResult.debugMessage}",
            )
            emptyList()
        }
    }

    override suspend fun loadPurchasedContributions(): List<Contribution> {
        val inAppPurchases = queryPurchase(ProductType.INAPP)
        val subscriptionPurchases = queryPurchase(ProductType.SUBS)
        val contributions = purchaseHandler.handlePurchases(
            clientProvider = clientProvider,
            purchases = inAppPurchases.purchasesList + subscriptionPurchases.purchasesList,
        )
        val recentContribution = if (inAppPurchases.purchasesList.isEmpty()) {
            loadInAppPurchaseHistory()
        } else {
            null
        }

        return if (recentContribution != null) {
            contributions + recentContribution
        } else {
            contributions
        }
    }

    private suspend fun loadInAppPurchaseHistory(): Contribution? {
        val queryPurchaseHistoryParams = QueryPurchaseHistoryParams.newBuilder()
            .setProductType(ProductType.INAPP)
            .build()

        val result = clientProvider.current.queryPurchaseHistory(queryPurchaseHistoryParams)
        return if (result.billingResult.responseCode == BillingResponseCode.OK) {
            val recentPurchaseId = result.purchaseHistoryRecordList.orEmpty().firstOrNull()?.products?.filter {
                productCache.hasKey(it)
            }?.firstOrNull()

            if (recentPurchaseId != null) {
                val recentPurchase = productCache[recentPurchaseId]
                productMapper.mapToContribution(recentPurchase!!)
            } else {
                Timber.e("No recent purchase found: ${result.billingResult.debugMessage}")
                null
            }
        } else {
            Timber.e("Error querying purchase history: ${result.billingResult.debugMessage}")
            null
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
    ): Outcome<Unit, BillingError> {
        val productDetails = productCache[contribution.id] ?: return Outcome.failure(
            BillingError.PurchaseFailed("Product details not found for ${contribution.id}"),
        )
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
                Timber.e("Error launching billing flow: ${error.message}")
                error
            },
        )
    }

    override fun onPurchasesUpdated(billingResult: BillingResult, purchases: MutableList<Purchase>?) {
        resultMapper.mapToOutcome(billingResult) { }.handle(
            onSuccess = {
                if (purchases != null) {
                    coroutineScope.launch {
                        val contributions = purchaseHandler.handlePurchases(clientProvider, purchases)
                        if (contributions.isNotEmpty()) {
                            _purchasedContribution.emit(
                                Outcome.success(
                                    contributions.firstOrNull(),
                                ),
                            )
                        }
                    }
                }
            },
            onFailure = { error ->
                Timber.e(
                    "Error onPurchasesUpdated: " +
                        "${billingResult.responseCode}: ${billingResult.debugMessage}",
                )
                _purchasedContribution.value = Outcome.failure(error)
            },
        )
    }
}
