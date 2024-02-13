package app.k9mail.autodiscovery.api

interface AutoDiscoveryRegistry {
    fun getAutoDiscoveries(): List<AutoDiscovery>
}
