package net.thunderbird.feature.funding.googleplay.domain.usecase

import assertk.assertThat
import assertk.assertions.isEqualTo
import assertk.assertions.isInstanceOf
import kotlinx.collections.immutable.persistentListOf
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import net.thunderbird.core.outcome.Outcome
import net.thunderbird.feature.funding.googleplay.domain.FundingDomainContract
import net.thunderbird.feature.funding.googleplay.domain.FundingDomainContract.ContributionError
import net.thunderbird.feature.funding.googleplay.domain.entity.OneTimeContribution
import net.thunderbird.feature.funding.googleplay.domain.entity.RecurringContribution
import net.thunderbird.feature.funding.googleplay.ui.contribution.FakeContributionRepository
import org.junit.Test

class GetAvailableContributionsTest {

    private val repository = FakeContributionRepository()
    private val contributionIdProvider = object : FundingDomainContract.ContributionIdProvider {
        override val oneTimeContributionIds = persistentListOf("ot1")
        override val recurringContributionIds = persistentListOf("rec1")
    }
    private val testSubject = GetAvailableContributions(repository, contributionIdProvider)

    @Test
    fun `invoke should return success when repository returns success`() = runTest {
        // Arrange
        val oneTime = OneTimeContribution("ot1", "Title", "Desc", 100L, "$1.00")
        val recurring = RecurringContribution("rec1", "Title", "Desc", 1000L, "$10.00")
        val purchased = OneTimeContribution("ot_purchased", "Title", "Desc", 500L, "$5.00")

        repository.oneTimeFlow = flowOf(Outcome.success(listOf(oneTime)))
        repository.recurringFlow = flowOf(Outcome.success(listOf(recurring)))
        repository.purchasedFlow = flowOf(Outcome.success(listOf(purchased)))

        // Act
        val result = testSubject().first()

        // Assert
        assertThat(result).isInstanceOf(Outcome.Success::class)
        val data = (result as Outcome.Success).data
        assertThat(data.oneTimeContributions).isEqualTo(listOf(oneTime))
        assertThat(data.recurringContributions).isEqualTo(listOf(recurring))
        assertThat(data.purchasedContribution).isEqualTo(purchased)
    }

    @Test
    fun `invoke should return failure when repository returns failure for purchased`() = runTest {
        // Arrange
        val error = ContributionError.UnknownError("Error")
        repository.purchasedFlow = flowOf(Outcome.failure(error))

        // Act
        val result = testSubject().first()

        // Assert
        assertThat(result).isEqualTo(Outcome.failure(error))
    }

    @Test
    fun `invoke should return success if at least one type of contribution is loaded`() = runTest {
        // Arrange
        val oneTime = OneTimeContribution("ot1", "Title", "Desc", 100L, "$1.00")

        repository.oneTimeFlow = flowOf(Outcome.success(listOf(oneTime)))
        repository.recurringFlow = flowOf(Outcome.failure(ContributionError.UnknownError("Error")))
        repository.purchasedFlow = flowOf(Outcome.success(emptyList()))

        // Act
        val result = testSubject().first()

        // Assert
        assertThat(result).isInstanceOf(Outcome.Success::class)
        val data = (result as Outcome.Success).data
        assertThat(data.oneTimeContributions).isEqualTo(listOf(oneTime))
        assertThat(data.recurringContributions).isEqualTo(emptyList())
    }
}
