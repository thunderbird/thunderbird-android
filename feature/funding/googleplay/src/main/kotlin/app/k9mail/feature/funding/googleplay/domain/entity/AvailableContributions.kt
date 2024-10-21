package app.k9mail.feature.funding.googleplay.domain.entity

data class AvailableContributions(
    val oneTimeContributions: List<OneTimeContribution>,
    val recurringContributions: List<RecurringContribution>,
    val purchasedContribution: Contribution? = null,
)
