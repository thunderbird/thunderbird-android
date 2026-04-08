package net.thunderbird.feature.funding.googleplay.domain.entity

/**
 * Represents a one-time contribution entity.
 *
 * @property id The unique identifier of the contribution
 * @property title The title of the contribution
 * @property description The description of the contribution
 * @property price The price of the contribution in micro-units
 * @property priceFormatted The price of the contribution formatted as a string
 */
internal data class OneTimeContribution(
    override val id: String,
    override val title: String,
    override val description: String,
    override val price: Long,
    override val priceFormatted: String,
) : Contribution
