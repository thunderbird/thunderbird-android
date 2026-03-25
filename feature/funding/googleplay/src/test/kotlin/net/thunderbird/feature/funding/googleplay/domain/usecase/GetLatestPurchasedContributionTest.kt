package net.thunderbird.feature.funding.googleplay.domain.usecase

import assertk.assertThat
import assertk.assertions.isEqualTo
import assertk.assertions.isInstanceOf
import assertk.assertions.isNotNull
import assertk.assertions.isNull
import kotlin.test.Test
import kotlin.time.Duration.Companion.days
import kotlin.time.Duration.Companion.seconds
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import kotlinx.datetime.LocalDateTime
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toInstant
import kotlinx.datetime.toLocalDateTime
import net.thunderbird.core.outcome.Outcome
import net.thunderbird.core.testing.TestClock
import net.thunderbird.feature.funding.googleplay.domain.FundingDomainContract.ContributionError
import net.thunderbird.feature.funding.googleplay.ui.contribution.FakeContributionRepository
import net.thunderbird.feature.funding.googleplay.ui.contribution.FakeData

internal class GetLatestPurchasedContributionTest {

    private val repository = FakeContributionRepository()
    private val clock = TestClock(currentTime = CURRENT_TIME)

    private val testSubject = GetLatestPurchasedContribution(repository, clock)

    @Test
    fun `returns the latest one-time contribution if there are no recurring contributions`() = runTest {
        // Given
        val purchase1 = FakeData.purchasedOneTimeContribution.copy(
            purchaseDate = VALID_PURCHASE_DATE_MINUS_3_DAY,
        )
        val purchase2 = FakeData.purchasedOneTimeContribution.copy(
            purchaseDate = VALID_PURCHASE_DATE_MINUS_1_DAY,
        )
        val purchase3 = FakeData.purchasedOneTimeContribution.copy(
            purchaseDate = VALID_PURCHASE_DATE_MINUS_2_DAY,
        )
        val purchases = listOf(purchase1, purchase2, purchase3)
        repository.purchasedFlow = flowOf(Outcome.success(purchases))

        // When
        val result = testSubject().first()

        // Then
        assertThat(result).isInstanceOf(Outcome.Success::class)
        val data = (result as Outcome.Success).data
        assertThat(data).isNotNull()
        assertThat(data).isEqualTo(purchase2)
    }

    @Test
    fun `returns the latest recurring contribution if there are no one-time contributions`() = runTest {
        // Given
        val purchase1 = FakeData.purchasedRecurringContribution.copy(
            purchaseDate = VALID_PURCHASE_DATE_MINUS_3_DAY,
        )
        val purchase2 = FakeData.purchasedRecurringContribution.copy(
            purchaseDate = VALID_PURCHASE_DATE_MINUS_1_DAY,
        )
        val purchase3 = FakeData.purchasedRecurringContribution.copy(
            purchaseDate = VALID_PURCHASE_DATE_MINUS_2_DAY,
        )
        val purchases = listOf(purchase1, purchase2, purchase3)
        repository.purchasedFlow = flowOf(Outcome.success(purchases))

        // When
        val result = testSubject().first()

        // Then
        assertThat(result).isInstanceOf(Outcome.Success::class)
        val data = (result as Outcome.Success).data
        assertThat(data).isNotNull()
        assertThat(data).isEqualTo(purchase2)
    }

    @Test
    fun `returns the latest recurring contribution if there are both one-time and recurring contributions`() = runTest {
        // Given
        val oneTimePurchase = FakeData.purchasedOneTimeContribution.copy(
            purchaseDate = VALID_PURCHASE_DATE_MINUS_2_DAY,
        )
        val recurringPurchase = FakeData.purchasedRecurringContribution.copy(
            purchaseDate = VALID_PURCHASE_DATE_MINUS_2_DAY,
        )
        val purchases = listOf(oneTimePurchase, recurringPurchase)
        repository.purchasedFlow = flowOf(Outcome.success(purchases))

        // When
        val result = testSubject().first()

        // Then

        assertThat(result).isInstanceOf(Outcome.Success::class)
        val data = (result as Outcome.Success).data
        assertThat(data).isNotNull()
        assertThat(data).isEqualTo(recurringPurchase)
    }

    @Test
    fun `return null if there are only expired recurring contributions`() = runTest {
        // Given
        val expiredRecurringPurchase = FakeData.purchasedRecurringContribution.copy(
            purchaseDate = INVALID_PURCHASE_DATE,
        )
        repository.purchasedFlow = flowOf(Outcome.success(listOf(expiredRecurringPurchase)))

        // When
        val result = testSubject().first()

        // Then
        assertThat(result).isInstanceOf(Outcome.Success::class)
        val data = (result as Outcome.Success).data
        assertThat(data).isNull()
    }

    @Test
    fun `return the latest one-time contribution if there are only expired recurring contributions`() = runTest {
        // Given
        val expiredRecurringPurchase = FakeData.purchasedRecurringContribution.copy(
            purchaseDate = INVALID_PURCHASE_DATE,
        )
        val validOneTimePurchase = FakeData.purchasedOneTimeContribution.copy(
            purchaseDate = VALID_PURCHASE_DATE_MINUS_1_DAY,
        )
        repository.purchasedFlow = flowOf(Outcome.success(listOf(expiredRecurringPurchase, validOneTimePurchase)))

        // When
        val result = testSubject().first()

        // Then
        assertThat(result).isInstanceOf(Outcome.Success::class)
        val data = (result as Outcome.Success).data
        assertThat(data).isNotNull()
        assertThat(data).isEqualTo(validOneTimePurchase)
    }

    @Test
    fun `returns null if there are no purchased contributions`() = runTest {
        // Given
        repository.purchasedFlow = flowOf(Outcome.success(emptyList()))

        // When
        val result = testSubject().first()

        // Then
        assertThat(result).isInstanceOf(Outcome.Success::class)
        val data = (result as Outcome.Success).data
        assertThat(data).isNull()
    }

    @Test
    fun `returns failure if repository returns failure`() = runTest {
        // Given
        val error = ContributionError.UnknownError(message = "Failed to load purchases")
        repository.purchasedFlow = flowOf(Outcome.Failure(error))

        // When
        val result = testSubject().first()

        // Then
        assertThat(result).isInstanceOf(Outcome.Failure::class)
        val failure = result as Outcome.Failure
        assertThat(failure.error).isEqualTo(error)
    }

    private companion object {
        private val CURRENT_TIME = LocalDateTime(2024, 6, 1, 12, 0).toInstant(TimeZone.currentSystemDefault())
        private val VALID_PURCHASE_DATE_MINUS_3_DAY =
            CURRENT_TIME.minus(3.days).toLocalDateTime(TimeZone.currentSystemDefault())
        private val VALID_PURCHASE_DATE_MINUS_2_DAY =
            CURRENT_TIME.minus(2.days).toLocalDateTime(TimeZone.currentSystemDefault())
        private val VALID_PURCHASE_DATE_MINUS_1_DAY =
            CURRENT_TIME.minus(1.days).toLocalDateTime(TimeZone.currentSystemDefault())

        private val VALIDITY_PERIOD = 90.days

        private val INVALID_PURCHASE_DATE = CURRENT_TIME.minus(
            VALIDITY_PERIOD + 1.seconds,
        ).toLocalDateTime(TimeZone.currentSystemDefault())
    }
}
