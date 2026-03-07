package net.thunderbird.feature.funding.googleplay.data.remote

import assertk.assertThat
import assertk.assertions.isEqualTo
import assertk.assertions.isInstanceOf
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import net.thunderbird.core.outcome.Outcome
import net.thunderbird.feature.funding.googleplay.domain.FundingDomainContract.ContributionError
import net.thunderbird.feature.funding.googleplay.domain.entity.OneTimeContribution
import net.thunderbird.feature.funding.googleplay.domain.entity.RecurringContribution
import org.junit.Test

class RemoteContributionDataSourceTest {

    private val billingClient = FakeBillingClient()
    private val testSubject = RemoteContributionDataSource(billingClient)

    @Test
    fun `getAllOneTime should return outcome from billingClient`() = runTest {
        // Arrange
        val productIds = listOf("one_time_1")
        val contributions = listOf(
            OneTimeContribution("id", "title", "desc", 100L, "$1.00"),
        )
        val expectedOutcome = Outcome.success(contributions)
        billingClient.connectOutcome = Outcome.success(Unit)
        billingClient.oneTimeOutcome = expectedOutcome

        // Act
        val result = testSubject.getAllOneTime(productIds).first()

        // Assert
        assertThat(result).isEqualTo(expectedOutcome)
    }

    @Test
    fun `getAllRecurring should return outcome from billingClient`() = runTest {
        // Arrange
        val productIds = listOf("recurring_1")
        val contributions = listOf(
            RecurringContribution("id", "title", "desc", 1000L, "$10.00"),
        )
        val expectedOutcome = Outcome.success(contributions)
        billingClient.connectOutcome = Outcome.success(Unit)
        billingClient.recurringOutcome = expectedOutcome

        // Act
        val result = testSubject.getAllRecurring(productIds).first()

        // Assert
        assertThat(result).isEqualTo(expectedOutcome)
    }

    @Test
    fun `getAllOneTime should return failure when billingClient fails`() = runTest {
        // Arrange
        val productIds = listOf("one_time_1")
        val expectedOutcome = Outcome.failure(ContributionError.UnknownError("error"))
        billingClient.connectOutcome = Outcome.success(Unit)
        billingClient.oneTimeOutcome = expectedOutcome

        // Act
        val result = testSubject.getAllOneTime(productIds).first()

        // Assert
        assertThat(result).isInstanceOf(Outcome.Failure::class)
        assertThat((result as Outcome.Failure).error).isEqualTo(ContributionError.UnknownError("error"))
    }

    @Test
    fun `getAllPurchased should combine one-time and recurring purchases`() = runTest {
        // Arrange
        val oneTime = OneTimeContribution("ot1", "OneTime", "Desc", 100L, "$1.00")
        val recurring = RecurringContribution("rec1", "Recurring", "Desc", 1000L, "$10.00")

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
        val oneTime = OneTimeContribution("ot1", "OneTime", "Desc", 100L, "$1.00")

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
        billingClient.connectOutcome = Outcome.failure(ContributionError.ServiceDisconnected("Disconnected"))

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
        val contribution = OneTimeContribution("ot1", "OneTime", "Desc", 100L, "$1.00")
        val expectedOutcome = Outcome.success(Unit)
        billingClient.connectOutcome = Outcome.success(Unit)
        billingClient.purchaseOutcome = expectedOutcome

        // Act
        val result = testSubject.purchaseContribution(contribution)

        // Assert
        assertThat(result).isEqualTo(expectedOutcome)
    }

    @Test
    fun `purchaseContribution should return failure when connection fails`() = runTest {
        // Arrange
        val contribution = OneTimeContribution("ot1", "OneTime", "Desc", 100L, "$1.00")
        billingClient.connectOutcome = Outcome.failure(ContributionError.ServiceDisconnected("Disconnected"))

        // Act
        val result = testSubject.purchaseContribution(contribution)

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
