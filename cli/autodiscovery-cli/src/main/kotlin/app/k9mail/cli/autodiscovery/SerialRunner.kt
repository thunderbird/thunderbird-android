package app.k9mail.cli.autodiscovery

import app.k9mail.autodiscovery.api.AutoDiscoveryResult
import app.k9mail.autodiscovery.api.AutoDiscoveryResult.NetworkError
import app.k9mail.autodiscovery.api.AutoDiscoveryResult.NoUsableSettingsFound
import app.k9mail.autodiscovery.api.AutoDiscoveryResult.Settings
import app.k9mail.autodiscovery.api.AutoDiscoveryResult.UnexpectedException
import app.k9mail.autodiscovery.api.AutoDiscoveryRunnable
import net.thunderbird.core.logging.legacy.Log

/**
 * Run a list of [AutoDiscoveryRunnable] one after the other until one returns a [Settings] result.
 */
class SerialRunner(private val runnables: List<AutoDiscoveryRunnable>) {
    suspend fun run(): AutoDiscoveryResult {
        var networkErrorCount = 0
        var networkError: NetworkError? = null

        for (runnable in runnables) {
            when (val discoveryResult = runnable.run()) {
                is Settings -> {
                    return discoveryResult
                }
                is NetworkError -> {
                    networkErrorCount++
                    if (networkError == null) {
                        networkError = discoveryResult
                    }
                }
                NoUsableSettingsFound -> { }
                is UnexpectedException -> {
                    Log.w(discoveryResult.exception, "Unexpected exception")
                }
            }
        }

        return if (networkError != null && networkErrorCount == runnables.size) {
            networkError
        } else {
            NoUsableSettingsFound
        }
    }
}
