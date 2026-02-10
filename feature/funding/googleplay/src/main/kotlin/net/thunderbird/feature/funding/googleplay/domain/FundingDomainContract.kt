package net.thunderbird.feature.funding.googleplay.domain

import android.app.Activity
import kotlinx.collections.immutable.ImmutableList
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.StateFlow
import net.thunderbird.core.outcome.Outcome
import net.thunderbird.feature.funding.googleplay.domain.entity.AvailableContributions
import net.thunderbird.feature.funding.googleplay.domain.entity.Contribution
import net.thunderbird.feature.funding.googleplay.domain.entity.OneTimeContribution
import net.thunderbird.feature.funding.googleplay.domain.entity.RecurringContribution

internal interface FundingDomainContract {

    interface UseCase {

        /**
         * Get available contributions.
         */
        fun interface GetAvailableContributions {
            operator fun invoke(): Flow<Outcome<AvailableContributions, ContributionError>>
        }
    }

    /**
     * Provider for contribution IDs.
     */
    interface ContributionIdProvider {
        val oneTimeContributionIds: ImmutableList<String>
        val recurringContributionIds: ImmutableList<String>
    }

    /**
     * Repository for managing contributions.
     */
    interface ContributionRepository {
        /**
         * Get one-time contributions.
         *
         * @param productIds The list of product IDs to fetch.
         * @return Flow of list of one-time contributions.
         */
        fun getAllOneTime(productIds: List<String>): Flow<Outcome<List<OneTimeContribution>, ContributionError>>

        /**
         * Get recurring contributions.
         *
         * @param productIds The list of product IDs to fetch.
         * @return Flow of list of recurring contributions.
         */
        fun getAllRecurring(productIds: List<String>): Flow<Outcome<List<RecurringContribution>, ContributionError>>

        /**
         * Get purchased contributions.
         *
         * @return Flow of list of purchased contributions.
         */
        fun getAllPurchased(): Flow<Outcome<List<Contribution>, ContributionError>>
    }

    interface ContributionManager {
        /**
         * Flow that emits the last purchased contribution.
         */
        val purchasedContribution: StateFlow<Outcome<Contribution?, ContributionError>>

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
        ): Outcome<Unit, ContributionError>

        /**
         * Release all resources.
         */
        fun clear()
    }

    /**
     * Error types related to contribution operations.
     */
    sealed interface ContributionError {
        val message: String

        data class UserCancelled(
            override val message: String,
        ) : ContributionError

        data class PurchaseFailed(
            override val message: String,
        ) : ContributionError

        data class ServiceDisconnected(
            override val message: String,
        ) : ContributionError

        data class DeveloperError(
            override val message: String,
        ) : ContributionError

        data class UnknownError(
            override val message: String,
        ) : ContributionError
    }
}
