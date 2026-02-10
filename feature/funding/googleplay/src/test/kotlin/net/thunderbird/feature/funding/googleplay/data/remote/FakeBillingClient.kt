package net.thunderbird.feature.funding.googleplay.data.remote

import android.app.Activity
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import net.thunderbird.core.outcome.Outcome
import net.thunderbird.feature.funding.googleplay.data.FundingDataContract
import net.thunderbird.feature.funding.googleplay.domain.FundingDomainContract.ContributionError
import net.thunderbird.feature.funding.googleplay.domain.entity.Contribution
import net.thunderbird.feature.funding.googleplay.domain.entity.OneTimeContribution
import net.thunderbird.feature.funding.googleplay.domain.entity.RecurringContribution

internal class FakeBillingClient : FundingDataContract.Remote.BillingClient {

    // Configuration
    var connectOutcome: Outcome<Unit, ContributionError> = Outcome.success(Unit)
    var oneTimeOutcome: Outcome<List<OneTimeContribution>, ContributionError> =
        Outcome.success(emptyList())
    var recurringOutcome: Outcome<List<RecurringContribution>, ContributionError> =
        Outcome.success(emptyList())

    var purchasedOneTimeOutcome: Outcome<List<OneTimeContribution>, ContributionError> =
        Outcome.success(emptyList())
    var purchasedRecurringOutcome: Outcome<List<RecurringContribution>, ContributionError> =
        Outcome.success(emptyList())
    var purchaseHistoryOutcome: Outcome<OneTimeContribution?, ContributionError> =
        Outcome.success(null)

    // State
    private val _purchasedContribution = MutableStateFlow<Outcome<Contribution?, ContributionError>>(
        Outcome.success(null),
    )

    override val purchasedContribution: StateFlow<Outcome<Contribution?, ContributionError>>
        get() = _purchasedContribution

    override suspend fun <T> connect(
        onConnected: suspend () -> Outcome<T, ContributionError>,
    ): Outcome<T, ContributionError> {
        return when (val result = connectOutcome) {
            is Outcome.Success -> onConnected()
            is Outcome.Failure -> Outcome.failure(result.error)
        }
    }

    override fun disconnect() {
        _purchasedContribution.value = Outcome.success(null)
    }

    override suspend fun loadOneTimeContributions(
        productIds: List<String>,
    ): Outcome<List<OneTimeContribution>, ContributionError> = oneTimeOutcome

    override suspend fun loadRecurringContributions(
        productIds: List<String>,
    ): Outcome<List<RecurringContribution>, ContributionError> = recurringOutcome

    override suspend fun loadPurchasedOneTimeContributions(): Outcome<List<OneTimeContribution>, ContributionError> =
        purchasedOneTimeOutcome

    override suspend fun loadPurchasedRecurringContributions():
        Outcome<List<RecurringContribution>, ContributionError> = purchasedRecurringOutcome

    override suspend fun loadPurchasedOneTimeContributionHistory(): Outcome<OneTimeContribution?, ContributionError> =
        purchaseHistoryOutcome

    override suspend fun purchaseContribution(
        activity: Activity,
        contribution: Contribution,
    ): Outcome<Unit, ContributionError> = Outcome.success(Unit)
}
