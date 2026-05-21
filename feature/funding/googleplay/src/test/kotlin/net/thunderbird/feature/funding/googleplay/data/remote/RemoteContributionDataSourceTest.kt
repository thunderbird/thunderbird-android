package net.thunderbird.feature.funding.googleplay.data.remote

import assertk.assertThat
import assertk.assertions.isEqualTo
import assertk.assertions.isInstanceOf
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import kotlinx.datetime.LocalDateTime
import net.thunderbird.core.logging.testing.TestLogger
import net.thunderbird.core.outcome.Outcome
import net.thunderbird.feature.funding.googleplay.domain.FundingDomainContract.ContributionError
import net.thunderbird.feature.funding.googleplay.domain.entity.ContributionId
import net.thunderbird.feature.funding.googleplay.domain.entity.OneTimeContribution
import net.thunderbird.feature.funding.googleplay.domain.entity.PurchasedContribution
import net.thunderbird.feature.funding.googleplay.domain.entity.RecurringContribution
import org.junit.Test

class RemoteContributionDataSourceTest {
    private val billingConnector = FakeBillingConnector()
    private val billingClient = FakeBillingClient()
    private val logger = TestLogger()

    private val testSubject = RemoteContributionDataSource(
        billingConnector,
        billingClient,
        logger,
    )

    @Test
    fun `getAllOneTime should return outcome from billingClient`() = runTest {
        // Arrange
        val contributionIds = listOf(ContributionId("one_time_1"))
        val contributions = listOf(
            OneTimeContribution(ContributionId("id"), "title", "desc", 100L, "$1.00"),
        )
        val expectedOutcome = Outcome.success(contributions)
        billingClient.oneTimeOutcome = expectedOutcome

        // Act
        val result = testSubject.getAllOneTime(contributionIds).first()

        // Assert
        assertThat(result).isEqualTo(expectedOutcome)
    }

    @Test
    fun `getAllRecurring should return outcome from billingClient`() = runTest {
        // Arrange
        val contributionIds = listOf(ContributionId("recurring_1"))
        val contributions = listOf(
            RecurringContribution(ContributionId("id"), "title", "desc", 1000L, "$10.00"),
        )
        val expectedOutcome = Outcome.success(contributions)
        billingClient.recurringOutcome = expectedOutcome

        // Act
        val result = testSubject.getAllRecurring(contributionIds).first()

        // Assert
        assertThat(result).isEqualTo(expectedOutcome)
    }

    @Test
    fun `getAllOneTime should return failure when billingClient fails`() = runTest {
        // Arrange
        val contributionIds = listOf(ContributionId("one_time_1"))
        val expectedOutcome = Outcome.failure(ContributionError.UnknownError("error"))
        billingClient.oneTimeOutcome = expectedOutcome

        // Act
        val result = testSubject.getAllOneTime(contributionIds).first()

        // Assert
        assertThat(result).isInstanceOf(Outcome.Failure::class)
        assertThat((result as Outcome.Failure).error).isEqualTo(ContributionError.UnknownError("error"))
    }

    @Test
    fun `getAllPurchased should combine one-time and recurring purchases`() = runTest {
        // Arrange
        val oneTime = PurchasedContribution(
            id = ContributionId("ot1"),
            contribution = OneTimeContribution(ContributionId("ot1"), "OneTime", "Desc", 100L, "$1.00"),
            purchaseDate = LocalDateTime(2024, 1, 1, 0, 0),
        )
        val recurring = PurchasedContribution(
            id = ContributionId("rec1"),
            contribution = RecurringContribution(ContributionId("rec1"), "Recurring", "Desc", 1000L, "$10.00"),
            purchaseDate = LocalDateTime(2024, 1, 1, 0, 0),
        )

        billingClient.purchasedRecurringOutcome = Outcome.success(listOf(recurring))
        billingClient.purchaseHistoryOutcome = Outcome.success(oneTime)

        // Act
        val result = testSubject.getAllPurchased().first()

        // Assert
        assertThat(result).isInstanceOf(Outcome.Success::class)
        val data = (result as Outcome.Success).data
        assertThat(data.size).isEqualTo(1)
        assertThat(data).isEqualTo(listOf(recurring))
    }

    @Test
    fun `getAllPurchased should return one-time when no recurring purchases`() = runTest {
        // Arrange
        val oneTime = PurchasedContribution(
            id = ContributionId("ot1"),
            contribution = OneTimeContribution(ContributionId("ot1"), "OneTime", "Desc", 100L, "$1.00"),
            purchaseDate = LocalDateTime(2024, 1, 1, 0, 0),
        )

        billingClient.purchasedRecurringOutcome = Outcome.success(emptyList())
        billingClient.purchaseHistoryOutcome = Outcome.success(oneTime)

        // Act
        val result = testSubject.getAllPurchased().first()

        // Assert
        assertThat(result).isInstanceOf(Outcome.Success::class)
        val data = (result as Outcome.Success).data
        assertThat(data.size).isEqualTo(1)
        assertThat(data).isEqualTo(listOf(oneTime))
    }

    @Test
    fun `getAllPurchased should return failure when connection fails`() = runTest {
        // Arrange
        billingConnector.connectOutcome = Outcome.failure(ContributionError.ServiceDisconnected("Disconnected"))

        // Act
        val result = testSubject.getAllPurchased().first()

        // Assert
        assertThat(result).isInstanceOf(Outcome.Failure::class)
        assertThat((result as Outcome.Failure).error).isEqualTo(ContributionError.ServiceDisconnected("Disconnected"))
    }

    @Test
    fun `getAllPurchased should return failure when recurring fetch fails`() = runTest {
        // Arrange
        val error = ContributionError.UnknownError("Failed")
        billingClient.purchasedRecurringOutcome = Outcome.failure(error)

        // Act
        val result = testSubject.getAllPurchased().first()

        // Assert
        assertThat(result).isInstanceOf(Outcome.Failure::class)
        assertThat((result as Outcome.Failure).error).isEqualTo(error)
    }

    @Test
    fun `purchaseContribution should delegate to billingClient`() = runTest {
        // Arrange
        val contributionId = ContributionId("ot1")
        val expectedOutcome = Outcome.success(Unit)
        billingClient.purchaseOutcome = expectedOutcome

        // Act
        val result = testSubject.purchaseContribution(contributionId)

        // Assert
        assertThat(result).isEqualTo(expectedOutcome)
    }

    @Test
    fun `purchaseContribution should return failure when connection fails`() = runTest {
        // Arrange
        val contributionId = ContributionId("ot1")
        billingConnector.connectOutcome = Outcome.failure(ContributionError.ServiceDisconnected("Disconnected"))

        // Act
        val result = testSubject.purchaseContribution(contributionId)

        // Assert
        assertThat(result).isInstanceOf(Outcome.Failure::class)
    }

    @Test
    fun `clear should call disconnect on billingClient`() {
        // Act
        testSubject.clear()

        // Assert
        assertThat(billingClient.clearCount).isEqualTo(1)
    }
}
