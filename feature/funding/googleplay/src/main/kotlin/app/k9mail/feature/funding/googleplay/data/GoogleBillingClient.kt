package app.k9mail.feature.funding.googleplay.data

import android.app.Activity
import app.k9mail.feature.funding.googleplay.data.DataContract.Remote
import app.k9mail.feature.funding.googleplay.data.remote.startConnection
import app.k9mail.feature.funding.googleplay.domain.DomainContract.BillingError
import app.k9mail.feature.funding.googleplay.domain.entity.Contribution
import app.k9mail.feature.funding.googleplay.domain.entity.OneTimeContribution
import app.k9mail.feature.funding.googleplay.domain.entity.RecurringContribution
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
import net.thunderbird.core.outcome.Outcome
import net.thunderbird.core.outcome.handleAsync
import net.thunderbird.core.outcome.mapFailure
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

    override suspend fun <T> connect(onConnected: suspend () -> Outcome<T, BillingError>): Outcome<T, BillingError> {
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

    override suspend fun loadOneTimeContributions(
        productIds: List<String>,
    ): Outcome<List<OneTimeContribution>, BillingError> {
        val oneTimeProductsResult = queryProducts(ProductType.INAPP, productIds)
        return resultMapper.mapToOutcome(oneTimeProductsResult.billingResult) {
            oneTimeProductsResult.productDetailsList.orEmpty().map {
                val contribution = productMapper.mapToOneTimeContribution(it)
                productCache[it.productId] = it
                contribution
            }
        }.mapFailure { billingError, _ ->
            Timber.e("Error loading one-time products: ${oneTimeProductsResult.billingResult.debugMessage}")
            billingError
        }
    }

    override suspend fun loadRecurringContributions(
        productIds: List<String>,
    ): Outcome<List<RecurringContribution>, BillingError> {
        val recurringProductsResult = queryProducts(ProductType.SUBS, productIds)
        return resultMapper.mapToOutcome(recurringProductsResult.billingResult) {
            recurringProductsResult.productDetailsList.orEmpty().map {
                val contribution = productMapper.mapToRecurringContribution(it)
                productCache[it.productId] = it
                contribution
            }
        }.mapFailure { billingError, _ ->
            Timber.e("Error loading recurring products: ${recurringProductsResult.billingResult.debugMessage}")
            billingError
        }
    }

    override suspend fun loadPurchasedOneTimeContributions(): Outcome<List<OneTimeContribution>, BillingError> {
        val purchasesResult = queryPurchase(ProductType.INAPP)
        return resultMapper.mapToOutcome(purchasesResult.billingResult) {
            purchaseHandler.handleOneTimePurchases(clientProvider, purchasesResult.purchasesList)
        }.mapFailure { billingError, _ ->
            Timber.e("Error loading one-time purchases: ${purchasesResult.billingResult.debugMessage}")
            billingError
        }
    }

    override suspend fun loadPurchasedRecurringContributions(): Outcome<List<RecurringContribution>, BillingError> {
        val purchasesResult = queryPurchase(ProductType.SUBS)
        return resultMapper.mapToOutcome(purchasesResult.billingResult) {
            purchaseHandler.handleRecurringPurchases(clientProvider, purchasesResult.purchasesList)
        }.mapFailure { billingError, _ ->
            Timber.e("Error loading recurring purchases: ${purchasesResult.billingResult.debugMessage}")
            billingError
        }
    }

    override suspend fun loadPurchasedOneTimeContributionHistory(): Outcome<OneTimeContribution?, BillingError> {
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
                Timber.e("No recent purchase found: ${purchasesResult.billingResult.debugMessage}")
                null
            }
        }.mapFailure { billingError, _ ->
            Timber.e("Error loading one-time purchase history: ${purchasesResult.billingResult.debugMessage}")
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
    ): Outcome<Unit, BillingError> {
        val productDetails = productCache[contribution.id]
            ?: return Outcome.failure(BillingError.PurchaseFailed("ProductDetails not found: ${contribution.id}"))
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
                    Timber.e(
                        "Error onPurchasesUpdated: " +
                            "${billingResult.responseCode}: ${billingResult.debugMessage}",
                    )
                    _purchasedContribution.value = Outcome.failure(error)
                },
            )
        }
    }
}
