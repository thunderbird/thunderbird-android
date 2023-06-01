package app.k9mail.autodiscovery.service

import app.k9mail.autodiscovery.api.AutoDiscoveryResult
import app.k9mail.autodiscovery.api.AutoDiscoveryResult.NetworkError
import app.k9mail.autodiscovery.api.AutoDiscoveryResult.NoUsableSettingsFound
import app.k9mail.autodiscovery.api.AutoDiscoveryResult.Settings
import app.k9mail.autodiscovery.api.AutoDiscoveryResult.UnexpectedException
import app.k9mail.autodiscovery.api.AutoDiscoveryRunnable
import kotlin.coroutines.cancellation.CancellationException
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineStart.LAZY
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope

/**
 * Runs a list of [AutoDiscoveryRunnable]s with descending priority in parallel and returns the result with the highest
 * priority.
 *
 * As soon as an [AutoDiscoveryRunnable] returns a [Settings] result, runnables with a lower priority are canceled.
 */
internal class PriorityParallelRunner(
    private val runnables: List<AutoDiscoveryRunnable>,
    private val coroutineDispatcher: CoroutineDispatcher = Dispatchers.IO,
) {
    suspend fun run(): AutoDiscoveryResult {
        return coroutineScope {
            val deferredList = buildList(capacity = runnables.size) {
                // Create coroutines in reverse order. So ones with lower priority are created first.
                for (runnable in runnables.reversed()) {
                    val lowerPriorityCoroutines = toList()

                    val deferred = async(coroutineDispatcher, start = LAZY) {
                        runnable.run().also { discoveryResult ->
                            if (discoveryResult is Settings) {
                                // We've got a positive result, so cancel all coroutines with lower priority.
                                lowerPriorityCoroutines.cancelAll()
                            }
                        }
                    }

                    add(deferred)
                }
            }.asReversed()

            for (deferred in deferredList) {
                deferred.start()
            }

            @Suppress("SwallowedException", "TooGenericExceptionCaught")
            val discoveryResults = deferredList.map { deferred ->
                try {
                    deferred.await()
                } catch (e: CancellationException) {
                    null
                } catch (e: Exception) {
                    UnexpectedException(e)
                }
            }

            val settingsResult = discoveryResults.firstOrNull { it is Settings }
            if (settingsResult != null) {
                settingsResult
            } else {
                val networkError = discoveryResults.firstOrNull { it is NetworkError }
                val networkErrorCount = discoveryResults.count { it is NetworkError }
                if (networkError != null && networkErrorCount == discoveryResults.size) {
                    networkError
                } else {
                    NoUsableSettingsFound
                }
            }
        }
    }

    private fun List<Deferred<AutoDiscoveryResult?>>.cancelAll() {
        for (deferred in this) {
            deferred.cancel()
        }
    }
}
