package app.k9mail.feature.funding.googleplay.data.mapper

import app.k9mail.feature.funding.googleplay.domain.DomainContract.BillingError
import app.k9mail.feature.funding.googleplay.domain.Outcome
import assertk.all
import assertk.assertThat
import assertk.assertions.isEqualTo
import assertk.assertions.isInstanceOf
import assertk.assertions.prop
import com.android.billingclient.api.BillingClient.BillingResponseCode
import com.android.billingclient.api.BillingResult
import kotlin.reflect.KClass
import kotlin.test.Test

class BillingResultMapperTest {

    private val testSubject = BillingResultMapper()

    @Test
    fun `mapToBillingClientResult returns Success when billing result is OK`() {
        val billingResult = BillingResult.newBuilder()
            .setResponseCode(BillingResponseCode.OK)
            .build()

        val result = testSubject.mapToOutcome(billingResult) {}

        assertThat(result).isInstanceOf(Outcome.Success::class)
    }

    @Test
    fun `mapToBillingClientResult returns ServiceDisconnected when billing result is SERVICE_DISCONNECTED`() {
        val errorResults = listOf(
            createErrorBillingResult(BillingResponseCode.SERVICE_DISCONNECTED),
            createErrorBillingResult(BillingResponseCode.SERVICE_UNAVAILABLE),
            createErrorBillingResult(BillingResponseCode.BILLING_UNAVAILABLE),
            createErrorBillingResult(BillingResponseCode.NETWORK_ERROR),
        )

        val results = errorResults.map { billingResult ->
            testSubject.mapToOutcome(billingResult) {}
        }

        results.forEach { result ->
            assertOutcomeFailure(result, BillingError.ServiceDisconnected::class)
        }
    }

    @Test
    fun `mapToBillingClientResult returns PurchaseFailed when billing result is ITEM_ALREADY_OWNED`() {
        val errorResults = listOf(
            createErrorBillingResult(BillingResponseCode.ITEM_ALREADY_OWNED),
            createErrorBillingResult(BillingResponseCode.ITEM_NOT_OWNED),
            createErrorBillingResult(BillingResponseCode.ITEM_UNAVAILABLE),
        )

        val results = errorResults.map { billingResult ->
            testSubject.mapToOutcome(billingResult) {}
        }

        results.forEach { result ->
            assertOutcomeFailure(result, BillingError.PurchaseFailed::class)
        }
    }

    @Test
    fun `mapToBillingClientResult returns UserCancelled when billing result is USER_CANCELED`() {
        val billingResult = createErrorBillingResult(BillingResponseCode.USER_CANCELED)

        val result = testSubject.mapToOutcome(billingResult) {}

        assertOutcomeFailure(result, BillingError.UserCancelled::class)
    }

    @Test
    fun `mapToBillingClientResult returns DeveloperError when billing result is DEVELOPER_ERROR`() {
        val billingResult = createErrorBillingResult(BillingResponseCode.DEVELOPER_ERROR)

        val result = testSubject.mapToOutcome(billingResult) {}

        assertOutcomeFailure(result, BillingError.DeveloperError::class)
    }

    @Test
    fun `mapToBillingClientResult returns UnknownError when billing result is unknown`() {
        val errorResult = listOf(
            createErrorBillingResult(BillingResponseCode.ERROR),
            createErrorBillingResult(BillingResponseCode.FEATURE_NOT_SUPPORTED),
        )

        val results = errorResult.map { billingResult ->
            testSubject.mapToOutcome(billingResult) {}
        }

        results.forEach { result ->
            assertOutcomeFailure(result, BillingError.UnknownError::class)
        }
    }

    private fun <E : BillingError> assertOutcomeFailure(result: Outcome<Unit, BillingError>, kClass: KClass<E>) {
        assertThat(result).isInstanceOf(Outcome.Failure::class)
        val error = (result as Outcome.Failure).error
        assertThat(error).all {
            isInstanceOf(kClass)
            prop(BillingError::message).isEqualTo(DEBUG_MESSAGE)
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
