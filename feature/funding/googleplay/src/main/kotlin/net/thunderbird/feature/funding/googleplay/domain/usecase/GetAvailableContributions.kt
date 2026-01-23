package net.thunderbird.feature.funding.googleplay.domain.usecase

import net.thunderbird.core.outcome.Outcome
import net.thunderbird.feature.funding.googleplay.domain.DomainContract.BillingError
import net.thunderbird.feature.funding.googleplay.domain.DomainContract.BillingManager
import net.thunderbird.feature.funding.googleplay.domain.DomainContract.UseCase
import net.thunderbird.feature.funding.googleplay.domain.entity.AvailableContributions

internal class GetAvailableContributions(
    private val billingManager: BillingManager,
) : UseCase.GetAvailableContributions {
    override suspend fun invoke(): Outcome<AvailableContributions, BillingError> {
        val oneTimeContributionsResult = billingManager.loadOneTimeContributions()
        val recurringContributionsResult = billingManager.loadRecurringContributions()
        val purchasedContributionResult = billingManager.loadPurchasedContributions()

        return if (oneTimeContributionsResult is Outcome.Success &&
            recurringContributionsResult is Outcome.Success &&
            purchasedContributionResult is Outcome.Success
        ) {
            Outcome.success(
                AvailableContributions(
                    oneTimeContributions = oneTimeContributionsResult.data,
                    recurringContributions = recurringContributionsResult.data,
                    purchasedContribution = purchasedContributionResult.data.firstOrNull(),
                ),
            )
        } else {
            // TODO handle errors
            Outcome.failure(BillingError.UnknownError("Failed to load contributions"))
        }
    }
}
