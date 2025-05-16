package net.thunderbird.core.common.net

/**
 * Represents a hostname, IPv4, or IPv6 address.
 */
@JvmInline
value class Hostname(val value: String) {
    init {
        requireNotNull(HostNameUtils.isLegalHostNameOrIP(value)) { "Not a valid domain or IP: '$value'" }
    }
}

fun String.toHostname() = Hostname(this)

fun Hostname.isIpAddress(): Boolean = HostNameUtils.isLegalIPAddress(value) != null
