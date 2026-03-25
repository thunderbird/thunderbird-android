package net.thunderbird.feature.funding.googleplay.data.remote

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import net.thunderbird.core.outcome.Outcome
import net.thunderbird.feature.funding.googleplay.data.FundingDataContract
import net.thunderbird.feature.funding.googleplay.domain.FundingDomainContract.ContributionError
import net.thunderbird.feature.funding.googleplay.domain.entity.ContributionId
import net.thunderbird.feature.funding.googleplay.domain.entity.OneTimeContribution
import net.thunderbird.feature.funding.googleplay.domain.entity.PurchasedContribution
import net.thunderbird.feature.funding.googleplay.domain.entity.RecurringContribution

internal class FakeBillingClient : FundingDataContract.Remote.BillingClient {

    // Configuration
    var oneTimeOutcome: Outcome<List<OneTimeContribution>, ContributionError> =
        Outcome.success(emptyList())
    var recurringOutcome: Outcome<List<RecurringContribution>, ContributionError> =
        Outcome.success(emptyList())

    var purchasedOneTimeOutcome: Outcome<List<PurchasedContribution>, ContributionError> =
        Outcome.success(emptyList())
    var purchasedRecurringOutcome: Outcome<List<PurchasedContribution>, ContributionError> =
        Outcome.success(emptyList())
    var purchaseHistoryOutcome: Outcome<PurchasedContribution?, ContributionError> =
        Outcome.success(null)
    var purchaseOutcome: Outcome<Unit, ContributionError> = Outcome.success(Unit)
    var clearCount = 0

    // State
    private val _purchasedContribution = MutableStateFlow<Outcome<PurchasedContribution?, ContributionError>>(
        Outcome.success(null),
    )

    override val purchasedContribution: StateFlow<Outcome<PurchasedContribution?, ContributionError>>
        get() = _purchasedContribution

    override fun disconnect() {
        clearCount++
        _purchasedContribution.value = Outcome.success(null)
    }

    override suspend fun loadOneTimeContributions(
        productIds: List<String>,
    ): Outcome<List<OneTimeContribution>, ContributionError> = oneTimeOutcome

    override suspend fun loadRecurringContributions(
        productIds: List<String>,
    ): Outcome<List<RecurringContribution>, ContributionError> = recurringOutcome

    override suspend fun loadPurchasedOneTimeContributions(): Outcome<List<PurchasedContribution>, ContributionError> =
        purchasedOneTimeOutcome

    override suspend fun loadPurchasedRecurringContributions():
        Outcome<List<PurchasedContribution>, ContributionError> = purchasedRecurringOutcome

    override suspend fun loadPurchasedOneTimeContributionHistory(): Outcome<PurchasedContribution?, ContributionError> =
        purchaseHistoryOutcome

    override suspend fun purchaseContribution(
        contributionId: ContributionId,
    ): Outcome<Unit, ContributionError> = purchaseOutcome
}
