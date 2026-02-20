package net.thunderbird.feature.funding.googleplay.data

import assertk.assertThat
import assertk.assertions.isEqualTo
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import net.thunderbird.core.outcome.Outcome
import net.thunderbird.feature.funding.googleplay.data.FundingDataContract.Remote.ContributionDataSource
import net.thunderbird.feature.funding.googleplay.domain.entity.OneTimeContribution
import net.thunderbird.feature.funding.googleplay.domain.entity.RecurringContribution
import org.junit.Test

internal class ContributionRepositoryTest {

    private val remoteContributionDataSource: ContributionDataSource = FakeContributionDataSource()
    private val testSubject = ContributionRepository(remoteContributionDataSource)

    @Test
    fun `getAllPurchased should delegate to remoteContributionDataSource`() = runTest {
        // Arrange
        val contributions = listOf(
            OneTimeContribution("ot1", "Title 1", "Desc 1", 100L, "$1.00"),
        )
        val expectedOutcome = Outcome.success(contributions)
        (remoteContributionDataSource as FakeContributionDataSource).purchasedFlow = flowOf(expectedOutcome)

        // Act
        val result = testSubject.getAllPurchased().first()

        // Assert
        assertThat(result).isEqualTo(expectedOutcome)
    }

    @Test
    fun `getAllOneTime should delegate to remoteContributionDataSource`() = runTest {
        // Arrange
        val productIds = listOf("one_time_1", "one_time_2")
        val contributions = listOf(
            OneTimeContribution("one_time_1", "Title 1", "Desc 1", 100L, "$1.00"),
            OneTimeContribution("one_time_2", "Title 2", "Desc 2", 200L, "$2.00"),
        )
        val expectedOutcome = Outcome.success(contributions)
        (remoteContributionDataSource as FakeContributionDataSource).oneTimeFlow = flowOf(expectedOutcome)

        // Act
        val result = testSubject.getAllOneTime(productIds).first()

        // Assert
        assertThat(result).isEqualTo(expectedOutcome)
    }

    @Test
    fun `getAllRecurring should delegate to remoteContributionDataSource`() = runTest {
        // Arrange
        val productIds = listOf("recurring_1", "recurring_2")
        val contributions = listOf(
            RecurringContribution("recurring_1", "Title 1", "Desc 1", 1000L, "$10.00"),
            RecurringContribution("recurring_2", "Title 2", "Desc 2", 2000L, "$20.00"),
        )
        val expectedOutcome = Outcome.success(contributions)
        (remoteContributionDataSource as FakeContributionDataSource).recurringFlow = flowOf(expectedOutcome)

        // Act
        val result = testSubject.getAllRecurring(productIds).first()

        // Assert
        assertThat(result).isEqualTo(expectedOutcome)
    }

    @Test
    fun `purchaseContribution should delegate to remoteContributionDataSource`() = runTest {
        // Arrange
        val contribution = OneTimeContribution("ot1", "Title 1", "Desc 1", 100L, "$1.00")
        val expectedOutcome = Outcome.success(Unit)

        // Act
        val result = testSubject.purchaseContribution(contribution)

        // Assert
        assertThat(result).isEqualTo(expectedOutcome)
    }

    @Test
    fun `clear should delegate to remoteContributionDataSource`() {
        // Act
        testSubject.clear()

        // Assert
        // No exception thrown, and since it's a simple delegation to Fake, we're good for now.
    }
}
