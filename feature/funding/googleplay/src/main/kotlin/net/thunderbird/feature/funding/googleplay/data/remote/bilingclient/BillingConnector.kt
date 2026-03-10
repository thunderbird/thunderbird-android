package net.thunderbird.feature.funding.googleplay.data.remote.bilingclient

import com.android.billingclient.api.BillingClientStateListener
import kotlin.coroutines.CoroutineContext
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import net.thunderbird.core.logging.Logger
import net.thunderbird.core.outcome.Outcome
import net.thunderbird.feature.funding.googleplay.data.FundingDataContract
import net.thunderbird.feature.funding.googleplay.data.FundingDataContract.Remote
import net.thunderbird.feature.funding.googleplay.domain.FundingDomainContract.ContributionError
import com.android.billingclient.api.BillingClient as GoogleBillingClient
import com.android.billingclient.api.BillingResult as GoogleBillingResult

internal class BillingConnector(
    private val clientProvider: Remote.BillingClientProvider,
    private val resultMapper: FundingDataContract.Mapper.BillingResult,
    private val productCache: Remote.BillingProductCache,
    private val logger: Logger,
    backgroundDispatcher: CoroutineContext = Dispatchers.IO,
) : Remote.BillingConnector {

    private val coroutineScope = CoroutineScope(backgroundDispatcher)
    private val connectionMutex = Mutex()

    override suspend fun <T> connect(
        onConnected: suspend () -> Outcome<T, ContributionError>,
    ): Outcome<T, ContributionError> {
        return safeConnect(clientProvider.current, resultMapper, scope = coroutineScope) {
            onConnected()
        }
    }

    override fun disconnect() {
        productCache.clear()
        clientProvider.clear()
    }

    /**
     * Helper to safely connect the Google BillingClient without triggering "already in process of connecting" errors.
     *
     * Usage:
     *   safeConnect(client, billingResultMapper) { /* onConnected work returning Outcome<T, ContributionError> */ }
     */
    internal suspend fun <T> safeConnect(
        client: GoogleBillingClient,
        billingResultMapper: FundingDataContract.Mapper.BillingResult,
        scope: CoroutineScope = CoroutineScope(Dispatchers.Main.immediate),
        onConnected: suspend () -> Outcome<T, ContributionError>,
    ): Outcome<T, ContributionError> {
        if (client.isReady) return onConnected()

        val connectionResult = connectionMutex.withLock {
            if (client.isReady) return@withLock Outcome.success(Unit)

            val deferred = CompletableDeferred<Outcome<Unit, ContributionError>>()

            client.startConnection(object : BillingClientStateListener {
                override fun onBillingSetupFinished(billingResult: GoogleBillingResult) {
                    logger.verbose { "Billing service connected" }
                    scope.launch {
                        val mapped = billingResultMapper.mapToOutcome(billingResult) { }
                        deferred.complete(mapped)
                    }
                }

                override fun onBillingServiceDisconnected() {
                    logger.verbose { "Billing service disconnected" }
                    // We clear the cache and provider on disconnect because ProductDetails and their
                    // associated offer tokens are tied to a specific BillingClient session.
                    // Using a ProductDetails instance from a previous, disconnected session
                    // to launch a purchase flow in a new session can lead to DEVELOPER_ERROR or
                    // other failures in the Google Play Billing Library.
                    disconnect()
                    if (!deferred.isCompleted) {
                        deferred.complete(
                            Outcome.failure(ContributionError.ServiceDisconnected("Service disconnected")),
                        )
                    }
                }
            })

            deferred.await()
        }

        return when (connectionResult) {
            is Outcome.Success -> onConnected()
            is Outcome.Failure -> connectionResult
        }
    }
}
