package net.thunderbird.feature.funding.googleplay.ui.contribution

import android.app.Activity
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import net.thunderbird.core.outcome.Outcome
import net.thunderbird.feature.funding.googleplay.domain.DomainContract
import net.thunderbird.feature.funding.googleplay.domain.DomainContract.BillingError
import net.thunderbird.feature.funding.googleplay.domain.entity.Contribution

class FakeBillingManager : DomainContract.BillingManager {

    override val purchasedContribution: StateFlow<Outcome<Contribution?, BillingError>> = MutableStateFlow(
        Outcome.success(null),
    )

    override suspend fun loadOneTimeContributions() = Outcome.success(FakeData.oneTimeContributions)

    override suspend fun loadRecurringContributions() = Outcome.success(FakeData.recurringContributions)

    override suspend fun loadPurchasedContributions(): Outcome<List<Contribution>, BillingError> {
        return Outcome.success(
            listOf(
                FakeData.oneTimeContributions.first(),
            ),
        )
    }

    override suspend fun purchaseContribution(activity: Activity, contribution: Contribution) =
        Outcome.success(Unit)

    override fun clear() = Unit
}
