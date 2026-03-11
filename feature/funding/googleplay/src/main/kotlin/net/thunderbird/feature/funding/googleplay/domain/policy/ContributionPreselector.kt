package net.thunderbird.feature.funding.googleplay.domain.policy

import net.thunderbird.feature.funding.googleplay.domain.FundingDomainContract.Policy
import net.thunderbird.feature.funding.googleplay.domain.entity.Contribution
import net.thunderbird.feature.funding.googleplay.domain.entity.ContributionId
import net.thunderbird.feature.funding.googleplay.domain.entity.ContributionPreselection
import net.thunderbird.feature.funding.googleplay.domain.entity.OneTimeContribution
import net.thunderbird.feature.funding.googleplay.domain.entity.RecurringContribution

internal class ContributionPreselector : Policy.ContributionPreselector {
    override fun preselect(
        oneTimeContributions: List<OneTimeContribution>,
        recurringContributions: List<RecurringContribution>,
    ): ContributionPreselection = ContributionPreselection(
        oneTimeId = getSecondLowestByPriceOrNull(oneTimeContributions),
        recurringId = getSecondLowestByPriceOrNull(recurringContributions),
    )

    private fun getSecondLowestByPriceOrNull(contributions: List<Contribution>): ContributionId? {
        return when {
            contributions.size > 1 -> contributions.sortedBy { it.price }[1].id
            contributions.size == 1 -> contributions[0].id
            else -> null
        }
    }
}
