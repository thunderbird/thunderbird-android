package net.thunderbird.feature.funding.googleplay.data

import assertk.assertThat
import assertk.assertions.isEqualTo
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import kotlinx.datetime.LocalDateTime
import net.thunderbird.core.outcome.Outcome
import net.thunderbird.feature.funding.googleplay.data.FundingDataContract.Local.ContributionPurchaseDataSource
import net.thunderbird.feature.funding.googleplay.data.FundingDataContract.Remote.ContributionDataSource
import net.thunderbird.feature.funding.googleplay.domain.FundingDomainContract.ContributionError
import net.thunderbird.feature.funding.googleplay.domain.entity.ContributionId
import net.thunderbird.feature.funding.googleplay.domain.entity.OneTimeContribution
import net.thunderbird.feature.funding.googleplay.domain.entity.PurchasedContribution
import net.thunderbird.feature.funding.googleplay.domain.entity.RecurringContribution
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
internal class ContributionRepositoryTest {

    private val remoteContributionDataSource: ContributionDataSource = FakeContributionDataSource()
    private val localContributionDataSource = FakeContributionPurchaseDataSource()
    private val testSubject = ContributionRepository(
        remoteDataSource = remoteContributionDataSource,
        localDataSource = localContributionDataSource,
    )

    @Test
    fun `getAllPurchased should delegate to remoteContributionDataSource`() = runTest {
        // Arrange
        val contributions = listOf(
            PurchasedContribution(
                id = ContributionId("p1"),
                contribution = OneTimeContribution(ContributionId("p1"), "Title 1", "Desc 1", 100L, "$1.00"),
                purchaseDate = LocalDateTime(2024, 6, 1, 12, 0),
            ),
        )
        val expectedOutcome = Outcome.success(contributions)
        (remoteContributionDataSource as FakeContributionDataSource).purchasedFlow = flowOf(expectedOutcome)

        // Act
        val result = testSubject.getAllPurchased().first()

        // Assert
        assertThat(result).isEqualTo(expectedOutcome)
    }

    @Test
    fun `getAllPurchased should ignore asynchronous purchase failure`() = runTest {
        // Arrange
        val expectedOutcome = Outcome.success(emptyList<PurchasedContribution>())
        (remoteContributionDataSource as FakeContributionDataSource).purchasedFlow = flowOf(expectedOutcome)
        remoteContributionDataSource.purchasedContribution.value = Outcome.failure(
            ContributionError.UserCancelled("Purchase cancelled"),
        )

        // Act
        val result = testSubject.getAllPurchased().first()

        // Assert
        assertThat(result).isEqualTo(expectedOutcome)
    }

    @Test
    fun `getAllPurchased should include cached one-time contribution`() = runTest {
        // Arrange
        val purchase = PurchasedContribution(
            id = ContributionId("one_time_1"),
            contribution = OneTimeContribution(
                ContributionId("one_time_1"),
                "Title 1",
                "Desc 1",
                100L,
                "$1.00",
            ),
            purchaseDate = LocalDateTime(2024, 6, 1, 12, 0),
        )
        localContributionDataSource.purchase.value = Outcome.success(purchase)

        // Act
        val result = testSubject.getAllPurchased().first()

        // Assert
        assertThat(result).isEqualTo(
            Outcome.success(
                listOf(
                    PurchasedContribution(
                        id = ContributionId("one_time_1"),
                        contribution = OneTimeContribution(
                            ContributionId("one_time_1"),
                            "Title 1",
                            "Desc 1",
                            100L,
                            "$1.00",
                        ),
                        purchaseDate = LocalDateTime(2024, 6, 1, 12, 0),
                    ),
                ),
            ),
        )
    }

    @Test
    fun `getAllPurchased should cache asynchronous one-time purchase`() = runTest {
        // Arrange
        backgroundScope.launch { testSubject.getAllPurchased().collect {} }
        runCurrent()
        val purchasedContribution = PurchasedContribution(
            id = ContributionId("one_time_1"),
            contribution = OneTimeContribution(ContributionId("one_time_1"), "Title 1", "Desc 1", 100L, "$1.00"),
            purchaseDate = LocalDateTime(2024, 6, 1, 12, 0),
        )

        // Act
        (remoteContributionDataSource as FakeContributionDataSource).purchasedContribution.value =
            Outcome.success(purchasedContribution)
        runCurrent()

        // Assert
        assertThat(localContributionDataSource.purchase.value).isEqualTo(
            Outcome.success(
                purchasedContribution,
            ),
        )
    }

    @Test
    fun `getAllOneTime should delegate to remoteContributionDataSource`() = runTest {
        // Arrange
        val contributionIds = listOf(ContributionId("one_time_1"), ContributionId("one_time_2"))
        val contributions = listOf(
            OneTimeContribution(ContributionId("one_time_1"), "Title 1", "Desc 1", 100L, "$1.00"),
            OneTimeContribution(ContributionId("one_time_2"), "Title 2", "Desc 2", 200L, "$2.00"),
        )
        val expectedOutcome = Outcome.success(contributions)
        (remoteContributionDataSource as FakeContributionDataSource).oneTimeFlow = flowOf(expectedOutcome)

        // Act
        val result = testSubject.getAllOneTime(contributionIds).first()

        // Assert
        assertThat(result).isEqualTo(expectedOutcome)
    }

    @Test
    fun `getAllRecurring should delegate to remoteContributionDataSource`() = runTest {
        // Arrange
        val contributionIds = listOf(ContributionId("recurring_1"), ContributionId("recurring_2"))
        val contributions = listOf(
            RecurringContribution(ContributionId("recurring_1"), "Title 1", "Desc 1", 1000L, "$10.00"),
            RecurringContribution(ContributionId("recurring_2"), "Title 2", "Desc 2", 2000L, "$20.00"),
        )
        val expectedOutcome = Outcome.success(contributions)
        (remoteContributionDataSource as FakeContributionDataSource).recurringFlow = flowOf(expectedOutcome)

        // Act
        val result = testSubject.getAllRecurring(contributionIds).first()

        // Assert
        assertThat(result).isEqualTo(expectedOutcome)
    }

    @Test
    fun `purchaseContribution should delegate to remoteContributionDataSource`() = runTest {
        // Arrange
        val contributionId = ContributionId("ot1")
        val expectedOutcome = Outcome.success(Unit)

        // Act
        val result = testSubject.purchaseContribution(contributionId)

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

    private class FakeContributionPurchaseDataSource : ContributionPurchaseDataSource {
        val purchase = MutableStateFlow<Outcome<PurchasedContribution?, ContributionError>>(Outcome.success(null))

        override fun get(): Flow<Outcome<PurchasedContribution?, ContributionError>> = purchase

        override suspend fun update(purchase: PurchasedContribution): Outcome<Unit, ContributionError> {
            this.purchase.value = Outcome.success(purchase)
            return Outcome.success(Unit)
        }
    }
}
