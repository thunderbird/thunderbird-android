package app.k9mail.feature.funding.googleplay.domain

import kotlinx.collections.immutable.ImmutableList

interface DomainContract {

    interface ContributionIdProvider {
        val oneTimeContributionIds: ImmutableList<String>
        val recurringContributionIds: ImmutableList<String>
    }
}
