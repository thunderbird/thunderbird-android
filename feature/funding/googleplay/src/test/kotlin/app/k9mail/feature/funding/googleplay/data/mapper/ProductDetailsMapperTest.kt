package app.k9mail.feature.funding.googleplay.data.mapper

import app.k9mail.feature.funding.googleplay.domain.entity.OneTimeContribution
import app.k9mail.feature.funding.googleplay.domain.entity.RecurringContribution
import assertk.assertFailure
import assertk.assertThat
import assertk.assertions.isEqualTo
import assertk.assertions.isInstanceOf
import com.android.billingclient.api.BillingClient.ProductType
import com.android.billingclient.api.ProductDetails
import kotlin.test.Test
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.mock

class ProductDetailsMapperTest {

    private val testSubject = ProductDetailsMapper()

    @Test
    fun `mapToOneTimeContribution returns OneTimeContribution when product type is INAPP`() {
        val productDetails = createInAppProductDetails()

        val result = testSubject.mapToOneTimeContribution(productDetails)

        assertThat(result).isEqualTo(ONE_TIME_CONTRIBUTION)
    }

    @Test
    fun `mapToOneTimeContribution throws IllegalStateException when in app product has no offer details`() {
        val productDetails = createInAppProductDetails(hasOfferDetails = false)

        assertFailure {
            testSubject.mapToOneTimeContribution(productDetails)
        }.isInstanceOf(IllegalStateException::class)
    }

    @Test
    fun `mapToRecurringContribution returns RecurringContribution when product type is SUBS`() {
        val productDetails = createSubscriptionProductDetails()

        val result = testSubject.mapToRecurringContribution(productDetails)

        assertThat(result).isEqualTo(RECURRING_CONTRIBUTION)
    }

    @Test
    fun `mapToRecurringContribution throws IllegalStateException when subscription product has no pricing phase`() {
        val productDetails = createSubscriptionProductDetails(hasPricingPhase = false)

        assertFailure {
            testSubject.mapToRecurringContribution(productDetails)
        }.isInstanceOf(IllegalStateException::class)
    }

    @Test
    fun `mapToContribution return contribution for all supported types`() {
        val inAppProductDetails = createInAppProductDetails()
        val subscriptionProductDetails = createSubscriptionProductDetails()

        val oneTimeContribution = testSubject.mapToContribution(inAppProductDetails)
        val recurringContribution = testSubject.mapToContribution(subscriptionProductDetails)

        assertThat(oneTimeContribution).isEqualTo(ONE_TIME_CONTRIBUTION)
        assertThat(recurringContribution).isEqualTo(RECURRING_CONTRIBUTION)
    }

    @Test
    fun `mapToContribution throws IllegalArgumentException when product type is unknown`() {
        val productDetails = mock<ProductDetails> {
            on { productType } doReturn "unknown"
        }

        assertFailure {
            testSubject.mapToContribution(productDetails)
        }.isInstanceOf(IllegalArgumentException::class)
    }

    private fun createInAppProductDetails(
        hasOfferDetails: Boolean = true,
    ): ProductDetails {
        val oneTimePurchaseOfferDetails = mock<ProductDetails.OneTimePurchaseOfferDetails> {
            on { priceAmountMicros }.thenReturn(ONE_TIME_PRICE)
            on { formattedPrice }.thenReturn(ONE_TIME_PRICE_FORMATTED)
        }

        return mock<ProductDetails> {
            on { productType } doReturn ProductType.INAPP
            on { productId } doReturn ONE_TIME_ID
            on { name } doReturn ONE_TIME_TITLE
            on { description } doReturn ONE_TIME_DESCRIPTION_WITH_NEW_LINE
            on { getOneTimePurchaseOfferDetails() } doReturn if (hasOfferDetails) {
                oneTimePurchaseOfferDetails
            } else {
                null
            }
        }
    }

    private fun createSubscriptionProductDetails(
        hasPricingPhase: Boolean = true,
    ): ProductDetails {
        val pricingPhase = mock<ProductDetails.PricingPhase> {
            on { priceAmountMicros } doReturn RECURRING_PRICE
            on { formattedPrice } doReturn RECURRING_PRICE_FORMATTED
        }

        val pricingPhaseList = mock<ProductDetails.PricingPhases> {
            on { pricingPhaseList } doReturn listOf(pricingPhase)
        }

        val subscriptionOfferDetails = mock<ProductDetails.SubscriptionOfferDetails> {
            on { pricingPhases } doReturn pricingPhaseList
        }

        return mock<ProductDetails> {
            on { productType } doReturn ProductType.SUBS
            on { productId } doReturn RECURRING_ID
            on { name } doReturn RECURRING_TITLE
            on { description } doReturn RECURRING_DESCRIPTION_WITH_NEW_LINE
            on { getSubscriptionOfferDetails() } doReturn if (hasPricingPhase) {
                listOf(subscriptionOfferDetails)
            } else {
                null
            }
        }
    }

    private companion object {
        const val ONE_TIME_ID = "one_time_id"
        const val ONE_TIME_TITLE = "One-Time"
        const val ONE_TIME_DESCRIPTION = "One-Time Description"
        const val ONE_TIME_DESCRIPTION_WITH_NEW_LINE = "One-Time\n Description"
        const val ONE_TIME_PRICE = 1_000L
        const val ONE_TIME_PRICE_FORMATTED = "$10.00"

        val ONE_TIME_CONTRIBUTION = OneTimeContribution(
            id = ONE_TIME_ID,
            title = ONE_TIME_TITLE,
            description = ONE_TIME_DESCRIPTION,
            price = ONE_TIME_PRICE,
            priceFormatted = ONE_TIME_PRICE_FORMATTED,
        )

        const val RECURRING_ID = "recurring_product_id"
        const val RECURRING_TITLE = "Recurring"
        const val RECURRING_DESCRIPTION = "Recurring Description"
        const val RECURRING_DESCRIPTION_WITH_NEW_LINE = "Recurring\n Description"
        const val RECURRING_PRICE = 2_000L
        const val RECURRING_PRICE_FORMATTED = "$20.00"

        val RECURRING_CONTRIBUTION = RecurringContribution(
            id = RECURRING_ID,
            title = RECURRING_TITLE,
            description = RECURRING_DESCRIPTION,
            price = RECURRING_PRICE,
            priceFormatted = RECURRING_PRICE_FORMATTED,
        )
    }
}
