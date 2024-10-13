package app.k9mail.feature.funding.googleplay.domain.entity

class OneTimeContribution(
    override val id: String,
    override val title: String,
    override val price: String,
) : Contribution
