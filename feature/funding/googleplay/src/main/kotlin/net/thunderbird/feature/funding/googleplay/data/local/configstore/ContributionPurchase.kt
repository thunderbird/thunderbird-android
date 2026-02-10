package net.thunderbird.feature.funding.googleplay.data.local.configstore

import kotlinx.serialization.Serializable

/**
 * Represents a contribution purchase with details about the purchase.
 *
 * NOTE:
 * When changing this class, ensure that the serialization format remains compatible with previous versions to avoid
 * issues when reading existing data from the config store. If you need to add new fields, consider making them
 * nullable or providing default values to maintain backward compatibility.
 *
 * @property id The unique identifier for the contribution
 * @property title The title of the contribution
 * @property description The description of the contribution
 * @property price The price of the contribution in micro-units
 * @property priceFormatted The price of the contribution formatted as a string
 *
 * @property productId The product ID of the purchase
 * @property orderId The order ID of the purchase, if any
 * @property purchaseTimeMillis The purchase time in milliseconds
 */
@Serializable
internal data class ContributionPurchase(
    // contribution details
    val id: String,
    val title: String,
    val description: String,
    val price: Long,
    val priceFormatted: String,
    // purchase details
    val productId: String,
    val orderId: String?,
    val purchaseTimeMillis: Long,
)
