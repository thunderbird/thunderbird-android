package app.k9mail.autodiscovery.api

/**
 * Performs a mail server settings lookup.
 *
 * This is an abstraction that allows us to run multiple lookups in parallel.
 */
fun interface AutoDiscoveryRunnable {
    suspend fun run(): AutoDiscoveryResult
}
