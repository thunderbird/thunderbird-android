package net.thunderbird.core.preference.network

import net.thunderbird.core.preference.BackgroundOps

val NETWORK_SETTINGS_DEFAULT_BACKGROUND_OPS = BackgroundOps.ALWAYS
const val NETWORK_SETTINGS_DEFAULT_IS_PROXY_ENABLED = false
val NETWORK_SETTINGS_DEFAULT_PROXY_TYPE = NetworkProxyType.NONE
const val NETWORK_SETTINGS_DEFAULT_PROXY_HOST = ""
const val NETWORK_SETTINGS_DEFAULT_PROXY_PORT = 0
const val NETWORK_SETTINGS_DEFAULT_PROXY_DNS = true
const val NETWORK_SETTINGS_DEFAULT_PROXY_USERNAME = ""
const val NETWORK_SETTINGS_DEFAULT_PROXY_PASSWORD = ""

enum class NetworkProxyType {
    NONE,
    HTTP,
    SOCKS4,
    SOCKS5,
}

data class NetworkSettings(
    val backgroundOps: BackgroundOps = NETWORK_SETTINGS_DEFAULT_BACKGROUND_OPS,
    val isProxyEnabled: Boolean = NETWORK_SETTINGS_DEFAULT_IS_PROXY_ENABLED,
    val proxyType: NetworkProxyType = NETWORK_SETTINGS_DEFAULT_PROXY_TYPE,
    val proxyHost: String = NETWORK_SETTINGS_DEFAULT_PROXY_HOST,
    val proxyPort: Int = NETWORK_SETTINGS_DEFAULT_PROXY_PORT,
    val proxyDns: Boolean = NETWORK_SETTINGS_DEFAULT_PROXY_DNS,
    val proxyUsername: String = NETWORK_SETTINGS_DEFAULT_PROXY_USERNAME,
    val proxyPassword: String = NETWORK_SETTINGS_DEFAULT_PROXY_PASSWORD,
)
