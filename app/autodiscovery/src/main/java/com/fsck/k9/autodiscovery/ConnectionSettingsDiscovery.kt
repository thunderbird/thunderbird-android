package com.fsck.k9.autodiscovery

interface ConnectionSettingsDiscovery {
    fun discover(email: String): ConnectionSettings?
}
