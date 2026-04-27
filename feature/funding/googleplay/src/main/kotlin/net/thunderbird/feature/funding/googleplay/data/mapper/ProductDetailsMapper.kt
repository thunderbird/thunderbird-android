package net.thunderbird.feature.funding.googleplay.data.mapper

import com.android.billingclient.api.BillingClient
import com.android.billingclient.api.ProductDetails
import com.android.billingclient.api.Purchase
import com.android.billingclient.api.PurchaseHistoryRecord
import kotlin.time.Instant
import kotlinx.datetime.LocalDateTime
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
import net.thunderbird.feature.funding.googleplay.data.FundingDataContract.Mapper
import net.thunderbird.feature.funding.googleplay.domain.entity.Contribution
import net.thunderbird.feature.funding.googleplay.domain.entity.ContributionId
import net.thunderbird.feature.funding.googleplay.domain.entity.OneTimeContribution
import net.thunderbird.feature.funding.googleplay.domain.entity.PurchasedContribution
import net.thunderbird.feature.funding.googleplay.domain.entity.RecurringContribution

internal class ProductDetailsMapper : Mapper.Product {

    override fun mapToContribution(product: ProductDetails): Contribution {
        return when (product.productType) {
            BillingClient.ProductType.INAPP -> mapToOneTimeContribution(product)
            BillingClient.ProductType.SUBS -> mapToRecurringContribution(product)
            else -> throw IllegalArgumentException("Unknown product type: ${product.productType}")
        }
    }

    override fun mapToOneTimeContribution(product: ProductDetails): OneTimeContribution {
        require(product.productType == BillingClient.ProductType.INAPP) { "Product type must be INAPP" }

        val offerDetails = product.oneTimePurchaseOfferDetails

        return if (offerDetails != null) {
            OneTimeContribution(
                id = ContributionId(product.productId),
                title = product.name,
                description = product.description.replace("\n", ""),
                price = offerDetails.priceAmountMicros,
                priceFormatted = offerDetails.formattedPrice,
            )
        } else {
            error("One-time product has no offer details: ${product.productId}")
        }
    }

    override fun mapToRecurringContribution(product: ProductDetails): RecurringContribution {
        require(product.productType == BillingClient.ProductType.SUBS) { "Product type must be SUBS" }

        // We assume the product has only one offer and one pricing phase
        val pricingPhase =
            product.subscriptionOfferDetails?.firstOrNull()?.pricingPhases?.pricingPhaseList?.firstOrNull()

        return if (pricingPhase != null) {
            RecurringContribution(
                id = ContributionId(product.productId),
                title = product.name,
                description = product.description.replace("\n", ""),
                price = pricingPhase.priceAmountMicros,
                priceFormatted = pricingPhase.formattedPrice,
            )
        } else {
            error("Subscription product has no pricing phase: ${product.productId}")
        }
    }

    override fun mapToPurchasedContribution(
        purchase: Purchase,
        productDetails: ProductDetails,
    ): PurchasedContribution {
        val contribution = mapToContribution(productDetails)
        val purchaseTime = mapToLocalDateTime(purchase.purchaseTime)

        return PurchasedContribution(
            id = contribution.id,
            contribution = contribution,
            purchaseDate = purchaseTime,
        )
    }

    override fun mapHistoryToPurchasedContribution(
        purchase: PurchaseHistoryRecord,
        productDetails: ProductDetails,
    ): PurchasedContribution {
        val contribution = mapToContribution(productDetails)
        val purchaseTime = mapToLocalDateTime(purchase.purchaseTime)

        return PurchasedContribution(
            id = contribution.id,
            contribution = contribution,
            purchaseDate = purchaseTime,
        )
    }

    private fun mapToLocalDateTime(timestamp: Long): LocalDateTime {
        val instant = Instant.fromEpochMilliseconds(timestamp)
        return instant.toLocalDateTime(TimeZone.currentSystemDefault())
    }
}
