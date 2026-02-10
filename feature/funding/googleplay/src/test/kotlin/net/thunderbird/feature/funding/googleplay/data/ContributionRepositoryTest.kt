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
        val contributions = listOf(
            OneTimeContribution("ot1", "Title 1", "Desc 1", 100L, "$1.00"),
        )
        val expectedOutcome = Outcome.success(contributions)
        (remoteContributionDataSource as FakeContributionDataSource).purchasedFlow = flowOf(expectedOutcome)

        val result = testSubject.getAllPurchased().first()

        assertThat(result).isEqualTo(expectedOutcome)
    }

    @Test
    fun `getAllOneTime should delegate to remoteContributionDataSource`() = runTest {
        val productIds = listOf("one_time_1", "one_time_2")
        val contributions = listOf(
            OneTimeContribution("one_time_1", "Title 1", "Desc 1", 100L, "$1.00"),
            OneTimeContribution("one_time_2", "Title 2", "Desc 2", 200L, "$2.00"),
        )
        val expectedOutcome = Outcome.success(contributions)
        (remoteContributionDataSource as FakeContributionDataSource).oneTimeFlow = flowOf(expectedOutcome)

        val result = testSubject.getAllOneTime(productIds).first()

        assertThat(result).isEqualTo(expectedOutcome)
    }

    @Test
    fun `getAllRecurring should delegate to remoteContributionDataSource`() = runTest {
        val productIds = listOf("recurring_1", "recurring_2")
        val contributions = listOf(
            RecurringContribution("recurring_1", "Title 1", "Desc 1", 1000L, "$10.00"),
            RecurringContribution("recurring_2", "Title 2", "Desc 2", 2000L, "$20.00"),
        )
        val expectedOutcome = Outcome.success(contributions)
        (remoteContributionDataSource as FakeContributionDataSource).recurringFlow = flowOf(expectedOutcome)

        val result = testSubject.getAllRecurring(productIds).first()

        assertThat(result).isEqualTo(expectedOutcome)
    }
}
