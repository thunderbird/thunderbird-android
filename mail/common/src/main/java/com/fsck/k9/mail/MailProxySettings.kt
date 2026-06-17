package com.fsck.k9.mail

private const val KEY_PROXY_TYPE = "proxy.type"
private const val KEY_PROXY_HOST = "proxy.host"
private const val KEY_PROXY_PORT = "proxy.port"
private const val KEY_PROXY_DNS = "proxy.dns"

enum class MailProxyType {
    NONE,
    HTTP,
    SOCKS,
}

data class MailProxySettings @JvmOverloads constructor(
    val type: MailProxyType,
    val host: String? = null,
    val port: Int = 0,
    val proxyDns: Boolean = true,
) {
    init {
        if (type == MailProxyType.NONE) {
            require(host.isNullOrBlank()) { "host must be blank when proxy is disabled" }
            require(port == 0) { "port must be 0 when proxy is disabled" }
        } else {
            require(!host.isNullOrBlank()) { "host must not be blank when proxy is enabled" }
            require(host.contains(LINE_BREAK).not()) { "host must not contain line break" }
            require(port in VALID_PORT_RANGE) { "port must be in range 1..65535" }
        }
    }

    fun toExtra(): Map<String, String?> {
        return if (type == MailProxyType.NONE) {
            mapOf(KEY_PROXY_TYPE to MailProxyType.NONE.name.lowercase())
        } else {
            mapOf(
                KEY_PROXY_TYPE to type.name.lowercase(),
                KEY_PROXY_HOST to host,
                KEY_PROXY_PORT to port.toString(),
                KEY_PROXY_DNS to proxyDns.toString(),
            )
        }
    }

    companion object {
        @JvmField
        val NONE = MailProxySettings(MailProxyType.NONE)

        private val LINE_BREAK = "[\\r\\n]".toRegex()
        private val VALID_PORT_RANGE = 1..65535

        @JvmStatic
        fun fromServerSettings(serverSettings: ServerSettings): MailProxySettings {
            return fromExtra(serverSettings.extra)
        }

        @JvmStatic
        fun fromExtra(extra: Map<String, String?>): MailProxySettings {
            val type = extra[KEY_PROXY_TYPE]?.let { proxyType ->
                MailProxyType.valueOf(proxyType.uppercase())
            } ?: MailProxyType.NONE

            if (type == MailProxyType.NONE) {
                return NONE
            }

            val host = extra[KEY_PROXY_HOST]
            val port = extra[KEY_PROXY_PORT]?.toIntOrNull() ?: 0
            val proxyDns = extra[KEY_PROXY_DNS]?.toBooleanStrictOrNull() ?: true

            return MailProxySettings(type, host, port, proxyDns)
        }
    }
}
