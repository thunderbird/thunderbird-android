package net.thunderbird.feature.funding.googleplay.data.remote.bilingclient

import android.app.Activity
import assertk.assertThat
import assertk.assertions.isEqualTo
import assertk.assertions.isInstanceOf
import com.android.billingclient.api.BillingResult
import com.android.billingclient.api.ProductDetails
import com.android.billingclient.api.Purchase
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import net.thunderbird.core.android.common.activity.ActivityProvider
import net.thunderbird.core.common.cache.Cache
import net.thunderbird.core.logging.testing.TestLogger
import net.thunderbird.core.outcome.Outcome
import net.thunderbird.feature.funding.googleplay.data.FundingDataContract
import net.thunderbird.feature.funding.googleplay.data.FundingDataContract.Remote
import net.thunderbird.feature.funding.googleplay.domain.FundingDomainContract.ContributionError
import net.thunderbird.feature.funding.googleplay.domain.entity.OneTimeContribution
import net.thunderbird.feature.funding.googleplay.domain.entity.RecurringContribution
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.kotlin.any
import org.mockito.kotlin.mock
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import org.robolectric.RobolectricTestRunner
import com.android.billingclient.api.BillingClient as GoogleBillingClient

@RunWith(RobolectricTestRunner::class)
class BillingClientTest {

    private val clientProvider = mock<Remote.BillingClientProvider>()
    private val productMapper = mock<FundingDataContract.Mapper.Product>()
    private val resultMapper = mock<FundingDataContract.Mapper.BillingResult>()
    private val productCache = mock<Cache<String, ProductDetails>>()
    private val purchaseHandler = mock<Remote.BillingPurchaseHandler>()
    private val activityProvider = mock<ActivityProvider>()
    private val logger = TestLogger()
    private val googleBillingClient = mock<GoogleBillingClient>()

    private val testSubject = BillingClient(
        clientProvider = clientProvider,
        productMapper = productMapper,
        resultMapper = resultMapper,
        productCache = productCache,
        purchaseHandler = purchaseHandler,
        activityProvider = activityProvider,
        logger = logger,
    )

    @Test
    fun `purchaseContribution should return failure when product details not in cache`() = runTest {
        // Arrange
        val contribution = OneTimeContribution("id", "title", "desc", 100L, "$1.00")
        whenever(productCache.get("id")).thenReturn(null)

        // Act
        val result = testSubject.purchaseContribution(contribution)

        // Assert
        assertThat(result).isInstanceOf(Outcome.Failure::class)
        val error = (result as Outcome.Failure).error
        assertThat(error).isInstanceOf(ContributionError.PurchaseFailed::class)
        assertThat(error.message).isEqualTo("ProductDetails not found: id")
    }

    @Test
    fun `purchaseContribution should return failure when activity not available`() = runTest {
        // Arrange
        val contribution = OneTimeContribution("id", "title", "desc", 100L, "$1.00")
        val productDetails = mock<ProductDetails>()
        whenever(productCache.get("id")).thenReturn(productDetails)
        whenever(activityProvider.getCurrent()).thenReturn(null)

        // Act
        val result = testSubject.purchaseContribution(contribution)

        // Assert
        assertThat(result).isInstanceOf(Outcome.Failure::class)
        val error = (result as Outcome.Failure).error
        assertThat(error).isInstanceOf(ContributionError.PurchaseFailed::class)
        assertThat(error.message).isEqualTo("Activity not available for purchase")
    }

    @Test
    fun `purchaseContribution should launch billing flow when conditions met`() = runTest {
        // Arrange
        val contribution = RecurringContribution("id", "title", "desc", 1000L, "$10.00")
        val productDetails = mock<ProductDetails>()
        val subscriptionOfferDetails = mock<ProductDetails.SubscriptionOfferDetails>()
        val activity = mock<Activity>()
        val billingResult = mock<BillingResult>()

        whenever(productDetails.productType).thenReturn(GoogleBillingClient.ProductType.SUBS)
        whenever(productDetails.productId).thenReturn("id")
        val zzaMethod = ProductDetails::class.java.getDeclaredMethod("zza")
        zzaMethod.isAccessible = true
        whenever(zzaMethod.invoke(productDetails)).thenReturn("id")
        whenever(productDetails.subscriptionOfferDetails).thenReturn(listOf(subscriptionOfferDetails))
        whenever(subscriptionOfferDetails.offerToken).thenReturn("token")

        whenever(productCache.get("id")).thenReturn(productDetails)
        whenever(activityProvider.getCurrent()).thenReturn(activity)
        whenever(clientProvider.current).thenReturn(googleBillingClient)
        whenever(googleBillingClient.launchBillingFlow(any(), any())).thenReturn(billingResult)
        whenever(resultMapper.mapToOutcome<Unit>(any(), any())).thenReturn(Outcome.success(Unit))

        // Act
        val result = testSubject.purchaseContribution(contribution)

        // Assert
        assertThat(result).isInstanceOf(Outcome.Success::class)
        verify(googleBillingClient).launchBillingFlow(any(), any())
    }

    @Test
    fun `onPurchasesUpdated should emit success when purchases updated successfully`() = runTest {
        // Arrange
        val billingResult = mock<BillingResult>()
        val purchases = mutableListOf(mock<Purchase>())
        val contribution = OneTimeContribution("id", "title", "desc", 100L, "$1.00")

        whenever(resultMapper.mapToOutcome<Unit>(any(), any())).thenReturn(Outcome.success(Unit))
        whenever(purchaseHandler.handlePurchases(any(), any())).thenReturn(listOf(contribution))

        // Act
        testSubject.onPurchasesUpdated(billingResult, purchases)

        // Assert
        // Wait for the coroutine launched in onPurchasesUpdated
        val result = testSubject.purchasedContribution.first { it is Outcome.Success && it.data != null }
        assertThat(result).isEqualTo(Outcome.success(contribution))
    }
}
