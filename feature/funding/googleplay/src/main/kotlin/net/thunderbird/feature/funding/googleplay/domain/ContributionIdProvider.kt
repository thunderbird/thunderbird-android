package net.thunderbird.feature.funding.googleplay.domain

import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.persistentListOf

// TODO should be provided externally per app variant
internal class ContributionIdProvider : FundingDomainContract.ContributionIdProvider {
    override val oneTimeContributionIds: ImmutableList<String> = persistentListOf(
        "contribution_tfa_onetime_xs",
        "contribution_tfa_onetime_s",
        "contribution_tfa_onetime_m",
        "contribution_tfa_onetime_l",
        "contribution_tfa_onetime_xl",
        "contribution_tfa_onetime_xxl",
    )

    override val recurringContributionIds: ImmutableList<String> = persistentListOf(
        "contribution_tfa_monthly_xs",
        "contribution_tfa_monthly_s",
        "contribution_tfa_monthly_m",
        "contribution_tfa_monthly_l",
        "contribution_tfa_monthly_xl",
        "contribution_tfa_monthly_xxl",
    )
}
