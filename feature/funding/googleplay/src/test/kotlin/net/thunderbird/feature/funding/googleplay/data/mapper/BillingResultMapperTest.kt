package net.thunderbird.feature.funding.googleplay.data.mapper

import assertk.all
import assertk.assertThat
import assertk.assertions.isEqualTo
import assertk.assertions.isInstanceOf
import assertk.assertions.prop
import com.android.billingclient.api.BillingClient.BillingResponseCode
import com.android.billingclient.api.BillingResult
import kotlin.reflect.KClass
import kotlin.test.Test
import kotlinx.coroutines.test.runTest
import net.thunderbird.core.outcome.Outcome
import net.thunderbird.feature.funding.googleplay.domain.FundingDomainContract.ContributionError

internal class BillingResultMapperTest {

    private val testSubject = BillingResultMapper()

    @Test
    fun `mapToBillingClientResult returns Success when billing result is OK`() = runTest {
        // Arrange
        val billingResult = BillingResult.newBuilder()
            .setResponseCode(BillingResponseCode.OK)
            .build()

        // Act
        val result = testSubject.mapToOutcome(billingResult) {}

        // Assert
        assertThat(result).isInstanceOf(Outcome.Success::class)
    }

    @Test
    fun `mapToBillingClientResult returns ServiceDisconnected when billing result is SERVICE_DISCONNECTED`() =
        runTest {
            // Arrange
            val errorResults = listOf(
                createErrorBillingResult(BillingResponseCode.SERVICE_DISCONNECTED),
                createErrorBillingResult(BillingResponseCode.SERVICE_UNAVAILABLE),
                createErrorBillingResult(BillingResponseCode.BILLING_UNAVAILABLE),
                createErrorBillingResult(BillingResponseCode.NETWORK_ERROR),
            )

            // Act
            val results = errorResults.map { billingResult ->
                testSubject.mapToOutcome(billingResult) {}
            }

            // Assert
            results.forEach { result ->
                assertOutcomeFailure(result, ContributionError.ServiceDisconnected::class)
            }
        }

    @Test
    fun `mapToBillingClientResult returns PurchaseFailed when billing result is ITEM_ALREADY_OWNED`() = runTest {
        // Arrange
        val errorResults = listOf(
            createErrorBillingResult(BillingResponseCode.ITEM_ALREADY_OWNED),
            createErrorBillingResult(BillingResponseCode.ITEM_NOT_OWNED),
            createErrorBillingResult(BillingResponseCode.ITEM_UNAVAILABLE),
        )

        // Act
        val results = errorResults.map { billingResult ->
            testSubject.mapToOutcome(billingResult) {}
        }

        // Assert
        results.forEach { result ->
            assertOutcomeFailure(result, ContributionError.PurchaseFailed::class)
        }
    }

    @Test
    fun `mapToBillingClientResult returns UserCancelled when billing result is USER_CANCELED`() = runTest {
        // Arrange
        val billingResult = createErrorBillingResult(BillingResponseCode.USER_CANCELED)

        // Act
        val result = testSubject.mapToOutcome(billingResult) {}

        // Assert
        assertOutcomeFailure(result, ContributionError.UserCancelled::class)
    }

    @Test
    fun `mapToBillingClientResult returns DeveloperError when billing result is DEVELOPER_ERROR`() = runTest {
        // Arrange
        val billingResult = createErrorBillingResult(BillingResponseCode.DEVELOPER_ERROR)

        // Act
        val result = testSubject.mapToOutcome(billingResult) {}

        // Assert
        assertOutcomeFailure(result, ContributionError.DeveloperError::class)
    }

    @Test
    fun `mapToBillingClientResult returns UnknownError when billing result is unknown`() = runTest {
        // Arrange
        val errorResult = listOf(
            createErrorBillingResult(BillingResponseCode.ERROR),
            createErrorBillingResult(BillingResponseCode.FEATURE_NOT_SUPPORTED),
        )

        // Act
        val results = errorResult.map { billingResult ->
            testSubject.mapToOutcome(billingResult) {}
        }

        // Assert
        results.forEach { result ->
            assertOutcomeFailure(result, ContributionError.UnknownError::class)
        }
    }

    private fun <E : ContributionError> assertOutcomeFailure(
        result: Outcome<Unit, ContributionError>,
        kClass: KClass<E>,
    ) {
        assertThat(result).isInstanceOf(Outcome.Failure::class)
        val error = (result as Outcome.Failure).error
        assertThat(error).all {
            isInstanceOf(kClass)
            prop(ContributionError::message).isEqualTo(DEBUG_MESSAGE)
        }
    }

    private fun createErrorBillingResult(responseCode: Int, debugMessage: String = DEBUG_MESSAGE): BillingResult {
        return BillingResult.newBuilder()
            .setResponseCode(responseCode)
            .setDebugMessage(debugMessage)
            .build()
    }

    private companion object {
        private const val DEBUG_MESSAGE = "Debug message"
    }
}
