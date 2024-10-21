package app.k9mail.feature.funding.googleplay.domain

import android.app.Activity
import app.k9mail.feature.funding.googleplay.domain.entity.Contribution
import app.k9mail.feature.funding.googleplay.domain.entity.OneTimeContribution
import app.k9mail.feature.funding.googleplay.domain.entity.RecurringContribution
import kotlinx.collections.immutable.ImmutableList
import kotlinx.coroutines.flow.StateFlow

interface DomainContract {

    interface ContributionIdProvider {
        val oneTimeContributionIds: ImmutableList<String>
        val recurringContributionIds: ImmutableList<String>
    }

    interface BillingManager {
        /**
         * Flow that emits the last purchased contribution.
         */
        val purchasedContribution: StateFlow<Outcome<Contribution?, BillingError>>

        /**
         * Load contributions.
         */
        suspend fun loadOneTimeContributions(): Outcome<List<OneTimeContribution>, BillingError>

        /**
         * Load recurring contributions.
         */
        suspend fun loadRecurringContributions(): Outcome<List<RecurringContribution>, BillingError>

        /**
         * Load purchased contributions.
         */
        suspend fun loadPurchasedContributions(): Outcome<List<Contribution>, BillingError>

        /**
         * Purchase a contribution.
         *
         * @param activity The activity to use for the purchase flow.
         * @param contribution The contribution to purchase.
         * @return Outcome of the purchase.
         */
        suspend fun purchaseContribution(
            activity: Activity,
            contribution: Contribution,
        ): Outcome<Unit, BillingError>

        /**
         * Release all resources.
         */
        fun clear()
    }

    sealed interface BillingError {
        val message: String

        data class UserCancelled(
            override val message: String,
        ) : BillingError

        data class PurchaseFailed(
            override val message: String,
        ) : BillingError

        data class ServiceDisconnected(
            override val message: String,
        ) : BillingError

        data class DeveloperError(
            override val message: String,
        ) : BillingError

        data class UnknownError(
            override val message: String,
        ) : BillingError
    }
}
