package net.thunderbird.feature.funding.googleplay.domain.entity

data class OneTimeContribution(
internal data class OneTimeContribution(
    override val id: String,
    override val title: String,
    override val description: String,
    override val price: Long,
    override val priceFormatted: String,
) : Contribution
