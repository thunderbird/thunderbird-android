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
        val productIds = listOf("one_time_1")
        val contributions = listOf(
            OneTimeContribution("id", "title", "desc", 100L, "$1.00"),
        )
        val expectedOutcome = Outcome.success(contributions)
        billingClient.connectOutcome = Outcome.success(Unit)
        billingClient.oneTimeOutcome = expectedOutcome

        val result = testSubject.getAllOneTime(productIds).first()

        assertThat(result).isEqualTo(expectedOutcome)
    }

    @Test
    fun `getAllRecurring should return outcome from billingClient`() = runTest {
        val productIds = listOf("recurring_1")
        val contributions = listOf(
            RecurringContribution("id", "title", "desc", 1000L, "$10.00"),
        )
        val expectedOutcome = Outcome.success(contributions)
        billingClient.connectOutcome = Outcome.success(Unit)
        billingClient.recurringOutcome = expectedOutcome

        val result = testSubject.getAllRecurring(productIds).first()

        assertThat(result).isEqualTo(expectedOutcome)
    }

    @Test
    fun `getAllOneTime should return failure when billingClient fails`() = runTest {
        val productIds = listOf("one_time_1")
        val expectedOutcome = Outcome.failure(ContributionError.UnknownError("error"))
        billingClient.connectOutcome = Outcome.success(Unit)
        billingClient.oneTimeOutcome = expectedOutcome

        val result = testSubject.getAllOneTime(productIds).first()

        assertThat(result).isInstanceOf(Outcome.Failure::class)
        assertThat((result as Outcome.Failure).error).isEqualTo(ContributionError.UnknownError("error"))
    }

    @Test
    fun `getAllPurchased should combine one-time and recurring purchases`() = runTest {
        val oneTime = OneTimeContribution("ot1", "OneTime", "Desc", 100L, "$1.00")
        val recurring = RecurringContribution("rec1", "Recurring", "Desc", 1000L, "$10.00")

        billingClient.purchasedRecurringOutcome = Outcome.success(listOf(recurring))
        billingClient.purchaseHistoryOutcome = Outcome.success(oneTime)

        val result = testSubject.getAllPurchased().first()

        assertThat(result).isInstanceOf(Outcome.Success::class)
        val data = (result as Outcome.Success).data
        assertThat(data.size).isEqualTo(1)
        assertThat(data).isEqualTo(listOf(recurring))
    }

    @Test
    fun `getAllPurchased should return one-time when no recurring purchases`() = runTest {
        val oneTime = OneTimeContribution("ot1", "OneTime", "Desc", 100L, "$1.00")

        billingClient.purchasedRecurringOutcome = Outcome.success(emptyList())
        billingClient.purchaseHistoryOutcome = Outcome.success(oneTime)

        val result = testSubject.getAllPurchased().first()

        assertThat(result).isInstanceOf(Outcome.Success::class)
        val data = (result as Outcome.Success).data
        assertThat(data.size).isEqualTo(1)
        assertThat(data).isEqualTo(listOf(oneTime))
    }

    @Test
    fun `getAllPurchased should return failure when connection fails`() = runTest {
        billingClient.connectOutcome = Outcome.failure(ContributionError.ServiceDisconnected("Disconnected"))

        val result = testSubject.getAllPurchased().first()

        assertThat(result).isInstanceOf(Outcome.Failure::class)
        assertThat((result as Outcome.Failure).error).isEqualTo(ContributionError.ServiceDisconnected("Disconnected"))
    }

    @Test
    fun `getAllPurchased should return failure when recurring fetch fails`() = runTest {
        val error = ContributionError.UnknownError("Failed")
        billingClient.purchasedRecurringOutcome = Outcome.failure(error)

        val result = testSubject.getAllPurchased().first()

        assertThat(result).isInstanceOf(Outcome.Failure::class)
        assertThat((result as Outcome.Failure).error).isEqualTo(error)
    }
}
