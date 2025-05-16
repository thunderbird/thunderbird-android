package app.k9mail.feature.account.server.certificate.ui

import net.thunderbird.core.common.net.HostNameUtils

/**
 * Format a hostname or IP address for display.
 *
 * Inserts zero width space (U+200B) after components separators to decrease the chance of long lines being displayed
 * with a line break in the middle of a component (DNS label or number component of an IP address).
 */
internal fun interface ServerNameFormatter {
    fun format(hostname: String): String
}

internal class DefaultServerNameFormatter : ServerNameFormatter {
    override fun format(hostname: String): String {
        val address = HostNameUtils.isLegalIPv6Address(hostname)
        return if (address != null) {
            formatIPv6Address(address)
        } else {
            formatDotName(hostname)
        }
    }

    private fun formatIPv6Address(address: String): String {
        return address.replace(":", ":\u200B")
    }

    private fun formatDotName(hostname: String): String {
        return hostname.replace(".", ".\u200B")
    }
}
