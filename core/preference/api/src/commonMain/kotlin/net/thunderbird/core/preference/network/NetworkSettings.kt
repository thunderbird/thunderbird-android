package net.thunderbird.core.preference.network

import net.thunderbird.core.preference.BackgroundOps

val NETWORK_SETTINGS_DEFAULT_BACKGROUND_OPS = BackgroundOps.ALWAYS
const val NETWORK_SETTINGS_DEFAULT_IS_CHECK_MAIL_ON_APP_START = false

data class NetworkSettings(
    val backgroundOps: BackgroundOps = NETWORK_SETTINGS_DEFAULT_BACKGROUND_OPS,
    val isCheckMailOnAppStart: Boolean = NETWORK_SETTINGS_DEFAULT_IS_CHECK_MAIL_ON_APP_START,
)
