package net.thunderbird.feature.funding.googleplay.data.remote.bilingclient

import assertk.assertThat
import assertk.assertions.isEqualTo
import com.android.billingclient.api.BillingClientStateListener
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import net.thunderbird.core.logging.testing.TestLogger
import net.thunderbird.core.outcome.Outcome
import net.thunderbird.feature.funding.googleplay.domain.FundingDomainContract.ContributionError
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.ArgumentCaptor
import org.mockito.kotlin.mock
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import org.robolectric.RobolectricTestRunner
import com.android.billingclient.api.BillingClient as GoogleBillingClient

@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(RobolectricTestRunner::class)
class BillingConnectorTest {

    private val clientProvider = FakeBillingClientProvider()
    private val resultMapper = FakeBillingResultMapper()
    private val productCache = BillingProductCache()
    private val testDispatcher = StandardTestDispatcher()
    private val googleBillingClient = mock<GoogleBillingClient>()
    private val logger = TestLogger()

    private lateinit var testSubject: BillingConnector

    @Before
    fun setUp() {
        clientProvider.billingClient = googleBillingClient

        testSubject = BillingConnector(
            clientProvider = clientProvider,
            resultMapper = resultMapper,
            productCache = productCache,
            backgroundDispatcher = testDispatcher,
            logger = logger,
        )
    }

    @Test
    fun `connect should return success when billing setup finished with success`() = runTest(testDispatcher) {
        // Arrange
        whenever(googleBillingClient.isReady).thenReturn(false)
        resultMapper.outcome = Outcome.success(Unit)

        val captor = ArgumentCaptor.forClass(BillingClientStateListener::class.java)

        // Act
        var result: Outcome<Unit, ContributionError>? = null
        val job = launch {
            result = testSubject.connect { Outcome.success(Unit) }
        }
        advanceUntilIdle()

        verify(googleBillingClient).startConnection(captor.capture())
        captor.value.onBillingSetupFinished(mock())
        advanceUntilIdle()

        // Assert
        assertThat(result is Outcome.Success).isEqualTo(true)
        job.cancel()
    }

    @Test
    fun `connect should be parallelized once ready`() = runTest(testDispatcher) {
        // Arrange
        whenever(googleBillingClient.isReady).thenReturn(false)
        resultMapper.outcome = Outcome.success(Unit)
        val captor = ArgumentCaptor.forClass(BillingClientStateListener::class.java)

        var call1Started = false
        var call2Started = false
        val call1Finished = CompletableDeferred<Unit>()
        val call2Finished = CompletableDeferred<Unit>()

        // Act
        val job1 = launch {
            testSubject.safeConnect(
                client = googleBillingClient,
                billingResultMapper = resultMapper,
                scope = CoroutineScope(testDispatcher),
                onConnected = {
                    call1Started = true
                    call1Finished.await()
                    Outcome.success(Unit)
                },
            )
        }
        val job2 = launch {
            testSubject.safeConnect(
                client = googleBillingClient,
                billingResultMapper = resultMapper,
                scope = CoroutineScope(testDispatcher),
                onConnected = {
                    call2Started = true
                    call2Finished.await()
                    Outcome.success(Unit)
                },
            )
        }

        // Wait for both calls to reach the mutex
        advanceUntilIdle()

        verify(googleBillingClient).startConnection(captor.capture())

        // Signal that billing setup is finished.
        // This should complete the deferred in safeConnect and release the mutex.
        whenever(googleBillingClient.isReady).thenReturn(true)
        captor.value.onBillingSetupFinished(mock())

        // We need to run the scope.launch from onBillingSetupFinished
        // AND we need to run the follow-up code in safeConnect
        advanceUntilIdle()

        // Assert
        // Both should have started because the mutex is released after connection is established
        assertThat(call1Started).isEqualTo(true)
        assertThat(call2Started).isEqualTo(true)

        call1Finished.complete(Unit)
        call2Finished.complete(Unit)
        advanceUntilIdle()
        job1.cancel()
        job2.cancel()
    }

    @Test
    fun `onBillingServiceDisconnected should be handled and fail connection`() = runTest(testDispatcher) {
        // Arrange
        whenever(googleBillingClient.isReady).thenReturn(false)
        val captor = ArgumentCaptor.forClass(BillingClientStateListener::class.java)

        var result: Outcome<Unit, ContributionError>? = null
        launch {
            result = testSubject.connect { Outcome.success(Unit) }
        }
        advanceUntilIdle()

        verify(googleBillingClient).startConnection(captor.capture())

        // Act
        captor.value.onBillingServiceDisconnected()
        advanceUntilIdle()

        // Assert
        assertThat(result is Outcome.Failure).isEqualTo(true)
        val error = (result as Outcome.Failure).error
        assertThat(error is ContributionError.ServiceDisconnected).isEqualTo(true)
    }
}
