package net.thunderbird.feature.funding.googleplay.domain.usecase

import kotlinx.collections.immutable.toImmutableList
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import net.thunderbird.core.outcome.Outcome
import net.thunderbird.feature.funding.googleplay.domain.FundingDomainContract.ContributionError
import net.thunderbird.feature.funding.googleplay.domain.FundingDomainContract.ContributionIdProvider
import net.thunderbird.feature.funding.googleplay.domain.FundingDomainContract.ContributionRepository
import net.thunderbird.feature.funding.googleplay.domain.FundingDomainContract.Policy
import net.thunderbird.feature.funding.googleplay.domain.FundingDomainContract.UseCase
import net.thunderbird.feature.funding.googleplay.domain.entity.AvailableContributions

internal class GetAvailableContributions(
    private val repository: ContributionRepository,
    private val contributionIdProvider: ContributionIdProvider,
    private val preselector: Policy.ContributionPreselector,
) : UseCase.GetAvailableContributions {
    override fun invoke(): Flow<Outcome<AvailableContributions, ContributionError>> {
        return combine(
            repository.getAllOneTime(contributionIdProvider.oneTimeContributionIds),
            repository.getAllRecurring(contributionIdProvider.recurringContributionIds),
        ) { oneTimeResult, recurringResult ->
            val isOneTimeSuccess = oneTimeResult is Outcome.Success
            val isRecurringSuccess = recurringResult is Outcome.Success

            if ((isOneTimeSuccess || isRecurringSuccess)) {
                val oneTime = (oneTimeResult as? Outcome.Success)?.data.orEmpty()
                val recurring = (recurringResult as? Outcome.Success)?.data.orEmpty()

                val onetimeContributions = oneTime.sortedByDescending { it.price }
                val recurringContributions = recurring.sortedByDescending { it.price }
                val preselection = preselector.preselect(onetimeContributions, recurringContributions)

                Outcome.success(
                    AvailableContributions(
                        oneTimeContributions = oneTime.sortedByDescending { it.price }.toImmutableList(),
                        recurringContributions = recurring.sortedByDescending { it.price }.toImmutableList(),
                        preselection = preselection,
                    ),
                )
            } else {
                Outcome.failure(ContributionError.UnknownError("Failed to load contributions"))
            }
        }
    }
}
