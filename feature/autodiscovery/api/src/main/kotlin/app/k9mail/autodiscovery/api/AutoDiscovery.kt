package app.k9mail.autodiscovery.api

import app.k9mail.core.common.mail.EmailAddress

/**
 * Provides a mechanism to find mail server settings for a given email address.
 */
interface AutoDiscovery {
    /**
     * Returns a list of [AutoDiscoveryRunnable]s that perform the actual mail server settings discovery.
     */
    fun initDiscovery(email: EmailAddress): List<AutoDiscoveryRunnable>
}
