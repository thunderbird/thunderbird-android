package app.k9mail.feature.funding.googleplay.ui.contribution

import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringArrayResource
import androidx.compose.ui.res.stringResource
import app.k9mail.feature.funding.googleplay.R
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.persistentListOf
import kotlinx.collections.immutable.toImmutableList

// Ids need to be kept in sync with ContributionIdProvider.kt
internal object ContributionIdStringMapper {

    @Composable
    fun mapToContributionTitle(contributionId: String): String {
        return when (contributionId) {
            "contribution_tfa_onetime_xs" -> stringResource(
                R.string.funding_googleplay_contribution_tfa_onetime_xs_title,
            )

            "contribution_tfa_onetime_s" -> stringResource(
                R.string.funding_googleplay_contribution_tfa_onetime_s_title,
            )

            "contribution_tfa_onetime_m" -> stringResource(
                R.string.funding_googleplay_contribution_tfa_onetime_m_title,
            )

            "contribution_tfa_onetime_l" -> stringResource(
                R.string.funding_googleplay_contribution_tfa_onetime_l_title,
            )

            "contribution_tfa_onetime_xl" -> stringResource(
                R.string.funding_googleplay_contribution_tfa_onetime_xl_title,
            )

            "contribution_tfa_onetime_xxl" -> stringResource(
                R.string.funding_googleplay_contribution_tfa_onetime_xxl_title,
            )

            "contribution_tfa_monthly_xs" -> stringResource(
                R.string.funding_googleplay_contribution_tfa_recurring_xs_title,
            )

            "contribution_tfa_monthly_s" -> stringResource(
                R.string.funding_googleplay_contribution_tfa_recurring_s_title,
            )

            "contribution_tfa_monthly_m" -> stringResource(
                R.string.funding_googleplay_contribution_tfa_recurring_m_title,
            )

            "contribution_tfa_monthly_l" -> stringResource(
                R.string.funding_googleplay_contribution_tfa_recurring_l_title,
            )

            "contribution_tfa_monthly_xl" -> stringResource(
                R.string.funding_googleplay_contribution_tfa_recurring_xl_title,
            )

            "contribution_tfa_monthly_xxl" -> stringResource(
                R.string.funding_googleplay_contribution_tfa_recurring_xxl_title,
            )

            else -> throw IllegalArgumentException("Unknown contribution ID: $contributionId")
        }
    }

    @Composable
    fun mapToContributionDescription(contributionId: String): String {
        return when (contributionId) {
            "contribution_tfa_onetime_xs" -> stringResource(
                R.string.funding_googleplay_contribution_tfa_onetime_xs_description,
            )

            "contribution_tfa_onetime_s" -> stringResource(
                R.string.funding_googleplay_contribution_tfa_onetime_s_description,
            )

            "contribution_tfa_onetime_m" -> stringResource(
                R.string.funding_googleplay_contribution_tfa_onetime_m_description,
            )

            "contribution_tfa_onetime_l" -> stringResource(
                R.string.funding_googleplay_contribution_tfa_onetime_l_description,
            )

            "contribution_tfa_onetime_xl" -> stringResource(
                R.string.funding_googleplay_contribution_tfa_onetime_xl_description,
            )

            "contribution_tfa_onetime_xxl" -> stringResource(
                R.string.funding_googleplay_contribution_tfa_onetime_xxl_description,
            )

            "contribution_tfa_monthly_xs" -> stringResource(
                R.string.funding_googleplay_contribution_tfa_recurring_xs_description,
            )

            "contribution_tfa_monthly_s" -> stringResource(
                R.string.funding_googleplay_contribution_tfa_recurring_s_description,
            )

            "contribution_tfa_monthly_m" -> stringResource(
                R.string.funding_googleplay_contribution_tfa_recurring_m_description,
            )

            "contribution_tfa_monthly_l" -> stringResource(
                R.string.funding_googleplay_contribution_tfa_recurring_l_description,
            )

            "contribution_tfa_monthly_xl" -> stringResource(
                R.string.funding_googleplay_contribution_tfa_recurring_xl_description,
            )

            "contribution_tfa_monthly_xxl" -> stringResource(
                R.string.funding_googleplay_contribution_tfa_recurring_xxl_description,
            )

            else -> throw IllegalArgumentException("Unknown contribution ID: $contributionId")
        }
    }

    @Composable
    fun mapToContributionBenefits(contributionId: String): ImmutableList<String> {
        return when (contributionId) {
            "contribution_tfa_monthly_xs" -> stringArrayResource(
                R.array.funding_googleplay_contribution_tfa_recurring_xs_benefits,
            ).toImmutableList()

            "contribution_tfa_monthly_s" -> stringArrayResource(
                R.array.funding_googleplay_contribution_tfa_recurring_s_benefits,
            ).toImmutableList()

            "contribution_tfa_monthly_m" -> stringArrayResource(
                R.array.funding_googleplay_contribution_tfa_recurring_m_benefits,
            ).toImmutableList()

            "contribution_tfa_monthly_l" -> stringArrayResource(
                R.array.funding_googleplay_contribution_tfa_recurring_l_benefits,
            ).toImmutableList()

            "contribution_tfa_monthly_xl" -> stringArrayResource(
                R.array.funding_googleplay_contribution_tfa_recurring_xl_benefits,
            ).toImmutableList()

            "contribution_tfa_monthly_xxl" -> stringArrayResource(
                R.array.funding_googleplay_contribution_tfa_recurring_xxl_benefits,
            ).toImmutableList()

            else -> persistentListOf()
        }
    }
}
