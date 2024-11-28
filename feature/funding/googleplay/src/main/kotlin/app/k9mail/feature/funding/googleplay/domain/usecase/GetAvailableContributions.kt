package app.k9mail.feature.funding.googleplay.domain.usecase

import app.k9mail.feature.funding.googleplay.domain.DomainContract.BillingError
import app.k9mail.feature.funding.googleplay.domain.DomainContract.BillingManager
import app.k9mail.feature.funding.googleplay.domain.DomainContract.UseCase
import app.k9mail.feature.funding.googleplay.domain.Outcome
import app.k9mail.feature.funding.googleplay.domain.entity.AvailableContributions

class GetAvailableContributions(
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
