package net.thunderbird.feature.funding.googleplay.data.remote.bilingclient

import com.android.billingclient.api.BillingClientStateListener
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import net.thunderbird.core.outcome.Outcome
import net.thunderbird.feature.funding.googleplay.data.FundingDataContract
import net.thunderbird.feature.funding.googleplay.domain.FundingDomainContract.ContributionError
import com.android.billingclient.api.BillingClient as GoogleBillingClient
import com.android.billingclient.api.BillingResult as GoogleBillingResult

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

    val connectMutex = ConnectMutexHolder.mutex

    return connectMutex.withLock {
        if (client.isReady) return@withLock onConnected()

        val deferred = CompletableDeferred<Outcome<T, ContributionError>>()

        client.startConnection(object : BillingClientStateListener {
            override fun onBillingSetupFinished(billingResult: GoogleBillingResult) {
                scope.launch {
                    val mapped = billingResultMapper.mapToOutcome(billingResult) { }
                    val result: Outcome<T, ContributionError> = when (mapped) {
                        is Outcome.Success -> onConnected()
                        is Outcome.Failure -> mapped
                    }
                    deferred.complete(result)
                }
            }

            override fun onBillingServiceDisconnected() {
                // No-op
            }
        })

        deferred.await()
    }
}

// Single shared mutex holder so multiple callers across the app coordinate.
private object ConnectMutexHolder {
    val mutex = Mutex()
}
