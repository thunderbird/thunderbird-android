package net.thunderbird.feature.funding.googleplay.data.remote.bilingclient

import android.app.Activity
import assertk.assertThat
import assertk.assertions.isEqualTo
import assertk.assertions.isInstanceOf
import com.android.billingclient.api.BillingResult
import com.android.billingclient.api.ProductDetails
import com.android.billingclient.api.Purchase
import kotlin.test.BeforeTest
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import kotlinx.datetime.LocalDateTime
import net.thunderbird.core.android.common.activity.ActivityProvider
import net.thunderbird.core.logging.testing.TestLogger
import net.thunderbird.core.outcome.Outcome
import net.thunderbird.feature.funding.googleplay.data.FundingDataContract
import net.thunderbird.feature.funding.googleplay.data.FundingDataContract.Remote
import net.thunderbird.feature.funding.googleplay.domain.FundingDomainContract.ContributionError
import net.thunderbird.feature.funding.googleplay.domain.entity.ContributionId
import net.thunderbird.feature.funding.googleplay.domain.entity.OneTimeContribution
import net.thunderbird.feature.funding.googleplay.domain.entity.PurchasedContribution
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.kotlin.any
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.mock
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import org.robolectric.RobolectricTestRunner
import com.android.billingclient.api.BillingClient as GoogleBillingClient

@RunWith(RobolectricTestRunner::class)
class BillingClientTest {

    private val clientProvider = mock<Remote.BillingClientProvider>()
    private val productMapper = mock<FundingDataContract.Mapper.Product>()
    private val productCache = BillingProductCache()
    private val purchaseHandler = mock<Remote.BillingPurchaseHandler>()
    private val activityProvider = mock<ActivityProvider>()
    private val logger = TestLogger()
    private val googleBillingClient = mock<GoogleBillingClient>()

    private val testSubject = BillingClient(
        clientProvider = clientProvider,
        productMapper = productMapper,
        productCache = productCache,
        purchaseHandler = purchaseHandler,
        activityProvider = activityProvider,
        logger = logger,
    )

    @BeforeTest
    fun setup() {
        productCache.clear()
    }

    @Test
    fun `purchaseContribution should return failure when product details not in cache`() = runTest {
        // Arrange
        val contributionId = ContributionId("id")

        // Act
        val result = testSubject.purchaseContribution(contributionId)

        // Assert
        assertThat(result).isInstanceOf(Outcome.Failure::class)
        val error = (result as Outcome.Failure).error
        assertThat(error).isInstanceOf(ContributionError.PurchaseFailed::class)
        assertThat(error.message).isEqualTo("ProductDetails not found for contributionId: id")
    }

    @Test
    fun `purchaseContribution should return failure when activity not available`() = runTest {
        // Arrange
        val contributionId = ContributionId("id")
        val productDetails = mock<ProductDetails>()
        productCache[contributionId] = productDetails
        whenever(activityProvider.getCurrent()).thenReturn(null)

        // Act
        val result = testSubject.purchaseContribution(contributionId)

        // Assert
        assertThat(result).isInstanceOf(Outcome.Failure::class)
        val error = (result as Outcome.Failure).error
        assertThat(error).isInstanceOf(ContributionError.PurchaseFailed::class)
        assertThat(error.message).isEqualTo("Activity not available for purchase")
    }

    @Test
    fun `purchaseContribution should launch billing flow when conditions met`() = runTest {
        // Arrange
        val contributionId = ContributionId("id")
        val productDetails = mock<ProductDetails>()
        val subscriptionOfferDetails = mock<ProductDetails.SubscriptionOfferDetails>()
        val activity = mock<Activity>()
        val billingResult = mock<BillingResult>()

        doReturn("id").whenever(productDetails).zza()
        whenever(productDetails.productType).thenReturn(GoogleBillingClient.ProductType.SUBS)
        whenever(productDetails.productId).thenReturn("id")
        whenever(productDetails.subscriptionOfferDetails).thenReturn(listOf(subscriptionOfferDetails))
        whenever(subscriptionOfferDetails.offerToken).thenReturn("token")

        productCache[contributionId] = productDetails
        whenever(activityProvider.getCurrent()).thenReturn(activity)
        whenever(clientProvider.current).thenReturn(googleBillingClient)
        whenever(googleBillingClient.launchBillingFlow(any(), any())).thenReturn(billingResult)

        // Act
        val result = testSubject.purchaseContribution(contributionId)

        // Assert
        assertThat(result).isInstanceOf(Outcome.Success::class)
        verify(googleBillingClient).launchBillingFlow(any(), any())
    }

    @Test
    fun `onPurchasesUpdated should emit success when purchases updated successfully`() = runTest {
        // Arrange
        val billingResult = mock<BillingResult>()
        val purchases = mutableListOf(mock<Purchase>())
        val contribution = PurchasedContribution(
            id = ContributionId("id"),
            contribution = OneTimeContribution(
                id = ContributionId("id"),
                title = "title",
                description = "desc",
                price = 100L,
                priceFormatted = "$1.00",
            ),
            purchaseDate = LocalDateTime(2024, 1, 1, 0, 0),
        )

        whenever(purchaseHandler.handlePurchases(any(), any())).thenReturn(listOf(contribution))

        // Act
        testSubject.onPurchasesUpdated(billingResult, purchases)

        // Assert
        // Wait for the coroutine launched in onPurchasesUpdated
        val result = testSubject.purchasedContribution.first { it is Outcome.Success && it.data != null }
        assertThat(result).isEqualTo(Outcome.success(contribution))
    }
}
