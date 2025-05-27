package app.k9mail.autodiscovery.api

import net.thunderbird.core.common.mail.EmailAddress

/**
 * Tries to find mail server settings for a given email address.
 */
interface AutoDiscoveryService {
    suspend fun discover(email: EmailAddress): AutoDiscoveryResult
}
