package net.thunderbird.feature.funding.googleplay.domain.entity

/**
 * Represents a collection of available contribution options, including both one-time and recurring choices.
 *
 * @property oneTimeContributions A list of one-time contribution options available for purchase.
 * @property recurringContributions A list of recurring (subscription) contribution options available for purchase.
 * @property preselection The configuration defining which contribution should be selected by default.
 * @property purchasedContribution The contribution currently owned by the user, if applicable.
 */
internal data class AvailableContributions(
    val oneTimeContributions: List<OneTimeContribution>,
    val recurringContributions: List<RecurringContribution>,
    val preselection: ContributionPreselection,
    val purchasedContribution: Contribution? = null,
)
