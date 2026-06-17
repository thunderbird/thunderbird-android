package com.fsck.k9.mail

private const val KEY_PROXY_TYPE = "proxy.type"
private const val KEY_PROXY_HOST = "proxy.host"
private const val KEY_PROXY_PORT = "proxy.port"
private const val KEY_PROXY_DNS = "proxy.dns"
private const val KEY_PROXY_USERNAME = "proxy.username"
private const val KEY_PROXY_PASSWORD = "proxy.password"

enum class MailProxyType {
    USE_GLOBAL,
    NONE,
    HTTP,
    SOCKS4,
    SOCKS5,
}

data class MailProxySettings @JvmOverloads constructor(
    val type: MailProxyType,
    val host: String? = null,
    val port: Int = 0,
    val proxyDns: Boolean = true,
    val username: String? = null,
    val password: String? = null,
) {
    init {
        if (type == MailProxyType.NONE || type == MailProxyType.USE_GLOBAL) {
            require(host.isNullOrBlank()) { "host must be blank when proxy is disabled" }
            require(port == 0) { "port must be 0 when proxy is disabled" }
            require(username.isNullOrBlank()) { "username must be blank when proxy is disabled" }
            require(password.isNullOrBlank()) { "password must be blank when proxy is disabled" }
        } else {
            require(!host.isNullOrBlank()) { "host must not be blank when proxy is enabled" }
            require(host.contains(LINE_BREAK).not()) { "host must not contain line break" }
            require(port in VALID_PORT_RANGE) { "port must be in range 1..65535" }
            require(username?.contains(LINE_BREAK) != true) { "username must not contain line break" }
            require(password?.contains(LINE_BREAK) != true) { "password must not contain line break" }
        }
    }

    fun toExtra(): Map<String, String?> {
        return if (type == MailProxyType.NONE || type == MailProxyType.USE_GLOBAL) {
            mapOf(KEY_PROXY_TYPE to type.name.lowercase())
        } else {
            mapOf(
                KEY_PROXY_TYPE to type.name.lowercase(),
                KEY_PROXY_HOST to host,
                KEY_PROXY_PORT to port.toString(),
                KEY_PROXY_DNS to proxyDns.toString(),
                KEY_PROXY_USERNAME to username,
                KEY_PROXY_PASSWORD to password,
            )
        }
    }

    companion object {
        @JvmField
        val NONE = MailProxySettings(MailProxyType.NONE)

        @JvmField
        val USE_GLOBAL = MailProxySettings(MailProxyType.USE_GLOBAL)

        private val LINE_BREAK = "[\\r\\n]".toRegex()
        private val VALID_PORT_RANGE = 1..65535

        @JvmStatic
        fun fromServerSettings(serverSettings: ServerSettings): MailProxySettings {
            return fromExtra(serverSettings.extra)
        }

        @JvmStatic
        fun fromExtra(extra: Map<String, String?>): MailProxySettings {
            val type = parseProxyType(extra[KEY_PROXY_TYPE])

            when (type) {
                MailProxyType.USE_GLOBAL -> return USE_GLOBAL
                MailProxyType.NONE -> return NONE
                else -> Unit
            }

            val host = extra[KEY_PROXY_HOST]
            val port = extra[KEY_PROXY_PORT]?.toIntOrNull() ?: 0
            val proxyDns = extra[KEY_PROXY_DNS]?.toBooleanStrictOrNull() ?: true
            val username = extra[KEY_PROXY_USERNAME]
            val password = extra[KEY_PROXY_PASSWORD]

            return MailProxySettings(type, host, port, proxyDns, username, password)
        }

        private fun parseProxyType(value: String?): MailProxyType {
            return when (value?.lowercase()) {
                null, "", "use_global" -> MailProxyType.USE_GLOBAL
                "none" -> MailProxyType.NONE
                "http" -> MailProxyType.HTTP
                "socks", "socks5" -> MailProxyType.SOCKS5
                "socks4" -> MailProxyType.SOCKS4
                else -> MailProxyType.valueOf(value.uppercase())
            }
        }
    }
}
