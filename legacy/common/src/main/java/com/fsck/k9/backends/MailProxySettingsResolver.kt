package com.fsck.k9.backends

import com.fsck.k9.mail.MailProxySettings
import com.fsck.k9.mail.MailProxyType
import com.fsck.k9.mail.ServerSettings
import net.thunderbird.core.preference.network.NetworkProxyType
import net.thunderbird.core.preference.network.NetworkSettings

internal fun ServerSettings.resolveInheritedProxySettings(networkSettings: NetworkSettings): ServerSettings {
    val accountProxySettings = MailProxySettings.fromServerSettings(this)
    if (accountProxySettings.type != MailProxyType.USE_GLOBAL) return this

    return copy(extra = extra + networkSettings.toMailProxySettings().toExtra())
}

private fun NetworkSettings.toMailProxySettings(): MailProxySettings {
    return if (
        !isProxyEnabled ||
        proxyType == NetworkProxyType.NONE ||
        proxyHost.isBlank() ||
        proxyPort !in VALID_PORT_RANGE
    ) {
        MailProxySettings.NONE
    } else {
        MailProxySettings(
            type = proxyType.toMailProxyType(),
            host = proxyHost,
            port = proxyPort,
            proxyDns = proxyDns,
            username = proxyUsername.ifBlank { null },
            password = proxyPassword.ifBlank { null },
        )
    }
}

private fun NetworkProxyType.toMailProxyType(): MailProxyType {
    return when (this) {
        NetworkProxyType.NONE -> MailProxyType.NONE
        NetworkProxyType.HTTP -> MailProxyType.HTTP
        NetworkProxyType.SOCKS4 -> MailProxyType.SOCKS4
        NetworkProxyType.SOCKS5 -> MailProxyType.SOCKS5
    }
}

private val VALID_PORT_RANGE = 1..65535
