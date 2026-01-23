package net.thunderbird.feature.funding.googleplay.domain.entity

/**
 * Represents a collection of available contributions.
 *
 * @property oneTimeContributions A list of one-time contribution options
 * @property recurringContributions A list of recurring contribution options
 * @property purchasedContribution The currently purchased contribution, if any
 */
internal data class AvailableContributions(
    val oneTimeContributions: List<OneTimeContribution>,
    val recurringContributions: List<RecurringContribution>,
    val purchasedContribution: Contribution? = null,
)
