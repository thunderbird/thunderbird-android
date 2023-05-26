package app.k9mail.cli.autodiscovery

import app.k9mail.autodiscovery.api.AutoDiscoveryResult
import app.k9mail.autodiscovery.api.AutoDiscoveryRunnable

/**
 * Run a list of [AutoDiscoveryRunnable] one after the other until one returns a non-`null` result.
 */
class SerialRunner(private val runnables: List<AutoDiscoveryRunnable>) {
    suspend fun run(): AutoDiscoveryResult? {
        for (runnable in runnables) {
            val discoveryResult = runnable.run()
            if (discoveryResult != null) {
                return discoveryResult
            }
        }

        return null
    }
}
