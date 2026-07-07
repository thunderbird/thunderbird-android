package net.thunderbird.feature.funding.googleplay.domain

import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.persistentListOf
import net.thunderbird.feature.funding.googleplay.domain.entity.ContributionId

// TODO should be provided externally per app variant
internal class ContributionIdProvider : FundingDomainContract.ContributionIdProvider {
    override val oneTimeContributionIds: ImmutableList<ContributionId> = persistentListOf(
        ContributionId("contribution_tfa_onetime_xs"),
        ContributionId("contribution_tfa_onetime_s"),
        ContributionId("contribution_tfa_onetime_m"),
        ContributionId("contribution_tfa_onetime_l"),
        ContributionId("contribution_tfa_onetime_xl"),
        ContributionId("contribution_tfa_onetime_xxl"),
    )

    override val recurringContributionIds: ImmutableList<ContributionId> = persistentListOf(
        ContributionId("contribution_tfa_monthly_xs"),
        ContributionId("contribution_tfa_monthly_s"),
        ContributionId("contribution_tfa_monthly_m"),
        ContributionId("contribution_tfa_monthly_l"),
        ContributionId("contribution_tfa_monthly_xl"),
        ContributionId("contribution_tfa_monthly_xxl"),
    )
}
