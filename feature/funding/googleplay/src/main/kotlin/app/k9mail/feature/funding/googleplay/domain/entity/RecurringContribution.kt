package app.k9mail.feature.funding.googleplay.domain.entity

data class RecurringContribution(
    override val id: String,
    override val title: String,
    override val price: String,
) : Contribution
