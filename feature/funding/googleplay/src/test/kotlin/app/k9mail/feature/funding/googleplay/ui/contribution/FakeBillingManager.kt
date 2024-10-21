package app.k9mail.feature.funding.googleplay.ui.contribution

import android.app.Activity
import app.k9mail.feature.funding.googleplay.domain.DomainContract
import app.k9mail.feature.funding.googleplay.domain.DomainContract.BillingError
import app.k9mail.feature.funding.googleplay.domain.Outcome
import app.k9mail.feature.funding.googleplay.domain.entity.Contribution
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

class FakeBillingManager : DomainContract.BillingManager {

    override val purchasedContribution: StateFlow<Outcome<Contribution?, BillingError>> = MutableStateFlow(
        Outcome.success(null),
    )

    override suspend fun loadOneTimeContributions() = FakeData.oneTimeContributions

    override suspend fun loadRecurringContributions() = FakeData.recurringContributions

    override suspend fun loadPurchasedContributions(): List<Contribution> {
        return listOf(
            FakeData.oneTimeContributions.first(),
        )
    }

    override suspend fun purchaseContribution(activity: Activity, contribution: Contribution) =
        Outcome.success(Unit)

    override fun clear() = Unit
}
