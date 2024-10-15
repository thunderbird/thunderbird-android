package app.k9mail.feature.funding.googleplay.data.mapper

import app.k9mail.feature.funding.googleplay.data.DataContract.Mapper
import app.k9mail.feature.funding.googleplay.domain.entity.Contribution
import app.k9mail.feature.funding.googleplay.domain.entity.OneTimeContribution
import app.k9mail.feature.funding.googleplay.domain.entity.RecurringContribution
import com.android.billingclient.api.BillingClient
import com.android.billingclient.api.ProductDetails

class ProductDetailsMapper : Mapper.Product {

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
                id = product.productId,
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
                id = product.productId,
                title = product.name,
                description = product.description.replace("\n", ""),
                price = pricingPhase.priceAmountMicros,
                priceFormatted = pricingPhase.formattedPrice,
            )
        } else {
            error("Subscription product has no pricing phase: ${product.productId}")
        }
    }
}
