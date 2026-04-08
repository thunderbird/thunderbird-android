package net.thunderbird.feature.funding.googleplay.data.remote.bilingclient

import com.android.billingclient.api.BillingClient.BillingResponseCode
import com.android.billingclient.api.BillingClientStateListener
import com.android.billingclient.api.BillingResult
import kotlin.coroutines.resume
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import net.thunderbird.core.logging.Logger
import net.thunderbird.core.outcome.Outcome
import net.thunderbird.core.outcome.flatMapSuccess
import net.thunderbird.feature.funding.googleplay.data.FundingDataContract.Remote
import net.thunderbird.feature.funding.googleplay.domain.FundingDomainContract.ContributionError

private const val TAG = "BillingConnector"

internal class BillingConnector(
    private val clientProvider: Remote.BillingClientProvider,
    private val productCache: Remote.BillingProductCache,
    private val logger: Logger,
) : Remote.BillingConnector {

    private val connectionMutex = Mutex()

    /**
     * Connect the Google BillingClient.
     *
     * @param onConnected Callback to execute when connection is successful
     * @return Outcome of the connection attempt
     */
    override suspend fun <T> connect(
        onConnected: suspend () -> Outcome<T, ContributionError>,
    ): Outcome<T, ContributionError> {
        logger.debug(TAG) { "connect() called" }
        return if (clientProvider.current.isReady) {
            logger.debug(TAG) { "Billing client is already ready" }
            onConnected()
        } else {
            connectSafely {
                onConnected()
            }
        }
    }

    override fun disconnect() {
        logger.debug(TAG) { "disconnect() called" }
        productCache.clear()
        clientProvider.clear()
    }

    /**
     * Ensures safe connection to the Google BillingClient by preventing concurrent connection attempts.
     *
     * @param onConnected Callback to execute when connection is successful
     * @return Outcome of the connection attempt
     */
    private suspend fun <T> connectSafely(
        onConnected: suspend () -> Outcome<T, ContributionError>,
    ): Outcome<T, ContributionError> {
        logger.debug(TAG) { "connectSafely() called, waiting for lock" }
        val connectionOutcome = connectionMutex.withLock {
            if (clientProvider.current.isReady) {
                logger.debug(TAG) { "Billing client became ready while waiting for lock" }
                Outcome.success(Unit)
            } else {
                logger.debug(TAG) { "Starting billing client connection" }
                startConnection()
            }
        }

        return connectionOutcome.flatMapSuccess {
            onConnected()
        }
    }

    /**
     * Initiates a connection to the billing service and handles the connection outcome.
     *
     * This method uses the `clientProvider` to start a connection with the billing client and
     * listens for the state using a [BillingClientStateListener]. Once the connection is established
     * or fails, the corresponding [Outcome] is returned.
     *
     * @return An [Outcome] representing the result of the connection:
     * - [Outcome.Success] if the connection is successful.
     * - [Outcome.Failure] if the connection fails or the service is disconnected.
     */
    private suspend fun startConnection(): Outcome<Unit, ContributionError> =
        suspendCancellableCoroutine { continuation ->
            clientProvider.current.startConnection(
                object : BillingClientStateListener {
                    override fun onBillingSetupFinished(billingResult: BillingResult) {
                        if (billingResult.responseCode == BillingResponseCode.OK) {
                            logger.debug(TAG) { "Billing service connected successfully" }
                        } else {
                            logger.error(TAG) {
                                "Billing service connection failed with responseCode: ${billingResult.responseCode}, " +
                                    "debugMessage: ${billingResult.debugMessage}"
                            }
                        }
                        continuation.resume(billingResult.mapToOutcome { })
                    }

                    override fun onBillingServiceDisconnected() {
                        logger.debug(TAG) { "onBillingServiceDisconnected() called" }
                        disconnect()
                        continuation.resume(
                            Outcome.failure(
                                ContributionError.ServiceDisconnected(
                                    "Service disconnected: onBillingServiceDisconnected",
                                ),
                            ),
                        )
                    }
                },
            )
        }
}
