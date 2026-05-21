package net.thunderbird.feature.funding.googleplay.domain.usecase

import kotlin.time.Clock
import kotlin.time.Duration.Companion.days
import kotlin.time.Instant
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.datetime.LocalDateTime
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toInstant
import net.thunderbird.core.outcome.Outcome
import net.thunderbird.feature.funding.googleplay.domain.FundingDomainContract
import net.thunderbird.feature.funding.googleplay.domain.FundingDomainContract.UseCase
import net.thunderbird.feature.funding.googleplay.domain.entity.OneTimeContribution
import net.thunderbird.feature.funding.googleplay.domain.entity.PurchasedContribution
import net.thunderbird.feature.funding.googleplay.domain.entity.RecurringContribution

internal class GetLatestPurchasedContribution(
    private val repository: FundingDomainContract.ContributionRepository,
    private val clock: Clock,
) : UseCase.GetLatestPurchasedContribution {
    override fun invoke(): Flow<Outcome<PurchasedContribution?, FundingDomainContract.ContributionError>> {
        return repository.getAllPurchased().map { outcome ->
            when (outcome) {
                is Outcome.Success -> {
                    val sortedPurchases = outcome.data
                        .filter { it.isActive() }
                        .sortedByDescending { it.purchaseDate }
                        .sortedByDescending { it.contribution is RecurringContribution }
                    Outcome.Success(sortedPurchases.firstOrNull())
                }

                is Outcome.Failure -> Outcome.Failure(outcome.error)
            }
        }
    }

    /**
     * A contribution is considered active if:
     * - For one-time contributions, they are always active.
     * - For recurring contributions, they are active if the purchase date plus the validity period has not yet expired.
     */
    private fun PurchasedContribution.isActive(): Boolean {
        return when (val contribution = this.contribution) {
            is OneTimeContribution -> true
            is RecurringContribution -> checkExpired(this.purchaseDate)
            else -> error("Unknown contribution type: ${contribution::class}")
        }
    }

    private fun checkExpired(purchaseDate: LocalDateTime): Boolean {
        val expiryDate = calculateExpiryDate(purchaseDate)
        return expiryDate > clock.now()
    }

    private fun calculateExpiryDate(purchaseDate: LocalDateTime): Instant = purchaseDate
        .toInstant(TimeZone.currentSystemDefault()).plus(RECURRING_CONTRIBUTION_VALIDITY_PERIOD)

    private companion object {
        private val RECURRING_CONTRIBUTION_VALIDITY_PERIOD = 90.days
    }
}
