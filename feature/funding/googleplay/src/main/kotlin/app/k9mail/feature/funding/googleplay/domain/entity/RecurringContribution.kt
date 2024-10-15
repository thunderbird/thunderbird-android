package app.k9mail.feature.funding.googleplay.domain.entity

data class RecurringContribution(
    override val id: String,
    override val title: String,
    override val description: String,
    override val price: Long,
    override val priceFormatted: String,
) : Contribution
