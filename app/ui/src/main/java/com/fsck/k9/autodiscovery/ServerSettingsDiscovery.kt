package com.fsck.k9.autodiscovery

import timber.log.Timber

class ServerSettingsDiscovery(private val discoveries: Array<ConnectionSettingsDiscovery>) {

    fun discover(email: String): ConnectionSettings? {
        discoveries.forEach {
            try {
                val settings = it.discover(email)
                if (settings != null) {
                    Timber.i("Discovered settings for %s using provider %s: %s", email, it, settings)
                    return settings
                }
            } catch (e: Exception) {
                Timber.e(e, "Autodiscovery error for %s using provider %s", email, it)
            }
        }
        return null
    }
}
