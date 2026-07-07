package net.thunderbird.feature.funding.googleplay.domain.entity

import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.persistentListOf

/**
 * Represents a collection of available contribution options, including both one-time and recurring choices.
 *
 * @property oneTimeContributions A list of one-time contribution options available for purchase.
 * @property recurringContributions A list of recurring (subscription) contribution options available for purchase.
 * @property preselection The configuration defining which contribution should be selected by default.
 */
internal data class AvailableContributions(
    val oneTimeContributions: ImmutableList<OneTimeContribution>,
    val recurringContributions: ImmutableList<RecurringContribution>,
    val preselection: ContributionPreselection,
) {
    companion object {
        val Empty = AvailableContributions(
            oneTimeContributions = persistentListOf(),
            recurringContributions = persistentListOf(),
            preselection = ContributionPreselection(
                oneTimeId = null,
                recurringId = null,
            ),
        )
    }
}
