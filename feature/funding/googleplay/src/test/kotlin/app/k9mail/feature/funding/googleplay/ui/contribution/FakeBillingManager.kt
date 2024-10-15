package app.k9mail.feature.funding.googleplay.ui.contribution

import android.app.Activity
import app.k9mail.feature.funding.googleplay.domain.DomainContract
import app.k9mail.feature.funding.googleplay.domain.entity.Contribution

class FakeBillingManager : DomainContract.BillingManager {

    override suspend fun loadOneTimeContributions() = FakeData.oneTimeContributions

    override suspend fun loadRecurringContributions() = FakeData.recurringContributions

    override suspend fun loadPurchasedContributions(): List<Contribution> {
        return listOf(
            FakeData.oneTimeContributions.first(),
        )
    }

    override suspend fun purchaseContribution(activity: Activity, contribution: Contribution) =
        FakeData.oneTimeContributions.first()

    override fun clear() = Unit
}
