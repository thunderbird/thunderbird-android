package net.thunderbird.feature.funding.googleplay.domain.usecase

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import net.thunderbird.core.outcome.Outcome
import net.thunderbird.feature.funding.googleplay.domain.FundingDomainContract.ContributionError
import net.thunderbird.feature.funding.googleplay.domain.FundingDomainContract.ContributionIdProvider
import net.thunderbird.feature.funding.googleplay.domain.FundingDomainContract.ContributionRepository
import net.thunderbird.feature.funding.googleplay.domain.FundingDomainContract.UseCase
import net.thunderbird.feature.funding.googleplay.domain.entity.AvailableContributions

internal class GetAvailableContributions(
    private val repository: ContributionRepository,
    private val contributionIdProvider: ContributionIdProvider,
) : UseCase.GetAvailableContributions {
    override fun invoke(): Flow<Outcome<AvailableContributions, ContributionError>> {
        return combine(
            repository.getAllOneTime(contributionIdProvider.oneTimeContributionIds),
            repository.getAllRecurring(contributionIdProvider.recurringContributionIds),
            repository.getAllPurchased(),
        ) { oneTimeResult, recurringResult, purchasedResult ->
            val isOneTimeSuccess = oneTimeResult is Outcome.Success
            val isRecurringSuccess = recurringResult is Outcome.Success
            val isPurchasedSuccess = purchasedResult is Outcome.Success

            if ((isOneTimeSuccess || isRecurringSuccess) && isPurchasedSuccess) {
                val oneTime = (oneTimeResult as? Outcome.Success)?.data.orEmpty()
                val recurring = (recurringResult as? Outcome.Success)?.data.orEmpty()
                val purchased = purchasedResult.data.firstOrNull()

                Outcome.success(
                    AvailableContributions(
                        oneTimeContributions = oneTime.sortedByDescending { it.price },
                        recurringContributions = recurring.sortedByDescending { it.price },
                        purchasedContribution = purchased,
                    ),
                )
            } else if (!isPurchasedSuccess) {
                Outcome.failure((purchasedResult as Outcome.Failure).error)
            } else {
                Outcome.failure(ContributionError.UnknownError("Failed to load contributions"))
            }
        }
    }
}
