package net.thunderbird.feature.funding.googleplay.domain

import kotlinx.collections.immutable.ImmutableList
import kotlinx.coroutines.flow.Flow
import net.thunderbird.core.outcome.Outcome
import net.thunderbird.feature.funding.googleplay.domain.entity.AvailableContributions
import net.thunderbird.feature.funding.googleplay.domain.entity.ContributionId
import net.thunderbird.feature.funding.googleplay.domain.entity.ContributionPreselection
import net.thunderbird.feature.funding.googleplay.domain.entity.OneTimeContribution
import net.thunderbird.feature.funding.googleplay.domain.entity.PurchasedContribution
import net.thunderbird.feature.funding.googleplay.domain.entity.RecurringContribution

internal interface FundingDomainContract {

    interface UseCase {

        /**
         * Get available contributions.
         */
        fun interface GetAvailableContributions {
            operator fun invoke(): Flow<Outcome<AvailableContributions, ContributionError>>
        }

        /**
         * Get the latest purchased contribution.
         */
        fun interface GetLatestPurchasedContribution {
            operator fun invoke(): Flow<Outcome<PurchasedContribution?, ContributionError>>
        }
    }

    interface Policy {

        /**
         * Policy for selecting contributions to be preselected for display.
         */
        fun interface ContributionPreselector {

            /**
             * Selects contributions to be preselected for display.
             *
             * @param oneTimeContributions List of available one-time contributions.
             * @param recurringContributions List of available recurring contributions.
             * @return ContributionPreselection representing the preselected contributions.
             */
            fun preselect(
                oneTimeContributions: List<OneTimeContribution>,
                recurringContributions: List<RecurringContribution>,
            ): ContributionPreselection
        }
    }

    /**
     * Provider for contribution IDs.
     */
    interface ContributionIdProvider {
        val oneTimeContributionIds: ImmutableList<ContributionId>
        val recurringContributionIds: ImmutableList<ContributionId>
    }

    /**
     * Repository for managing contributions.
     */
    interface ContributionRepository {
        /**
         * Get one-time contributions.
         *
         * @param contributionIds The list of contribution IDs to fetch.
         * @return Flow of list of one-time contributions.
         */
        fun getAllOneTime(
            contributionIds: List<ContributionId>,
        ): Flow<Outcome<List<OneTimeContribution>, ContributionError>>

        /**
         * Get recurring contributions.
         *
         * @param contributionIds The list of contribution IDs to fetch.
         * @return Outcome flow containing a list of recurring contributions or an error if the operation fails.
         */
        fun getAllRecurring(
            contributionIds: List<ContributionId>,
        ): Flow<Outcome<List<RecurringContribution>, ContributionError>>

        /**
         * Get purchased contributions.
         *
         * @return Outcome flow containing a list of recurring contributions or an error if the operation fails.
         */
        fun getAllPurchased(): Flow<Outcome<List<PurchasedContribution>, ContributionError>>

        /**
         * Purchase a contribution.
         *
         * @param contributionId The contribution to purchase.
         * @return Outcome of the purchase.
         */
        suspend fun purchaseContribution(
            contributionId: ContributionId,
        ): Outcome<Unit, ContributionError>

        /**
         * Clears contribution resources.
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
