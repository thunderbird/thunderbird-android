package net.thunderbird.feature.funding.googleplay.domain.entity

/**
 * Represents a contribution entity.
 *
 * @property id The unique identifier of the contribution
 * @property title The title of the contribution
 * @property description The description of the contribution
 * @property price The price of the contribution in micro-units
 * @property priceFormatted The price of the contribution formatted as a string
 */
internal interface Contribution {

    /**
     * The unique identifier of the contribution.
     */
    val id: String

    /**
     * The title of the contribution.
     */
    val title: String

    /**
     * The description of the contribution.
     */
    val description: String

    /**
     * The price of the contribution in micro-units.
     */
    val price: Long

    /**
     * The price of the contribution formatted as a string.
     */
    val priceFormatted: String
}
