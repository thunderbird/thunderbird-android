package app.k9mail.feature.funding.googleplay.domain

import android.app.Activity
import app.k9mail.feature.funding.googleplay.domain.entity.Contribution
import app.k9mail.feature.funding.googleplay.domain.entity.OneTimeContribution
import app.k9mail.feature.funding.googleplay.domain.entity.RecurringContribution
import kotlinx.collections.immutable.ImmutableList

interface DomainContract {

    interface ContributionIdProvider {
        val oneTimeContributionIds: ImmutableList<String>
        val recurringContributionIds: ImmutableList<String>
    }

    interface BillingManager {
        /**
         * Load contributions.
         */
        suspend fun loadOneTimeContributions(): List<OneTimeContribution>

        /**
         * Load recurring contributions.
         */
        suspend fun loadRecurringContributions(): List<RecurringContribution>

        /**
         * Load purchased contributions.
         */
        suspend fun loadPurchasedContributions(): List<Contribution>

        /**
         * Purchase a contribution.
         *
         * @param activity The activity to use for the purchase flow.
         * @param contribution The contribution to purchase.
         * @return The purchased contribution or null if the purchase failed.
         */
        suspend fun purchaseContribution(
            activity: Activity,
            contribution: Contribution,
        ): Contribution?

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
