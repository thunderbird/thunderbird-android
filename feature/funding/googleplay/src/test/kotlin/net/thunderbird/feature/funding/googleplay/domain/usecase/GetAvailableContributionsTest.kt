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
import net.thunderbird.feature.funding.googleplay.domain.entity.ContributionId
import net.thunderbird.feature.funding.googleplay.domain.entity.ContributionPreselection
import net.thunderbird.feature.funding.googleplay.domain.entity.OneTimeContribution
import net.thunderbird.feature.funding.googleplay.domain.entity.RecurringContribution
import net.thunderbird.feature.funding.googleplay.ui.contribution.FakeContributionRepository
import org.junit.Test

class GetAvailableContributionsTest {

    private val repository = FakeContributionRepository()
    private val contributionIdProvider = object : FundingDomainContract.ContributionIdProvider {
        override val oneTimeContributionIds = persistentListOf(ContributionId("ot1"))
        override val recurringContributionIds = persistentListOf(ContributionId("rec1"))
    }

    private val preselector = FundingDomainContract.Policy.ContributionPreselector { _, _ ->
        ContributionPreselection(
            ContributionId("ot1"),
            ContributionId("rec1"),
        )
    }
    private val testSubject = GetAvailableContributions(repository, contributionIdProvider, preselector)

    @Test
    fun `invoke should return success when repository returns success`() = runTest {
        // Arrange
        val oneTime = OneTimeContribution(ContributionId("ot1"), "Title", "Desc", 100L, "$1.00")
        val recurring = RecurringContribution(ContributionId("rec1"), "Title", "Desc", 1000L, "$10.00")

        repository.oneTimeFlow = flowOf(Outcome.success(listOf(oneTime)))
        repository.recurringFlow = flowOf(Outcome.success(listOf(recurring)))

        // Act
        val result = testSubject().first()

        // Assert
        assertThat(result).isInstanceOf(Outcome.Success::class)
        val data = (result as Outcome.Success).data
        assertThat(data.oneTimeContributions).isEqualTo(listOf(oneTime))
        assertThat(data.recurringContributions).isEqualTo(listOf(recurring))
    }

    @Test
    fun `invoke should return success if one type (oneTime) of contributions is loaded`() = runTest {
        // Arrange
        val oneTime = OneTimeContribution(ContributionId("ot1"), "Title", "Desc", 100L, "$1.00")

        repository.oneTimeFlow = flowOf(Outcome.success(listOf(oneTime)))
        repository.recurringFlow = flowOf(Outcome.failure(ContributionError.UnknownError("Error")))

        // Act
        val result = testSubject().first()

        // Assert
        assertThat(result).isInstanceOf(Outcome.Success::class)
        val data = (result as Outcome.Success).data
        assertThat(data.oneTimeContributions).isEqualTo(listOf(oneTime))
        assertThat(data.recurringContributions).isEqualTo(emptyList())
    }

    @Test
    fun `invoke should return success if one type (recurring) of contributions is loaded`() = runTest {
        // Arrange
        val recurring = RecurringContribution(ContributionId("rec1"), "Title", "Desc", 1000L, "$10.00")

        repository.oneTimeFlow = flowOf(Outcome.failure(ContributionError.UnknownError("Error")))
        repository.recurringFlow = flowOf(Outcome.success(listOf(recurring)))

        // Act
        val result = testSubject().first()

        // Assert
        assertThat(result).isInstanceOf(Outcome.Success::class)
        val data = (result as Outcome.Success).data
        assertThat(data.recurringContributions).isEqualTo(listOf(recurring))
        assertThat(data.oneTimeContributions).isEqualTo(emptyList())
    }

    @Test
    fun `invoke should return failure if both contribution types fail to load`() = runTest {
        // Arrange
        val error = ContributionError.UnknownError("Failed to load contributions")
        repository.oneTimeFlow = flowOf(Outcome.failure(error))
        repository.recurringFlow = flowOf(Outcome.failure(error))

        // Act
        val result = testSubject().first()

        // Assert
        assertThat(result).isEqualTo(Outcome.failure(error))
    }
}
